import React from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export function Toast({ message, type = 'success', onClose }) {
  if (!message) return null;

  const getIcon = () => {
    switch (type) {
      case 'error':
        return <AlertCircle size={18} color="var(--color-error)" />;
      case 'info':
        return <Info size={18} color="var(--color-secondary)" />;
      case 'success':
      default:
        return <CheckCircle2 size={18} color="var(--color-success)" />;
    }
  };

  const getBorderColor = () => {
    switch (type) {
      case 'error':
        return 'var(--color-error-border)';
      case 'info':
        return 'var(--color-secondary-light)';
      case 'success':
      default:
        return 'var(--color-success-border)';
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 'var(--space-6)',
        right: 'var(--space-6)',
        zIndex: 'var(--z-toast)',
        display: 'flex',
        alignItems: 'center',
        gap: 'var(--space-3)',
        padding: 'var(--space-3) var(--space-4)',
        backgroundColor: 'var(--color-surface)',
        border: `1px solid ${getBorderColor()}`,
        borderRadius: 'var(--radius-md)',
        boxShadow: 'var(--shadow-lg)',
        animation: 'slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
        maxWidth: '400px',
      }}
      role="status"
    >
      {getIcon()}
      <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-medium)', color: 'var(--color-text-primary)' }}>
        {message}
      </span>
      {onClose && (
        <button
          onClick={onClose}
          style={{
            marginLeft: 'var(--space-2)',
            color: 'var(--color-text-muted)',
            display: 'flex',
            alignItems: 'center',
          }}
          aria-label="Close notification"
        >
          <X size={14} />
        </button>
      )}
      <style>{`
        @keyframes slideUp {
          from {
            transform: translateY(100%);
            opacity: 0;
          }
          to {
            transform: translateY(0);
            opacity: 1;
          }
        }
      `}</style>
    </div>
  );
}
