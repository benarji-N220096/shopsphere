import React from 'react';

export function Badge({ children, variant = 'default', size = 'md', className = '' }) {
  const getStyles = () => {
    switch (variant) {
      case 'success':
        return {
          backgroundColor: 'var(--color-success-bg)',
          color: 'var(--color-success)',
          border: '1px solid var(--color-success-border)',
        };
      case 'warning':
        return {
          backgroundColor: 'var(--color-warning-bg)',
          color: 'var(--color-warning)',
          border: '1px solid var(--color-warning-border)',
        };
      case 'error':
        return {
          backgroundColor: 'var(--color-error-bg)',
          color: 'var(--color-error)',
          border: '1px solid var(--color-error-border)',
        };
      case 'primary':
        return {
          backgroundColor: 'var(--color-primary-light)',
          color: 'var(--color-primary)',
          border: '1px solid #C4D7EB',
        };
      case 'accent':
        return {
          backgroundColor: 'var(--color-accent-light)',
          color: 'var(--color-accent)',
          border: '1px solid #B8E4E0',
        };
      case 'neutral':
      default:
        return {
          backgroundColor: 'var(--color-surface-subtle)',
          color: 'var(--color-text-secondary)',
          border: '1px solid var(--color-border)',
        };
    }
  };

  const sizeStyles = size === 'sm' ? {
    padding: '0.125rem 0.375rem',
    fontSize: 'var(--font-size-xs)',
  } : {
    padding: '0.25rem 0.625rem',
    fontSize: 'var(--font-size-xs)',
  };

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        fontWeight: 'var(--font-weight-semibold)',
        borderRadius: 'var(--radius-full)',
        lineHeight: 1.2,
        letterSpacing: '0.02em',
        textTransform: 'uppercase',
        ...getStyles(),
        ...sizeStyles,
      }}
      className={`badge badge-${variant} ${className}`}
    >
      {children}
    </span>
  );
}
