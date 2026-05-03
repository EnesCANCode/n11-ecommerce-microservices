import { useState, useEffect } from 'react';
import { FiPlus, FiPackage } from 'react-icons/fi';
import toast from 'react-hot-toast';
import api from '../lib/api';
import './SellerPanel.css';

export default function SellerPanel({ user }) {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', price: '', categoryId: '', imageUrl: '' });

  useEffect(() => {
    if (!user) return;
    api.get(`/api/v1/products/seller/${user.id}?page=0&size=50`)
      .then((res) => setProducts(res.data?.data?.content || []))
      .catch(() => {});
    api.get('/api/v1/categories')
      .then((res) => setCategories(res.data?.data || []))
      .catch(() => {});
  }, [user]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/v1/products', {
        ...form,
        price: parseFloat(form.price),
      });
      toast.success('Ürün eklendi!');
      setShowForm(false);
      setForm({ name: '', description: '', price: '', categoryId: '', imageUrl: '' });
      const res = await api.get(`/api/v1/products/seller/${user.id}?page=0&size=50`);
      setProducts(res.data?.data?.content || []);
    } catch {
      toast.error('Ürün eklenemedi');
    }
  };

  if (!user) {
    return (
      <div className="container empty-state" style={{ paddingTop: 80 }}>
        <span className="empty-icon">🏪</span>
        <h3>Giriş yapın</h3>
        <p>Mağaza paneline erişmek için giriş yapın.</p>
      </div>
    );
  }

  return (
    <div className="seller-page container animate-fade-in">
      <div className="seller-header">
        <h1 className="page-title"><FiPackage /> Mağazam</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          <FiPlus /> Ürün Ekle
        </button>
      </div>

      {showForm && (
        <form className="seller-form card animate-fade-in" onSubmit={handleSubmit}>
          <input className="input" placeholder="Ürün Adı" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <textarea className="input" placeholder="Açıklama" rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <input className="input" type="number" step="0.01" placeholder="Fiyat (TL)" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
          <select className="input" value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
            <option value="">Kategori Seçin</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <input className="input" placeholder="Görsel URL (opsiyonel)" value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
          <button className="btn btn-accent" type="submit">Ürünü Kaydet</button>
        </form>
      )}

      <div className="seller-products">
        {products.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">📦</span>
            <h3>Henüz ürün eklenmemiş</h3>
          </div>
        ) : (
          <table className="seller-table">
            <thead>
              <tr><th>Ürün</th><th>Kategori</th><th>Fiyat</th><th>Durum</th></tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>{p.categoryName}</td>
                  <td>{new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(p.price)}</td>
                  <td><span className={`badge ${p.active ? 'badge-success' : 'badge-danger'}`}>{p.active ? 'Aktif' : 'Pasif'}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
