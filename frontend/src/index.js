import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

// Preload icons into memory so Edge can't discard them
const ICON_FILES = ['product.png', 'att.png', 'sprint.png', 'tmo.png', 'Senior.png', 'toyo.png', 'dish.png', 'Android.png', 'orange.png'];
window.__iconCache = ICON_FILES.map(name => {
  const img = new Image();
  img.src = `/icons/${name}`;
  return img;
});

const root = createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
