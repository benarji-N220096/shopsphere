import React from 'react';
import { Minus, Plus } from 'lucide-react';

export function QuantitySelector({ quantity, onChange, min = 1, max = 99, disabled = false, size = 'md' }) {
  const isSmall = size === 'sm';

  const handleDecrement = (e) => {
    e.stopPropagation();
    if (quantity > min && !disabled) {
      onChange(quantity - 1);
    }
  };

  const handleIncrement = (e) => {
    e.stopPropagation();
    if (quantity < max && !disabled) {
      onChange(quantity + 1);
    }
  };

  return (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-sm)',
        backgroundColor: 'var(--color-surface)',
        overflow: 'hidden',
        height: isSmall ? '28px' : '36px',
      }}
    >
      <button
        type="button"
        onClick={handleDecrement}
        disabled={disabled || quantity <= min}
        style={{
          width: isSmall ? '28px' : '36px',
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: quantity <= min || disabled ? 'var(--color-border)' : 'var(--color-text-primary)',
          backgroundColor: 'transparent',
          transition: 'background-color var(--transition-fast)',
        }}
        aria-label="Decrease quantity"
      >
        <Minus size={isSmall ? 12 : 14} />
      </button>

      <span
        style={{
          minWidth: isSmall ? '28px' : '36px',
          textAlign: 'center',
          fontSize: isSmall ? 'var(--font-size-xs)' : 'var(--font-size-sm)',
          fontWeight: 'var(--font-weight-semibold)',
          userSelect: 'none',
        }}
      >
        {quantity}
      </span>

      <button
        type="button"
        onClick={handleIncrement}
        disabled={disabled || quantity >= max}
        style={{
          width: isSmall ? '28px' : '36px',
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: quantity >= max || disabled ? 'var(--color-border)' : 'var(--color-text-primary)',
          backgroundColor: 'transparent',
          transition: 'background-color var(--transition-fast)',
        }}
        aria-label="Increase quantity"
      >
        <Plus size={isSmall ? 12 : 14} />
      </button>
    </div>
  );
}
