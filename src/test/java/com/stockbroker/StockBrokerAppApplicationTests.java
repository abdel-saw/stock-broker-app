package com.stockbroker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "stock-prices",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class StockBrokerAppApplicationTests {
    @Test
    void contextLoadsWithAnEmbeddedBroker() {
    }
}
