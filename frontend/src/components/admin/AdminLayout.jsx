import React, { useState } from 'react';
import { Outlet, useLocation, Link } from 'react-router-dom';
import { AdminSidebar } from './AdminSidebar';
import { Menu, Shield, ExternalLink } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export function AdminLayout() {
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const location = useLocation();
  const { user } = useAuth();

  // Compute breadcrumb / page title from path
  const getPageTitle = (pathname) => {
    if (pathname === '/admin') return 'Admin Overview';
    if (pathname.startsWith('/admin/products')) return 'Product Management';
    if (pathname.startsWith('/admin/inventory')) return 'Inventory & Stock Control';
    if (pathname.startsWith('/admin/orders')) {
      if (pathname !== '/admin/orders') return 'Order Inspection';
      return 'Order Management';
    }
    return 'Admin Console';
  };

  return (
    <div className="admin-root" style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#f8fafc' }}>
      {/* Sidebar */}
      <AdminSidebar
        mobileOpen={mobileSidebarOpen}
        onClose={() => setMobileSidebarOpen(false)}
      />

      {/* Main Admin Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, overflowX: 'hidden' }}>
        {/* Admin Header / Top Bar */}
        <header
          style={{
            height: '64px',
            backgroundColor: '#ffffff',
            borderBottom: '1px solid #e2e8f0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 1.5rem',
            position: 'sticky',
            top: 0,
            zIndex: 900,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            {/* Mobile Hamburger Toggle */}
            <button
              onClick={() => setMobileSidebarOpen(true)}
              className="admin-hamburger"
              style={{
                display: 'none',
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                color: '#334155',
                padding: '6px',
                borderRadius: '6px',
              }}
              aria-label="Open navigation menu"
            >
              <Menu size={22} />
            </button>

            <div>
              <h1 style={{ fontSize: '1.125rem', fontWeight: 700, color: '#0f172a', margin: 0, lineHeight: 1.2 }}>
                {getPageTitle(location.pathname)}
              </h1>
              <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                ShopSphere Enterprise Control Plane
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <Link
              to="/"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.375rem',
                fontSize: '0.8125rem',
                color: '#3b82f6',
                textDecoration: 'none',
                fontWeight: 600,
                padding: '0.375rem 0.75rem',
                borderRadius: '6px',
                backgroundColor: 'rgba(59, 130, 246, 0.08)',
                transition: 'all 0.15s ease',
              }}
            >
              <span>View Storefront</span>
              <ExternalLink size={14} />
            </Link>

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.25rem 0.625rem',
                backgroundColor: '#f1f5f9',
                borderRadius: '20px',
                fontSize: '0.75rem',
                fontWeight: 600,
                color: '#334155',
              }}
            >
              <Shield size={14} color="#059669" />
              <span>{user?.email || 'admin@shopsphere.com'}</span>
            </div>
          </div>
        </header>

        {/* Content Body */}
        <main style={{ flex: 1, padding: '1.75rem', maxWidth: '1440px', width: '100%', margin: '0 auto' }}>
          <Outlet />
        </main>
      </div>

      {/* Responsive Styles for Admin UI */}
      <style>{`
        @media (max-width: 1024px) {
          .admin-sidebar {
            position: fixed !important;
            left: -260px;
            top: 0;
            bottom: 0;
            z-index: 1000 !important;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
          }
          .admin-sidebar.open {
            transform: translateX(260px);
          }
          .admin-sidebar .mobile-close-btn {
            display: block !important;
          }
          .admin-hamburger {
            display: flex !important;
            align-items: center;
            justify-content: center;
          }
        }
      `}</style>
    </div>
  );
}
