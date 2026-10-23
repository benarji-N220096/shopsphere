import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  ClipboardList,
  Search,
  RefreshCw,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  CheckCircle,
  Clock,
  XCircle,
  AlertCircle,
} from 'lucide-react';
import { orderApi } from '../../services/api/orderApi';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { formatCurrency } from '../../utils/formatters';

export function AdminOrdersPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  const [recoveringId, setRecoveringId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  const loadOrders = async () => {
    try {
      setLoading(true);
      const data = await orderApi.getAllOrders();
      setOrders(data || []);
    } catch (err) {
      setFeedback({ type: 'error', message: err.message || 'Failed to fetch platform orders' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
  }, []);

  const handleRecoverOrder = async (orderId) => {
    setRecoveringId(orderId);
    try {
      await orderApi.recoverOrder(orderId);
      setFeedback({
        type: 'success',
        message: `Recovery cycle triggered for Order #${orderId.slice(0, 8)}. Status updated.`,
      });
      await loadOrders();
    } catch (err) {
      setFeedback({
        type: 'error',
        message: `Recovery failed for Order #${orderId.slice(0, 8)}: ${err.message}`,
      });
    } finally {
      setRecoveringId(null);
    }
  };

  // Filter & Search
  const filteredOrders = orders.filter((o) => {
    const matchesSearch =
      o.id?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      o.userId?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || o.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  // Client-side pagination
  const totalPages = Math.ceil(filteredOrders.length / pageSize) || 1;
  const paginatedOrders = filteredOrders.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Page Header */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '1rem',
          backgroundColor: '#ffffff',
          padding: '1.25rem 1.5rem',
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
        }}
      >
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
            Order Management & Telemetry
          </h2>
          <p style={{ fontSize: '0.8125rem', color: '#64748b', margin: '4px 0 0' }}>
            Comprehensive overview of customer checkouts, payments, and saga reconciliation across all users.
          </p>
        </div>

        <button
          onClick={loadOrders}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.5rem 0.875rem',
            borderRadius: '8px',
            border: '1px solid #cbd5e1',
            backgroundColor: '#ffffff',
            color: '#334155',
            fontSize: '0.8125rem',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          <RefreshCw size={15} />
          <span>Refresh Orders</span>
        </button>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          style={{
            padding: '0.875rem 1.25rem',
            borderRadius: '8px',
            backgroundColor: feedback.type === 'success' ? '#ecfdf5' : '#fef2f2',
            border: `1px solid ${feedback.type === 'success' ? '#a7f3d0' : '#fecaca'}`,
            color: feedback.type === 'success' ? '#065f46' : '#991b1b',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {feedback.type === 'success' ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
            <span>{feedback.message}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            style={{ background: 'transparent', border: 'none', cursor: 'pointer', fontWeight: 700 }}
          >
            ✕
          </button>
        </div>
      )}

      {/* Search & Filter Bar */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          gap: '1rem',
          backgroundColor: '#ffffff',
          padding: '1rem',
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
        }}
      >
        <div style={{ position: 'relative', flex: 1, minWidth: '240px' }}>
          <Search
            size={16}
            style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }}
          />
          <input
            type="text"
            placeholder="Search by Order ID or Customer UUID..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setCurrentPage(1);
            }}
            style={{
              width: '100%',
              padding: '0.5rem 0.75rem 0.5rem 2.25rem',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '0.875rem',
              outline: 'none',
            }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#475569' }}>Status:</label>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setCurrentPage(1);
            }}
            style={{
              padding: '0.5rem 0.75rem',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '0.875rem',
              backgroundColor: '#ffffff',
            }}
          >
            <option value="ALL">All Statuses ({orders.length})</option>
            <option value="CONFIRMED">
              CONFIRMED ({orders.filter((o) => o.status === 'CONFIRMED').length})
            </option>
            <option value="PENDING">
              PENDING ({orders.filter((o) => o.status === 'PENDING').length})
            </option>
            <option value="CANCELLED">
              CANCELLED ({orders.filter((o) => o.status === 'CANCELLED').length})
            </option>
          </select>
        </div>
      </div>

      {/* Orders Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          overflow: 'hidden',
        }}
      >
        {loading ? (
          <div style={{ padding: '4rem 0', display: 'flex', justifyContent: 'center' }}>
            <LoadingSpinner text="Retrieving orders across platform..." size="lg" />
          </div>
        ) : filteredOrders.length === 0 ? (
          <div style={{ padding: '3.5rem 1rem', textAlign: 'center' }}>
            <ClipboardList size={36} color="#94a3b8" style={{ margin: '0 auto 8px' }} />
            <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#0f172a' }}>No orders found</h3>
            <p style={{ fontSize: '0.8125rem', color: '#64748b' }}>
              No orders matched your search criteria or status filter.
            </p>
          </div>
        ) : (
          <>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                <thead>
                  <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', textAlign: 'left', color: '#475569' }}>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Order ID</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Customer ID</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Date Placed</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Items</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Total Amount</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Status</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedOrders.map((order) => {
                    const isConfirmed = order.status === 'CONFIRMED';
                    const isPending = order.status === 'PENDING';
                    const isCancelled = order.status === 'CANCELLED';

                    const itemCount = order.items?.reduce((s, i) => s + (i.quantity || 1), 0) || order.items?.length || 0;

                    return (
                      <tr key={order.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td style={{ padding: '0.875rem 1rem', fontFamily: 'monospace', fontWeight: 600, color: '#0f172a' }}>
                          #{order.id.slice(0, 8)}...
                        </td>
                        <td style={{ padding: '0.875rem 1rem', fontFamily: 'monospace', fontSize: '0.75rem', color: '#64748b' }}>
                          {order.userId ? `${order.userId.slice(0, 8)}...` : 'Unknown'}
                        </td>
                        <td style={{ padding: '0.875rem 1rem', color: '#475569', fontSize: '0.8125rem' }}>
                          {order.createdAt ? new Date(order.createdAt).toLocaleString() : 'N/A'}
                        </td>
                        <td style={{ padding: '0.875rem 1rem', color: '#475569' }}>
                          {itemCount} {itemCount === 1 ? 'item' : 'items'}
                        </td>
                        <td style={{ padding: '0.875rem 1rem', fontWeight: 700, color: '#0f172a' }}>
                          {formatCurrency(order.totalAmount)}
                        </td>
                        <td style={{ padding: '0.875rem 1rem' }}>
                          <span
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '4px',
                              padding: '2px 8px',
                              borderRadius: '12px',
                              fontSize: '0.7rem',
                              fontWeight: 700,
                              backgroundColor: isConfirmed ? '#ecfdf5' : isPending ? '#fffbeb' : '#f1f5f9',
                              color: isConfirmed ? '#059669' : isPending ? '#d97706' : '#475569',
                              border: `1px solid ${isConfirmed ? '#a7f3d0' : isPending ? '#fde68a' : '#cbd5e1'}`,
                            }}
                          >
                            {isConfirmed && <CheckCircle size={11} />}
                            {isPending && <Clock size={11} />}
                            {isCancelled && <XCircle size={11} />}
                            {order.status}
                          </span>
                        </td>
                        <td style={{ padding: '0.875rem 1rem', textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
                            {isPending && (
                              <span
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '3px',
                                  padding: '3px 6px',
                                  borderRadius: '4px',
                                  backgroundColor: 'rgba(245, 158, 11, 0.1)',
                                  color: '#b45309',
                                  fontSize: '0.7rem',
                                  fontWeight: 600,
                                }}
                                title="Autonomous Order Recovery Daemon reconciles pending states"
                              >
                                <ShieldAlert size={11} />
                                <span>Reconciling</span>
                              </span>
                            )}
                            <Link
                              to={`/admin/orders/${order.id}`}
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '3px',
                                padding: '4px 8px',
                                borderRadius: '4px',
                                border: '1px solid #cbd5e1',
                                backgroundColor: '#ffffff',
                                color: '#3b82f6',
                                fontSize: '0.75rem',
                                fontWeight: 600,
                                textDecoration: 'none',
                              }}
                            >
                              <span>Inspect</span>
                              <ExternalLink size={12} />
                            </Link>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.75rem 1rem',
                  borderTop: '1px solid #e2e8f0',
                  backgroundColor: '#f8fafc',
                  fontSize: '0.8125rem',
                  color: '#64748b',
                }}
              >
                <div>
                  Showing {(currentPage - 1) * pageSize + 1} to{' '}
                  {Math.min(currentPage * pageSize, filteredOrders.length)} of {filteredOrders.length} orders
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <button
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                    disabled={currentPage === 1}
                    style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      border: '1px solid #cbd5e1',
                      backgroundColor: '#ffffff',
                      color: currentPage === 1 ? '#94a3b8' : '#334155',
                      cursor: currentPage === 1 ? 'not-allowed' : 'pointer',
                    }}
                  >
                    <ChevronLeft size={16} />
                  </button>
                  <span style={{ fontWeight: 600, color: '#0f172a' }}>
                    Page {currentPage} of {totalPages}
                  </span>
                  <button
                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                    disabled={currentPage === totalPages}
                    style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      border: '1px solid #cbd5e1',
                      backgroundColor: '#ffffff',
                      color: currentPage === totalPages ? '#94a3b8' : '#334155',
                      cursor: currentPage === totalPages ? 'not-allowed' : 'pointer',
                    }}
                  >
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
