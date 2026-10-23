import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LoadingSpinner } from '../feedback/LoadingSpinner';

export function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div style={{ minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <LoadingSpinner text="Verifying administrative access..." size="lg" />
      </div>
    );
  }

  // 1. Unauthenticated: Redirect to login with intended destination
  if (!isAuthenticated) {
    return (
      <Navigate
        to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`}
        replace
        state={{ from: location }}
      />
    );
  }

  // 2. Authenticated but lacks ROLE_ADMIN: Redirect to 403 Forbidden page
  if (!isAdmin) {
    return <Navigate to="/403" replace state={{ from: location.pathname }} />;
  }

  // 3. Authenticated Admin: allow access
  return children;
}
