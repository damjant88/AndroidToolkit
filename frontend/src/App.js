import React from 'react';
import DeviceList from './components/DeviceList';
import ScreenshotViewer from './components/ScreenshotViewer';
import ErrorBoundary from './components/ErrorBoundary';
import './App.css';

function App() {
  const params = new URLSearchParams(window.location.search);
  if (params.get('view') === 'screenshot') {
    return <ScreenshotViewer />;
  }

  return (
    <ErrorBoundary>
      <div className="app">
        <header className="app-header">
          <h1>🤖 Adb Toolkit</h1>
        </header>
        <main>
          <DeviceList />
        </main>
      </div>
    </ErrorBoundary>
  );
}

export default App;
