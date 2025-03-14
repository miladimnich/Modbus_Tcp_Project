import { useState, useEffect } from 'react';
import useWebSocket from "./useWebSocket";
import { fetchDevices,getCurrentValue,startMeasure,stopMeasure } from '../api/apiClient';
import { EnergyCalculationType, HeatingCalculationTypes } from '../constants/calculationTypes';
 

const useDeviceMonitor = () => {
    const [devices, setDevices] = useState([]);
    const [selectedDevice, setSelectedDevice] = useState(null);
    const {socket, message, isOpen } = useWebSocket(selectedDevice);
    const [currentValues, setCurrentValues] = useState({});
    const [firstValues, setFirstValues] = useState({});
    const [lastValues, setLastValues] = useState({});
    const [isRunning, setIsRunning] = useState(false);
    const [difference, setDifference] = useState({});

    useEffect(() => {
        fetchDevices()
          .then((response) => {
            setDevices(response.data);
          })
          .catch((error) => {
            console.error("Error fetching devices:", error);
          });
      }, []);

  // Handle device selection
      const handleDeviceChange = (event) => {
        const deviceId = event.target.value;
        setSelectedDevice(deviceId);
    
        // Ensure socket is open before sending data
        if (socket && isOpen && deviceId) {
          socket.send(JSON.stringify({ deviceId }));
        }
      };

        // Reset energy data when device is selected
  useEffect(() => {
    if (selectedDevice) {
      setFirstValues({});
      setCurrentValues({});
      setLastValues({});
      setDifference({});
      setIsRunning(false);
      getCurrentValue(selectedDevice);
    }
  }, [selectedDevice]);


   // Update current values based on WebSocket message
   useEffect(() => {
    if (message) {
      const { ...values } = message;
  
      setCurrentValues((prevCurrentValues) => {
        const updatedData = { ...prevCurrentValues };
        const updatedDifferences = {};
  
        Object.keys(values).forEach((key) => {
          if (EnergyCalculationType[key] || HeatingCalculationTypes[key]) {
            updatedData[key] = { data: values[key] };
  
            // Use functional update for firstValues
            setFirstValues((prevFirstValues) => {
              const initialValue = prevFirstValues[key];
              updatedDifferences[key] =
                initialValue !== undefined ? values[key] - initialValue : "N/A";
              return prevFirstValues; // Return unchanged state to avoid extra re-renders
            });
          }
        });
  
        setDifference((prevDifference) => ({
          ...prevDifference,
          ...updatedDifferences,
        }));
  
        return updatedData;
      });
    }
  }, [message]);  
  

  // Start measurement
  useEffect(() => {
    if (selectedDevice) {
      setFirstValues({});
      setCurrentValues({});
      setLastValues({});
      setDifference({});
      setIsRunning(false);
      getCurrentValue(selectedDevice);
    }
  }, [selectedDevice]);

  // Start measurement
  const handleStartTask = async () => {
    if (selectedDevice) {
      setIsRunning(true); // Mark the system as running
      try {
        const initialData = await startMeasure(selectedDevice);
        setFirstValues(initialData); // Store the first values for comparison
      } catch (error) {
        console.error("Error starting measurement:", error);
        setIsRunning(false); // Stop the system if there's an error
      }
    }
  };

  // Stop measurement and preserve values
  const handleStopTask = async () => {
    if (selectedDevice) {
      try {
        const lastData = await stopMeasure(selectedDevice);
        setLastValues(lastData);
        setIsRunning(false); // Mark task as stopped

        // Do NOT reset first values or current values here, so they persist after stop
      } catch (error) {
        console.error("Error stopping measurement:", error);
      }
    }
  };

      return {
        devices,
        selectedDevice,
        socket,
        message,
        isOpen,
        currentValues,
        firstValues,
        lastValues,
        difference,
        isRunning,
        handleDeviceChange,
        handleStartTask,
        handleStopTask
       
      };
    };
    
    export default useDeviceMonitor;
