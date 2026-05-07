import React from 'react';
import { AuthProvider, useAuth } from './api/AuthContext';
import DeviceList from './components/DeviceList';
import ScreenshotViewer from './components/ScreenshotViewer';
import LoginPage from './components/LoginPage';
import ErrorBoundary from './components/ErrorBoundary';
import './App.css';

function AppContent() {
  const { isAuthenticated, user, logout } = useAuth();

  const params = new URLSearchParams(window.location.search);
  if (params.get('view') === 'screenshot') {
    return <ScreenshotViewer />;
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>{'\ud83e\udd16'} Adb Toolkit</h1>
        <div className="header-right">
          <span className="user-info">{user?.username} ({user?.tier})</span>
          <button className="logout-btn" onClick={logout}>Logout</button>
        </div>
      </header>
      <main>
        <DeviceList />
      </main>
    </div>
  );
}

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;
