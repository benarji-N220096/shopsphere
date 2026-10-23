import { apiClient } from './apiClient';

export const orderApi = {
  /**
   * Places an order from the user's active cart.
   * @param {{ paymentMethod: string, mockPaymentAction?: string }} payload
   */
  async createOrder(payload) {
    return await apiClient.post('/api/v1/orders', payload);
  },

  /**
   * Retrieves all orders for the currently authenticated user.
   */
  async getMyOrders() {
    return await apiClient.get('/api/v1/orders');
  },

  /**
   * Admin: Retrieves all orders across the platform.
   */
  async getAllOrders() {
    return await apiClient.get('/api/v1/orders');
  },

  /**
   * Retrieves a specific order by ID.
   * @param {string} orderId
   */
  async getOrderById(orderId) {
    if (!orderId) throw new Error('Order ID is required');
    return await apiClient.get(`/api/v1/orders/${orderId}`);
  },

  /**
   * Admin: Triggers targeted recovery for a specific stuck PENDING order.
   * @param {string} orderId
   */
  async recoverOrder(orderId) {
    if (!orderId) throw new Error('Order ID is required');
    return await apiClient.post(`/api/v1/orders/${orderId}/recover`);
  },

  /**
   * Admin: Triggers batch recovery for all stale PENDING orders.
   */
  async recoverAllPending() {
    return await apiClient.post('/api/v1/orders/recover-pending');
  },
};
