import { apiClient } from './apiClient';

export const cartApi = {
  async getCart() {
    return await apiClient.get('/api/v1/cart');
  },

  async addItem(productId, quantity = 1) {
    return await apiClient.post('/api/v1/cart/items', {
      productId,
      quantity,
    });
  },

  async updateQuantity(productId, quantity) {
    return await apiClient.put(`/api/v1/cart/items/${productId}`, {
      quantity,
    });
  },

  async removeItem(productId) {
    return await apiClient.delete(`/api/v1/cart/items/${productId}`);
  },

  async clearCart() {
    return await apiClient.delete('/api/v1/cart');
  }
};
