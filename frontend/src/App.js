import React from 'react';
import DeviceList from './components/DeviceList';
import ScreenshotViewer from './components/ScreenshotViewer';
import './App.css';

function App() {
  // If opened as a screenshot popup, show only the viewer
  const params = new URLSearchParams(window.location.search);
  if (params.get('view') === 'screenshot') {
    return <ScreenshotViewer />;
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>🤖 Adb Toolkit</h1>
      </header>
      <main>
        <DeviceList />
      </main>
    </div>
  );
}

export default App;
