import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

/**
 * Hook that fetches initial logcat data via REST and subscribes to real-time updates via WebSocket.
 * Clears data when appInstalled becomes false, re-fetches when it becomes true.
 * Returns { logcatData }
 */
export function useLogcatWebSocket(serial, appInstalled) {
  const [logcatData, setLogcatData] = useState(null);
  const clientRef = useRef(null);

  // Clear data when app is uninstalled
  useEffect(() => {
    if (!appInstalled) {
      setLogcatData(null);
    }
  }, [appInstalled]);

  // Fetch initial state via REST from local agent, then poll for updates
  useEffect(() => {
    if (!serial || !appInstalled) return;
    
    const fetchData = () => {
      axios.get(`http://localhost:8081/api/agent/devices/${encodeURIComponent(serial)}/logcat-data`)
        .then(res => {
          if (res.data && (res.data.environment || res.data.clientVersion || res.data.serverProductVersion || res.data.accessToken)) {
            setLogcatData(prev => {
              if (!prev) return res.data;
              // Merge: only overwrite fields that have values (don't clear populated fields with empty)
              return {
                ...prev,
                environment: res.data.environment || prev.environment,
                clientVersion: res.data.clientVersion || prev.clientVersion,
                serverProductVersion: res.data.serverProductVersion || prev.serverProductVersion,
                serverProjectVersion: res.data.serverProjectVersion || prev.serverProjectVersion,
                accessToken: res.data.accessToken || prev.accessToken,
                tokenType: res.data.tokenType || prev.tokenType,
              };
            });
          }
        })
        .catch(() => {});
    };
    
    fetchData();
    const interval = setInterval(fetchData, 3000); // Poll every 3s for real-time feel
    return () => clearInterval(interval);
  }, [serial, appInstalled]);

  // Subscribe to real-time updates via WebSocket
  useEffect(() => {
    if (!serial) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
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
