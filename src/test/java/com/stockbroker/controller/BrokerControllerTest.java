package com.stockbroker.controller;

import com.stockbroker.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BrokerControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var controller = new BrokerController();
        ReflectionTestUtils.setField(controller, "subscriptionService", new SubscriptionService());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void subscribeLookupAndUnsubscribeWorkThroughHttp() throws Exception {
        mvc.perform(post("/api/brokers/demo/subscribe/aapl")).andExpect(status().isOk());
        mvc.perform(get("/api/brokers/demo/subscriptions"))
                .andExpect(status().isOk()).andExpect(content().json("[\"AAPL\"]"));
        mvc.perform(delete("/api/brokers/demo/unsubscribe/AAPL")).andExpect(status().isOk());
        mvc.perform(get("/api/brokers/demo/is-subscribed/aapl"))
                .andExpect(status().isOk()).andExpect(content().string("false"));
    }

    @Test
    void blankSymbolReturnsBadRequestAndDoesNotCreateASubscription() throws Exception {
        mvc.perform(post("/api/brokers/{id}/subscribe/{symbol}", "demo", " "))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/brokers/demo/subscriptions"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
