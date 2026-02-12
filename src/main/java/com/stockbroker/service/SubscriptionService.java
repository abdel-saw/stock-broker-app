package com.stockbroker.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubscriptionService {
    
    
    private final Map<String, Set<String>> brokerSubscriptions = new ConcurrentHashMap<>();
    
    
    private final Map<String, Set<String>> stockSubscribers = new ConcurrentHashMap<>();
    
    
    public void subscribe(String brokerId, String stockSymbol) {
       
        brokerId = brokerId.trim();
        stockSymbol = stockSymbol.trim().toUpperCase();
        
        
        brokerSubscriptions.computeIfAbsent(brokerId, k -> ConcurrentHashMap.newKeySet())
                          .add(stockSymbol);
        
        
        stockSubscribers.computeIfAbsent(stockSymbol, k -> ConcurrentHashMap.newKeySet())
                       .add(brokerId);
        
        System.out.println("✅ Broker '" + brokerId + "' abonné à '" + stockSymbol + "'");
        System.out.println("📊 Abonnements actuels: " + brokerSubscriptions);
    }
    
    
    public void unsubscribe(String brokerId, String stockSymbol) {
        brokerId = brokerId.trim();
        stockSymbol = stockSymbol.trim().toUpperCase();
        
       
        if (brokerSubscriptions.containsKey(brokerId)) {
            brokerSubscriptions.get(brokerId).remove(stockSymbol);
            if (brokerSubscriptions.get(brokerId).isEmpty()) {
                brokerSubscriptions.remove(brokerId);
            }
        }
        
        
        if (stockSubscribers.containsKey(stockSymbol)) {
            stockSubscribers.get(stockSymbol).remove(brokerId);
            if (stockSubscribers.get(stockSymbol).isEmpty()) {
                stockSubscribers.remove(stockSymbol);
            }
        }
        
        System.out.println("❌ Broker '" + brokerId + "' désabonné de '" + stockSymbol + "'");
    }
    
    
    public Set<String> getSubscribersForStock(String stockSymbol) {
        return stockSubscribers.getOrDefault(stockSymbol, Collections.emptySet());
    }
    
   
    public Set<String> getSubscriptionsForBroker(String brokerId) {
        return brokerSubscriptions.getOrDefault(brokerId, Collections.emptySet());
    }
    
    
    public Map<String, Set<String>> getAllSubscriptions() {
        return new HashMap<>(brokerSubscriptions);
    }
    
    
    public boolean isSubscribed(String brokerId, String stockSymbol) {
        return brokerSubscriptions.getOrDefault(brokerId, Collections.emptySet())
                                 .contains(stockSymbol.toUpperCase());
    }
}
