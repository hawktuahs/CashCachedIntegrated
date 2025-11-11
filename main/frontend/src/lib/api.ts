import axios from 'axios';

const isTunnel = typeof window !== 'undefined' &&
  window.location.hostname.endsWith('lucidity.sbs');

export const api = axios.create({
  baseURL: isTunnel ? 'https://api.lucidity.sbs' : '/',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      const currentPath = window.location.pathname;
      if (currentPath !== '/login' && !error.config?.url?.includes('/verify-otp')) {
        localStorage.removeItem('token');
        delete api.defaults.headers.common['Authorization'];
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers || {};
    if (!config.headers['Authorization']) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
  }
  return config;
});