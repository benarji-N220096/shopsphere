import React, { useState, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Filter, SlidersHorizontal, Search, RefreshCw, X } from 'lucide-react';
import { Container } from '../components/common/Container';
import { ProductGrid } from '../components/product/ProductGrid';
import { EmptyState } from '../components/feedback/EmptyState';
import { ErrorState } from '../components/feedback/ErrorState';
import { Button } from '../components/common/Button';
import { useProducts } from '../hooks/useProducts';

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { products, loading, error, categories, refresh } = useProducts();

  const searchQuery = searchParams.get('search') || '';
  const selectedCategory = searchParams.get('category') || 'ALL';
  const [sortBy, setSortBy] = useState('featured');
  const [inStockOnly, setInStockOnly] = useState(false);

  // Filter and sort products on client side (safe and responsive)
  const filteredProducts = useMemo(() => {
    let result = [...products];

    // Search query filter (matches name, description, category)
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter(
        (p) =>
          p.name?.toLowerCase().includes(q) ||
          p.description?.toLowerCase().includes(q) ||
          p.category?.toLowerCase().includes(q)
      );
    }

    // Category filter
    if (selectedCategory !== 'ALL') {
      result = result.filter(
        (p) => p.category?.toLowerCase() === selectedCategory.toLowerCase()
      );
    }

    // In Stock filter
    if (inStockOnly) {
      result = result.filter((p) => p.stockQuantity > 0);
    }

    // Sorting
    switch (sortBy) {
      case 'price-asc':
        result.sort((a, b) => parseFloat(a.price) - parseFloat(b.price));
        break;
      case 'price-desc':
        result.sort((a, b) => parseFloat(b.price) - parseFloat(a.price));
        break;
      case 'name-asc':
        result.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
        break;
      case 'featured':
      default:
        // preserve natural database order
        break;
    }

    return result;
  }, [products, searchQuery, selectedCategory, sortBy, inStockOnly]);

  const handleCategoryChange = (cat) => {
    const next = new URLSearchParams(searchParams);
    if (cat === 'ALL') {
      next.delete('category');
    } else {
      next.set('category', cat);
    }
    setSearchParams(next);
  };

  const handleSearchChange = (e) => {
    const val = e.target.value;
    const next = new URLSearchParams(searchParams);
    if (val.trim()) {
      next.set('search', val);
    } else {
      next.delete('search');
    }
    setSearchParams(next);
  };

  const clearAllFilters = () => {
    setSearchParams({});
    setSortBy('featured');
    setInStockOnly(false);
  };

  const hasActiveFilters = searchQuery || selectedCategory !== 'ALL' || inStockOnly;

  return (
    <div style={{ padding: 'var(--space-8) 0' }}>
      <Container>
        {/* Page Header */}
        <div style={{ marginBottom: 'var(--space-6)' }}>
          <h1 style={{ fontSize: 'var(--font-size-3xl)', marginBottom: 'var(--space-1)' }}>
            Product Catalog
          </h1>
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
            Browse verified products backed by real inventory and pricing.
          </p>
        </div>

        {/* Filter & Sort Bar */}
        <div
          className="card"
          style={{
            padding: 'var(--space-4) var(--space-6)',
            marginBottom: 'var(--space-8)',
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 'var(--space-4)',
          }}
        >
          {/* Search Box */}
          <div style={{ position: 'relative', width: '280px', maxWidth: '100%' }}>
            <Search
              size={16}
              color="var(--color-text-muted)"
              style={{ position: 'absolute', left: '10px', top: '10px', pointerEvents: 'none' }}
            />
            <input
              type="text"
              value={searchQuery}
              onChange={handleSearchChange}
              placeholder="Search products..."
              style={{
                width: '100%',
                height: '36px',
                paddingLeft: '32px',
                paddingRight: '12px',
                fontSize: 'var(--font-size-sm)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: 'var(--color-surface)',
                outline: 'none',
              }}
            />
          </div>

          {/* Category Dropdown */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>
              Category:
            </span>
            <select
              value={selectedCategory}
              onChange={(e) => handleCategoryChange(e.target.value)}
              style={{
                height: '36px',
                padding: '0 12px',
                fontSize: 'var(--font-size-sm)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: 'var(--color-surface)',
                color: 'var(--color-text-primary)',
                outline: 'none',
                cursor: 'pointer',
              }}
              aria-label="Filter by category"
            >
              <option value="ALL">All Categories</option>
              {categories.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>

          {/* Sort By Dropdown */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>
              Sort:
            </span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              style={{
                height: '36px',
                padding: '0 12px',
                fontSize: 'var(--font-size-sm)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: 'var(--color-surface)',
                color: 'var(--color-text-primary)',
                outline: 'none',
                cursor: 'pointer',
              }}
              aria-label="Sort products"
            >
              <option value="featured">Featured / Default</option>
              <option value="price-asc">Price: Low to High</option>
              <option value="price-desc">Price: High to Low</option>
              <option value="name-asc">Name: A to Z</option>
            </select>
          </div>

          {/* In Stock Checkbox */}
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: 'var(--font-size-sm)' }}>
            <input
              type="checkbox"
              checked={inStockOnly}
              onChange={(e) => setInStockOnly(e.target.checked)}
              style={{ accentColor: 'var(--color-primary)' }}
            />
            <span>In Stock Only</span>
          </label>
        </div>

        {/* Results Metadata Bar */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: 'var(--space-6)',
          }}
        >
          <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-medium)', color: 'var(--color-text-secondary)' }}>
            {loading ? 'Loading catalog...' : `Showing ${filteredProducts.length} product${filteredProducts.length === 1 ? '' : 's'}`}
          </span>

          {hasActiveFilters && (
            <button
              onClick={clearAllFilters}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                fontSize: 'var(--font-size-xs)',
                color: 'var(--color-secondary)',
                fontWeight: 'var(--font-weight-semibold)',
              }}
            >
              <X size={14} />
              <span>Clear Filters</span>
            </button>
          )}
        </div>

        {/* Main Product Grid */}
        {error ? (
          <ErrorState
            title="Failed to Load Products"
            message={error}
            onRetry={refresh}
          />
        ) : !loading && filteredProducts.length === 0 ? (
          <EmptyState
            title="No Matching Products"
            description="Try changing your search term or category filters to find what you're looking for."
            actionLabel="Reset All Filters"
            onAction={clearAllFilters}
          />
        ) : (
          <ProductGrid
            products={filteredProducts}
            loading={loading}
            skeletonCount={8}
          />
        )}
      </Container>
    </div>
  );
}
