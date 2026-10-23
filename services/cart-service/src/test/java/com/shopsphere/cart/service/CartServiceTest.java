package com.shopsphere.cart.service;

import com.shopsphere.cart.client.ProductClient;
import com.shopsphere.cart.config.CurrentUserProvider;
import com.shopsphere.cart.dto.AddCartItemRequest;
import com.shopsphere.cart.dto.CartResponse;
import com.shopsphere.cart.dto.ProductDto;
import com.shopsphere.cart.entity.Cart;
import com.shopsphere.cart.exception.ProductNotFoundException;
import com.shopsphere.cart.repository.CartItemRepository;
import com.shopsphere.cart.repository.CartRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CartService cartService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCart_createsNewIfNotFound() {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        Cart newCart = new Cart();
        newCart.setId(UUID.randomUUID());
        newCart.setUserId(userId);
        newCart.setCreatedAt(LocalDateTime.now());
        
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        CartResponse response = cartService.getCart();

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testAddCartItem_productNotFound() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        FeignException.NotFound feignNotFound = mock(FeignException.NotFound.class);
        when(feignNotFound.status()).thenReturn(404);
        when(productClient.getProductById(productId)).thenThrow(feignNotFound);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(productId);
        request.setQuantity(1);

        assertThrows(ProductNotFoundException.class, () -> cartService.addCartItem(request));
    }

    @Test
    void testAddCartItem_success() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        ProductDto productDto = new ProductDto();
        productDto.setId(productId);
        when(productClient.getProductById(productId)).thenReturn(productDto);

        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        cart.setCreatedAt(LocalDateTime.now());

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)).thenReturn(Optional.empty());

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(productId);
        request.setQuantity(2);

        CartResponse response = cartService.addCartItem(request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getItems().get(0).getQuantity());
        // New items are added to cart.getItems() and persisted via CascadeType.ALL — no explicit save
        assertEquals(1, cart.getItems().size());
        assertEquals(productId, cart.getItems().get(0).getProductId());
    }
}
