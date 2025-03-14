import { useEffect, useState } from "react";

// Custom hook for WebSocket logic
const useWebSocket = (deviceId) => {
  const [socket, setSocket] = useState(null); // WebSocket instance
  const [message, setMessage] = useState(null); // Store the latest WebSocket message
  const [error, setError] = useState(null); // Store connection errors
  const [isOpen, setIsOpen] = useState(false); // WebSocket connection state

  useEffect(() => {
    if (!deviceId) return;

    // Create a new WebSocket connection
    const newSocket = new WebSocket(`ws://localhost:8080/ws/energyData`);

    newSocket.onopen = () => {
      console.log(`WebSocket connection established for device ${deviceId}`);
      setIsOpen(true);
      // Send device ID after the connection is established
      newSocket.send(JSON.stringify({ deviceId }));
    };

    newSocket.onmessage = (event) => {
      if (event.data && event.data.trim() !== "") {
        try {
          const parsedMessage = JSON.parse(event.data);
          console.log("Received WebSocket message:", parsedMessage);
          setMessage(parsedMessage);
        } catch (error) {
          console.error("Error parsing JSON:", error);
        }
      } else {
        console.warn("Received empty message or invalid JSON format");
      }
    };

    newSocket.onerror = (event) => {
      console.error("WebSocket error:", event);
      setError("WebSocket error occurred");
    };

    newSocket.onclose = () => {
      console.log(`WebSocket connection closed for device ${deviceId}`);
      setIsOpen(false);
    };

    setSocket(newSocket);

    // Cleanup function: Close WebSocket connection when deviceId changes or component unmounts
    return () => {
      if (newSocket) {
        console.log("Closing WebSocket connection...");
        newSocket.close();
      }
    };
  }, [deviceId]); // Re-run the effect when deviceId changes

  return { socket, message, error, isOpen };
};

export default useWebSocket;