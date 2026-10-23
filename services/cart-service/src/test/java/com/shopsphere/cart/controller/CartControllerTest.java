package com.shopsphere.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.cart.dto.AddCartItemRequest;
import com.shopsphere.cart.dto.CartResponse;
import com.shopsphere.cart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CartController cartController = new CartController(cartService);
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
    }

    @Test
    void testGetCart() throws Exception {
        CartResponse response = new CartResponse();
        when(cartService.getCart()).thenReturn(response);

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk());
    }

    @Test
    void testAddCartItem_valid() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(UUID.randomUUID());
        request.setQuantity(2);

        CartResponse response = new CartResponse();
        when(cartService.addCartItem(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testAddCartItem_invalidQuantity() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(UUID.randomUUID());
        request.setQuantity(0); // Invalid

        // The validation logic might require GlobalExceptionHandler if we want it to return 400.
        // In standaloneSetup without exception handler, it throws an exception which MockMvc wraps as 400.
        mockMvc.perform(post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
