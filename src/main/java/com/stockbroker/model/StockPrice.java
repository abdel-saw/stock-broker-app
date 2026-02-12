package com.stockbroker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockPrice {
    private String symbol;          
    private double currentPrice;    
    private double change;          
    private double changePercent;   
    private LocalDateTime timestamp;
    private double volume;         
    
    
    public StockPrice(String symbol, double currentPrice) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.timestamp = LocalDateTime.now();
    }
}
