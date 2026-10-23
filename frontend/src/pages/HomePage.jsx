import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ShieldCheck, Zap, RefreshCw, CheckCircle, Package } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { ProductGrid } from '../components/product/ProductGrid';
import { ErrorState } from '../components/feedback/ErrorState';
import { useProducts } from '../hooks/useProducts';

export function HomePage() {
  const { products, loading, error, categories, refresh } = useProducts();

  // Featured slice (first 4-6 products)
  const featuredProducts = products.slice(0, 4);

  return (
    <div>
      {/* Hero Section */}
      <section
        style={{
          backgroundColor: '#0F1E36',
          color: '#FFFFFF',
          padding: 'var(--space-16) 0 var(--space-12)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            position: 'absolute',
            top: 0,
            right: 0,
            width: '50%',
            height: '100%',
            background: 'radial-gradient(circle at 80% 20%, rgba(47, 117, 181, 0.15) 0%, transparent 60%)',
            pointerEvents: 'none',
          }}
        />

        <Container>
          <div style={{ maxWidth: '680px', position: 'relative', zIndex: 2 }}>
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                padding: '4px 12px',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'rgba(255, 255, 255, 0.08)',
                border: '1px solid rgba(255, 255, 255, 0.12)',
                fontSize: 'var(--font-size-xs)',
                fontWeight: 'var(--font-weight-medium)',
                color: '#93C5FD',
                marginBottom: 'var(--space-4)',
              }}
            >
              <Zap size={14} color="#FBBF24" />
              <span>Engineered on Distributed Microservices</span>
            </div>

            <h1
              style={{
                color: '#FFFFFF',
                fontSize: 'clamp(2rem, 5vw, 3.25rem)',
                fontWeight: 'var(--font-weight-extrabold)',
                lineHeight: 1.15,
                letterSpacing: '-0.03em',
                marginBottom: 'var(--space-4)',
              }}
            >
              Discover Quality Products Built for Everyday Life.
            </h1>

            <p
              style={{
                color: '#CBD5E1',
                fontSize: 'var(--font-size-lg)',
                lineHeight: 1.6,
                marginBottom: 'var(--space-8)',
              }}
            >
              ShopSphere delivers high-reliability e-commerce powered by real-time inventory reservations, idempotent payment pipelines, and zero-compromise security.
            </p>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
              <Link to="/products">
                <Button variant="secondary" size="lg" icon={ArrowRight} iconPosition="right">
                  Browse Catalog
                </Button>
              </Link>

              <Link to="/products?category=Electronics">
                <Button
                  variant="outline"
                  size="lg"
                  style={{ color: '#FFFFFF', borderColor: 'rgba(255, 255, 255, 0.25)' }}
                >
                  Explore Electronics
                </Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>

      {/* Category Discovery Pills */}
      {categories.length > 0 && (
        <section
          style={{
            backgroundColor: 'var(--color-surface)',
            borderBottom: '1px solid var(--color-border)',
            padding: 'var(--space-4) 0',
          }}
        >
          <Container>
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', overflowX: 'auto', paddingBottom: '4px' }}>
              <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Categories:
              </span>
              <Link
                to="/products"
                style={{
                  padding: '6px 14px',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor: 'var(--color-primary-light)',
                  color: 'var(--color-primary)',
                  fontSize: 'var(--font-size-sm)',
                  fontWeight: 'var(--font-weight-semibold)',
                  whiteSpace: 'nowrap',
                }}
              >
                All Items
              </Link>
              {categories.map((cat) => (
                <Link
                  key={cat}
                  to={`/products?category=${encodeURIComponent(cat)}`}
                  style={{
                    padding: '6px 14px',
                    borderRadius: 'var(--radius-full)',
                    backgroundColor: 'var(--color-surface-subtle)',
                    color: 'var(--color-text-primary)',
                    border: '1px solid var(--color-border)',
                    fontSize: 'var(--font-size-sm)',
                    fontWeight: 'var(--font-weight-medium)',
                    whiteSpace: 'nowrap',
                    transition: 'all var(--transition-fast)',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.backgroundColor = 'var(--color-primary-light)';
                    e.currentTarget.style.borderColor = 'var(--color-secondary)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = 'var(--color-surface-subtle)';
                    e.currentTarget.style.borderColor = 'var(--color-border)';
                  }}
                >
                  {cat}
                </Link>
              ))}
            </div>
          </Container>
        </section>
      )}

      {/* Featured Products Section */}
      <section style={{ padding: 'var(--space-12) 0' }}>
        <Container>
          <div
            style={{
              display: 'flex',
              alignItems: 'flex-end',
              justifyContent: 'space-between',
              marginBottom: 'var(--space-6)',
              flexWrap: 'wrap',
              gap: 'var(--space-4)',
            }}
          >
            <div>
              <span
                style={{
                  fontSize: 'var(--font-size-xs)',
                  fontWeight: 'var(--font-weight-bold)',
                  color: 'var(--color-secondary)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  display: 'block',
                  marginBottom: 'var(--space-1)',
                }}
              >
                Featured Selection
              </span>
              <h2 style={{ fontSize: 'var(--font-size-3xl)' }}>Popular in Store</h2>
            </div>

            <Link
              to="/products"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: 'var(--font-size-sm)',
                fontWeight: 'var(--font-weight-semibold)',
                color: 'var(--color-secondary)',
              }}
            >
              <span>View All Products</span>
              <ArrowRight size={16} />
            </Link>
          </div>

          {error ? (
            <ErrorState
              title="Catalog Temporarily Unavailable"
              message={error}
              onRetry={refresh}
            />
          ) : (
            <ProductGrid
              products={featuredProducts}
              loading={loading}
              skeletonCount={4}
            />
          )}
        </Container>
      </section>

      {/* Value Propositions / Engineering Transparency Section */}
      <section
        style={{
          backgroundColor: 'var(--color-surface)',
          borderTop: '1px solid var(--color-border)',
          borderBottom: '1px solid var(--color-border)',
          padding: 'var(--space-12) 0',
        }}
      >
        <Container>
          <div style={{ textAlign: 'center', maxWidth: '600px', margin: '0 auto var(--space-10)' }}>
            <h2 style={{ fontSize: 'var(--font-size-2xl)', marginBottom: 'var(--space-2)' }}>
              Built for Enterprise Reliability
            </h2>
            <p style={{ fontSize: 'var(--font-size-sm)' }}>
              ShopSphere combines modern user experience with battle-tested microservice patterns.
            </p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
              gap: 'var(--space-6)',
            }}
          >
            <div className="card" style={{ padding: 'var(--space-6)' }}>
              <div
                style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-primary-light)',
                  color: 'var(--color-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: 'var(--space-4)',
                }}
              >
                <Package size={24} />
              </div>
              <h3 style={{ fontSize: 'var(--font-size-lg)', marginBottom: 'var(--space-2)' }}>
                Real-Time Stock Checks
              </h3>
              <p style={{ fontSize: 'var(--font-size-sm)' }}>
                Pessimistic locking and automated reservation prevents overselling and guarantees fulfillment consistency.
              </p>
            </div>

            <div className="card" style={{ padding: 'var(--space-6)' }}>
              <div
                style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-secondary-light)',
                  color: 'var(--color-secondary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: 'var(--space-4)',
                }}
              >
                <ShieldCheck size={24} />
              </div>
              <h3 style={{ fontSize: 'var(--font-size-lg)', marginBottom: 'var(--space-2)' }}>
                Zero-Trust Gateway
              </h3>
              <p style={{ fontSize: 'var(--font-size-sm)' }}>
                Every incoming request is verified via JWT authorization with internal headers stripped at the perimeter.
              </p>
            </div>

            <div className="card" style={{ padding: 'var(--space-6)' }}>
              <div
                style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-accent-light)',
                  color: 'var(--color-accent)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: 'var(--space-4)',
                }}
              >
                <RefreshCw size={24} />
              </div>
              <h3 style={{ fontSize: 'var(--font-size-lg)', marginBottom: 'var(--space-2)' }}>
                Automated Order Recovery
              </h3>
              <p style={{ fontSize: 'var(--font-size-sm)' }}>
                Scheduled reconciliation background jobs resolve any eventual consistency anomalies automatically.
              </p>
            </div>
          </div>
        </Container>
      </section>
    </div>
  );
}
