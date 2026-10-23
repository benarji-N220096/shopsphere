import React, { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, ArrowLeft, Package, CheckCircle2, AlertTriangle, ShieldCheck, Truck, RefreshCw } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { ProductPrice } from '../components/product/ProductPrice';
import { QuantitySelector } from '../components/cart/QuantitySelector';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorState } from '../components/feedback/ErrorState';
import { useProductDetail } from '../hooks/useProductDetail';
import { useCart } from '../context/CartContext';
import { formatDate } from '../utils/formatters';
import { getProductImage } from '../utils/productImages';

export function ProductDetailPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const { product, loading, error, refresh } = useProductDetail(productId);
  const { addToCart } = useCart();
  const [quantity, setQuantity] = useState(1);
  const [imageError, setImageError] = useState(false);

  if (loading) {
    return (
      <div style={{ padding: 'var(--space-16) 0' }}>
        <Container>
          <LoadingSpinner size="lg" label="Retrieving product details..." />
        </Container>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div style={{ padding: 'var(--space-12) 0' }}>
        <Container>
          <ErrorState
            title="Product Not Found"
            message={error || "We couldn't locate the specified product in the catalog."}
            onRetry={() => navigate('/products')}
          />
        </Container>
      </div>
    );
  }

  const isOutOfStock = product.stockQuantity === 0;
  const maxStock = Math.min(product.stockQuantity || 1, 15);

  const handleAddToCart = () => {
    if (!isOutOfStock) {
      addToCart(product, quantity);
    }
  };

  return (
    <div style={{ padding: 'var(--space-8) 0 var(--space-16)' }}>
      <Container>
        {/* Breadcrumb Navigation */}
        <nav
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: 'var(--font-size-sm)',
            color: 'var(--color-text-muted)',
            marginBottom: 'var(--space-6)',
          }}
          aria-label="Breadcrumbs"
        >
          <Link to="/" style={{ color: 'var(--color-text-secondary)' }}>Home</Link>
          <span>/</span>
          <Link to="/products" style={{ color: 'var(--color-text-secondary)' }}>Products</Link>
          {product.category && (
            <>
              <span>/</span>
              <Link
                to={`/products?category=${encodeURIComponent(product.category)}`}
                style={{ color: 'var(--color-text-secondary)' }}
              >
                {product.category}
              </Link>
            </>
          )}
          <span>/</span>
          <span style={{ color: 'var(--color-text-primary)', fontWeight: 'var(--font-weight-medium)' }}>
            {product.name}
          </span>
        </nav>

        {/* Product Details Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: 'var(--space-12)',
            alignItems: 'start',
          }}
        >
          {/* Left Column: Image Presentation */}
          <div
            className="card"
            style={{
              padding: 'var(--space-6)',
              backgroundColor: '#F8FAFC',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: '380px',
              borderRadius: 'var(--radius-lg)',
              overflow: 'hidden',
            }}
          >
            <img
              src={imageError ? '/images/products/headphones.jpg' : getProductImage(product)}
              alt={product.name}
              onError={() => setImageError(true)}
              style={{
                maxHeight: '400px',
                width: 'auto',
                maxWidth: '100%',
                objectFit: 'contain',
              }}
            />
          </div>

          {/* Right Column: Purchasing Actions & Information */}
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', gap: 'var(--space-2)', marginBottom: 'var(--space-3)' }}>
              {product.category && (
                <Badge variant="primary">{product.category}</Badge>
              )}
              {isOutOfStock ? (
                <Badge variant="error">Out of Stock</Badge>
              ) : (
                <Badge variant="success">In Stock ({product.stockQuantity} units)</Badge>
              )}
            </div>

            <h1
              style={{
                fontSize: 'var(--font-size-3xl)',
                fontWeight: 'var(--font-weight-bold)',
                lineHeight: 1.25,
                marginBottom: 'var(--space-3)',
              }}
            >
              {product.name}
            </h1>

            <div style={{ marginBottom: 'var(--space-6)' }}>
              <ProductPrice price={product.price} size="xl" />
            </div>

            <div
              style={{
                borderTop: '1px solid var(--color-border)',
                borderBottom: '1px solid var(--color-border)',
                padding: 'var(--space-6) 0',
                marginBottom: 'var(--space-6)',
              }}
            >
              <h3 style={{ fontSize: 'var(--font-size-sm)', textTransform: 'uppercase', color: 'var(--color-text-muted)', letterSpacing: '0.05em', marginBottom: 'var(--space-2)' }}>
                About this item
              </h3>
              <p style={{ fontSize: 'var(--font-size-md)', lineHeight: 1.6, color: 'var(--color-text-secondary)' }}>
                {product.description || 'No detailed description provided for this catalog item.'}
              </p>
            </div>

            {/* Quantity Selector & Add Button */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)', marginBottom: 'var(--space-8)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)' }}>
                <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-semibold)' }}>
                  Quantity:
                </span>
                <QuantitySelector
                  quantity={quantity}
                  max={maxStock}
                  onChange={setQuantity}
                  disabled={isOutOfStock}
                />
                {!isOutOfStock && product.stockQuantity < 10 && (
                  <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-warning)', fontWeight: 'var(--font-weight-medium)' }}>
                    Only {product.stockQuantity} available
                  </span>
                )}
              </div>

              <div style={{ display: 'flex', gap: 'var(--space-4)' }}>
                <Button
                  variant="primary"
                  size="lg"
                  icon={ShoppingCart}
                  disabled={isOutOfStock}
                  onClick={handleAddToCart}
                  style={{ flex: 1 }}
                >
                  {isOutOfStock ? 'Currently Unavailable' : 'Add to Shopping Bag'}
                </Button>
              </div>
            </div>

            {/* Trust and Technical Guarantees */}
            <div
              className="card"
              style={{
                padding: 'var(--space-4)',
                backgroundColor: 'var(--color-surface-subtle)',
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: 'var(--space-3)',
                fontSize: 'var(--font-size-xs)',
                color: 'var(--color-text-secondary)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ShieldCheck size={18} color="var(--color-primary)" />
                <span>Pessimistic stock lock guaranteed</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <CheckCircle2 size={18} color="var(--color-success)" />
                <span>Verified inventory item</span>
              </div>
            </div>

            {/* Technical Metadata (Placement/Recruiter Information) */}
            <div style={{ marginTop: 'var(--space-6)', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
              <p>System Product UUID: <code>{product.id}</code></p>
              {product.createdAt && <p>Registered: {formatDate(product.createdAt)}</p>}
            </div>
          </div>
        </div>
      </Container>
    </div>
  );
}
