import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { cartApi } from '../services/api/cartApi';
import { productApi } from '../services/api/productApi';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [items, setItems] = useState(() => {
    try {
      const saved = localStorage.getItem('shopsphere_guest_cart');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });
  const [isLoading, setIsLoading] = useState(false);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [notification, setNotification] = useState(null);

  useEffect(() => {
    try {
      localStorage.setItem('shopsphere_guest_cart', JSON.stringify(items));
    } catch {
      // ignore
    }
  }, [items]);

  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3500);
  };

  // Sync server cart when user logs in
  const fetchServerCart = useCallback(async () => {
    if (!isAuthenticated) return;
    setIsLoading(true);
    try {
      const cartData = await cartApi.getCart();
      if (cartData && Array.isArray(cartData.items)) {
        // Hydrate items with product metadata if needed
        const hydrated = await Promise.all(
          cartData.items.map(async (item) => {
            try {
              const product = await productApi.getProductById(item.productId);
              return {
                productId: item.productId,
                quantity: item.quantity,
                product,
              };
            } catch {
              return {
                productId: item.productId,
                quantity: item.quantity,
                product: { id: item.productId, name: 'Product Item', price: 0, stockQuantity: 10 },
              };
            }
          })
        );
        setItems(hydrated);
      }
    } catch {
      // Silently fall back to existing items if cart service is empty
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated) {
      fetchServerCart();
    }
  }, [isAuthenticated, fetchServerCart]);

  const addToCart = async (product, quantity = 1) => {
    if (!product || !product.id) return;
    const qty = Math.max(1, parseInt(quantity, 10) || 1);

    // Update local state immediately for responsive UI
    setItems((prev) => {
      const existingIndex = prev.findIndex((i) => i.productId === product.id);
      if (existingIndex > -1) {
        const next = [...prev];
        const newQty = next[existingIndex].quantity + qty;
        next[existingIndex] = {
          ...next[existingIndex],
          quantity: newQty,
          product: product || next[existingIndex].product,
        };
        return next;
      }
      return [...prev, { productId: product.id, quantity: qty, product }];
    });

    setIsDrawerOpen(true);
    showNotification(`Added "${product.name}" to cart`);

    // Sync to backend if authenticated
    if (isAuthenticated) {
      try {
        await cartApi.addItem(product.id, qty);
      } catch (err) {
        console.warn('Backend cart sync warning:', err.message);
      }
    }
  };

  const updateQuantity = async (productId, newQuantity) => {
    const qty = parseInt(newQuantity, 10);
    if (isNaN(qty) || qty <= 0) {
      return removeFromCart(productId);
    }

    setItems((prev) =>
      prev.map((item) =>
        item.productId === productId ? { ...item, quantity: qty } : item
      )
    );

    if (isAuthenticated) {
      try {
        await cartApi.updateQuantity(productId, qty);
      } catch (err) {
        console.warn('Backend cart update warning:', err.message);
      }
    }
  };

  const removeFromCart = async (productId) => {
    setItems((prev) => prev.filter((item) => item.productId !== productId));

    if (isAuthenticated) {
      try {
        await cartApi.removeItem(productId);
      } catch (err) {
        console.warn('Backend cart remove warning:', err.message);
      }
    }
  };

  const clearCart = async () => {
    setItems([]);
    try {
      localStorage.removeItem('shopsphere_guest_cart');
    } catch {}
    if (isAuthenticated) {
      try {
        await cartApi.clearCart();
      } catch (err) {
        console.warn('Backend cart clear warning:', err.message);
      }
    }
  };

  const totalItemCount = items.reduce((acc, curr) => acc + curr.quantity, 0);
  const subtotal = items.reduce(
    (acc, curr) => acc + (parseFloat(curr.product?.price || 0) * curr.quantity),
    0
  );

  const clearLocalCart = () => {
    setItems([]);
    try {
      localStorage.removeItem('shopsphere_guest_cart');
    } catch {}
  };

  const value = {
    items,
    isLoading,
    isDrawerOpen,
    openDrawer: () => setIsDrawerOpen(true),
    closeDrawer: () => setIsDrawerOpen(false),
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    clearLocalCart,
    refreshCart: fetchServerCart,
    totalItemCount,
    subtotal,
    notification,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}
