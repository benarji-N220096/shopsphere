import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  CheckCircle,
  Clock,
  XCircle,
  Package,
  Calendar,
  User,
  CreditCard,
  ShieldAlert,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';
import { orderApi } from '../../services/api/orderApi';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { formatCurrency } from '../../utils/formatters';
import { getProductImage } from '../../utils/productImages';

export function AdminOrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [recovering, setRecovering] = useState(false);
  const [recoveryMessage, setRecoveryMessage] = useState(null);

  const loadOrderDetail = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await orderApi.getOrderById(orderId);
      setOrder(data);
    } catch (err) {
      setError(err.data?.message || err.message || 'Failed to retrieve order inspection details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (orderId) {
      loadOrderDetail();
    }
  }, [orderId]);

  const handleRecover = async () => {
    setRecovering(true);
    setRecoveryMessage(null);
    try {
      await orderApi.recoverOrder(orderId);
      setRecoveryMessage({
        type: 'success',
        text: 'Targeted recovery triggered successfully! Status updated.',
      });
      await loadOrderDetail();
    } catch (err) {
      setRecoveryMessage({
        type: 'error',
        text: `Targeted recovery failed: ${err.message || 'Service unavailable'}`,
      });
    } finally {
      setRecovering(false);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '4rem 0', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Retrieving complete order and saga details..." size="lg" />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '12px',
          border: '1px solid #fee2e2',
          padding: '2.5rem',
          textAlign: 'center',
          maxWidth: '540px',
          margin: '2rem auto',
        }}
      >
        <AlertCircle size={40} color="#ef4444" style={{ margin: '0 auto 1rem' }} />
        <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: '#0f172a', margin: '0 0 0.5rem' }}>
          Order Not Found or Inaccessible
        </h3>
        <p style={{ color: '#64748b', fontSize: '0.875rem', marginBottom: '1.5rem' }}>
          {error || `Unable to locate order with ID "${orderId}".`}
        </p>
        <button
          onClick={() => navigate('/admin/orders')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: '6px',
            backgroundColor: '#3b82f6',
            color: '#ffffff',
            border: 'none',
            fontSize: '0.875rem',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Return to Orders Table
        </button>
      </div>
    );
  }

  const isConfirmed = order.status === 'CONFIRMED';
  const isPending = order.status === 'PENDING';
  const isCancelled = order.status === 'CANCELLED';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Back button & Title */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button
          onClick={() => navigate('/admin/orders')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.375rem 0.75rem',
            borderRadius: '6px',
            border: '1px solid #cbd5e1',
            backgroundColor: '#ffffff',
            color: '#475569',
            fontSize: '0.8125rem',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          <ArrowLeft size={16} />
          <span>Back to All Orders</span>
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          {isPending && (
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.375rem 0.875rem',
                borderRadius: '6px',
                border: '1px solid #f59e0b',
                backgroundColor: 'rgba(245, 158, 11, 0.08)',
                color: '#b45309',
                fontSize: '0.8125rem',
                fontWeight: 600,
              }}
              title="ShopSphere background recovery scheduler continually reconciles stale pending orders"
            >
              <ShieldAlert size={15} />
              <span>Saga Recovery: In Progress</span>
            </div>
          )}

          <button
            onClick={loadOrderDetail}
            style={{
              padding: '0.375rem 0.625rem',
              borderRadius: '6px',
              border: '1px solid #cbd5e1',
              backgroundColor: '#ffffff',
              color: '#475569',
              cursor: 'pointer',
            }}
            title="Refresh order"
          >
            <RefreshCw size={15} />
          </button>
        </div>
      </div>

      {/* Recovery notification message */}
      {recoveryMessage && (
        <div
          style={{
            padding: '0.875rem 1.25rem',
            borderRadius: '8px',
            backgroundColor: recoveryMessage.type === 'success' ? '#ecfdf5' : '#fef2f2',
            border: `1px solid ${recoveryMessage.type === 'success' ? '#a7f3d0' : '#fecaca'}`,
            color: recoveryMessage.type === 'success' ? '#065f46' : '#991b1b',
            fontSize: '0.875rem',
          }}
        >
          {recoveryMessage.text}
        </div>
      )}

      {/* Header Summary Card */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
          padding: '1.5rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
        }}
      >
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1rem',
            borderBottom: '1px solid #f1f5f9',
            paddingBottom: '1rem',
            marginBottom: '1rem',
          }}
        >
          <div>
            <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#64748b' }}>
              Order Inspection Record
            </div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a', margin: '2px 0 0', fontFamily: 'monospace' }}>
              #{order.id}
            </h2>
          </div>

          <div>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '6px 14px',
                borderRadius: '20px',
                fontSize: '0.8125rem',
                fontWeight: 700,
                backgroundColor: isConfirmed ? '#ecfdf5' : isPending ? '#fffbeb' : '#f1f5f9',
                color: isConfirmed ? '#059669' : isPending ? '#d97706' : '#475569',
                border: `1px solid ${isConfirmed ? '#a7f3d0' : isPending ? '#fde68a' : '#cbd5e1'}`,
              }}
            >
              {isConfirmed && <CheckCircle size={14} />}
              {isPending && <Clock size={14} />}
              {isCancelled && <XCircle size={14} />}
              {order.status}
            </span>
          </div>
        </div>

        {/* Metadata Details Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '1rem',
            fontSize: '0.875rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: '#64748b', fontSize: '0.75rem', marginBottom: '4px' }}>
              <User size={13} />
              <span>Customer UUID</span>
            </div>
            <div style={{ fontFamily: 'monospace', fontSize: '0.8125rem', color: '#0f172a', wordBreak: 'break-all' }}>
              {order.userId || 'N/A'}
            </div>
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: '#64748b', fontSize: '0.75rem', marginBottom: '4px' }}>
              <Calendar size={13} />
              <span>Created Timestamp</span>
            </div>
            <div style={{ color: '#0f172a' }}>
              {order.createdAt ? new Date(order.createdAt).toLocaleString() : 'N/A'}
            </div>
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: '#64748b', fontSize: '0.75rem', marginBottom: '4px' }}>
              <Clock size={13} />
              <span>Last Modified</span>
            </div>
            <div style={{ color: '#0f172a' }}>
              {order.updatedAt ? new Date(order.updatedAt).toLocaleString() : 'Unchanged'}
            </div>
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: '#64748b', fontSize: '0.75rem', marginBottom: '4px' }}>
              <CreditCard size={13} />
              <span>Total Settlement</span>
            </div>
            <div style={{ fontSize: '1.125rem', fontWeight: 700, color: '#0f172a' }}>
              {formatCurrency(order.totalAmount)}
            </div>
          </div>
        </div>
      </div>

      {/* Line Items Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid #e2e8f0' }}>
          <h3 style={{ fontSize: '0.9375rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
            Order Line Items & Snapshots ({order.items?.length || 0})
          </h3>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', textAlign: 'left', color: '#475569' }}>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Product Name & ID</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Quantity</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Unit Price</th>
                <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {order.items?.map((item) => (
                <tr key={item.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '0.875rem 1rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <img
                        src={getProductImage({ name: item.productName, category: '' })}
                        alt={item.productName}
                        style={{
                          width: '40px',
                          height: '40px',
                          borderRadius: '6px',
                          objectFit: 'cover',
                          border: '1px solid #e2e8f0',
                          flexShrink: 0,
                        }}
                      />
                      <div>
                        <div style={{ fontWeight: 600, color: '#0f172a' }}>{item.productName}</div>
                        <div style={{ fontSize: '0.75rem', fontFamily: 'monospace', color: '#94a3b8' }}>
                          PID: {item.productId}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td style={{ padding: '0.875rem 1rem', color: '#334155' }}>
                    {item.quantity}
                  </td>
                  <td style={{ padding: '0.875rem 1rem', color: '#334155' }}>
                    {formatCurrency(item.unitPrice)}
                  </td>
                  <td style={{ padding: '0.875rem 1rem', fontWeight: 700, color: '#0f172a', textAlign: 'right' }}>
                    {formatCurrency(item.subtotal)}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr style={{ backgroundColor: '#f8fafc' }}>
                <td colSpan={3} style={{ padding: '0.875rem 1rem', fontWeight: 700, color: '#0f172a', textAlign: 'right' }}>
                  Order Total:
                </td>
                <td style={{ padding: '0.875rem 1rem', fontWeight: 700, fontSize: '1rem', color: '#0f172a', textAlign: 'right' }}>
                  {formatCurrency(order.totalAmount)}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  );
}
