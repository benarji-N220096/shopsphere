import { apiClient } from './apiClient';

export const productApi = {
  /**
   * Retrieves all active products.
   */
  async getAllProducts() {
    return await apiClient.get('/api/v1/products');
  },

  /**
   * Retrieves a specific product by ID.
   * @param {string} id
   */
  async getProductById(id) {
    if (!id) throw new Error('Product ID is required');
    return await apiClient.get(`/api/v1/products/${id}`);
  },

  /**
   * Creates a new product (Admin only).
   * @param {{ name: string, description: string, price: number, stockQuantity: number, category: string, imageUrl: string, active: boolean }} productData
   */
  async createProduct(productData) {
    return await apiClient.post('/api/v1/products', productData);
  },

  /**
   * Updates an existing product (Admin only).
   * @param {string} id
   * @param {{ name: string, description: string, price: number, stockQuantity: number, category: string, imageUrl: string, active: boolean }} productData
   */
  async updateProduct(id, productData) {
    if (!id) throw new Error('Product ID is required');
    return await apiClient.put(`/api/v1/products/${id}`, productData);
  },

  /**
   * Deletes a product by ID (Admin only).
   * @param {string} id
   */
  async deleteProduct(id) {
    if (!id) throw new Error('Product ID is required');
    return await apiClient.delete(`/api/v1/products/${id}`);
  },
};
