import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingBag, ArrowRight, ArrowLeft, Trash2, ShieldCheck, Check } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { CartItemRow } from '../components/cart/CartItemRow';
import { EmptyState } from '../components/feedback/EmptyState';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { formatCurrency } from '../utils/formatters';

export function CartPage() {
  const { items, totalItemCount, subtotal, updateQuantity, removeFromCart, clearCart } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const shipping = subtotal >= 50 ? 0 : 5.99;
  const grandTotal = subtotal + shipping;

  if (items.length === 0) {
    return (
      <div style={{ padding: 'var(--space-12) 0' }}>
        <Container>
          <EmptyState
            icon={ShoppingBag}
            title="Your shopping cart is empty"
            description="Explore our catalog and find verified products backed by real inventory."
            actionLabel="Discover Products"
            onAction={() => navigate('/products')}
          />
        </Container>
      </div>
    );
  }

  return (
    <div style={{ padding: 'var(--space-8) 0 var(--space-16)' }}>
      <Container>
        {/* Header */}
        <div
          style={{
            display: 'flex',
            alignItems: 'baseline',
            justifyContent: 'space-between',
            marginBottom: 'var(--space-8)',
            flexWrap: 'wrap',
            gap: 'var(--space-2)',
          }}
        >
          <div>
            <h1 style={{ fontSize: 'var(--font-size-3xl)', marginBottom: 'var(--space-1)' }}>
              Shopping Cart
            </h1>
            <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
              You have {totalItemCount} item{totalItemCount === 1 ? '' : 's'} in your cart.
            </p>
          </div>

          <button
            onClick={clearCart}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: 'var(--font-size-sm)',
              color: 'var(--color-text-muted)',
              transition: 'color var(--transition-fast)',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-error)')}
            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-text-muted)')}
          >
            <Trash2 size={16} />
            <span>Clear Cart</span>
          </button>
        </div>

        {/* 2-Column Layout */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: 'var(--space-8)',
            alignItems: 'start',
          }}
        >
          {/* Items List (Left Column) */}
          <div className="card" style={{ padding: 'var(--space-6)' }}>
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              {items.map((item) => (
                <CartItemRow
                  key={item.productId}
                  item={item}
                  compact={false}
                  onUpdateQuantity={updateQuantity}
                  onRemove={removeFromCart}
                />
              ))}
            </div>

            <div style={{ marginTop: 'var(--space-6)', paddingTop: 'var(--space-4)', borderTop: '1px solid var(--color-border)' }}>
              <Link
                to="/products"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontSize: 'var(--font-size-sm)',
                  fontWeight: 'var(--font-weight-medium)',
                  color: 'var(--color-secondary)',
                }}
              >
                <ArrowLeft size={16} />
                <span>Continue Shopping</span>
              </Link>
            </div>
          </div>

          {/* Order Summary (Right Column) */}
          <div className="card" style={{ padding: 'var(--space-6)', position: 'sticky', top: '5rem' }}>
            <h3 style={{ fontSize: 'var(--font-size-lg)', marginBottom: 'var(--space-4)', borderBottom: '1px solid var(--color-border)', paddingBottom: 'var(--space-3)' }}>
              Order Summary
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', marginBottom: 'var(--space-6)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-sm)' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Items Subtotal</span>
                <span style={{ fontWeight: 'var(--font-weight-semibold)' }}>{formatCurrency(subtotal)}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-sm)' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Shipping</span>
                <span>{shipping === 0 ? <strong style={{ color: 'var(--color-success)' }}>FREE</strong> : formatCurrency(shipping)}</span>
              </div>

              {shipping > 0 && (
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', backgroundColor: 'var(--color-surface-subtle)', padding: '8px', borderRadius: 'var(--radius-sm)' }}>
                  Add <strong>{formatCurrency(50 - subtotal)}</strong> more for free standard delivery!
                </div>
              )}

              <hr style={{ border: 'none', borderTop: '1px solid var(--color-border-light)', margin: 'var(--space-2) 0' }} />

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: 'var(--font-size-md)', fontWeight: 'var(--font-weight-bold)' }}>Total</span>
                <span style={{ fontSize: 'var(--font-size-2xl)', fontWeight: 'var(--font-weight-extrabold)', color: 'var(--color-primary)' }}>
                  {formatCurrency(grandTotal)}
                </span>
              </div>
            </div>

            <Button
              variant="primary"
              size="lg"
              icon={ArrowRight}
              iconPosition="right"
              style={{ width: '100%', marginBottom: 'var(--space-4)' }}
              onClick={() => {
                if (!isAuthenticated) {
                  navigate('/login?redirect=/checkout');
                } else {
                  navigate('/checkout');
                }
              }}
            >
              {isAuthenticated ? 'Proceed to Checkout' : 'Sign In to Checkout'}
            </Button>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <ShieldCheck size={14} color="var(--color-success)" />
                <span>Zero-Trust API Gateway with JWT Encryption</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Check size={14} color="var(--color-secondary)" />
                <span>Idempotent order reservations</span>
              </div>
            </div>
          </div>
        </div>
      </Container>
    </div>
  );
}
