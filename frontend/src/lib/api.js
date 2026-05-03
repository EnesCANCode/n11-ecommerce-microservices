import axios from 'axios';
import keycloak from './keycloak';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:8000' : ''),
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  if (keycloak.authenticated && keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (err.response?.status === 401 && keycloak.authenticated) {
      try {
        await keycloak.updateToken(30);
        err.config.headers.Authorization = `Bearer ${keycloak.token}`;
        return api(err.config);
      } catch {
        keycloak.logout();
      }
    }
    return Promise.reject(err);
  }
);

export default api;
