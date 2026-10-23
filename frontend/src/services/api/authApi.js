import { apiClient } from './apiClient';

export const authApi = {
  async login(email, password) {
    return await apiClient.post('/api/v1/auth/login', {
      email,
      password,
    });
  },

  async register(registrationData) {
    return await apiClient.post('/api/v1/auth/register', registrationData);
  },

  async loginWithGoogle(idToken) {
    return await apiClient.post('/api/v1/auth/google', { idToken });
  }
};
