import React from 'react';
import DeviceList from './components/DeviceList';
import './App.css';

function App() {
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
