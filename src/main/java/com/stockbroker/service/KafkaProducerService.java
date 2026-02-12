package com.stockbroker.service;

import com.stockbroker.model.StockPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@EnableScheduling
@Slf4j
public class KafkaProducerService {
    
    private static final String TOPIC = "stock-prices";
    private static final String[] STOCK_SYMBOLS = {
        "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", 
        "META", "NVDA", "NFLX", "INTC", "AMD"
    };
    
    private final Random random = new Random();
    
    @Autowired
    private KafkaTemplate<String, StockPrice> kafkaTemplate;
    
    
    public void publishStockPrice(StockPrice stockPrice) {
        stockPrice.setTimestamp(LocalDateTime.now());
        kafkaTemplate.send(TOPIC, stockPrice.getSymbol(), stockPrice);
        log.info("📤 Publié: {} - ${}", stockPrice.getSymbol(), stockPrice.getCurrentPrice());
    }
    
    
    @Scheduled(fixedRate = 2000)
    public void simulateStockPrices() {
        for (String symbol : STOCK_SYMBOLS) {
            double basePrice = getBasePrice(symbol);
            double change = (random.nextDouble() * 8) - 4; 
            double newPrice = Math.max(1, basePrice + change); 
            double changePercent = (change / basePrice) * 100;
            
            StockPrice stock = new StockPrice(
                symbol,
                Math.round(newPrice * 100.0) / 100.0,
                Math.round(change * 100.0) / 100.0,
                Math.round(changePercent * 100.0) / 100.0,
                LocalDateTime.now(),
                random.nextDouble() * 1000000
            );
            
            publishStockPrice(stock);
        }
    }
    
   
    private double getBasePrice(String symbol) {
        return switch (symbol) {
            case "AAPL" -> 170.0;
            case "GOOGL" -> 135.0;
            case "MSFT" -> 330.0;
            case "AMZN" -> 145.0;
            case "TSLA" -> 240.0;
            case "META" -> 320.0;
            case "NVDA" -> 480.0;
            case "NFLX" -> 380.0;
            case "INTC" -> 44.0;
            case "AMD" -> 125.0;
            default -> 100.0;
        };
    }
}