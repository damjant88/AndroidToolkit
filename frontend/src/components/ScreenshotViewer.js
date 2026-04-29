import React, { useState, useEffect } from 'react';

function ScreenshotViewer() {
  const [copied, setCopied] = useState(false);

  // Get the screenshot URL and device name from URL parameters
  const params = new URLSearchParams(window.location.search);
  const imageUrl = params.get('url');
  const deviceName = params.get('device');

  useEffect(() => {
    if (deviceName) {
      document.title = `Screenshot - ${deviceName}`;
    }
  }, [deviceName]);

  async function handleCopy() {
    try {
      const response = await fetch(imageUrl);
      const blob = await response.blob();
      await navigator.clipboard.write([
        new ClipboardItem({ 'image/png': blob })
      ]);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      alert('Copy failed: ' + err.message);
    }
  }

  async function handleDownload() {
    try {
      const response = await fetch(imageUrl);
      if (!response.ok) throw new Error('File not found');
      const blob = await response.blob();
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = `screenshot_${deviceName}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(blobUrl);
    } catch (err) {
      alert('Download failed: ' + err.message);
    }
  }

  if (!imageUrl) {
    return <p style={{ padding: 20 }}>No screenshot available.</p>;
  }

  return (
    <div style={{ margin: 0, padding: 0, background: '#222', minHeight: '100vh' }}>
      <div style={{
        display: 'flex',
        gap: 8,
        padding: '8px 12px',
        background: '#333',
        borderBottom: '1px solid #444'
      }}>
        <button onClick={handleCopy} style={btnStyle}>
          {copied ? '✅ Copied!' : '📋 Copy to Clipboard'}
        </button>
        <button onClick={handleDownload} style={btnStyle}>
          💾 Download
        </button>
      </div>
      <div style={{ display: 'flex', justifyContent: 'center', padding: 0 }}>
        <img
          src={imageUrl}
          alt="device screenshot"
          style={{ maxWidth: '100%', height: 'auto', display: 'block' }}
        />
      </div>
    </div>
  );
}

const btnStyle = {
  flex: 1,
  padding: '6px 12px',
  fontSize: '0.8rem',
  border: '1px solid #555',
  borderRadius: 4,
  background: 'linear-gradient(to bottom, #555, #333)',
  color: 'white',
  cursor: 'pointer',
};

export default ScreenshotViewer;
