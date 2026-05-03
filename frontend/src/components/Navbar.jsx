import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiSearch, FiShoppingCart, FiUser, FiBell, FiPackage, FiLogOut, FiMenu, FiX } from 'react-icons/fi';
import './Navbar.css';

export default function Navbar({ user, onLogin, onLogout, onRegister }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(0);
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
              {user.roles?.includes('SELLER') && (
                <Link to="/seller" className="nav-action" title="Mağazam">
                  <FiUser />
                </Link>
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
    </nav>
  );
}
