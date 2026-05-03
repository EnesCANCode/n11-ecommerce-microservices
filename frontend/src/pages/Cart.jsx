import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiTrash2, FiMinus, FiPlus, FiShoppingBag } from 'react-icons/fi';
import toast from 'react-hot-toast';
import api from '../lib/api';
import './Cart.css';

export default function Cart({ user }) {
  const [basket, setBasket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [paymentDetails, setPaymentDetails] = useState({
    cardHolderName: 'N11 Test User',
    cardNumber: '5528790000000008',
    expireMonth: '12',
    expireYear: '2030',
    cvc: '123'
  });
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) return setLoading(false);
    api.get('/api/v1/baskets')
      .then((res) => setBasket(res.data?.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user]);

  const updateQty = async (pid, qty) => {
    try { const r = await api.put(`/api/v1/baskets/items/${pid}?quantity=${qty}`); setBasket(r.data?.data); window.dispatchEvent(new Event('cart_updated')); }
    catch { toast.error('Güncelleme hatası'); }
  };

  const remove = async (pid) => {
    try { const r = await api.delete(`/api/v1/baskets/items/${pid}`); setBasket(r.data?.data); toast.success('Kaldırıldı'); window.dispatchEvent(new Event('cart_updated')); }
    catch { toast.error('Hata'); }
  };

  const checkout = async () => {
    if (!basket?.items?.length) return;
    try {
      await api.post('/api/v1/orders', {
        paymentMethod: 'CREDIT_CARD',
        paymentDetails,
        items: basket.items.map(i => ({ productId: i.productId, productName: i.productName, quantity: i.quantity, price: i.price, sellerId: i.sellerId })),
      }, { headers: { 'Idempotency-Key': crypto.randomUUID() } });
      await api.delete('/api/v1/baskets');
      toast.success('Sipariş oluşturuldu!');
      window.dispatchEvent(new Event('cart_updated'));
      navigate('/orders');
    } catch { toast.error('Sipariş hatası'); }
  };

  if (!user) return <div className="container empty-state" style={{paddingTop:80}}><span className="empty-icon">🛒</span><h3>Giriş yapın</h3></div>;
  if (loading) return <div className="container" style={{paddingTop:40}}><div className="skeleton" style={{height:400}}/></div>;

  const items = basket?.items || [];
  const totalAmount = items.reduce((acc, item) => acc + (item.price * item.quantity), 0);
  const fmt = (v) => new Intl.NumberFormat('tr-TR',{style:'currency',currency:'TRY'}).format(v);

  return (
    <div className="cart-page container animate-fade-in">
      <h1 className="page-title">Sepetim ({items.length})</h1>
      {items.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">🛒</span><h3>Sepetiniz boş</h3>
          <button className="btn btn-primary" onClick={() => navigate('/')}><FiShoppingBag /> Alışverişe Başla</button>
        </div>
      ) : (
        <div className="cart-layout">
          <div className="cart-items">
            {items.map(item => (
              <div key={item.productId} className="cart-item card animate-slide-in">
                <div className="cart-item-image">{item.imageUrl ? <img src={item.imageUrl} alt={item.productName}/> : <span>📦</span>}</div>
                <div className="cart-item-info"><h3>{item.productName}</h3><p className="cart-item-seller">{item.sellerName}</p></div>
                <div className="cart-item-quantity">
                  <button className="qty-btn" onClick={() => updateQty(item.productId, item.quantity-1)}><FiMinus/></button>
                  <span>{item.quantity}</span>
                  <button className="qty-btn" onClick={() => updateQty(item.productId, item.quantity+1)}><FiPlus/></button>
                </div>
                <span className="cart-item-price">{fmt(item.price*item.quantity)}</span>
                <button className="cart-item-remove" onClick={() => remove(item.productId)}><FiTrash2/></button>
              </div>
            ))}
          </div>
          <div className="cart-summary card">
            <h3>Sipariş Özeti</h3>
            <div className="summary-row"><span>Ürünler ({items.length})</span><span>{fmt(totalAmount)}</span></div>
            <div className="summary-row"><span>Kargo</span><span className="free-shipping">Ücretsiz</span></div>
            <div className="summary-divider"/>
            <div className="summary-row total"><span>Toplam</span><span>{fmt(totalAmount)}</span></div>
            
            <div className="payment-form" style={{ marginTop: '20px', borderTop: '1px solid var(--border-color)', paddingTop: '20px' }}>
              <h4 style={{ marginBottom: '15px' }}>Kredi Kartı Bilgileri</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <input type="text" placeholder="Kart Üzerindeki İsim" value={paymentDetails.cardHolderName} onChange={e => setPaymentDetails({...paymentDetails, cardHolderName: e.target.value})} style={{ padding: '10px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'var(--bg-lighter)', color: 'var(--text-color)' }} />
                <input type="text" placeholder="Kart Numarası" value={paymentDetails.cardNumber} onChange={e => setPaymentDetails({...paymentDetails, cardNumber: e.target.value})} style={{ padding: '10px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'var(--bg-lighter)', color: 'var(--text-color)' }} />
                <div style={{ display: 'flex', gap: '10px' }}>
                  <input type="text" placeholder="AA" value={paymentDetails.expireMonth} onChange={e => setPaymentDetails({...paymentDetails, expireMonth: e.target.value})} style={{ padding: '10px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'var(--bg-lighter)', color: 'var(--text-color)', width: '60px' }} />
                  <input type="text" placeholder="YYYY" value={paymentDetails.expireYear} onChange={e => setPaymentDetails({...paymentDetails, expireYear: e.target.value})} style={{ padding: '10px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'var(--bg-lighter)', color: 'var(--text-color)', width: '80px' }} />
                  <input type="text" placeholder="CVC" value={paymentDetails.cvc} onChange={e => setPaymentDetails({...paymentDetails, cvc: e.target.value})} style={{ padding: '10px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'var(--bg-lighter)', color: 'var(--text-color)', width: '60px' }} />
                </div>
              </div>
            </div>

            <button className="btn btn-accent checkout-btn" onClick={checkout} style={{ marginTop: '20px' }}>Siparişi Tamamla</button>
          </div>
        </div>
      )}
    </div>
  );
}
