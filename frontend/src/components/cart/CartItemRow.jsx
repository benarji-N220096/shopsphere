import React from 'react';
import { Trash2, Package } from 'lucide-react';
import { formatCurrency } from '../../utils/formatters';
import { getProductImage } from '../../utils/productImages';
import { QuantitySelector } from './QuantitySelector';

export function CartItemRow({ item, onUpdateQuantity, onRemove, compact = false }) {
  const { productId, quantity, product } = item;
  const price = parseFloat(product?.price || 0);
  const total = price * quantity;
  const maxStock = product?.stockQuantity || 50;

  return (
    <div
      style={{
        display: 'flex',
        gap: compact ? 'var(--space-3)' : 'var(--space-4)',
        padding: compact ? 'var(--space-3) 0' : 'var(--space-4) 0',
        borderBottom: '1px solid var(--color-border-light)',
        alignItems: 'center',
      }}
    >
      <div
        style={{
          width: compact ? '56px' : '72px',
          height: compact ? '56px' : '72px',
          borderRadius: 'var(--radius-sm)',
          backgroundColor: '#F1F5F9',
          border: '1px solid var(--color-border-light)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          overflow: 'hidden',
        }}
      >
        <img
          src={getProductImage(product)}
          alt={product?.name || 'Product'}
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        />
      </div>

      <div style={{ flex: '1 1 auto', minWidth: 0 }}>
        <h4
          style={{
            fontSize: compact ? 'var(--font-size-sm)' : 'var(--font-size-md)',
            fontWeight: 'var(--font-weight-medium)',
            color: 'var(--color-text-primary)',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            marginBottom: '4px',
          }}
        >
          {product?.name || 'Product'}
        </h4>

        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginBottom: '8px' }}>
          {formatCurrency(price)} each
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          <QuantitySelector
            quantity={quantity}
            max={maxStock}
            onChange={(newQty) => onUpdateQuantity(productId, newQty)}
            size={compact ? 'sm' : 'md'}
          />

          <button
            type="button"
            onClick={() => onRemove(productId)}
            style={{
              color: 'var(--color-text-muted)',
              display: 'flex',
              alignItems: 'center',
              padding: '4px',
              transition: 'color var(--transition-fast)',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-error)')}
            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-text-muted)')}
            aria-label={`Remove ${product?.name} from cart`}
          >
            <Trash2 size={16} />
          </button>
        </div>
      </div>

      <div
        style={{
          textAlign: 'right',
          flexShrink: 0,
          fontWeight: 'var(--font-weight-bold)',
          fontSize: compact ? 'var(--font-size-sm)' : 'var(--font-size-md)',
          color: 'var(--color-primary)',
        }}
      >
        {formatCurrency(total)}
      </div>
    </div>
  );
}
