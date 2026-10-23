import React, { useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { X, ShoppingBag, ArrowRight } from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { formatCurrency } from '../../utils/formatters';
import { CartItemRow } from './CartItemRow';
import { Button } from '../common/Button';

export function CartDrawer() {
  const { isDrawerOpen, closeDrawer, items, totalItemCount, subtotal, updateQuantity, removeFromCart } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isDrawerOpen) {
        closeDrawer();
      }
    };
    if (isDrawerOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isDrawerOpen, closeDrawer]);

  if (!isDrawerOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 'var(--z-drawer-backdrop)',
        display: 'flex',
        justifyContent: 'flex-end',
      }}
    >
      {/* Backdrop */}
      <div
        onClick={closeDrawer}
        style={{
          position: 'absolute',
          inset: 0,
          backgroundColor: 'rgba(23, 32, 51, 0.45)',
          backdropFilter: 'blur(2px)',
          transition: 'opacity var(--transition-normal)',
        }}
        aria-hidden="true"
      />

      {/* Drawer Panel */}
      <aside
        style={{
          position: 'relative',
          width: '100%',
          maxWidth: '420px',
          height: '100%',
          backgroundColor: 'var(--color-surface)',
          boxShadow: 'var(--shadow-xl)',
          display: 'flex',
          flexDirection: 'column',
          zIndex: 'var(--z-drawer)',
          animation: 'slideInRight 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
        }}
        aria-label="Shopping Cart Drawer"
      >
        {/* Drawer Header */}
        <div
          style={{
            padding: 'var(--space-4) var(--space-6)',
            borderBottom: '1px solid var(--color-border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <ShoppingBag size={20} color="var(--color-primary)" />
            <h3 style={{ fontSize: 'var(--font-size-lg)', fontWeight: 'var(--font-weight-bold)' }}>
              Your Cart ({totalItemCount})
            </h3>
          </div>
          <button
            onClick={closeDrawer}
            style={{
              color: 'var(--color-text-secondary)',
              padding: '4px',
              borderRadius: 'var(--radius-sm)',
              display: 'flex',
            }}
            aria-label="Close cart drawer"
          >
            <X size={20} />
          </button>
        </div>

        {/* Drawer Body */}
        <div
          style={{
            flex: '1 1 auto',
            overflowY: 'auto',
            padding: 'var(--space-4) var(--space-6)',
          }}
        >
          {items.length === 0 ? (
            <div
              style={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                textAlign: 'center',
                color: 'var(--color-text-muted)',
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
                }}
              >
                <ShoppingBag size={32} />
              </div>
              <h4 style={{ fontSize: 'var(--font-size-md)', color: 'var(--color-text-primary)', marginBottom: '8px' }}>
                Your cart is empty
              </h4>
              <p style={{ fontSize: 'var(--font-size-sm)', maxWidth: '260px', marginBottom: 'var(--space-6)' }}>
                Looks like you haven't added any products to your shopping bag yet.
              </p>
              <Button
                variant="primary"
                size="sm"
                onClick={() => {
                  closeDrawer();
                  navigate('/products');
                }}
              >
                Start Shopping
              </Button>
            </div>
          ) : (
            <div>
              {items.map((item) => (
                <CartItemRow
                  key={item.productId}
                  item={item}
                  compact={true}
                  onUpdateQuantity={updateQuantity}
                  onRemove={removeFromCart}
                />
              ))}
            </div>
          )}
        </div>

        {/* Drawer Footer */}
        {items.length > 0 && (
          <div
            style={{
              padding: 'var(--space-4) var(--space-6)',
              borderTop: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-surface-subtle)',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 'var(--space-2)',
              }}
            >
              <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
                Subtotal
              </span>
              <span style={{ fontSize: 'var(--font-size-xl)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>
                {formatCurrency(subtotal)}
              </span>
            </div>

            <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginBottom: 'var(--space-4)' }}>
              Shipping and taxes calculated during checkout.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
              <Button
                variant="primary"
                size="lg"
                icon={ArrowRight}
                iconPosition="right"
                onClick={() => {
                  closeDrawer();
                  if (!isAuthenticated) {
                    navigate('/login?redirect=/checkout');
                  } else {
                    navigate('/checkout');
                  }
                }}
              >
                {isAuthenticated ? 'Proceed to Checkout' : 'Sign in to Checkout'}
              </Button>

              <Link
                to="/cart"
                onClick={closeDrawer}
                style={{
                  textAlign: 'center',
                  fontSize: 'var(--font-size-sm)',
                  fontWeight: 'var(--font-weight-medium)',
                  color: 'var(--color-secondary)',
                  padding: '6px',
                }}
              >
                View and Edit Full Cart
              </Link>
            </div>
          </div>
        )}
      </aside>

      <style>{`
        @keyframes slideInRight {
          from {
            transform: translateX(100%);
          }
          to {
            transform: translateX(0);
          }
        }
      `}</style>
    </div>
  );
}
