import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Hook that subscribes to crash events for a specific device via STOMP WebSocket.
 * Returns real-time crash events and unacknowledged crash count.
 */
export function useCrashWebSocket(deviceSerial) {
  const [crashes, setCrashes] = useState([]);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);

  useEffect(() => {
    if (!deviceSerial) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/crash/${deviceSerial}`, (message) => {
          const crashEvent = JSON.parse(message.body);
          setCrashes(prev => [crashEvent, ...prev]);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [deviceSerial]);

  const acknowledge = useCallback((crashId) => {
    setCrashes(prev => prev.filter(c => c.id !== crashId));
  }, []);

  return { crashes, connected, acknowledge };
}
