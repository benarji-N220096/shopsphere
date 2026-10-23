import { apiClient } from './apiClient';

export const paymentApi = {
  /**
   * Retrieves payment transaction details for a given order ID.
   * @param {string} orderId
   */
  async getPaymentByOrderId(orderId) {
    try {
      return await apiClient.get(`/api/v1/payments/order/${orderId}`);
    } catch {
      // Payment record may not yet be accessible or internal only
      return null;
    }
  },
};
