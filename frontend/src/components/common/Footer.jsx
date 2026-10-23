import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldCheck, Cpu, Activity, RefreshCw } from 'lucide-react';
import { Container } from './Container';

export function Footer() {
  return (
    <footer
      style={{
        backgroundColor: '#0F172A',
        color: '#E2E8F0',
        paddingTop: 'var(--space-12)',
        paddingBottom: 'var(--space-8)',
        borderTop: '1px solid #1E293B',
      }}
    >
      <Container>
        {/* Architecture Highlights Bar */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: 'var(--space-6)',
            paddingBottom: 'var(--space-8)',
            marginBottom: 'var(--space-8)',
            borderBottom: '1px solid #1E293B',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--space-3)' }}>
            <Cpu size={24} color="#38BDF8" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <h4 style={{ color: '#FFFFFF', fontSize: 'var(--font-size-sm)', marginBottom: '4px' }}>
                Spring Cloud Architecture
              </h4>
              <p style={{ color: '#94A3B8', fontSize: 'var(--font-size-xs)' }}>
                Decoupled microservices coordinated via Netflix Eureka and Spring Cloud Gateway.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--space-3)' }}>
            <Activity size={24} color="#34D399" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <h4 style={{ color: '#FFFFFF', fontSize: 'var(--font-size-sm)', marginBottom: '4px' }}>
                Distributed Resilience
              </h4>
              <p style={{ color: '#94A3B8', fontSize: 'var(--font-size-xs)' }}>
                Resilience4j circuit breakers, bounded timeouts, and Prometheus metric telemetry.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--space-3)' }}>
            <RefreshCw size={24} color="#FBBF24" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <h4 style={{ color: '#FFFFFF', fontSize: 'var(--font-size-sm)', marginBottom: '4px' }}>
                Eventual Consistency
              </h4>
              <p style={{ color: '#94A3B8', fontSize: 'var(--font-size-xs)' }}>
                Phase 3 automated compensation and scheduled reconciliation for distributed orders.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--space-3)' }}>
            <ShieldCheck size={24} color="#818CF8" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <h4 style={{ color: '#FFFFFF', fontSize: 'var(--font-size-sm)', marginBottom: '4px' }}>
                Perimeter Security
              </h4>
              <p style={{ color: '#94A3B8', fontSize: 'var(--font-size-xs)' }}>
                JWT authentication, secret header stripping, and internal endpoint lockdown.
              </p>
            </div>
          </div>
        </div>

        {/* Footer Navigation Columns */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: 'var(--space-8)',
            marginBottom: 'var(--space-8)',
          }}
        >
          {/* Brand Info */}
          <div style={{ gridColumn: 'span 1' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: 'var(--space-3)' }}>
              <div
                style={{
                  width: '28px',
                  height: '28px',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: 'var(--color-secondary)',
                  color: '#FFFFFF',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 'bold',
                }}
              >
                S
              </div>
              <span style={{ fontSize: 'var(--font-size-lg)', fontWeight: 'bold', color: '#FFFFFF' }}>
                ShopSphere
              </span>
            </div>
            <p style={{ color: '#94A3B8', fontSize: 'var(--font-size-sm)', lineHeight: 1.6 }}>
              A high-reliability e-commerce platform built to demonstrate production microservices engineering and modern frontend standards.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h5 style={{ color: '#FFFFFF', fontSize: 'var(--font-size-sm)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 'var(--space-3)' }}>
              Storefront
            </h5>
            <ul style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
              <li>
                <Link to="/" style={{ color: '#94A3B8', fontSize: 'var(--font-size-sm)' }}>
                  Home
                </Link>
              </li>
              <li>
                <Link to="/products" style={{ color: '#94A3B8', fontSize: 'var(--font-size-sm)' }}>
                  All Products
                </Link>
              </li>
              <li>
                <Link to="/cart" style={{ color: '#94A3B8', fontSize: 'var(--font-size-sm)' }}>
                  Shopping Bag
                </Link>
              </li>
            </ul>
          </div>

          {/* Account */}
          <div>
            <h5 style={{ color: '#FFFFFF', fontSize: 'var(--font-size-sm)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 'var(--space-3)' }}>
              Account & Security
            </h5>
            <ul style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
              <li>
                <Link to="/login" style={{ color: '#94A3B8', fontSize: 'var(--font-size-sm)' }}>
                  Customer Sign In
                </Link>
              </li>
              <li>
                <Link to="/register" style={{ color: '#94A3B8', fontSize: 'var(--font-size-sm)' }}>
                  Register Account
                </Link>
              </li>
            </ul>
          </div>

          {/* Technology Info */}
          <div>
            <h5 style={{ color: '#FFFFFF', fontSize: 'var(--font-size-sm)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 'var(--space-3)' }}>
              Technical Stack
            </h5>
            <p style={{ color: '#94A3B8', fontSize: 'var(--font-size-xs)', lineHeight: 1.6 }}>
              Java 23 &bull; Spring Boot 4 &bull; MySQL &bull; OpenFeign &bull; Brave / Zipkin &bull; Micrometer &bull; React 19 &bull; Vite 8
            </p>
          </div>
        </div>

        {/* Bottom Copyright */}
        <div
          style={{
            paddingTop: 'var(--space-6)',
            borderTop: '1px solid #1E293B',
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: 'var(--font-size-xs)',
            color: '#64748B',
            gap: 'var(--space-4)',
          }}
        >
          <span>&copy; {new Date().getFullYear()} ShopSphere Microservices Platform. All rights reserved.</span>
          <span>Placement Engineering Portfolio Showcase</span>
        </div>
      </Container>
    </footer>
  );
}
