import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Hook that connects to the backend WebSocket and receives real-time device updates.
 * Returns { deviceUpdate, connected }
 */
export function useDeviceWebSocket() {
  const [deviceUpdate, setDeviceUpdate] = useState(null);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/devices', (message) => {
          const data = JSON.parse(message.body);
          setDeviceUpdate(data);
        });
      },
      onDisconnect: () => {
        setConnected(false);
      },
      onStompError: (frame) => {
        setConnected(false);
        console.error('WebSocket error:', frame.headers['message']);
      },
      onWebSocketClose: () => {
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, []);

  return { deviceUpdate, connected };
}
