import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FiTrendingUp, FiGrid, FiFilter } from 'react-icons/fi';
import api from '../lib/api';
import ProductCard from '../components/ProductCard';
import './Home.css';

export default function Home() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q');
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [sort, setSort] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/api/v1/categories')
      .then((res) => setCategories(res.data?.data || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    setLoading(true);
    let url;
    let sortQuery = sort ? `&sort=${sort}` : '';

    if (query) {
      url = `/api/v1/products/search?q=${encodeURIComponent(query)}&page=0&size=40${sortQuery}`;
    } else if (selectedCategory) {
      url = `/api/v1/products/category/${selectedCategory}?page=0&size=40${sortQuery}`;
    } else {
      url = `/api/v1/products?page=0&size=40${sortQuery}`;
    }

    api.get(url)
      .then((res) => {
        const data = res.data?.data;
        setProducts(data?.content || []);
      })
      .catch(() => setProducts([]))
      .finally(() => setLoading(false));
  }, [query, selectedCategory, sort]);

  return (
    <div className="home container animate-fade-in">
      {/* Karşılama (Hero) Alanı */}
      {!query && (
        <section className="hero">
          <div className="hero-content">
            <h1 className="hero-title">
              Alışverişin <span className="gradient-text">Yeni Adresi</span>
            </h1>
            <p className="hero-desc">
              Binlerce mağaza, milyonlarca ürün. Yapay zeka destekli arama ile aradığınızı anında bulun.
            </p>
          </div>
          <div className="hero-glow" />
        </section>
      )}

      {/* Kategoriler */}
      <section className="categories-section">
        <div className="categories-scroll">
          <button
            className={`category-chip ${!selectedCategory ? 'active' : ''}`}
            onClick={() => setSelectedCategory(null)}
          >
            <FiGrid /> Tümü
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              className={`category-chip ${selectedCategory === cat.id ? 'active' : ''}`}
              onClick={() => setSelectedCategory(cat.id)}
            >
              {cat.iconUrl || '📦'} {cat.name}
            </button>
          ))}
        </div>

        <div className="sort-actions">
          <button
            className={`sort-chip ${sort === 'price_asc' ? 'active' : ''}`}
            onClick={() => setSort(sort === 'price_asc' ? null : 'price_asc')}
          >
            Fiyat ↑
          </button>
          <button
            className={`sort-chip ${sort === 'price_desc' ? 'active' : ''}`}
            onClick={() => setSort(sort === 'price_desc' ? null : 'price_desc')}
          >
            Fiyat ↓
          </button>
        </div>
      </section>

      {/* Arama Sonuçları Başlığı */}
      {query && (
        <h2 className="search-title">
          <FiFilter /> "<span className="gradient-text">{query}</span>" için sonuçlar
        </h2>
      )}

      {/* Ürün Listesi */}
      {loading ? (
        <div className="grid-products">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="skeleton-card">
              <div className="skeleton" style={{ aspectRatio: '1', borderRadius: 'var(--radius-lg) var(--radius-lg) 0 0' }} />
              <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div className="skeleton" style={{ height: 16, width: '80%' }} />
                <div className="skeleton" style={{ height: 12, width: '50%' }} />
                <div className="skeleton" style={{ height: 20, width: '40%', marginTop: 8 }} />
              </div>
            </div>
          ))}
        </div>
      ) : products.length > 0 ? (
        <div className="grid-products">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <span className="empty-icon">🔍</span>
          <h3>Ürün bulunamadı</h3>
          <p>Farklı bir arama terimi deneyin veya kategori seçin.</p>
        </div>
      )}
    </div>
  );
}
