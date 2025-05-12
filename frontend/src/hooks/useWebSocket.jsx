/* eslint-disable react-hooks/exhaustive-deps */
import { useEffect, useState } from "react";
import { toast } from "react-toastify";

// Custom hook for WebSocket logic
const useWebSocket = (deviceId) => {
  const [socket, setSocket] = useState(null); // WebSocket instance
  const [message, setMessage] = useState(null); // Store the latest WebSocket message
  const [error, setError] = useState(null); // Store connection errors
  const [isOpen, setIsOpen] = useState(false); // WebSocket connection state
  const [toastId, setToastId] = useState(null); // Store toast ID for dismissal
  const [onStopPolling, setOnStopPolling] = useState(false);

  const closeErrorToast = () => {
    console.log("Closing toast with ID:", toastId); // Debug log to ensure it's working
    if (toastId) {
      toast.dismiss(toastId); // Dismiss the toast by its ID
      setToastId(null); // Reset the toast ID after dismissal
    }
    setError(null); // Clear the error state
  };

  useEffect(() => {
    if (!deviceId) return;

    // Create a new WebSocket connection
    const newSocket = new WebSocket(`ws://localhost:8080/ws/measure`);

    newSocket.onopen = () => {
      console.log(`WebSocket connection established for device ${deviceId}`);
      setIsOpen(true);
      // Send device ID after the connection is established
      newSocket.send(JSON.stringify({ deviceId }));
    };

    newSocket.onmessage = (event) => {
      console.log("Raw WebSocket Data:", event.data); // Log the full message

      try {
        const parsedMessage = JSON.parse(event.data);
        console.log("Received WebSocket message:", parsedMessage);

        if (parsedMessage.error) {
          console.error("WebSocket Error Received:", parsedMessage.error);
          setError(parsedMessage.error);

          // Show error toast and store the toast ID for later dismissal
          const id = toast.error(`Error: ${parsedMessage.error}`, {
            autoClose: 20000,
          });
          setToastId(id); // Store the toast ID for later dismissal
        } else if (parsedMessage.stopPolling) {
          toast.info(`Polling stopped for device ${parsedMessage.deviceId}`, {
            autoClose: 5000, // Close after 5 seconds
          });
          setOnStopPolling(true);
          console.log("Setting stopPolling to true", onStopPolling);
        } else {
          setMessage(parsedMessage);
          // toast.success("Data received successfully!", { autoClose: 3000 });
        }
      } catch (error) {
        console.error("Error parsing WebSocket message:", error);
      }
    };

    newSocket.onerror = (event) => {
      console.error("WebSocket error:", event);
      setError("WebSocket error occurred");

      // Show WebSocket error toast and store toast ID
      const id = toast.error("WebSocket error occurred", { autoClose: 5000 });
      setToastId(id); // Store the toast ID for later dismissal
    };

    newSocket.onclose = (event) => {
      console.log(`WebSocket connection closed for device ${deviceId}`);
      console.log(`Close code: ${event.code}`);
      console.log(`Close reason: ${event.reason}`);
      console.log(`Was the closure clean? ${event.wasClean}`);
      setIsOpen(false);
    };

    setSocket(newSocket);

    // Cleanup function: Close WebSocket connection when deviceId changes or component unmounts
    return () => {
      newSocket.close(); //  On the frontend, the connection is closed with this line:
    };
  }, [deviceId]); // Re-run the effect when deviceId changes

  return {
    socket,
    message,
    error,
    isOpen,
    closeErrorToast,
    onStopPolling,
    setOnStopPolling,
  };
};

export default useWebSocket;
