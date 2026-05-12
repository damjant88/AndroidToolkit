import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

/**
 * Hook that fetches initial logcat data via REST and subscribes to real-time updates via WebSocket.
 * Returns { logcatData } with fields: environment, clientVersion, serverProductVersion, accessToken
 */
export function useLogcatWebSocket(serial) {
  const [logcatData, setLogcatData] = useState(null);
  const clientRef = useRef(null);

  // Fetch initial state via REST
  useEffect(() => {
    if (!serial) return;
    axios.get(`/api/devices/${encodeURIComponent(serial)}/logcat-data`)
      .then(res => {
        if (res.data && (res.data.environment || res.data.clientVersion || res.data.serverProductVersion || res.data.accessToken)) {
          setLogcatData(res.data);
        }
      })
      .catch(() => {});
  }, [serial]);

  // Subscribe to real-time updates via WebSocket
  useEffect(() => {
    if (!serial) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/logcat/${serial}`, (message) => {
          const data = JSON.parse(message.body);
          setLogcatData(data);
        });
      },
      onStompError: (frame) => {
        console.error('Logcat WebSocket error:', frame.headers['message']);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [serial]);

  return { logcatData };
}
