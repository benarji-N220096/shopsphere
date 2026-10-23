import { useState, useEffect, useCallback, useMemo } from 'react';
import { productApi } from '../services/api/productApi';

export function useProducts() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await productApi.getAllProducts();
      setProducts(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to fetch product catalog');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const categories = useMemo(() => {
    const set = new Set();
    products.forEach((p) => {
      if (p.category && typeof p.category === 'string' && p.category.trim() !== '') {
        set.add(p.category.trim());
      }
    });
    return Array.from(set).sort();
  }, [products]);

  return {
    products,
    loading,
    error,
    categories,
    refresh: fetchProducts,
  };
}
