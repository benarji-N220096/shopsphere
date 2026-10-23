import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ServerCrash, RefreshCw, Home } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';

export function ServerErrorPage() {
  const navigate = useNavigate();

  return (
    <div style={{ padding: 'var(--space-16) 0' }}>
      <Container>
        <div
          className="card"
          style={{
            maxWidth: '540px',
            margin: '0 auto',
            textAlign: 'center',
            padding: 'var(--space-12) var(--space-8)',
            borderTop: '4px solid var(--color-warning)',
          }}
        >
          <div
            style={{
              width: '72px',
              height: '72px',
              borderRadius: '50%',
              backgroundColor: 'rgba(245, 158, 11, 0.1)',
              color: 'var(--color-warning)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 'var(--space-4)',
            }}
          >
            <ServerCrash size={38} />
          </div>

          <h1 style={{ fontSize: 'var(--font-size-3xl)', marginBottom: 'var(--space-2)', color: 'var(--color-text)' }}>
            500 — Server Error
          </h1>

          <p style={{ fontSize: 'var(--font-size-md)', color: 'var(--color-text-secondary)', marginBottom: 'var(--space-6)', lineHeight: 1.6 }}>
            The ShopSphere platform encountered an unexpected internal error or microservice connection issue. Our automated resilience circuits are active.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: 'var(--space-4)', flexWrap: 'wrap' }}>
            <Button variant="primary" onClick={() => window.location.reload()}>
              <RefreshCw size={18} style={{ marginRight: '6px' }} /> Retry Page
            </Button>
            <Button variant="outline" onClick={() => navigate('/')}>
              <Home size={18} style={{ marginRight: '6px' }} /> Return to Storefront
            </Button>
          </div>
        </div>
      </Container>
    </div>
  );
}
