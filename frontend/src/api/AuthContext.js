import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { login as apiLogin, register as apiRegister, refreshToken as apiRefresh, logout as apiLogout } from '../api/authApi';

const AuthContext = createContext(null);

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem('accessToken'));
  const [loading, setLoading] = useState(true);

  const saveSession = useCallback((data) => {
    setAccessToken(data.accessToken);
    localStorage.setItem('accessToken', data.accessToken);
    const userData = { username: data.username, tier: data.tier, role: data.role };
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
  }, []);

  const clearSession = useCallback(() => {
    setAccessToken(null);
    setUser(null);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    localStorage.removeItem('refreshToken');
  }, []);

  // Try to refresh token on mount (for "keep me logged in")
  useEffect(() => {
    const tryRefresh = async () => {
      const rt = localStorage.getItem('refreshToken');
      if (rt && !accessToken) {
        try {
          const data = await apiRefresh(rt);
          saveSession(data);
        } catch {
          clearSession();
        }
      }
      setLoading(false);
    };
    tryRefresh();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Auto-refresh access token every 25 minutes
  useEffect(() => {
    if (!accessToken) return;
    const interval = setInterval(async () => {
      const rt = localStorage.getItem('refreshToken');
      if (rt) {
        try {
          const data = await apiRefresh(rt);
          saveSession(data);
        } catch {
          clearSession();
        }
      }
    }, 25 * 60 * 1000);
    return () => clearInterval(interval);
  }, [accessToken, saveSession, clearSession]);

  async function login(username, password, rememberMe) {
    const data = await apiLogin(username, password, rememberMe);
    saveSession(data);
  }

  async function register(username, email, password) {
    return await apiRegister(username, email, password);
  }

  async function logout() {
    const rt = localStorage.getItem('refreshToken');
    if (rt) {
      try { await apiLogout(rt); } catch {} // eslint-disable-line no-empty
    }
    clearSession();
  }

  if (loading) return null;

  return (
    <AuthContext.Provider value={{ user, accessToken, login, register, logout, isAuthenticated: !!accessToken }}>
      {children}
    </AuthContext.Provider>
  );
}
