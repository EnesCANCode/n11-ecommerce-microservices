import axios from 'axios';
import keycloak from './keycloak';

const api = axios.create({
  baseURL: 'http://localhost:8000',
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
