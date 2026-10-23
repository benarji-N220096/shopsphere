import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Package, ArrowRight, RefreshCw, ShoppingBag, Calendar, DollarSign } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { OrderStatusBadge } from '../components/order/OrderStatusBadge';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { EmptyState } from '../components/feedback/EmptyState';
import { ErrorState } from '../components/feedback/ErrorState';
import { orderApi } from '../services/api/orderApi';
import { formatCurrency, formatDate } from '../utils/formatters';

export function OrdersPage() {
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const fetchOrders = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const data = await orderApi.getMyOrders();
      setOrders(Array.isArray(data) ? data : []);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load your order history.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  return (
    <div style={{ padding: 'var(--space-8) 0 var(--space-16)' }}>
      <Container>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: 'var(--space-6)',
          }}
        >
          <div>
            <h1 style={{ fontSize: 'var(--font-size-3xl)' }}>My Orders</h1>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', marginTop: '4px' }}>
              View and track all orders associated with your ShopSphere account.
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchOrders}
            icon={RefreshCw}
            isLoading={isLoading}
          >
            Refresh
          </Button>
        </div>

        {isLoading ? (
          <div style={{ padding: 'var(--space-16) 0', display: 'flex', justifyContent: 'center' }}>
            <LoadingSpinner text="Retrieving your orders from secure order service..." size="lg" />
          </div>
        ) : errorMessage ? (
          <ErrorState
            title="Unable to Retrieve Orders"
            message={errorMessage}
            onRetry={fetchOrders}
          />
        ) : orders.length === 0 ? (
          <EmptyState
            icon={Package}
            title="No Orders Yet"
            description="You haven't placed any orders with ShopSphere so far. Explore our catalog and place your first order!"
            actionText="Explore Catalog"
            actionLink="/products"
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
            {orders.map((order) => {
              const itemCount = (order.items || []).reduce((acc, curr) => acc + (curr.quantity || 1), 0);

              return (
                <div
                  key={order.id}
                  className="card"
                  style={{
                    padding: 'var(--space-5)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 'var(--space-4)',
                    transition: 'border-color var(--transition-fast)',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: 'var(--space-3)',
                      borderBottom: '1px solid var(--color-border)',
                      paddingBottom: 'var(--space-4)',
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                          Order ID
                        </span>
                        <OrderStatusBadge status={order.status} />
                      </div>
                      <div style={{ fontFamily: 'monospace', fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-primary)', marginTop: '2px' }}>
                        {order.id}
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-6)' }}>
                      <div>
                        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', display: 'block' }}>Date Placed</span>
                        <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-medium)' }}>
                          {formatDate(order.createdAt)}
                        </span>
                      </div>
                      <div>
                        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', display: 'block' }}>Total Amount</span>
                        <span style={{ fontSize: 'var(--font-size-base)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>
                          {formatCurrency(order.totalAmount || 0)}
                        </span>
                      </div>
                      <Link to={`/orders/${order.id}`}>
                        <Button variant="outline" size="sm" icon={ArrowRight} iconPosition="right">
                          View Details
                        </Button>
                      </Link>
                    </div>
                  </div>

                  {/* Order items preview row */}
                  {order.items && order.items.length > 0 && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)', flexWrap: 'wrap', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                      <span>
                        <strong style={{ color: 'var(--color-text)' }}>{itemCount}</strong> {itemCount === 1 ? 'item' : 'items'} in order:
                      </span>
                      {order.items.slice(0, 3).map((item, idx) => (
                        <span
                          key={item.id || idx}
                          style={{
                            backgroundColor: 'var(--color-surface-subtle)',
                            padding: '3px 8px',
                            borderRadius: 'var(--radius-sm)',
                            border: '1px solid var(--color-border)',
                          }}
                        >
                          {item.productName || 'Product'} × {item.quantity}
                        </span>
                      ))}
                      {order.items.length > 3 && (
                        <span style={{ color: 'var(--color-text-muted)' }}>
                          +{order.items.length - 3} more items
                        </span>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </Container>
    </div>
  );
}
