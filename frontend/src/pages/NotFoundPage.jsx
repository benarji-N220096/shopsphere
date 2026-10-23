import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div style={{ padding: 'var(--space-16) 0' }}>
      <Container>
        <div
          className="card"
          style={{
            maxWidth: '520px',
            margin: '0 auto',
            textAlign: 'center',
            padding: 'var(--space-12) var(--space-6)',
          }}
        >
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              backgroundColor: 'var(--color-primary-light)',
              color: 'var(--color-primary)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 'var(--space-4)',
            }}
          >
            <Compass size={32} />
          </div>

          <h1 style={{ fontSize: 'var(--font-size-3xl)', marginBottom: 'var(--space-2)' }}>
            404 - Page Not Found
          </h1>

          <p style={{ fontSize: 'var(--font-size-md)', color: 'var(--color-text-secondary)', marginBottom: 'var(--space-6)' }}>
            The page you are looking for might have been moved, removed, or never existed in the ShopSphere storefront.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: 'var(--space-4)' }}>
            <Button variant="primary" onClick={() => navigate('/')}>
              Return to Storefront
            </Button>
            <Button variant="outline" onClick={() => navigate('/products')}>
              Browse Catalog
            </Button>
          </div>
        </div>
      </Container>
    </div>
  );
}
