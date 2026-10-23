import React, { createContext, useContext, useState, useEffect } from 'react';
import { tokenStorage } from '../services/storage/tokenStorage';
import { authApi } from '../services/api/authApi';

const AuthContext = createContext(null);

function decodeJwt(token) {
  try {
    if (!token) return null;
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => tokenStorage.getToken());
  const [user, setUser] = useState(() => tokenStorage.getUser());
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (token) {
      const payload = decodeJwt(token);
      if (payload && payload.exp && payload.exp * 1000 < Date.now()) {
        // Token has expired
        tokenStorage.clear();
        setToken(null);
        setUser(null);
        return;
      }
      if (!user) {
        const storedUser = tokenStorage.getUser();
        if (storedUser) setUser(storedUser);
      }
    }

    const handleUnauthorized = () => {
      setToken(null);
      setUser(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [token, user]);

  const login = async (email, password) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(email, password);
      const accessToken = response.accessToken;
      
      // Extract authoritative roles from JWT payload and response
      const payload = decodeJwt(accessToken);
      const rawRoles = payload?.roles || response.roles || ['CUSTOMER'];
      const normalizedRoles = rawRoles.map((r) => (r.startsWith('ROLE_') ? r : `ROLE_${r}`));

      const userInfo = {
        id: response.id || payload?.sub,
        email: response.email || payload?.email,
        firstName: response.firstName,
        lastName: response.lastName,
        roles: normalizedRoles,
      };

      tokenStorage.setToken(accessToken);
      tokenStorage.setUser(userInfo);

      setToken(accessToken);
      setUser(userInfo);
      return { success: true, user: userInfo };
    } catch (error) {
      return { success: false, error: error.message || 'Login failed' };
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (formData) => {
    setIsLoading(true);
    try {
      const response = await authApi.register(formData);
      return { success: true, data: response };
    } catch (error) {
      return { success: false, error: error.message || 'Registration failed' };
    } finally {
      setIsLoading(false);
    }
  };

  const loginWithGoogle = async (idToken) => {
    setIsLoading(true);
    try {
      const response = await authApi.loginWithGoogle(idToken);
      const accessToken = response.accessToken;

      const payload = decodeJwt(accessToken);
      const rawRoles = payload?.roles || response.roles || ['CUSTOMER'];
      const normalizedRoles = rawRoles.map((r) => (r.startsWith('ROLE_') ? r : `ROLE_${r}`));

      const userInfo = {
        id: response.id || payload?.sub,
        email: response.email || payload?.email,
        firstName: response.firstName,
        lastName: response.lastName,
        roles: normalizedRoles,
      };

      tokenStorage.setToken(accessToken);
      tokenStorage.setUser(userInfo);

      setToken(accessToken);
      setUser(userInfo);
      return { success: true, user: userInfo };
    } catch (error) {
      return { success: false, error: error.message || 'Google authentication failed' };
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    tokenStorage.clear();
    setToken(null);
    setUser(null);
  };

  // Derive admin status directly from valid JWT payload if token exists, fallback to state
  const jwtPayload = decodeJwt(token);
  const jwtRoles = jwtPayload?.roles || user?.roles || [];
  const isAdmin = jwtRoles.some((r) => r === 'ROLE_ADMIN' || r === 'ADMIN');

  const value = {
    user,
    token,
    isAuthenticated: Boolean(token),
    isAdmin,
    isLoading,
    login,
    loginWithGoogle,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
