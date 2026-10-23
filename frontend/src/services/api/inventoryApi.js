import { apiClient } from './apiClient';

export const inventoryApi = {
  /**
   * Retrieves inventory record for a specific product.
   * @param {string} productId
   */
  async getInventory(productId) {
    if (!productId) throw new Error('Product ID is required');
    return await apiClient.get(`/api/v1/inventory/${productId}`);
  },

  /**
   * Updates total stock quantity for a product.
   * @param {string} productId
   * @param {number} quantity
   */
  async updateInventory(productId, quantity) {
    if (!productId) throw new Error('Product ID is required');
    return await apiClient.put(`/api/v1/inventory/${productId}`, { quantity: Number(quantity) });
  },

  /**
   * Creates initial inventory record for a product.
   * @param {string} productId
   * @param {number} quantity
   */
  async createInventory(productId, quantity) {
    if (!productId) throw new Error('Product ID is required');
    return await apiClient.post('/api/v1/inventory', {
      productId,
      quantity: Number(quantity),
    });
  },

  /**
   * Deletes inventory entry for a product.
   * @param {string} productId
   */
  async deleteInventory(productId) {
    if (!productId) throw new Error('Product ID is required');
    return await apiClient.delete(`/api/v1/inventory/${productId}`);
  },
};
