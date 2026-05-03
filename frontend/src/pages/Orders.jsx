import { useState, useEffect } from 'react';
import { FiPackage } from 'react-icons/fi';
import api from '../lib/api';
import './Orders.css';

const STATUS_MAP = {
  PENDING: { label: 'Beklemede', cls: 'badge-warning' },
  PAYMENT_PROCESSING: { label: 'Ödeme İşleniyor', cls: 'badge-warning' },
  PAYMENT_COMPLETED: { label: 'Ödeme Alındı', cls: 'badge-primary' },
  CONFIRMED: { label: 'Onaylandı', cls: 'badge-success' },
  CANCELLED: { label: 'İptal Edildi', cls: 'badge-danger' },
  DELIVERED: { label: 'Teslim Edildi', cls: 'badge-success' },
};

export default function Orders({ user }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return setLoading(false);
    api.get('/api/v1/orders?page=0&size=20')
      .then((res) => setOrders(res.data?.data?.content || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user]);

  if (!user) {
    return (
      <div className="container empty-state" style={{ paddingTop: 80 }}>
        <span className="empty-icon">📦</span>
        <h3>Giriş yapın</h3>
        <p>Siparişlerinizi görüntülemek için giriş yapın.</p>
      </div>
    );
  }

  return (
    <div className="orders-page container animate-fade-in">
      <h1 className="page-title"><FiPackage /> Siparişlerim</h1>
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {[1,2,3].map(i => <div key={i} className="skeleton" style={{ height: 100 }} />)}
        </div>
      ) : orders.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📦</span>
          <h3>Henüz sipariş yok</h3>
        </div>
      ) : (
        <div className="orders-list">
          {orders.map((order) => {
            const s = STATUS_MAP[order.status] || { label: order.status, cls: 'badge-primary' };
            return (
              <div key={order.id} className="order-card card">
                <div className="order-header">
                  <span className="order-id">#{order.id?.substring(0, 8)}</span>
                  <span className={`badge ${s.cls}`}>{s.label}</span>
                </div>
                <div className="order-items-list">
                  {order.items?.map((item, idx) => (
                    <div key={idx} className="order-item-row">
                      <span>{item.productName}</span>
                      <span>x{item.quantity}</span>
                      <span>{new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(item.price)}</span>
                    </div>
                  ))}
                </div>
                <div className="order-footer">
                  <span className="order-date">{new Date(order.createdAt).toLocaleDateString('tr-TR')}</span>
                  <span className="order-total">
                    {new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(order.totalAmount)}
                  </span>
                </div>
                {order.failureReason && <p className="order-failure">{order.failureReason}</p>}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
