package com.stockbroker.controller;

import com.stockbroker.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/brokers")
@CrossOrigin(origins = "*")
public class BrokerController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    /**
     * S'abonner à un symbole
     */
    @PostMapping("/{brokerId}/subscribe/{stockSymbol}")
    public ResponseEntity<String> subscribe(
            @PathVariable String brokerId,
            @PathVariable String stockSymbol) {
        
        subscriptionService.subscribe(brokerId, stockSymbol);
        return ResponseEntity.ok("✅ Broker '" + brokerId + 
                               "' abonné à '" + stockSymbol + "'");
    }
    
    /**
     * Se désabonner d'un symbole
     */
    @DeleteMapping("/{brokerId}/unsubscribe/{stockSymbol}")
    public ResponseEntity<String> unsubscribe(
            @PathVariable String brokerId,
            @PathVariable String stockSymbol) {
        
        subscriptionService.unsubscribe(brokerId, stockSymbol);
        return ResponseEntity.ok("✅ Broker '" + brokerId + 
                               "' désabonné de '" + stockSymbol + "'");
    }
    
    /**
     * Obtenir les abonnements d'un broker
     */
    @GetMapping("/{brokerId}/subscriptions")
    public ResponseEntity<Set<String>> getSubscriptions(
            @PathVariable String brokerId) {
        
        Set<String> subscriptions = subscriptionService.getSubscriptionsForBroker(brokerId);
        return ResponseEntity.ok(subscriptions);
    }
    
    /**
     * Obtenir tous les abonnements (admin)
     */
    @GetMapping("/subscriptions/all")
    public ResponseEntity<Map<String, Set<String>>> getAllSubscriptions() {
        Map<String, Set<String>> allSubscriptions = subscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(allSubscriptions);
    }
    
    /**
     * Vérifier un abonnement
     */
    @GetMapping("/{brokerId}/is-subscribed/{stockSymbol}")
    public ResponseEntity<Boolean> isSubscribed(
            @PathVariable String brokerId,
            @PathVariable String stockSymbol) {
        
        boolean isSubscribed = subscriptionService.isSubscribed(brokerId, stockSymbol);
        return ResponseEntity.ok(isSubscribed);
    }
}