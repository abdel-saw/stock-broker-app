package com.stockbroker.service;

import com.stockbroker.model.StockPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumerService {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    
    @KafkaListener(topics = "stock-prices", groupId = "broker-group")
    public void consumeStockPrice(StockPrice stockPrice) {
        log.info("📥 Reçu: {} - ${}", stockPrice.getSymbol(), stockPrice.getCurrentPrice());
        
        String stockSymbol = stockPrice.getSymbol();
        
        
        var subscribers = subscriptionService.getSubscribersForStock(stockSymbol);
        
        for (String brokerId : subscribers) {
            String destination = "/topic/stocks/" + brokerId;
            messagingTemplate.convertAndSend(destination, stockPrice);
            log.debug("   → Envoyé à broker: {}", brokerId);
        }
        
       
        messagingTemplate.convertAndSend("/topic/stocks/public", stockPrice);
        
        messagingTemplate.convertAndSend("/topic/stocks/symbol/" + stockSymbol, stockPrice);
    }
}