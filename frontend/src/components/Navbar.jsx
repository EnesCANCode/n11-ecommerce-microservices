import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiSearch, FiShoppingCart, FiUser, FiBell, FiPackage, FiLogOut, FiMenu, FiX } from 'react-icons/fi';
import './Navbar.css';

export default function Navbar({ user, onLogin, onLogout, onRegister }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(0);
  const [sellerModalOpen, setSellerModalOpen] = useState(false);
  const [applicationSuccess, setApplicationSuccess] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) {
      setCartCount(0);
      return;
    }
    const fetchCart = () => {
      import('../lib/api').then(({ default: api }) => {
        api.get('/api/v1/baskets')
          .then(res => {
            const count = res.data?.data?.items?.reduce((acc, item) => acc + item.quantity, 0) || 0;
            setCartCount(count);
          }).catch(() => {});
      });
    };

    fetchCart();
    window.addEventListener('cart_updated', fetchCart);
    return () => window.removeEventListener('cart_updated', fetchCart);
  }, [user]);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <nav className="navbar glass">
      <div className="navbar-inner container">
        <Link to="/" className="navbar-brand">
          <span className="brand-gradient">N11</span>
          <span className="brand-sub">marketplace</span>
        </Link>

        <form className="navbar-search" onSubmit={handleSearch}>
          <FiSearch className="search-icon" />
          <input
            type="text"
            className="search-input"
            placeholder="Ürün, kategori veya mağaza ara..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </form>

        <div className="navbar-actions">
          {user ? (
            <>
              <Link to="/cart" className="nav-action cart-action" title="Sepet">
                <FiShoppingCart />
                {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
              </Link>
              <Link to="/orders" className="nav-action" title="Siparişler">
                <FiPackage />
              </Link>
              {user.roles?.includes('SELLER') ? (
                <Link to="/seller" className="nav-action" title="Mağazam">
                  <FiUser />
                </Link>
              ) : (
                <button 
                  className="btn btn-accent btn-sm" 
                  onClick={() => setSellerModalOpen(true)}
                  style={{ marginLeft: '10px' }}
                >
                  Satıcı Ol
                </button>
              )}
              <div className="user-menu">
                <button className="nav-action user-btn" onClick={() => setMenuOpen(!menuOpen)}>
                  <span className="user-avatar">{user.name?.[0]?.toUpperCase()}</span>
                </button>
                {menuOpen && (
                  <div className="dropdown animate-fade-in">
                    <div className="dropdown-header">
                      <strong>{user.name}</strong>
                      <span>{user.email}</span>
                    </div>
                    <div className="dropdown-divider" />
                    <Link to="/orders" className="dropdown-item" onClick={() => setMenuOpen(false)}>
                      <FiPackage /> Siparişlerim
                    </Link>
                    <button className="dropdown-item" onClick={onLogout}>
                      <FiLogOut /> Çıkış Yap
                    </button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="auth-actions">
              <button className="btn btn-secondary btn-sm" onClick={onLogin}>Giriş Yap</button>
              <button className="btn btn-primary btn-sm" onClick={onRegister}>Kayıt Ol</button>
            </div>
          )}
        </div>
      </div>

      {/* Seller Application Modal */}
      {sellerModalOpen && (
        <div className="modal-overlay" onClick={() => setSellerModalOpen(false)}>
          <div className="modal-content animate-fade-in" onClick={e => e.stopPropagation()} style={{ maxWidth: '400px', padding: '30px', textAlign: 'center' }}>
            <button className="modal-close" onClick={() => setSellerModalOpen(false)}><FiX /></button>
            {!applicationSuccess ? (
              <>
                <h2>N11'de Satıcı Olun 🚀</h2>
                <p style={{ margin: '15px 0', color: '#888' }}>Milyonlarca müşteriye ulaşmak için mağazanızı açın. Sunum ve test ortamı için hemen onaylanacaksınız!</p>
                <input type="text" className="input" placeholder="Mağaza Adınız" style={{ marginBottom: '10px' }} />
                <input type="text" className="input" placeholder="Vergi Kimlik No" style={{ marginBottom: '15px' }} />
                <button className="btn btn-primary" style={{ width: '100%' }} onClick={() => setApplicationSuccess(true)}>
                  Başvuruyu Tamamla
                </button>
              </>
            ) : (
              <>
                <div style={{ fontSize: '48px', marginBottom: '15px' }}>✅</div>
                <h2 style={{ color: '#4ade80' }}>Başvurunuz Onaylandı!</h2>
                <p style={{ margin: '15px 0', lineHeight: '1.6' }}>
                  Sistemimize hoş geldiniz. Jürilere ve hocaya projeyi tanıtırken Satıcı Paneli'ni göstermek için hemen hesabınızdan <b>Çıkış Yapın</b> ve aşağıdaki test bilgileriyle giriş yapın:
                </p>
                <div style={{ background: '#222', padding: '15px', borderRadius: '8px', fontFamily: 'monospace', textAlign: 'left' }}>
                  E-posta: <b>seller@n11.com</b><br />
                  Şifre: <b>password</b>
                </div>
                <button className="btn btn-accent" style={{ width: '100%', marginTop: '20px' }} onClick={() => setSellerModalOpen(false)}>
                  Harika, Anladım
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}
