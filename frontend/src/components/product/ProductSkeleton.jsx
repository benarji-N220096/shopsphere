import React from 'react';

export function ProductSkeleton() {
  return (
    <div
      className="card"
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        overflow: 'hidden',
        padding: 'var(--space-4)',
      }}
    >
      <div
        className="skeleton"
        style={{
          width: '100%',
          paddingTop: '80%',
          borderRadius: 'var(--radius-sm)',
          marginBottom: 'var(--space-4)',
        }}
      />
      <div
        className="skeleton"
        style={{
          width: '35%',
          height: '14px',
          marginBottom: 'var(--space-2)',
        }}
      />
      <div
        className="skeleton"
        style={{
          width: '85%',
          height: '18px',
          marginBottom: 'var(--space-2)',
        }}
      />
      <div
        className="skeleton"
        style={{
          width: '60%',
          height: '18px',
          marginBottom: 'var(--space-4)',
        }}
      />
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginTop: 'auto',
          paddingTop: 'var(--space-2)',
        }}
      >
        <div
          className="skeleton"
          style={{
            width: '30%',
            height: '24px',
          }}
        />
        <div
          className="skeleton"
          style={{
            width: '40%',
            height: '36px',
            borderRadius: 'var(--radius-sm)',
          }}
        />
      </div>
    </div>
  );
}
