package com.stockbroker.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SubscriptionService {
    // One monitor protects both indexes, including reads and snapshots.
    private final Map<String, Set<String>> brokerSubscriptions = new HashMap<>();
    private final Map<String, Set<String>> stockSubscribers = new HashMap<>();

    public synchronized void subscribe(String brokerId, String stockSymbol) {
        brokerId = normalizeBroker(brokerId);
        stockSymbol = normalizeSymbol(stockSymbol);
        brokerSubscriptions.computeIfAbsent(brokerId, key -> new HashSet<>()).add(stockSymbol);
        stockSubscribers.computeIfAbsent(stockSymbol, key -> new HashSet<>()).add(brokerId);
    }

    public synchronized void unsubscribe(String brokerId, String stockSymbol) {
        brokerId = normalizeBroker(brokerId);
        stockSymbol = normalizeSymbol(stockSymbol);
        remove(brokerSubscriptions, brokerId, stockSymbol);
        remove(stockSubscribers, stockSymbol, brokerId);
    }

    public synchronized Set<String> getSubscribersForStock(String stockSymbol) {
        return Set.copyOf(stockSubscribers.getOrDefault(normalizeSymbol(stockSymbol), Set.of()));
    }

    public synchronized Set<String> getSubscriptionsForBroker(String brokerId) {
        return Set.copyOf(brokerSubscriptions.getOrDefault(normalizeBroker(brokerId), Set.of()));
    }

    public synchronized Map<String, Set<String>> getAllSubscriptions() {
        Map<String, Set<String>> snapshot = new HashMap<>();
        brokerSubscriptions.forEach((broker, symbols) -> snapshot.put(broker, Set.copyOf(symbols)));
        return Map.copyOf(snapshot);
    }

    public synchronized boolean isSubscribed(String brokerId, String stockSymbol) {
        return brokerSubscriptions.getOrDefault(normalizeBroker(brokerId), Set.of())
                .contains(normalizeSymbol(stockSymbol));
    }

    private static void remove(Map<String, Set<String>> index, String key, String value) {
        Set<String> values = index.get(key);
        if (values != null) {
            values.remove(value);
            if (values.isEmpty()) {
                index.remove(key);
            }
        }
    }

    private static String normalizeBroker(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Broker ID must not be blank");
        }
        return value.trim();
    }

    private static String normalizeSymbol(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Stock symbol must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
