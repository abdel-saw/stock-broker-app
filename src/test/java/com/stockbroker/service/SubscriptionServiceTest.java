package com.stockbroker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionServiceTest {
    @Test
    void normalizesEveryOperationAndDeduplicatesSubscriptions() {
        var service = new SubscriptionService();
        service.subscribe(" broker-1 ", " aapl ");
        service.subscribe("broker-1", "AAPL");
        assertEquals(Set.of("AAPL"), service.getSubscriptionsForBroker(" broker-1 "));
        assertEquals(Set.of("broker-1"), service.getSubscribersForStock(" aapl "));
        assertTrue(service.isSubscribed(" broker-1 ", " aapl "));
        service.unsubscribe(" broker-1 ", " aapl ");
        assertTrue(service.getAllSubscriptions().isEmpty());
        assertTrue(service.getSubscribersForStock("AAPL").isEmpty());
    }

    @Test
    void removingOneBrokerRetainsTheOtherSubscriber() {
        var service = new SubscriptionService();
        service.subscribe("one", "AAPL");
        service.subscribe("two", "AAPL");
        service.unsubscribe("one", "AAPL");
        service.unsubscribe("missing", "MSFT");
        assertEquals(Set.of("two"), service.getSubscribersForStock("AAPL"));
        assertEquals(Set.of("AAPL"), service.getSubscriptionsForBroker("two"));
        assertFalse(service.isSubscribed("one", "AAPL"));
    }

    @Test
    void returnedCollectionsCannotMutateLiveIndexesAndRemainSnapshots() {
        var service = new SubscriptionService();
        service.subscribe("one", "AAPL");
        var brokers = service.getSubscribersForStock("AAPL");
        var symbols = service.getSubscriptionsForBroker("one");
        var all = service.getAllSubscriptions();
        assertThrows(UnsupportedOperationException.class, () -> brokers.clear());
        assertThrows(UnsupportedOperationException.class, () -> symbols.clear());
        assertThrows(UnsupportedOperationException.class, () -> all.clear());
        assertThrows(UnsupportedOperationException.class, () -> all.get("one").clear());
        service.unsubscribe("one", "AAPL");
        assertEquals(Set.of("one"), brokers);
        assertEquals(Set.of("AAPL"), all.get("one"));
        assertTrue(service.getAllSubscriptions().isEmpty());
    }

    @Test
    void rejectsBlankInputWithoutWritingEitherIndex() {
        var service = new SubscriptionService();
        assertThrows(IllegalArgumentException.class, () -> service.subscribe(null, "AAPL"));
        assertThrows(IllegalArgumentException.class, () -> service.subscribe("one", " "));
        assertThrows(IllegalArgumentException.class, () -> service.unsubscribe("", "AAPL"));
        assertTrue(service.getAllSubscriptions().isEmpty());
        assertTrue(service.getSubscribersForStock("AAPL").isEmpty());
    }

    @Test
    void symbolsAreIndependentOfTheServerLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var service = new SubscriptionService();
            service.subscribe("one", "intc");
            assertEquals(Set.of("INTC"), service.getSubscriptionsForBroker("one"));
            assertTrue(service.isSubscribed("one", "intc"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @Timeout(15)
    void concurrentSubscribeAndUnsubscribeKeepBothIndexesConsistent() throws Exception {
        var service = new SubscriptionService();
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int id = 0; id < 8; id++) {
                String broker = "broker-" + id;
                tasks.add(() -> {
                    for (int i = 0; i < 1000; i++) {
                        service.subscribe(broker, "AAPL");
                        assertTrue(service.isSubscribed(broker, "AAPL"));
                        assertTrue(service.getSubscribersForStock("AAPL").contains(broker));
                        service.unsubscribe(broker, "AAPL");
                    }
                    return null;
                });
            }
            for (Future<Void> result : executor.invokeAll(tasks)) {
                result.get();
            }
            assertTrue(service.getAllSubscriptions().isEmpty());
            assertTrue(service.getSubscribersForStock("AAPL").isEmpty());
        } finally {
            executor.shutdownNow();
        }
    }
}
