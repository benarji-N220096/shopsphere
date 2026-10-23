import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  Package, 
  ArrowLeft, 
  Calendar, 
  CreditCard, 
  ShieldCheck, 
  ShoppingBag,
  RefreshCw,
  Clock,
  CheckCircle2,
  FileText
} from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { OrderStatusBadge } from '../components/order/OrderStatusBadge';
import { PaymentStatusBadge } from '../components/order/PaymentStatusBadge';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorState } from '../components/feedback/ErrorState';
import { orderApi } from '../services/api/orderApi';
import { paymentApi } from '../services/api/paymentApi';
import { formatCurrency, formatDate } from '../utils/formatters';
import { getProductImage } from '../utils/productImages';

export function OrderDetailPage() {
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [payment, setPayment] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const fetchOrderDetails = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const orderData = await orderApi.getOrderById(orderId);
      setOrder(orderData);

      // Attempt to load associated payment record if route is available
      try {
        const paymentData = await paymentApi.getPaymentByOrderId(orderId);
        if (paymentData) {
          setPayment(paymentData);
        }
      } catch {
        // Payment info is optional/internal
      }
    } catch (err) {
      if (err.status === 404) {
        setErrorMessage('This order was not found or does not belong to your account.');
      } else if (err.status === 403) {
        setErrorMessage('You do not have permission to view this order.');
      } else {
        setErrorMessage(err.message || 'Failed to retrieve order details.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (orderId) {
      fetchOrderDetails();
    }
  }, [orderId]);

  if (isLoading) {
    return (
      <div style={{ padding: 'var(--space-16) 0', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Loading order details..." size="lg" />
      </div>
    );
  }

  if (errorMessage || !order) {
    return (
      <div style={{ padding: 'var(--space-12) 0' }}>
        <Container>
          <ErrorState
            title="Order Not Accessible"
            message={errorMessage || 'Order details could not be loaded.'}
            onRetry={fetchOrderDetails}
          />
          <div style={{ textAlign: 'center', marginTop: 'var(--space-6)' }}>
            <Link to="/orders">
              <Button variant="outline" size="md" icon={ArrowLeft}>
                Back to My Orders
              </Button>
            </Link>
          </div>
        </Container>
      </div>
    );
  }

  const items = order.items || [];
  const itemCount = items.reduce((acc, curr) => acc + (curr.quantity || 1), 0);

  return (
    <div style={{ padding: 'var(--space-8) 0 var(--space-16)' }}>
      <Container>
        {/* Navigation Breadcrumb */}
        <div style={{ marginBottom: 'var(--space-6)' }}>
          <Link
            to="/orders"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: 'var(--font-size-sm)',
              color: 'var(--color-text-secondary)',
              marginBottom: 'var(--space-3)',
            }}
          >
            <ArrowLeft size={16} /> Back to My Orders
          </Link>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <h1 style={{ fontSize: 'var(--font-size-3xl)', margin: 0 }}>Order Details</h1>
                <OrderStatusBadge status={order.status} />
              </div>
              <div style={{ fontFamily: 'monospace', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
                Order #{order.id}
              </div>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={fetchOrderDetails}
              icon={RefreshCw}
            >
              Refresh
            </Button>
          </div>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(0, 2fr) minmax(300px, 1fr)',
            gap: 'var(--space-6)',
            alignItems: 'start',
          }}
        >
          {/* Left Column: Order Items Table */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}>
            <div className="card" style={{ padding: 'var(--space-6)' }}>
              <h2 style={{ fontSize: 'var(--font-size-lg)', marginBottom: 'var(--space-4)', borderBottom: '1px solid var(--color-border)', paddingBottom: 'var(--space-3)' }}>
                Ordered Products ({itemCount} {itemCount === 1 ? 'unit' : 'units'})
              </h2>

              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: 'var(--font-size-sm)' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-muted)', fontSize: 'var(--font-size-xs)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                      <th style={{ padding: '8px 12px 12px 0' }}>Item</th>
                      <th style={{ padding: '8px 12px 12px', textAlign: 'right' }}>Price</th>
                      <th style={{ padding: '8px 12px 12px', textAlign: 'center' }}>Qty</th>
                      <th style={{ padding: '8px 0 12px 12px', textAlign: 'right' }}>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr key={item.id || item.productId} style={{ borderBottom: '1px solid var(--color-border)' }}>
                        <td style={{ padding: '16px 12px 16px 0' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <div
                              style={{
                                width: '40px',
                                height: '40px',
                                borderRadius: 'var(--radius-sm)',
                                overflow: 'hidden',
                                flexShrink: 0,
                                border: '1px solid var(--color-border-light)',
                              }}
                            >
                              <img
                                src={getProductImage({ name: item.productName })}
                                alt={item.productName || 'Product'}
                                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                              />
                            </div>
                            <div>
                              <div style={{ fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text)' }}>
                                {item.productName || 'Catalog Product'}
                              </div>
                              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', fontFamily: 'monospace' }}>
                                ID: {item.productId?.slice(0, 8)}...
                              </div>
                            </div>
                          </div>
                        </td>
                        <td style={{ padding: '16px 12px', textAlign: 'right', color: 'var(--color-text-secondary)' }}>
                          {formatCurrency(item.unitPrice || 0)}
                        </td>
                        <td style={{ padding: '16px 12px', textAlign: 'center', fontWeight: 'var(--font-weight-semibold)' }}>
                          {item.quantity}
                        </td>
                        <td style={{ padding: '16px 0 16px 12px', textAlign: 'right', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>
                          {formatCurrency(item.subtotal || ((item.unitPrice || 0) * (item.quantity || 1)))}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Totals Summary */}
              <div style={{ marginTop: 'var(--space-6)', borderTop: '1px solid var(--color-border)', paddingTop: 'var(--space-4)', display: 'flex', flexDirection: 'column', gap: '8px', maxWidth: '320px', marginLeft: 'auto' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
                  <span>Items Subtotal:</span>
                  <span>{formatCurrency(order.totalAmount || 0)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
                  <span>Standard Shipping:</span>
                  <span style={{ color: 'var(--color-accent)', fontWeight: 'var(--font-weight-semibold)' }}>FREE</span>
                </div>
                <div style={{ borderTop: '1px solid var(--color-border)', paddingTop: 'var(--space-2)', display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-base)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>
                  <span>Order Total:</span>
                  <span>{formatCurrency(order.totalAmount || 0)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column: Order Metadata & Payment Information */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-6)' }}>
            {/* Order Timestamps Card */}
            <div className="card" style={{ padding: 'var(--space-6)' }}>
              <h3 style={{ fontSize: 'var(--font-size-base)', marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Clock size={18} color="var(--color-primary)" /> Order Timeline
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', fontSize: 'var(--font-size-sm)' }}>
                <div>
                  <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', display: 'block' }}>Date Placed</span>
                  <span style={{ fontWeight: 'var(--font-weight-medium)' }}>
                    {formatDate(order.createdAt)}
                  </span>
                </div>
                {order.updatedAt && (
                  <div>
                    <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', display: 'block' }}>Last Updated</span>
                    <span style={{ fontWeight: 'var(--font-weight-medium)' }}>
                      {formatDate(order.updatedAt)}
                    </span>
                  </div>
                )}
                <div>
                  <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', display: 'block' }}>Fulfillment State</span>
                  <span style={{ fontWeight: 'var(--font-weight-semibold)', color: order.status === 'CONFIRMED' ? 'var(--color-success)' : 'var(--color-text)' }}>
                    {order.status === 'CONFIRMED' ? 'Confirmed & Reserved for Dispatch' : order.status}
                  </span>
                </div>
              </div>
            </div>

            {/* Payment & Security Card */}
            <div className="card" style={{ padding: 'var(--space-6)' }}>
              <h3 style={{ fontSize: 'var(--font-size-base)', marginBottom: 'var(--space-4)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <CreditCard size={18} color="var(--color-primary)" /> Payment & Transaction
              </h3>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', fontSize: 'var(--font-size-sm)' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--color-text-secondary)' }}>Payment Status</span>
                  <PaymentStatusBadge status={payment?.status || (order.status === 'CONFIRMED' ? 'SUCCESS' : order.status === 'CANCELLED' ? 'FAILED' : 'INITIATED')} />
                </div>

                {payment?.paymentMethod && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--color-text-secondary)' }}>Channel</span>
                    <span style={{ fontWeight: 'var(--font-weight-medium)' }}>{payment.paymentMethod}</span>
                  </div>
                )}

                {payment?.transactionId && (
                  <div>
                    <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', display: 'block' }}>Transaction ID</span>
                    <span style={{ fontFamily: 'monospace', fontSize: 'var(--font-size-xs)', wordBreak: 'break-all' }}>
                      {payment.transactionId}
                    </span>
                  </div>
                )}

                <div
                  style={{
                    marginTop: 'var(--space-3)',
                    padding: 'var(--space-3)',
                    backgroundColor: 'var(--color-surface-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: 'var(--font-size-xs)',
                    color: 'var(--color-text-muted)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                >
                  <ShieldCheck size={14} color="var(--color-accent)" /> Validated by ShopSphere Payment Gateway
                </div>
              </div>
            </div>
          </div>
        </div>
      </Container>
    </div>
  );
}
