import React from 'react';

export function Container({ children, className = '', style = {} }) {
  return (
    <div className={`container ${className}`} style={style}>
      {children}
    </div>
  );
}
