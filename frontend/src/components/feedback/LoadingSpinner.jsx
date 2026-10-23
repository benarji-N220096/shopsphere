import React from 'react';

export function LoadingSpinner({ size = 'md', label = 'Loading...', className = '' }) {
  const sizePx = size === 'sm' ? 20 : size === 'lg' ? 44 : 32;

  return (
    <div
      className={`loading-spinner-container ${className}`}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'var(--space-8)',
        gap: 'var(--space-3)',
      }}
      role="status"
      aria-live="polite"
    >
      <div
        style={{
          width: sizePx,
          height: sizePx,
          border: '3px solid var(--color-border)',
          borderTopColor: 'var(--color-secondary)',
          borderRadius: '50%',
          animation: 'spin 0.75s linear infinite',
        }}
      />
      {label && <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>{label}</span>}
      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}
