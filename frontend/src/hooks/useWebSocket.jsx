import { useEffect, useState } from "react";
import { toast } from "react-toastify";

// Custom hook for WebSocket logic
const useWebSocket = (testStationId) => {
  const [socket, setSocket] = useState(null); // WebSocket instance
  const [message, setMessage] = useState(null); // Store the latest WebSocket message
  const [error, setError] = useState(null); // Store connection errors
  const [isOpen, setIsOpen] = useState(false); // WebSocket connection state
  const [toastId, setToastId] = useState(null); // Store toast ID for dismissal


  const closeErrorToast = () => {
    console.log("Closing toast with ID:", toastId); // Debug log to ensure it's working
    if (toastId) {
      toast.dismiss(toastId); // Dismiss the toast by its ID
      setToastId(null); // Reset the toast ID after dismissal
    }
    setError(null); // Clear the error state
  };


  useEffect(() => {
    if (!testStationId) return;

    // Create a new WebSocket connection
    const newSocket = new WebSocket(`ws://localhost:8080/ws/measure`);

    newSocket.onopen = () => {
      console.log(`WebSocket connection established for test stationId ${testStationId}`);
      setIsOpen(true);
      // Send testStationId after the connection is established
      newSocket.send(JSON.stringify({ testStationId }));

    };

    newSocket.onmessage = (event) => {
      console.log("Raw WebSocket Data:", event.data); // Log the full message
      try {
        const parsedMessage = JSON.parse(event.data);
        console.log("Received WebSocket message:", parsedMessage);

        if (parsedMessage.error) {
          console.error("WebSocket Error Received:", parsedMessage.error);
          setError(parsedMessage.error);
          if (parsedMessage.retry === true || parsedMessage.retry === "true") {
            // Retry warning, just toast info, no socket close
            toast.info(`Retrying: ${parsedMessage.error}`, { autoClose: 10000 });
          } else {
            // Fatal error: show toast, then close socket to trigger cleanup
            toast.error(`Error: ${parsedMessage.error}`);
            newSocket.close();
          }
        } else {
          setMessage(parsedMessage);

        }
      } catch (error) {
        console.error("Error parsing WebSocket message:", error);
      }
    };

    newSocket.onerror = (event) => {
      console.error("WebSocket error:", event);
      setError("WebSocket error occurred");
      // Show WebSocket error toast and store toast ID
      const id = toast.error("WebSocket error occurred", { autoClose: 500 });
      setToastId(id); // Store the toast ID for later dismissal
    };

    newSocket.onclose = (event) => {
      const { code } = event;
      let message;

      // Add logic based on the code or reason
      if (code === 1000) {
        message = "WebSocket sauber geschlossen. Programm gestoppt.";
      } else if (code === 1006) {
        message = "Verbindung unerwartet abgebrochen.";
      } else {
        message = "Verbindung wurde getrennt.";
      }
      toast.info(message, {
        autoClose: 2000,
      });

      setIsOpen(false);
    };

    setSocket(newSocket);

    // Cleanup function: Close WebSocket connection when deviceId changes or component unmounts
    return () => {
      //newSocket.close(); //  On the frontend, the connection is closed with this line:

      setTimeout(() => {
        newSocket.close();
      }, 100);
    };
  }, [testStationId]); // Re-run the effect when deviceId changes


  return { socket, message, error, isOpen, closeErrorToast };
};

export default useWebSocket;

// if device has been changed  it closes the current WebSocket.Then immediately opens a new one and sends a new testStationId.