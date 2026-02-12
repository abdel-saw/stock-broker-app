package com.stockbroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockBrokerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(StockBrokerApplication.class, args);
        System.out.println("""
            ========================================
            🚀 Application Courtier Boursier Démarrée!
            ========================================
            Port: 8080
            Kafka: localhost:9092
            WebSocket: ws://localhost:8080/ws
            API: http://localhost:8080/api/stocks/test
            
            Endpoints disponibles:
            - GET  /api/stocks/test
            - POST /api/stocks/publish
            - POST /api/brokers/{id}/subscribe/{symbol}
            - DELETE /api/brokers/{id}/unsubscribe/{symbol}
            - GET  /api/brokers/{id}/subscriptions
            
            Interface web: http://localhost:8080
            ========================================
            """);
    }
}

