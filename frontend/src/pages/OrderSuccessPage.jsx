import React, { useState } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import { CheckCircle2, Copy, Check, ArrowRight, ShoppingBag, Package } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { OrderStatusBadge } from '../components/order/OrderStatusBadge';
import { formatCurrency, formatDate } from '../utils/formatters';

export function OrderSuccessPage() {
  const { orderId } = useParams();
  const location = useLocation();
  const order = location.state?.order;
  const [copied, setCopied] = useState(false);

  const handleCopyId = () => {
    if (orderId) {
      navigator.clipboard.writeText(orderId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div style={{ padding: 'var(--space-12) 0 var(--space-16)' }}>
      <Container>
        <div style={{ maxWidth: '620px', margin: '0 auto' }}>
          <div className="card" style={{ padding: 'var(--space-10)', textAlign: 'center' }}>
            <div
              style={{
                width: '72px',
                height: '72px',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--color-success-bg)',
                color: 'var(--color-success)',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 'var(--space-4)',
                border: '1px solid var(--color-success-border)',
              }}
            >
              <CheckCircle2 size={40} />
            </div>

            <h1 style={{ fontSize: 'var(--font-size-3xl)', marginBottom: 'var(--space-2)' }}>
              Thank You! Your Order is Confirmed
            </h1>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-base)', marginBottom: 'var(--space-6)' }}>
              We have received your order and payment was processed successfully. A confirmation email has been dispatched.
            </p>

            {/* Order Reference Box */}
            <div
              style={{
                backgroundColor: 'var(--color-surface-subtle)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                padding: 'var(--space-4) var(--space-5)',
                marginBottom: 'var(--space-6)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div style={{ textAlign: 'left' }}>
                <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Order Reference ID
                </span>
                <div style={{ fontFamily: 'monospace', fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-primary)', marginTop: '2px' }}>
                  {orderId}
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={handleCopyId}
                icon={copied ? Check : Copy}
              >
                {copied ? 'Copied' : 'Copy'}
              </Button>
            </div>

            {/* Order Details Preview if passed in navigation state */}
            {order && (
              <div
                style={{
                  borderTop: '1px solid var(--color-border)',
                  borderBottom: '1px solid var(--color-border)',
                  padding: 'var(--space-4) 0',
                  marginBottom: 'var(--space-6)',
                  textAlign: 'left',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>Status</div>
                  <div style={{ marginTop: '4px' }}>
                    <OrderStatusBadge status={order.status || 'CONFIRMED'} />
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>Order Total</div>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-base)', color: 'var(--color-primary)', marginTop: '2px' }}>
                    {formatCurrency(order.totalAmount || 0)}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>Date</div>
                  <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text)', marginTop: '2px' }}>
                    {formatDate(order.createdAt || new Date().toISOString())}
                  </div>
                </div>
              </div>
            )}

            <div style={{ display: 'flex', gap: 'var(--space-4)', justifyContent: 'center', flexWrap: 'wrap' }}>
              <Link to={`/orders/${orderId}`}>
                <Button variant="primary" size="md" icon={Package}>
                  View Order Details
                </Button>
              </Link>
              <Link to="/products">
                <Button variant="outline" size="md" icon={ShoppingBag}>
                  Continue Shopping
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </Container>
    </div>
  );
}
