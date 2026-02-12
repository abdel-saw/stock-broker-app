package com.stockbroker.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    private String brokerId;        
    private String stockSymbol;     
    private LocalDateTime subscribedAt; 
}
