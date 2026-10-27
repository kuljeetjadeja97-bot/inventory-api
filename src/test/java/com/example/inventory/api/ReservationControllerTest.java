package com.example.inventory.api;

import com.example.inventory.InventoryApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = InventoryApplication.class)
@AutoConfigureMockMvc
class ReservationControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createAndReadReservation() throws Exception {
        String body = """
                {"customerId":"store-9","lines":{"WIDGET":2}}
                """;

        String response = mvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "http-1")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/v1/reservations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("store-9"));

        mvc.perform(get("/v1/skus/WIDGET/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved").value(2));
    }

    @Test
    void unknownReservationIs404() throws Exception {
        mvc.perform(get("/v1/reservations/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNotFound());
    }
}
