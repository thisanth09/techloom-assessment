package com.techloom.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techloom.pos.dto.ProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pos_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndFetchProducts() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setCategory("Electronics");
        request.setName("Laptop");
        request.setPrice(250000.0);
        request.setStock(10);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(250000));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void shouldCreateOrderAndReserveInventory() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setCategory("Electronics");
        request.setName("Monitor");
        request.setPrice(45000.0);
        request.setStock(8);

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        String checkoutPayload = String.format("""
            {
              "customerName": "Alice",
              "items": [
                {
                  "productId": %d,
                  "quantity": 2
                }
              ]
            }
            """, productId);

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.totalAmount").value(90000.0))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(6));
    }

    @Test
    void shouldReleaseReservationWhenOrderIsCancelled() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setCategory("Electronics");
        request.setName("Tablet");
        request.setPrice(12000.0);
        request.setStock(5);

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        String checkoutPayload = String.format("""
            {
              "customerName": "Bob",
              "items": [
                {
                  "productId": %d,
                  "quantity": 2
                }
              ]
            }
            """, productId);

        MvcResult orderResult = mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutPayload))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void shouldRejectDuplicatePayment() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setCategory("Office");
        request.setName("Printer");
        request.setPrice(8000.0);
        request.setStock(2);

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        String checkoutPayload = String.format("""
            {
              "customerName": "Charlie",
              "items": [
                {
                  "productId": %d,
                  "quantity": 1
                }
              ]
            }
            """, productId);

        MvcResult orderResult = mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutPayload))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        String paymentPayload = """
            {
              "paymentMethod": "CARD",
              "paymentReference": "pay-123",
              "amount": 8000.0,
              "status": "SUCCESS"
            }
            """;

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentPayload))
                .andExpect(status().isConflict());
    }
}
