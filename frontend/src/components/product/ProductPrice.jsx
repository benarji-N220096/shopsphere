import React from 'react';
import { formatCurrency } from '../../utils/formatters';

export function ProductPrice({ price, size = 'md', className = '' }) {
  const getFontSize = () => {
    switch (size) {
      case 'sm':
        return 'var(--font-size-sm)';
      case 'lg':
        return 'var(--font-size-2xl)';
      case 'xl':
        return 'var(--font-size-3xl)';
      case 'md':
      default:
        return 'var(--font-size-lg)';
    }
  };

  return (
    <span
      className={`product-price ${className}`}
      style={{
        fontSize: getFontSize(),
        fontWeight: 'var(--font-weight-bold)',
        color: 'var(--color-primary)',
        letterSpacing: '-0.02em',
      }}
    >
      {formatCurrency(price)}
    </span>
  );
}
