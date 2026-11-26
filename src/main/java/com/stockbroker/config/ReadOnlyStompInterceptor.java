package com.stockbroker.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

/** Browser clients may receive prices but cannot inject messages into any topic. */
public final class ReadOnlyStompInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (StompHeaderAccessor.wrap(message).getCommand() == StompCommand.SEND) {
            throw new MessageDeliveryException("Client publication is disabled");
        }
        return message;
    }
}
