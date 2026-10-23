import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ShieldAlert, ArrowLeft, Home, LogOut } from 'lucide-react';
import { Container } from '../components/common/Container';
import { Button } from '../components/common/Button';
import { useAuth } from '../context/AuthContext';

export function ForbiddenPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const attemptedPath = location.state?.from || 'restricted area';

  return (
    <div style={{ padding: 'var(--space-16) 0' }}>
      <Container>
        <div
          className="card"
          style={{
            maxWidth: '560px',
            margin: '0 auto',
            textAlign: 'center',
            padding: 'var(--space-12) var(--space-8)',
            borderTop: '4px solid var(--color-error)',
          }}
        >
          <div
            style={{
              width: '72px',
              height: '72px',
              borderRadius: '50%',
              backgroundColor: 'rgba(239, 68, 68, 0.1)',
              color: 'var(--color-error)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 'var(--space-4)',
            }}
          >
            <ShieldAlert size={38} />
          </div>

          <h1 style={{ fontSize: 'var(--font-size-3xl)', marginBottom: 'var(--space-2)', color: 'var(--color-text)' }}>
            403 — Access Denied
          </h1>

          <p style={{ fontSize: 'var(--font-size-md)', color: 'var(--color-text-secondary)', marginBottom: 'var(--space-4)', lineHeight: 1.6 }}>
            You do not have administrative permissions (<code style={{ background: 'var(--color-surface-hover)', padding: '2px 6px', borderRadius: '4px', fontSize: '0.9em' }}>ROLE_ADMIN</code>) required to access <strong>{attemptedPath}</strong>.
          </p>

          {user && (
            <div
              style={{
                backgroundColor: 'var(--color-surface-hover)',
                padding: 'var(--space-3) var(--space-4)',
                borderRadius: 'var(--radius-md)',
                marginBottom: 'var(--space-6)',
                fontSize: 'var(--font-size-sm)',
                color: 'var(--color-text-secondary)',
                display: 'inline-block',
              }}
            >
              Signed in as: <strong>{user.email}</strong> (Roles: {user.roles?.join(', ') || 'CUSTOMER'})
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'center', gap: 'var(--space-3)', flexWrap: 'wrap' }}>
            <Button variant="primary" onClick={() => navigate('/')}>
              <Home size={18} style={{ marginRight: '6px' }} /> Return to Storefront
            </Button>
            <Button variant="outline" onClick={() => navigate(-1)}>
              <ArrowLeft size={18} style={{ marginRight: '6px' }} /> Go Back
            </Button>
            <Button
              variant="outline"
              onClick={() => {
                logout();
                navigate('/login');
              }}
            >
              <LogOut size={18} style={{ marginRight: '6px' }} /> Switch Account
            </Button>
          </div>
        </div>
      </Container>
    </div>
  );
}
