import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FiShoppingCart, FiMinus, FiPlus, FiArrowLeft } from 'react-icons/fi';
import toast from 'react-hot-toast';
import api from '../lib/api';
import './ProductDetail.css';

export default function ProductDetail({ user }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get(`/api/v1/products/${id}`)
      .then((res) => setProduct(res.data?.data))
      .catch(() => toast.error('Ürün bulunamadı'))
      .finally(() => setLoading(false));
  }, [id]);

  const addToCart = async () => {
    if (!user) {
      toast.error('Sepete eklemek için giriş yapın');
      return;
    }
    try {
      await api.post('/api/v1/baskets/items', {
        productId: product.id,
        productName: product.name,
        price: product.price,
        quantity,
        sellerId: product.sellerId,
        sellerName: product.sellerName,
        imageUrl: product.imageUrl,
      });
      toast.success('Sepete eklendi!');
      window.dispatchEvent(new Event('cart_updated'));
    } catch {
      toast.error('Sepete eklenirken hata oluştu');
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ paddingTop: 40 }}>
        <div style={{ display: 'flex', gap: 40 }}>
          <div className="skeleton" style={{ width: 480, height: 480, borderRadius: 'var(--radius-lg)' }} />
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div className="skeleton" style={{ height: 32, width: '70%' }} />
            <div className="skeleton" style={{ height: 20, width: '30%' }} />
            <div className="skeleton" style={{ height: 80, width: '100%' }} />
            <div className="skeleton" style={{ height: 48, width: 200, marginTop: 'auto' }} />
          </div>
        </div>
      </div>
    );
  }

  if (!product) return null;

  return (
    <div className="product-detail container animate-fade-in">
      <button className="back-btn" onClick={() => navigate(-1)}>
        <FiArrowLeft /> Geri
      </button>

      <div className="detail-grid">
        <div className="detail-image-wrapper">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} className="detail-image" />
          ) : (
            <div className="detail-image-placeholder">📦</div>
          )}
        </div>

        <div className="detail-info">
          {product.categoryName && (
            <span className="badge badge-primary">{product.categoryName}</span>
          )}
          <h1 className="detail-name">{product.name}</h1>
          <p className="detail-seller">
            Satıcı: <strong>{product.sellerName || 'N11 Marketplace'}</strong>
          </p>

          <div className="detail-price">
            {new Intl.NumberFormat('tr-TR', {
              style: 'currency',
              currency: 'TRY',
            }).format(product.price)}
          </div>

          {product.description && (
            <p className="detail-desc">{product.description}</p>
          )}

          <div className="detail-actions">
            <div className="quantity-control">
              <button className="qty-btn" onClick={() => setQuantity(Math.max(1, quantity - 1))}>
                <FiMinus />
              </button>
              <span className="qty-value">{quantity}</span>
              <button className="qty-btn" onClick={() => setQuantity(quantity + 1)}>
                <FiPlus />
              </button>
            </div>
            <button className="btn btn-accent detail-cart-btn" onClick={addToCart}>
              <FiShoppingCart /> Sepete Ekle
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
