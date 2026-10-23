import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LoadingSpinner } from '../feedback/LoadingSpinner';

export function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div style={{ padding: 'var(--space-16) 0', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Checking authentication session..." size="lg" />
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect to login while saving the intended path
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace state={{ from: location }} />;
  }

  return children;
}
