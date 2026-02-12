package com.stockbroker.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    
    /**
     * Endpoint pour recevoir des messages des clients
     * Format: /app/message
     */
    @MessageMapping("/message")
    @SendTo("/topic/messages")
    public String handleMessage(String message) {
        return "Message reçu: " + message + " à " + java.time.LocalTime.now();
    }
}