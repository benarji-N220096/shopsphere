import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from '../common/Button';

export function ErrorState({
  title = 'Unable to Load Content',
  message = 'An unexpected error occurred while communicating with the ShopSphere gateway.',
  onRetry,
  className = '',
}) {
  return (
    <div
      className={`error-state-card card ${className}`}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: 'var(--space-12) var(--space-6)',
        margin: 'var(--space-8) 0',
        borderColor: 'var(--color-error-border)',
        backgroundColor: '#FFFDFD',
      }}
      role="alert"
    >
      <div
        style={{
          width: '56px',
          height: '56px',
          borderRadius: '50%',
          backgroundColor: 'var(--color-error-bg)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: 'var(--space-4)',
          color: 'var(--color-error)',
        }}
      >
        <AlertTriangle size={28} />
      </div>
      <h3 style={{ fontSize: 'var(--font-size-xl)', color: 'var(--color-error)', marginBottom: 'var(--space-2)' }}>
        {title}
      </h3>
      <p style={{ maxWidth: '480px', marginBottom: onRetry ? 'var(--space-6)' : 0 }}>
        {message}
      </p>
      {onRetry && (
        <Button variant="outline" icon={RefreshCw} onClick={onRetry}>
          Try Again
        </Button>
      )}
    </div>
  );
}
