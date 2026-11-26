package com.stockbroker.controller;

import com.stockbroker.model.StockPrice;
import com.stockbroker.service.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class StockControllerTest {
    private StockController controller;
    private KafkaProducerService producer;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        controller = new StockController();
        producer = mock(KafkaProducerService.class);
        ReflectionTestUtils.setField(controller, "producerService", producer);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void enableLocalPublishing() {
        ReflectionTestUtils.setField(controller, "manualPublishingEnabled", true);
    }

    @Test
    void bothPublishRoutesAreDisabledByDefault() throws Exception {
        mvc.perform(post("/api/stocks/publish/simple").param("symbol", "AAPL").param("price", "10"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/stocks/publish").contentType("application/json")
                .content("{\"symbol\":\"AAPL\",\"currentPrice\":10}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(producer);
    }

    @Test
    void processCheckRemainsAvailableWithoutKafka() throws Exception {
        mvc.perform(get("/api/stocks/test")).andExpect(status().isOk());
        verifyNoInteractions(producer);
    }

    @Test
    void invalidSymbolsAndNonfiniteOrNegativePricesNeverReachKafka() throws Exception {
        enableLocalPublishing();
        for (String price : new String[]{"NaN", "Infinity", "0", "-1"}) {
            mvc.perform(post("/api/stocks/publish/simple").param("symbol", "AAPL").param("price", price))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/stocks/publish/simple").param("symbol", "<img>").param("price", "10"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/stocks/publish").contentType("application/json")
                .content("{\"symbol\":\"AAPL\",\"currentPrice\":10,\"volume\":-1}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(producer);
    }

    @Test
    void successWaitsForKafkaAcknowledgementAndNormalizesSymbol() throws Exception {
        enableLocalPublishing();
        CompletableFuture<SendResult<String, StockPrice>> acknowledgement = new CompletableFuture<>();
        when(producer.publishStockPrice(any())).thenReturn(acknowledgement);
        var result = mvc.perform(post("/api/stocks/publish/simple")
                .param("symbol", " aapl ").param("price", "10"))
                .andExpect(request().asyncStarted()).andReturn();
        assertFalse(acknowledgement.isDone());
        verify(producer).publishStockPrice(argThat(stock -> stock.getSymbol().equals("AAPL")));
        acknowledgement.complete(null);
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().string("Published to Kafka: AAPL"));
    }

    @Test
    void asynchronousKafkaFailureReturnsUnavailable() throws Exception {
        enableLocalPublishing();
        when(producer.publishStockPrice(any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        var result = mvc.perform(post("/api/stocks/publish/simple")
                .param("symbol", "AAPL").param("price", "10"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Kafka did not acknowledge publication"));
    }

    @Test
    void synchronousKafkaFailureAlsoReturnsUnavailable() throws Exception {
        enableLocalPublishing();
        when(producer.publishStockPrice(any())).thenThrow(new IllegalStateException("broker unavailable"));
        var result = mvc.perform(post("/api/stocks/publish/simple")
                .param("symbol", "AAPL").param("price", "10"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isServiceUnavailable());
    }
}
