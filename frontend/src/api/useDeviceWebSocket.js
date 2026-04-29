import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Hook that connects to the backend WebSocket and receives real-time device updates.
 * Returns the latest device discovery result pushed by the server.
 */
export function useDeviceWebSocket() {
  const [deviceUpdate, setDeviceUpdate] = useState(null);
  const clientRef = useRef(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/topic/devices', (message) => {
          const data = JSON.parse(message.body);
          setDeviceUpdate(data);
        });
      },
      onStompError: (frame) => {
        console.error('WebSocket error:', frame.headers['message']);
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

  return deviceUpdate;
}
