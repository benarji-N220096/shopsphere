import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Package,
  Boxes,
  ClipboardList,
  AlertTriangle,
  CheckCircle,
  Clock,
  XCircle,
  TrendingUp,
  RefreshCw,
  ExternalLink,
  ShieldAlert,
} from 'lucide-react';
import { productApi } from '../../services/api/productApi';
import { orderApi } from '../../services/api/orderApi';
import { inventoryApi } from '../../services/api/inventoryApi';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';
import { formatCurrency } from '../../utils/formatters';

export function AdminDashboardPage() {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [inventoryMap, setInventoryMap] = useState({});
  const [recovering, setRecovering] = useState(false);
  const [recoveryNotice, setRecoveryNotice] = useState(null);

  const loadData = async () => {
    try {
      setError(null);
      const [fetchedProducts, fetchedOrders] = await Promise.all([
        productApi.getAllProducts().catch((e) => {
          console.error('Failed to fetch products', e);
          return [];
        }),
        orderApi.getAllOrders().catch((e) => {
          console.error('Failed to fetch orders', e);
          return [];
        }),
      ]);

      setProducts(fetchedProducts || []);
      setOrders(fetchedOrders || []);

      // Fetch inventory details for products in parallel
      const invMap = {};
      if (fetchedProducts && fetchedProducts.length > 0) {
        await Promise.allSettled(
          fetchedProducts.slice(0, 30).map(async (p) => {
            try {
              const inv = await inventoryApi.getInventory(p.id);
              if (inv) invMap[p.id] = inv;
            } catch {
              // fallback to product.stockQuantity if inventory record absent
              invMap[p.id] = { availableQuantity: p.stockQuantity, quantity: p.stockQuantity };
            }
          })
        );
      }
      setInventoryMap(invMap);
    } catch (err) {
      setError(err.message || 'Failed to load platform telemetry and metrics');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRefresh = () => {
    setRefreshing(true);
    loadData();
  };

  const handleBatchRecovery = async () => {
    setRecovering(true);
    setRecoveryNotice(null);
    try {
      await orderApi.recoverAllPending();
      setRecoveryNotice({
        type: 'success',
        message: 'Batch recovery cycle completed successfully. Order statuses refreshed.',
      });
      await loadData();
    } catch (err) {
      setRecoveryNotice({
        type: 'error',
        message: `Recovery cycle failed: ${err.message || 'Service unavailable'}`,
      });
    } finally {
      setRecovering(false);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '4rem 0', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Aggregating platform telemetry and metrics..." size="lg" />
      </div>
    );
  }

  // Derive Platform Metrics
  const totalProducts = products.length;
  const totalOrders = orders.length;

  const confirmedOrders = orders.filter((o) => o.status === 'CONFIRMED');
  const pendingOrders = orders.filter((o) => o.status === 'PENDING');
  const cancelledOrders = orders.filter((o) => o.status === 'CANCELLED');

  const totalGrossRevenue = confirmedOrders.reduce(
    (sum, o) => sum + (Number(o.totalAmount) || 0),
    0
  );

  // Compute low stock items (< 10 threshold)
  const lowStockThreshold = 10;
  const lowStockProducts = products.filter((p) => {
    const inv = inventoryMap[p.id];
    const qty = inv ? (inv.availableQuantity ?? inv.quantity) : (p.stockQuantity ?? 0);
    return qty <= lowStockThreshold;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      {/* Top Banner & Quick Actions */}
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
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
        }}
      >
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
            Platform Overview & Operations
          </h2>
          <p style={{ fontSize: '0.8125rem', color: '#64748b', margin: '4px 0 0' }}>
            Live microservices health, aggregate metrics, and order recovery controls.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '0.5rem 0.875rem',
              borderRadius: '8px',
              border: '1px solid #10b981',
              backgroundColor: 'rgba(16, 185, 129, 0.08)',
              color: '#047857',
              fontSize: '0.8125rem',
              fontWeight: 600,
            }}
            title="ShopSphere background recovery daemon autonomously reconciles stale PENDING orders every 60s"
          >
            <ShieldAlert size={16} color="#059669" />
            <span>Order Recovery Daemon: ACTIVE</span>
          </div>

          <button
            onClick={handleRefresh}
            disabled={refreshing}
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
              cursor: refreshing ? 'not-allowed' : 'pointer',
              transition: 'all 0.15s ease',
            }}
          >
            <RefreshCw size={15} className={refreshing ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Recovery notification banner */}
      {recoveryNotice && (
        <div
          style={{
            padding: '0.875rem 1.25rem',
            borderRadius: '8px',
            backgroundColor: recoveryNotice.type === 'success' ? '#ecfdf5' : '#fef2f2',
            border: `1px solid ${recoveryNotice.type === 'success' ? '#a7f3d0' : '#fecaca'}`,
            color: recoveryNotice.type === 'success' ? '#065f46' : '#991b1b',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <span>{recoveryNotice.message}</span>
          <button
            onClick={() => setRecoveryNotice(null)}
            style={{ background: 'transparent', border: 'none', cursor: 'pointer', fontWeight: 700 }}
          >
            ✕
          </button>
        </div>
      )}

      {/* Metric Cards Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '1.25rem',
        }}
      >
        {/* Products Metric */}
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#64748b' }}>Total Products</span>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                color: '#3b82f6',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Package size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#0f172a' }}>{totalProducts}</div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            <Link to="/admin/products" style={{ color: '#3b82f6', textDecoration: 'none', fontWeight: 600 }}>
              Manage Catalog →
            </Link>
          </div>
        </div>

        {/* Low Stock Metric */}
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#64748b' }}>Low Stock Alerts</span>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: lowStockProducts.length > 0 ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
                color: lowStockProducts.length > 0 ? '#ef4444' : '#10b981',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <AlertTriangle size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: lowStockProducts.length > 0 ? '#dc2626' : '#059669' }}>
            {lowStockProducts.length}
          </div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            <Link to="/admin/inventory" style={{ color: '#3b82f6', textDecoration: 'none', fontWeight: 600 }}>
              Inspect Stock levels →
            </Link>
          </div>
        </div>

        {/* Total Orders Metric */}
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#64748b' }}>Total Orders</span>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: 'rgba(99, 102, 241, 0.1)',
                color: '#6366f1',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <ClipboardList size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#0f172a' }}>{totalOrders}</div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            <Link to="/admin/orders" style={{ color: '#3b82f6', textDecoration: 'none', fontWeight: 600 }}>
              View All Orders →
            </Link>
          </div>
        </div>

        {/* Confirmed Orders Metric */}
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#64748b' }}>Confirmed Orders</span>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                color: '#10b981',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <CheckCircle size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#059669' }}>{confirmedOrders.length}</div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Revenue: <strong>{formatCurrency(totalGrossRevenue)}</strong>
          </div>
        </div>

        {/* Pending Orders Metric */}
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#64748b' }}>Pending Orders</span>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: 'rgba(245, 158, 11, 0.1)',
                color: '#f59e0b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Clock size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#d97706' }}>{pendingOrders.length}</div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Awaiting reconciliation or payment
          </div>
        </div>

        {/* Cancelled Orders Metric */}
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '1.25rem',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#64748b' }}>Cancelled Orders</span>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: 'rgba(100, 116, 139, 0.1)',
                color: '#64748b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <XCircle size={20} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#475569' }}>{cancelledOrders.length}</div>
          <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
            Declined, failed, or rolled back
          </div>
        </div>
      </div>

      {/* Two-Column Layout: Recent Orders & Inventory Alerts */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '1.5rem' }}>
        {/* Recent Orders Table */}
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            padding: '1.25rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '1rem',
            }}
          >
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
                Recent Orders
              </h3>
              <p style={{ fontSize: '0.75rem', color: '#64748b', margin: '2px 0 0' }}>
                Latest transactions placed through API Gateway
              </p>
            </div>
            <Link
              to="/admin/orders"
              style={{ fontSize: '0.8125rem', color: '#3b82f6', textDecoration: 'none', fontWeight: 600 }}
            >
              View all ({orders.length}) →
            </Link>
          </div>

          {orders.length === 0 ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: '#64748b', fontSize: '0.875rem' }}>
              No orders placed yet.
            </div>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8125rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid #e2e8f0', textAlign: 'left', color: '#64748b' }}>
                    <th style={{ padding: '0.5rem 0.75rem', fontWeight: 600 }}>Order ID</th>
                    <th style={{ padding: '0.5rem 0.75rem', fontWeight: 600 }}>Amount</th>
                    <th style={{ padding: '0.5rem 0.75rem', fontWeight: 600 }}>Status</th>
                    <th style={{ padding: '0.5rem 0.75rem', fontWeight: 600, textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.slice(0, 6).map((order) => {
                    const statusColor =
                      order.status === 'CONFIRMED'
                        ? { bg: '#ecfdf5', text: '#059669', border: '#a7f3d0' }
                        : order.status === 'PENDING'
                        ? { bg: '#fffbeb', text: '#d97706', border: '#fde68a' }
                        : { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };

                    return (
                      <tr key={order.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td style={{ padding: '0.625rem 0.75rem', fontFamily: 'monospace', fontWeight: 600 }}>
                          #{order.id.slice(0, 8)}...
                        </td>
                        <td style={{ padding: '0.625rem 0.75rem', fontWeight: 600, color: '#0f172a' }}>
                          {formatCurrency(order.totalAmount)}
                        </td>
                        <td style={{ padding: '0.625rem 0.75rem' }}>
                          <span
                            style={{
                              display: 'inline-block',
                              padding: '2px 8px',
                              borderRadius: '12px',
                              fontSize: '0.7rem',
                              fontWeight: 700,
                              backgroundColor: statusColor.bg,
                              color: statusColor.text,
                              border: `1px solid ${statusColor.border}`,
                            }}
                          >
                            {order.status}
                          </span>
                        </td>
                        <td style={{ padding: '0.625rem 0.75rem', textAlign: 'right' }}>
                          <Link
                            to={`/admin/orders/${order.id}`}
                            style={{
                              color: '#3b82f6',
                              textDecoration: 'none',
                              fontWeight: 600,
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '2px',
                            }}
                          >
                            <span>Inspect</span>
                            <ExternalLink size={12} />
                          </Link>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Inventory & Low Stock Alerts */}
        <div
          style={{
            backgroundColor: '#ffffff',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            padding: '1.25rem',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '1rem',
            }}
          >
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a', margin: 0 }}>
                Inventory & Stock Alerts
              </h3>
              <p style={{ fontSize: '0.75rem', color: '#64748b', margin: '2px 0 0' }}>
                Items below threshold (&le; {lowStockThreshold} units)
              </p>
            </div>
            <Link
              to="/admin/inventory"
              style={{ fontSize: '0.8125rem', color: '#3b82f6', textDecoration: 'none', fontWeight: 600 }}
            >
              Restock →
            </Link>
          </div>

          {lowStockProducts.length === 0 ? (
            <div
              style={{
                padding: '2.5rem 1rem',
                textAlign: 'center',
                backgroundColor: '#f8fafc',
                borderRadius: '8px',
                border: '1px dashed #cbd5e1',
              }}
            >
              <CheckCircle size={28} color="#10b981" style={{ margin: '0 auto 8px' }} />
              <div style={{ fontWeight: 600, color: '#0f172a', fontSize: '0.875rem' }}>
                All inventory levels are healthy!
              </div>
              <div style={{ color: '#64748b', fontSize: '0.75rem', marginTop: '4px' }}>
                No catalog items are currently below the low-stock alert threshold.
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {lowStockProducts.slice(0, 5).map((prod) => {
                const inv = inventoryMap[prod.id];
                const available = inv ? (inv.availableQuantity ?? inv.quantity) : (prod.stockQuantity ?? 0);
                const isOutOfStock = available <= 0;

                return (
                  <div
                    key={prod.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.75rem',
                      borderRadius: '8px',
                      backgroundColor: isOutOfStock ? '#fef2f2' : '#fffbeb',
                      border: `1px solid ${isOutOfStock ? '#fecaca' : '#fde68a'}`,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <div
                        style={{
                          width: '32px',
                          height: '32px',
                          borderRadius: '6px',
                          backgroundColor: '#ffffff',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          color: isOutOfStock ? '#ef4444' : '#d97706',
                        }}
                      >
                        <AlertTriangle size={16} />
                      </div>
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.8125rem', color: '#0f172a' }}>
                          {prod.name}
                        </div>
                        <div style={{ fontSize: '0.7rem', color: '#64748b' }}>
                          Category: {prod.category || 'General'}
                        </div>
                      </div>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <span
                        style={{
                          display: 'inline-block',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontWeight: 700,
                          fontSize: '0.75rem',
                          backgroundColor: isOutOfStock ? '#ef4444' : '#f59e0b',
                          color: '#ffffff',
                        }}
                      >
                        {isOutOfStock ? 'OUT OF STOCK' : `${available} left`}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
