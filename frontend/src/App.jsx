import { useState, useEffect, useCallback } from 'react';
import { Routes, Route } from 'react-router-dom';
import keycloak from './lib/keycloak';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import Orders from './pages/Orders';
import SellerPanel from './pages/SellerPanel';

export default function App() {
  const [keycloakReady, setKeycloakReady] = useState(false);
  const [user, setUser] = useState(null);

  useEffect(() => {
    keycloak
      .init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
      .then((authenticated) => {
        setKeycloakReady(true);
        if (authenticated) {
          setUser({
            id: keycloak.subject,
            name: keycloak.tokenParsed?.preferred_username,
            email: keycloak.tokenParsed?.email,
            roles: keycloak.tokenParsed?.realm_access?.roles || [],
          });
        }
      })
      .catch((err) => {
        console.error('Keycloak init error', err);
        setKeycloakReady(true);
      });
  }, []);

  const login = useCallback(() => keycloak.login(), []);
  const logout = useCallback(() => keycloak.logout({ redirectUri: window.location.origin }), []);
  const register = useCallback(() => keycloak.register(), []);

  if (!keycloakReady) {
    return (
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        height: '100vh', background: 'var(--bg-primary)'
      }}>
        <div className="skeleton" style={{ width: 200, height: 24 }} />
      </div>
    );
  }

  return (
    <>
      <Navbar user={user} onLogin={login} onLogout={logout} onRegister={register} />
      <main style={{ paddingTop: 80 }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/product/:id" element={<ProductDetail user={user} />} />
          <Route path="/cart" element={<Cart user={user} />} />
          <Route path="/orders" element={<Orders user={user} />} />
          <Route path="/seller" element={<SellerPanel user={user} />} />
        </Routes>
      </main>
    </>
  );
}
