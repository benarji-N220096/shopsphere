import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { ShoppingBag, User, LogOut, Menu, X, ShieldCheck } from 'lucide-react';
import { Container } from './Container';
import { SearchBar } from './SearchBar';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';

export function Header() {
  const { totalItemCount, openDrawer } = useCart();
  const { user, isAuthenticated, logout, isAdmin } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header
      style={{
        position: 'sticky',
        top: 0,
        zIndex: 'var(--z-sticky)',
        backgroundColor: 'var(--color-surface)',
        borderBottom: '1px solid var(--color-border)',
        boxShadow: 'var(--shadow-sm)',
      }}
    >
      {/* Top Banner */}
      <div
        style={{
          backgroundColor: 'var(--color-primary)',
          color: 'var(--color-text-inverse)',
          fontSize: 'var(--font-size-xs)',
          padding: '0.375rem 0',
          textAlign: 'center',
          fontWeight: 'var(--font-weight-medium)',
          letterSpacing: '0.02em',
        }}
      >
        <Container>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 'var(--space-2)' }}>
            <ShieldCheck size={14} color="#34D399" />
            <span>ShopSphere Enterprise Microservices Storefront — Real-Time Stock & Idempotent Payments</span>
          </div>
        </Container>
      </div>

      {/* Main Navigation Bar */}
      <Container>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            height: '4.25rem',
            gap: 'var(--space-6)',
          }}
        >
          {/* Brand Logo */}
          <Link
            to="/"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 'var(--space-2)',
              color: 'var(--color-primary)',
              fontWeight: 'var(--font-weight-extrabold)',
              fontSize: 'var(--font-size-2xl)',
              letterSpacing: '-0.03em',
              flexShrink: 0,
            }}
            aria-label="ShopSphere Home"
          >
            <div
              style={{
                width: '34px',
                height: '34px',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-primary)',
                color: 'var(--color-text-inverse)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 'bold',
                fontSize: '18px',
              }}
            >
              S
            </div>
            <span>ShopSphere</span>
          </Link>

          {/* Desktop Search Bar */}
          <div style={{ flex: '1 1 420px', maxWidth: '480px' }} className="header-search-container">
            <SearchBar placeholder="Search catalog..." />
          </div>

          {/* Desktop Navigation Links */}
          <nav
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 'var(--space-6)',
            }}
            className="desktop-nav"
          >
            <NavLink
              to="/"
              style={({ isActive }) => ({
                fontSize: 'var(--font-size-sm)',
                fontWeight: 'var(--font-weight-semibold)',
                color: isActive ? 'var(--color-secondary)' : 'var(--color-text-primary)',
                transition: 'color var(--transition-fast)',
              })}
            >
              Home
            </NavLink>

            <NavLink
              to="/products"
              style={({ isActive }) => ({
                fontSize: 'var(--font-size-sm)',
                fontWeight: 'var(--font-weight-semibold)',
                color: isActive ? 'var(--color-secondary)' : 'var(--color-text-primary)',
                transition: 'color var(--transition-fast)',
              })}
            >
              Shop Catalog
            </NavLink>

            {isAuthenticated && (
              <NavLink
                to="/orders"
                style={({ isActive }) => ({
                  fontSize: 'var(--font-size-sm)',
                  fontWeight: 'var(--font-weight-semibold)',
                  color: isActive ? 'var(--color-secondary)' : 'var(--color-text-primary)',
                  transition: 'color var(--transition-fast)',
                })}
              >
                My Orders
              </NavLink>
            )}

            {isAdmin && (
              <NavLink
                to="/admin"
                style={{
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  color: '#ffffff',
                  backgroundColor: '#0f172a',
                  padding: '4px 10px',
                  borderRadius: '16px',
                  textDecoration: 'none',
                  border: '1px solid #334155',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <ShieldCheck size={13} color="#60a5fa" />
                <span>Admin Console</span>
              </NavLink>
            )}

            {/* Cart Trigger */}
            <button
              onClick={openDrawer}
              style={{
                position: 'relative',
                display: 'flex',
                alignItems: 'center',
                padding: 'var(--space-2)',
                borderRadius: 'var(--radius-md)',
                color: 'var(--color-primary)',
                transition: 'background-color var(--transition-fast)',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-surface-subtle)')}
              onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
              aria-label={`View Cart, ${totalItemCount} items`}
            >
              <ShoppingBag size={22} />
              {totalItemCount > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: '-2px',
                    right: '-4px',
                    backgroundColor: 'var(--color-secondary)',
                    color: 'var(--color-text-inverse)',
                    fontSize: '11px',
                    fontWeight: 'bold',
                    borderRadius: 'var(--radius-full)',
                    minWidth: '18px',
                    height: '18px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '0 4px',
                  }}
                >
                  {totalItemCount}
                </span>
              )}
            </button>

            {/* User Account / Auth */}
            {isAuthenticated ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
                <span
                  style={{
                    fontSize: 'var(--font-size-sm)',
                    fontWeight: 'var(--font-weight-medium)',
                    color: 'var(--color-text-secondary)',
                  }}
                >
                  Hi, {user?.firstName || 'Customer'}
                </span>
                <button
                  onClick={handleLogout}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    color: 'var(--color-text-muted)',
                    padding: '6px',
                    borderRadius: 'var(--radius-sm)',
                  }}
                  title="Sign out"
                  aria-label="Sign out"
                >
                  <LogOut size={18} />
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                <Link
                  to="/login"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 'var(--space-1)',
                    fontSize: 'var(--font-size-sm)',
                    fontWeight: 'var(--font-weight-semibold)',
                    color: 'var(--color-primary)',
                    padding: '0.375rem 0.75rem',
                    borderRadius: 'var(--radius-sm)',
                  }}
                >
                  <User size={16} />
                  <span>Sign In</span>
                </Link>

                <Link
                  to="/register"
                  style={{
                    fontSize: 'var(--font-size-sm)',
                    fontWeight: 'var(--font-weight-semibold)',
                    color: 'var(--color-text-inverse)',
                    backgroundColor: 'var(--color-primary)',
                    padding: '0.375rem 0.875rem',
                    borderRadius: 'var(--radius-sm)',
                    transition: 'background-color var(--transition-fast)',
                  }}
                  onMouseEnter={(e) => (e.target.style.backgroundColor = 'var(--color-primary-hover)')}
                  onMouseLeave={(e) => (e.target.style.backgroundColor = 'var(--color-primary)')}
                >
                  Sign Up
                </Link>
              </div>
            )}
          </nav>

          {/* Mobile Menu Button */}
          <div className="mobile-menu-trigger" style={{ display: 'none' }}>
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              style={{
                color: 'var(--color-text-primary)',
                padding: 'var(--space-2)',
              }}
              aria-label="Toggle mobile menu"
            >
              {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>

        {/* Mobile Dropdown Menu */}
        {mobileMenuOpen && (
          <div
            style={{
              padding: 'var(--space-4) 0',
              borderTop: '1px solid var(--color-border)',
              display: 'flex',
              flexDirection: 'column',
              gap: 'var(--space-4)',
            }}
          >
            <SearchBar onSearch={() => setMobileMenuOpen(false)} />
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
              <Link
                to="/"
                onClick={() => setMobileMenuOpen(false)}
                style={{ fontSize: 'var(--font-size-md)', fontWeight: 'var(--font-weight-medium)' }}
              >
                Home
              </Link>
              <Link
                to="/products"
                onClick={() => setMobileMenuOpen(false)}
                style={{ fontSize: 'var(--font-size-md)', fontWeight: 'var(--font-weight-medium)' }}
              >
                Shop Catalog
              </Link>
              {isAuthenticated && (
                <Link
                  to="/orders"
                  onClick={() => setMobileMenuOpen(false)}
                  style={{ fontSize: 'var(--font-size-md)', fontWeight: 'var(--font-weight-medium)' }}
                >
                  My Orders
                </Link>
              )}
              <Link
                to="/cart"
                onClick={() => setMobileMenuOpen(false)}
                style={{ fontSize: 'var(--font-size-md)', fontWeight: 'var(--font-weight-medium)' }}
              >
                Shopping Cart ({totalItemCount})
              </Link>
              <hr style={{ border: 'none', borderTop: '1px solid var(--color-border)' }} />
              {isAuthenticated ? (
                <button
                  onClick={() => {
                    handleLogout();
                    setMobileMenuOpen(false);
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 'var(--space-2)',
                    fontSize: 'var(--font-size-md)',
                    color: 'var(--color-error)',
                    textAlign: 'left',
                  }}
                >
                  <LogOut size={18} />
                  <span>Sign Out</span>
                </button>
              ) : (
                <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
                  <Link
                    to="/login"
                    onClick={() => setMobileMenuOpen(false)}
                    style={{
                      flex: 1,
                      textAlign: 'center',
                      padding: 'var(--space-2)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-sm)',
                    }}
                  >
                    Sign In
                  </Link>
                  <Link
                    to="/register"
                    onClick={() => setMobileMenuOpen(false)}
                    style={{
                      flex: 1,
                      textAlign: 'center',
                      padding: 'var(--space-2)',
                      backgroundColor: 'var(--color-primary)',
                      color: 'var(--color-text-inverse)',
                      borderRadius: 'var(--radius-sm)',
                    }}
                  >
                    Sign Up
                  </Link>
                </div>
              )}
            </div>
          </div>
        )}
      </Container>

      <style>{`
        @media (max-width: 768px) {
          .header-search-container {
            display: none !important;
          }
          .desktop-nav {
            display: none !important;
          }
          .mobile-menu-trigger {
            display: block !important;
          }
        }
      `}</style>
    </header>
  );
}
