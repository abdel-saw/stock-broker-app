package com.stockbroker.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.ExecutorSubscribableChannel;

import static org.junit.jupiter.api.Assertions.*;

class ReadOnlyStompInterceptorTest {
    @Test
    void blocksBothTopicInjectionAndApplicationSends() {
        var interceptor = new ReadOnlyStompInterceptor();
        for (String target : new String[]{"/topic/stocks/public", "/topic/stocks/demo", "/app/message"}) {
            var headers = StompHeaderAccessor.create(StompCommand.SEND);
            headers.setDestination(target);
            var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            assertThrows(MessageDeliveryException.class,
                    () -> interceptor.preSend(message, new ExecutorSubscribableChannel()));
        }
    }

    @Test
    void permitsReceivingAndConnectionLifecycle() {
        var interceptor = new ReadOnlyStompInterceptor();
        for (StompCommand command : new StompCommand[]{
                StompCommand.CONNECT, StompCommand.SUBSCRIBE, StompCommand.UNSUBSCRIBE, StompCommand.DISCONNECT}) {
            var headers = StompHeaderAccessor.create(command);
            var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            assertSame(message, interceptor.preSend(message, new ExecutorSubscribableChannel()));
        }
    }
}
