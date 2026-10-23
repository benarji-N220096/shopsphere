package com.shopsphere.cart.service;

import com.shopsphere.cart.client.ProductClient;
import com.shopsphere.cart.config.CurrentUserProvider;
import com.shopsphere.cart.dto.AddCartItemRequest;
import com.shopsphere.cart.dto.CartItemResponse;
import com.shopsphere.cart.dto.CartResponse;
import com.shopsphere.cart.dto.ProductDto;
import com.shopsphere.cart.dto.UpdateCartItemRequest;
import com.shopsphere.cart.entity.Cart;
import com.shopsphere.cart.entity.CartItem;
import com.shopsphere.cart.exception.CartItemNotFoundException;
import com.shopsphere.cart.exception.CartNotFoundException;
import com.shopsphere.cart.exception.ProductNotFoundException;
import com.shopsphere.cart.repository.CartItemRepository;
import com.shopsphere.cart.repository.CartRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final CurrentUserProvider currentUserProvider;
    private final com.shopsphere.cart.metrics.CartMetrics cartMetrics;

    public CartService(CartRepository cartRepository, 
                       CartItemRepository cartItemRepository, 
                       ProductClient productClient,
                       CurrentUserProvider currentUserProvider) {
        this(cartRepository, cartItemRepository, productClient, currentUserProvider, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CartService(CartRepository cartRepository, 
                       CartItemRepository cartItemRepository, 
                       ProductClient productClient,
                       CurrentUserProvider currentUserProvider,
                       com.shopsphere.cart.metrics.CartMetrics cartMetrics) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productClient = productClient;
        this.currentUserProvider = currentUserProvider;
        this.cartMetrics = cartMetrics;
    }

    @Transactional
    public CartResponse getCart() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return getCartForUser(userId);
    }

    @Transactional
    public CartResponse getCartForUser(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse addCartItem(AddCartItemRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        
        // 1. Validate Product exists in Product Service
        try {
            ProductDto product = productClient.getProductById(request.getProductId());
            if (product == null) {
                throw new ProductNotFoundException("Product not found with ID: " + request.getProductId());
            }
        } catch (FeignException.NotFound e) {
            throw new ProductNotFoundException("Product not found with ID: " + request.getProductId());
        }

        // 2. Get or create Cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));

        // 3. Add or update CartItem
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId());
        
        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            // No need to explicitly save, as it's dirty in the transaction
        } else {
            CartItem newItem = new CartItem();
            newItem.setId(UUID.randomUUID());
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setQuantity(request.getQuantity());
            newItem.setCreatedAt(LocalDateTime.now());
            cart.getItems().add(newItem);
            // No need to call cartItemRepository.save(newItem) due to CascadeType.ALL
        }

        if (cartMetrics != null) {
            cartMetrics.incrementItemsAdded(request.getQuantity());
        }

        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItemQuantity(UUID productId, UpdateCartItemRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Cart cart = getExistingCart(userId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new CartItemNotFoundException("Item not found in cart for product ID: " + productId));

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return mapToCartResponse(cart);
    }

    @Transactional
    public void removeCartItem(UUID productId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Cart cart = getExistingCart(userId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new CartItemNotFoundException("Item not found in cart for product ID: " + productId));

        cartItemRepository.delete(item);
        cart.getItems().remove(item);
    }

    @Transactional
    public void clearCart() {
        UUID userId = currentUserProvider.getCurrentUserId();
        clearCartForUser(userId);
    }

    @Transactional
    public void clearCartForUser(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
            cart.getItems().clear();
            if (cartMetrics != null) {
                cartMetrics.incrementCartCleared();
            }
        });
    }

    private Cart getExistingCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user ID: " + userId));
    }

    private Cart createCart(UUID userId) {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        cart.setCreatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    private CartResponse mapToCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setUserId(cart.getUserId());
        response.setCreatedAt(cart.getCreatedAt());
        response.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    CartItemResponse itemResponse = new CartItemResponse();
                    itemResponse.setId(item.getId());
                    itemResponse.setProductId(item.getProductId());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setCreatedAt(item.getCreatedAt());
                    itemResponse.setUpdatedAt(item.getUpdatedAt());
                    return itemResponse;
                }).collect(Collectors.toList());
        
        response.setItems(itemResponses);
        return response;
    }
}
