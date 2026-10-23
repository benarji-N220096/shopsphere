import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ShoppingCart, Package } from 'lucide-react';
import { ProductPrice } from './ProductPrice';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { useCart } from '../../context/CartContext';
import { getProductImage } from '../../utils/productImages';

export function ProductCard({ product }) {
  const { addToCart } = useCart();
  const [imageError, setImageError] = useState(false);
  const isOutOfStock = product.stockQuantity === 0;

  const handleAddToCart = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isOutOfStock) {
      addToCart(product, 1);
    }
  };

  return (
    <article
      className="card product-card"
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        overflow: 'hidden',
        position: 'relative',
        transition: 'transform var(--transition-normal), box-shadow var(--transition-normal)',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-3px)';
        e.currentTarget.style.boxShadow = 'var(--shadow-lg)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.boxShadow = 'var(--shadow-sm)';
      }}
    >
      <Link
        to={`/products/${product.id}`}
        style={{
          display: 'block',
          position: 'relative',
          backgroundColor: '#F8FAFC',
          aspectRatio: '4 / 3',
          overflow: 'hidden',
          borderBottom: '1px solid var(--color-border-light)',
        }}
        aria-label={`View details for ${product.name}`}
      >
        <img
          src={imageError ? '/images/products/headphones.jpg' : getProductImage(product)}
          alt={product.name}
          onError={() => setImageError(true)}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            transition: 'transform var(--transition-normal)',
          }}
        />

        <div style={{ position: 'absolute', top: '0.75rem', left: '0.75rem', display: 'flex', gap: '0.5rem' }}>
          {product.category && (
            <Badge variant="primary" size="sm">
              {product.category}
            </Badge>
          )}
        </div>

        <div style={{ position: 'absolute', top: '0.75rem', right: '0.75rem' }}>
          {isOutOfStock ? (
            <Badge variant="error" size="sm">Out of Stock</Badge>
          ) : product.stockQuantity < 10 ? (
            <Badge variant="warning" size="sm">Only {product.stockQuantity} left</Badge>
          ) : (
            <Badge variant="success" size="sm">In Stock</Badge>
          )}
        </div>
      </Link>

      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          flex: '1 0 auto',
          padding: 'var(--space-4)',
        }}
      >
        <h3
          style={{
            fontSize: 'var(--font-size-md)',
            fontWeight: 'var(--font-weight-semibold)',
            marginBottom: 'var(--space-2)',
            lineHeight: 1.4,
          }}
        >
          <Link
            to={`/products/${product.id}`}
            style={{ color: 'var(--color-text-primary)', transition: 'color var(--transition-fast)' }}
            onMouseEnter={(e) => (e.target.style.color = 'var(--color-secondary)')}
            onMouseLeave={(e) => (e.target.style.color = 'var(--color-text-primary)')}
          >
            {product.name}
          </Link>
        </h3>

        {product.description && (
          <p
            style={{
              fontSize: 'var(--font-size-sm)',
              color: 'var(--color-text-secondary)',
              marginBottom: 'var(--space-4)',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              lineHeight: 1.4,
            }}
          >
            {product.description}
          </p>
        )}

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginTop: 'auto',
            paddingTop: 'var(--space-2)',
            gap: 'var(--space-2)',
          }}
        >
          <ProductPrice price={product.price} size="md" />

          <Button
            variant={isOutOfStock ? 'outline' : 'primary'}
            size="sm"
            icon={ShoppingCart}
            disabled={isOutOfStock}
            onClick={handleAddToCart}
            aria-label={`Add ${product.name} to cart`}
          >
            {isOutOfStock ? 'Sold Out' : 'Add'}
          </Button>
        </div>
      </div>
    </article>
  );
}
