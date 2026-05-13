import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './api/AuthContext';
import DeviceList from './components/DeviceList';
import ScreenshotViewer from './components/ScreenshotViewer';
import LoginPage from './components/LoginPage';
import AllowedUsersPanel from './components/AllowedUsersPanel';
import AdminProjectsPanel from './components/AdminProjectsPanel';
import UserProjectsPanel from './components/UserProjectsPanel';
import MyDevicesPanel from './components/MyDevicesPanel';
import AccessRequestPopup from './components/AccessRequestPopup';
import ErrorBoundary from './components/ErrorBoundary';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import './App.css';

function AppContent() {
  const { isAuthenticated, user, logout } = useAuth();
  const [accessRequest, setAccessRequest] = useState(null);
  const [showAllowedUsers, setShowAllowedUsers] = useState(false);
  const [showAdminProjects, setShowAdminProjects] = useState(false);
  const [showProjects, setShowProjects] = useState(false);
  const [showMyDevices, setShowMyDevices] = useState(false);

  const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';

  // Listen for access requests (local owner) and access revocation (remote user)
  useEffect(() => {
    if (!isAuthenticated || !user) return;
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      onConnect: () => {
        if (isLocal) {
          client.subscribe(`/topic/access-request/${user.username}`, (msg) => {
            const data = JSON.parse(msg.body);
            setAccessRequest(data);
          });
        }
        // All users listen for revocation
        client.subscribe(`/topic/access-revoked/${user.username}`, () => {
          alert('Your access has been revoked. You will be logged out.');
          logout();
        });
      },
    });
    client.activate();
    return () => client.deactivate();
  }, [isAuthenticated, user, isLocal, logout]);

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
          <button className="toolbar-small-btn" onClick={() => setShowMyDevices(!showMyDevices)}>
            {showMyDevices ? 'Hide My Devices' : '📱 My Devices'}
          </button>
          {user?.role === 'ADMIN' && (
            <button className="toolbar-small-btn" onClick={() => setShowAllowedUsers(!showAllowedUsers)}>
              {showAllowedUsers ? 'Hide Users' : 'Allowed Users'}
            </button>
          )}
          {user?.role === 'ADMIN' && (
            <button className="toolbar-small-btn" onClick={() => setShowAdminProjects(!showAdminProjects)}>
              {showAdminProjects ? 'Hide Manage Projects' : 'Manage Projects'}
            </button>
          )}
          {user?.role === 'ADMIN' && (
            <button className="toolbar-small-btn" onClick={() => setShowProjects(!showProjects)}>
              {showProjects ? 'Hide Projects' : 'Projects'}
            </button>
          )}
          <span className="user-info">{user?.username} ({user?.role === 'ADMIN' ? user.role : user?.tier})</span>
          <button className="logout-btn" onClick={logout}>Logout</button>
        </div>
      </header>
      {showAllowedUsers && <AllowedUsersPanel />}
      {showAdminProjects && user?.role === 'ADMIN' && <AdminProjectsPanel />}
      {showProjects && <UserProjectsPanel />}
      {showMyDevices && <MyDevicesPanel />}
      <main>
        <DeviceList />
      </main>

      {accessRequest && (
        <AccessRequestPopup
          requestId={accessRequest.requestId}
          fromUser={accessRequest.fromUser}
          onDismiss={() => setAccessRequest(null)}
        />
      )}
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
