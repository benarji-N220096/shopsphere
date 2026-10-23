import React from 'react';
import { ProductCard } from './ProductCard';
import { ProductSkeleton } from './ProductSkeleton';

export function ProductGrid({ products = [], loading = false, skeletonCount = 4, className = '' }) {
  if (loading) {
    return (
      <div className={`product-grid ${className}`} style={gridStyles}>
        {Array.from({ length: skeletonCount }).map((_, index) => (
          <ProductSkeleton key={index} />
        ))}
        <style>{responsiveGridStyles}</style>
      </div>
    );
  }

  return (
    <div className={`product-grid ${className}`} style={gridStyles}>
      {products.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
      <style>{responsiveGridStyles}</style>
    </div>
  );
}

const gridStyles = {
  display: 'grid',
  gap: 'var(--space-6)',
  gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
};

const responsiveGridStyles = `
  @media (max-width: 640px) {
    .product-grid {
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)) !important;
      gap: var(--space-4) !important;
    }
  }
`;
