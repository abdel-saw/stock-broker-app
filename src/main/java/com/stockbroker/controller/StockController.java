package com.stockbroker.controller;

import com.stockbroker.model.StockPrice;
import com.stockbroker.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*")
public class StockController {
    
    @Autowired
    private KafkaProducerService producerService;
    
    /**
     * Publier manuellement un cours boursier
     */
    @PostMapping("/publish")
    public ResponseEntity<String> publishStockPrice(@RequestBody StockPrice stockPrice) {
        stockPrice.setTimestamp(LocalDateTime.now());
        producerService.publishStockPrice(stockPrice);
        return ResponseEntity.ok("✅ Cours publié pour: " + stockPrice.getSymbol());
    }
    
    /**
     * API de test
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("🚀 Stock Broker API is running! " + LocalDateTime.now());
    }
    
    /**
     * Publier un cours simplifié
     */
    @PostMapping("/publish/simple")
    public ResponseEntity<String> publishSimple(
            @RequestParam String symbol,
            @RequestParam double price) {
        StockPrice stockPrice = new StockPrice(symbol, price);
        producerService.publishStockPrice(stockPrice);
        return ResponseEntity.ok("✅ Cours simple publié");
    }
}