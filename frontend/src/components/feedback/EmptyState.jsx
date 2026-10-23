import React from 'react';
import { PackageOpen } from 'lucide-react';
import { Button } from '../common/Button';

export function EmptyState({
  title = 'No items found',
  description = 'We couldn\'t find any items matching your current filters.',
  actionLabel,
  onAction,
  icon: Icon = PackageOpen,
  className = '',
}) {
  return (
    <div
      className={`empty-state-card card ${className}`}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: 'var(--space-12) var(--space-6)',
        margin: 'var(--space-8) 0',
      }}
    >
      <div
        style={{
          width: '64px',
          height: '64px',
          borderRadius: '50%',
          backgroundColor: 'var(--color-surface-subtle)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: 'var(--space-4)',
          color: 'var(--color-text-secondary)',
        }}
      >
        <Icon size={32} />
      </div>
      <h3 style={{ fontSize: 'var(--font-size-xl)', marginBottom: 'var(--space-2)' }}>{title}</h3>
      <p style={{ maxWidth: '440px', marginBottom: actionLabel ? 'var(--space-6)' : 0 }}>
        {description}
      </p>
      {actionLabel && onAction && (
        <Button variant="primary" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
}
