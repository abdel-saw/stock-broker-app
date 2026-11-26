package com.stockbroker.controller;

import com.stockbroker.model.StockPrice;
import com.stockbroker.service.KafkaProducerService;
import com.stockbroker.validation.DemoInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    @Autowired
    private KafkaProducerService producerService;

    @Value("${app.manual-publishing.enabled:false}")
    private boolean manualPublishingEnabled;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalidPrice(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(error.getMessage());
    }

    @PostMapping("/publish")
    public CompletableFuture<ResponseEntity<String>> publishStockPrice(@RequestBody StockPrice stockPrice) {
        return publish(stockPrice);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Stock Broker API is running! " + LocalDateTime.now());
    }

    @PostMapping("/publish/simple")
    public CompletableFuture<ResponseEntity<String>> publishSimple(
            @RequestParam String symbol, @RequestParam double price) {
        return publish(new StockPrice(symbol, price));
    }

    private CompletableFuture<ResponseEntity<String>> publish(StockPrice stock) {
        if (!manualPublishingEnabled) {
            throw new ResponseStatusException(FORBIDDEN, "Manual publishing is disabled");
        }
        DemoInput.price(stock);
        try {
            return producerService.publishStockPrice(stock)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .handle((result, failure) -> failure == null
                            ? ResponseEntity.ok("Published to Kafka: " + stock.getSymbol())
                            : ResponseEntity.status(503).body("Kafka did not acknowledge publication"));
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(503).body("Kafka did not acknowledge publication"));
        }
    }
}
