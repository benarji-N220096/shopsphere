import React, { useState, useEffect } from 'react';
import {
  Boxes,
  Search,
  RefreshCw,
  PlusCircle,
  AlertTriangle,
  CheckCircle,
  AlertCircle,
  X,
  Package,
} from 'lucide-react';
import { productApi } from '../../services/api/productApi';
import { inventoryApi } from '../../services/api/inventoryApi';
import { LoadingSpinner } from '../../components/feedback/LoadingSpinner';

export function AdminInventoryPage() {
  const [products, setProducts] = useState([]);
  const [inventoryMap, setInventoryMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [stockFilter, setStockFilter] = useState('ALL'); // 'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'
  const [feedback, setFeedback] = useState(null);

  // Restock Modal
  const [activeItem, setActiveItem] = useState(null); // { product, inventory }
  const [newQuantity, setNewQuantity] = useState('');
  const [quantityError, setQuantityError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const prods = await productApi.getAllProducts();
      setProducts(prods || []);

      const invMap = {};
      if (prods && prods.length > 0) {
        await Promise.allSettled(
          prods.map(async (p) => {
            try {
              const inv = await inventoryApi.getInventory(p.id);
              if (inv) invMap[p.id] = inv;
            } catch {
              invMap[p.id] = {
                productId: p.id,
                quantity: p.stockQuantity ?? 0,
                availableQuantity: p.stockQuantity ?? 0,
                reservedQuantity: 0,
                isVirtual: true,
              };
            }
          })
        );
      }
      setInventoryMap(invMap);
    } catch (err) {
      setFeedback({ type: 'error', message: err.message || 'Failed to fetch inventory' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openRestockModal = (product, inv) => {
    setActiveItem({ product, inv });
    setNewQuantity(inv ? String(inv.quantity ?? product.stockQuantity ?? 0) : String(product.stockQuantity ?? 0));
    setQuantityError('');
  };

  const closeRestockModal = () => {
    setActiveItem(null);
    setNewQuantity('');
    setQuantityError('');
  };

  const handleUpdateStock = async (e) => {
    e.preventDefault();
    if (newQuantity === '' || isNaN(newQuantity) || Number(newQuantity) < 0 || !Number.isInteger(Number(newQuantity))) {
      setQuantityError('Stock quantity must be a non-negative whole number (0 or higher)');
      return;
    }

    setSubmitting(true);
    const qty = parseInt(newQuantity, 10);
    const productId = activeItem.product.id;

    try {
      if (activeItem.inv && !activeItem.inv.isVirtual) {
        await inventoryApi.updateInventory(productId, qty);
      } else {
        try {
          await inventoryApi.createInventory(productId, qty);
        } catch {
          await inventoryApi.updateInventory(productId, qty);
        }
      }

      // Also sync product.stockQuantity
      try {
        await productApi.updateProduct(productId, {
          name: activeItem.product.name,
          description: activeItem.product.description,
          price: activeItem.product.price,
          stockQuantity: qty,
          category: activeItem.product.category,
          imageUrl: activeItem.product.imageUrl,
          active: activeItem.product.active,
        });
      } catch {
        // non-critical
      }

      setFeedback({
        type: 'success',
        message: `Inventory stock updated for "${activeItem.product.name}" to ${qty} units.`,
      });
      closeRestockModal();
      await loadData();
    } catch (err) {
      setFeedback({
        type: 'error',
        message: err.data?.message || err.message || 'Failed to update stock quantity',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Filter products by stock and search
  const filteredProducts = products.filter((p) => {
    const inv = inventoryMap[p.id];
    const available = inv ? (inv.availableQuantity ?? inv.quantity ?? 0) : (p.stockQuantity ?? 0);

    const matchesSearch =
      p.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.id?.toLowerCase().includes(searchQuery.toLowerCase());

    let matchesFilter = true;
    if (stockFilter === 'OUT_OF_STOCK') matchesFilter = available <= 0;
    else if (stockFilter === 'LOW_STOCK') matchesFilter = available > 0 && available < 10;
    else if (stockFilter === 'IN_STOCK') matchesFilter = available >= 10;

    return matchesSearch && matchesFilter;
  });

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
            Inventory & Stock Control
          </h2>
          <p style={{ fontSize: '0.8125rem', color: '#64748b', margin: '4px 0 0' }}>
            Monitor real-time available vs. reserved units, restock products, and prevent stockouts.
          </p>
        </div>

        <button
          onClick={loadData}
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
          <span>Refresh Stock</span>
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

      {/* Search & Stock Filter */}
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
            placeholder="Search by product name or UUID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
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
            value={stockFilter}
            onChange={(e) => setStockFilter(e.target.value)}
            style={{
              padding: '0.5rem 0.75rem',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '0.875rem',
              backgroundColor: '#ffffff',
            }}
          >
            <option value="ALL">All Levels</option>
            <option value="IN_STOCK">In Stock (&ge; 10)</option>
            <option value="LOW_STOCK">Low Stock (1 - 9)</option>
            <option value="OUT_OF_STOCK">Out of Stock (0)</option>
          </select>
        </div>
      </div>

      {/* Inventory Table */}
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
            <LoadingSpinner text="Querying Inventory Service telemetry..." size="lg" />
          </div>
        ) : filteredProducts.length === 0 ? (
          <div style={{ padding: '3.5rem 1rem', textAlign: 'center' }}>
            <Boxes size={36} color="#94a3b8" style={{ margin: '0 auto 8px' }} />
            <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#0f172a' }}>No inventory records match</h3>
            <p style={{ fontSize: '0.8125rem', color: '#64748b' }}>
              Try clearing your search query or changing the stock filter.
            </p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', textAlign: 'left', color: '#475569' }}>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Product Name & ID</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Total Units</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Reserved Units</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Available Units</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Stock Health</th>
                  <th style={{ padding: '0.75rem 1rem', fontWeight: 600, textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredProducts.map((p) => {
                  const inv = inventoryMap[p.id];
                  const total = inv ? (inv.quantity ?? p.stockQuantity ?? 0) : (p.stockQuantity ?? 0);
                  const reserved = inv ? (inv.reservedQuantity ?? 0) : 0;
                  const available = inv ? (inv.availableQuantity ?? total - reserved) : total;

                  const isOut = available <= 0;
                  const isLow = available > 0 && available < 10;

                  return (
                    <tr key={p.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                      <td style={{ padding: '0.875rem 1rem' }}>
                        <div style={{ fontWeight: 600, color: '#0f172a' }}>{p.name}</div>
                        <div style={{ fontSize: '0.75rem', fontFamily: 'monospace', color: '#94a3b8' }}>
                          ID: {p.id}
                        </div>
                      </td>
                      <td style={{ padding: '0.875rem 1rem', fontWeight: 600, color: '#334155' }}>
                        {total}
                      </td>
                      <td style={{ padding: '0.875rem 1rem', color: reserved > 0 ? '#d97706' : '#64748b' }}>
                        {reserved} {reserved > 0 && <span style={{ fontSize: '0.7rem' }}>(In cart/checkout)</span>}
                      </td>
                      <td style={{ padding: '0.875rem 1rem', fontWeight: 700, color: isOut ? '#ef4444' : isLow ? '#d97706' : '#059669' }}>
                        {available}
                      </td>
                      <td style={{ padding: '0.875rem 1rem' }}>
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            padding: '3px 8px',
                            borderRadius: '12px',
                            fontSize: '0.7rem',
                            fontWeight: 700,
                            backgroundColor: isOut ? '#fee2e2' : isLow ? '#fef3c7' : '#ecfdf5',
                            color: isOut ? '#b91c1c' : isLow ? '#b45309' : '#047857',
                          }}
                        >
                          {isOut ? (
                            <>
                              <AlertTriangle size={12} /> OUT OF STOCK
                            </>
                          ) : isLow ? (
                            <>
                              <AlertTriangle size={12} /> LOW STOCK (&lt; 10)
                            </>
                          ) : (
                            <>
                              <CheckCircle size={12} /> IN STOCK
                            </>
                          )}
                        </span>
                      </td>
                      <td style={{ padding: '0.875rem 1rem', textAlign: 'right' }}>
                        <button
                          onClick={() => openRestockModal(p, inv)}
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            padding: '4px 10px',
                            borderRadius: '6px',
                            border: '1px solid #cbd5e1',
                            backgroundColor: '#ffffff',
                            color: '#3b82f6',
                            fontWeight: 600,
                            fontSize: '0.8125rem',
                            cursor: 'pointer',
                          }}
                        >
                          <PlusCircle size={14} />
                          <span>Adjust Stock</span>
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* RESTOCK / ADJUST STOCK MODAL */}
      {activeItem && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1100,
            padding: '1rem',
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '12px',
              maxWidth: '460px',
              width: '100%',
              padding: '1.5rem',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 700, margin: 0, color: '#0f172a' }}>
                Adjust Product Stock
              </h3>
              <button
                onClick={closeRestockModal}
                style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#64748b' }}
              >
                <X size={20} />
              </button>
            </div>

            <p style={{ fontSize: '0.875rem', color: '#475569', marginBottom: '1rem' }}>
              Updating inventory level for <strong>{activeItem.product.name}</strong>.
            </p>

            <form onSubmit={handleUpdateStock} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, color: '#334155', marginBottom: '4px' }}>
                  New Total Stock Quantity *
                </label>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={newQuantity}
                  onChange={(e) => setNewQuantity(e.target.value)}
                  placeholder="e.g. 100"
                  style={{
                    width: '100%',
                    padding: '0.5rem 0.75rem',
                    borderRadius: '6px',
                    border: `1px solid ${quantityError ? '#ef4444' : '#cbd5e1'}`,
                    fontSize: '0.875rem',
                  }}
                />
                {quantityError && (
                  <span style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '2px', display: 'block' }}>
                    {quantityError}
                  </span>
                )}
                <span style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px', display: 'block' }}>
                  Setting this will adjust available inventory units in real-time.
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  onClick={closeRestockModal}
                  style={{
                    padding: '0.5rem 1rem',
                    borderRadius: '6px',
                    border: '1px solid #cbd5e1',
                    backgroundColor: '#ffffff',
                    color: '#475569',
                    fontSize: '0.875rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  style={{
                    padding: '0.5rem 1.25rem',
                    borderRadius: '6px',
                    border: 'none',
                    backgroundColor: '#3b82f6',
                    color: '#ffffff',
                    fontSize: '0.875rem',
                    fontWeight: 600,
                    cursor: submitting ? 'not-allowed' : 'pointer',
                  }}
                >
                  {submitting ? 'Updating...' : 'Save Stock'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
