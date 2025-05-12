/* eslint-disable no-unused-vars */
/* eslint-disable react-hooks/exhaustive-deps */
import { useState, useEffect } from "react";
import useWebSocket from "./useWebSocket";
import {
  fetchDevices,
  startMeasure,
  stopMeasure,
  fetchMaschineConfig,
  fetchMachineTypes,
  fetchMaschineBordersDefault,
  fetchProductStatus,
  ubertragenData,
} from "../api/apiClient";
import { toast } from "react-toastify";

const useDeviceMonitor = () => {
  const [devices, setDevices] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const { socket, message, isOpen, error, onStopPolling, setOnStopPolling } =
    useWebSocket(selectedDevice);
  const [currentValues, setCurrentValues] = useState({});
  const [firstValues, setFirstValues] = useState({});
  const [lastValues, setLastValues] = useState({});
  const [isRunning, setIsRunning] = useState(false);
  const [difference, setDifference] = useState({});
  const [gasMeterDifference, setGasMeterDifference] = useState({});
  const [selectedMachineType, setSelectedMachineType] = useState(""); // Initially no machine type selected
  const [machineTypeBorders, setMachineTypeBorders] = useState({}); // Default min/max for ASV_20
  const [machineTypes, setMachineTypes] = useState([]);
  const [isMachineTypeSelected, setIsMachineTypeSelected] = useState(false);
  const [serienNummer, setSerienNummer] = useState("");
  const [productStatus, setProductStatus] = useState(null);

  const [outOfRangeState, setOutOfRangeState] = useState({});
  const [defaultBorders, setDefaultBorders] = useState({});
  const [isButtonDisabled, setIsButtonDisabled] = useState(false); // Initially, the button is not disabled

  // Timer-related states
  const [elapsedTime, setElapsedTime] = useState(0); // Track elapsed time
  const [intervalId, setIntervalId] = useState(null); // Store interval ID
  const [isStopped, setStopped] = useState(false); // Track if the task has been started

  useEffect(() => {
    if (onStopPolling || !isOpen) {
      console.log("onStopPolling changed:", onStopPolling, isOpen);
      if (!isOpen) {
        handleStopTask();
        setOnStopPolling(false); // Reset the flag after stopping
      }
    }
  }, [onStopPolling, isOpen]);

  //fetch all machines
  useEffect(() => {
    fetchMachineTypes()
      .then((types) => {
        setMachineTypes(types);
      })
      .catch((error) => {
        console.error("Error fetching machine types:", error);
      });
  }, []);

  useEffect(() => {
    if (!selectedMachineType) return;

    const fetchConfig = async () => {
      try {
        const machineBorders = await fetchMaschineConfig(selectedMachineType); // already is Map<String, ValueRange>
        console.log("Fetched machine borders:", machineBorders);
        setMachineTypeBorders(machineBorders);
      } catch (error) {
        console.error("Error fetching configuration:", error);
      }
    };

    fetchConfig();
  }, [selectedMachineType]);

  // Handle change in selected machine type
  const handleMachineTypeChange = (event) => {
    setSelectedMachineType(event.target.value); // Update selected machine type
  };

  const handleInputChange = (e) => {
    setSerienNummer(e.target.value);
  };

  const handleUbertragen = async () => {
    try {
      const response = await ubertragenData(selectedDevice); // Assuming selectedDevice is the deviceId
      toast.success(response.message || "Daten erfolgreich übertragen"); // Show success message if available
    } catch (error) {
      toast.error("Fehler beim Übertragen der Daten"); // Show error message
    }
  };

  const fetchStatus = async () => {
    if (!serienNummer) return;
    try {
      const data = await fetchProductStatus(serienNummer);
      setProductStatus(data);
      // Check if the product status is 70
      if (data === 20) {
        toast.success(`Product Status: ${data}`);
        setIsButtonDisabled(true); // Disable the button if status is 70
      } else {
        toast.info(`Product Status: ${data}`);
      }
    } catch (error) {
      console.error("Error fetching status:", error);
      setProductStatus(null);
      toast.error("Fehler beim Abrufen des Status.");
    }
  };

  useEffect(() => {
    fetchDevices()
      .then((response) => {
        setDevices(response.data);
      })
      .catch((error) => {
        console.error("Error fetching devices:", error);
      });
  }, []);

  const handleDeviceChange = async (event) => {
    const deviceId = event.target.value;
    setSelectedDevice(deviceId);

    // Send WebSocket message
    if (socket && isOpen && deviceId) {
      socket.send(JSON.stringify({ deviceId }));
    }
    if (!isRunning) {
      resetTimer();
    }
    // Fetch default borders
    try {
      const defaultBorders = await fetchMaschineBordersDefault(deviceId);
      console.log("Setting defaultBorders to:", defaultBorders);
      setDefaultBorders(defaultBorders);
      console.log("After setDefaultBorders:", defaultBorders);
    } catch (error) {
      console.error("Failed to fetch borders for device:", deviceId, error);
    }
  };

  const resetTimer = () => {
    setElapsedTime(0); // Reset the elapsed time
    setIsRunning(false); // Stop the timer
    if (intervalId) {
      clearInterval(intervalId); // Clear the interval
      setIntervalId(null);
    }
  };

  const startTimer = (backendStartTime) => {
    if (!intervalId) {
      // intervalId is null means its falsy
      const id = setInterval(() => {
        const elapsed = Math.floor((Date.now() - backendStartTime) / 1000);
        setElapsedTime(elapsed);
      }, 1000);
      setIntervalId(id);
    }
  };

  const stopTimer = () => {
    if (intervalId) {
      clearInterval(intervalId); // Clear interval
      setIntervalId(null);
    }
  };

  const evaluateOutOfRangeState = () => {
    const updatedOutOfRangeState = {};

    Object.keys(currentValues).forEach((key) => {
      const value = currentValues[key]?.data;

      if (value !== undefined) {
        let border;
        if (isMachineTypeSelected) {
          border = machineTypeBorders[key] || defaultBorders[key];
        } else {
          border = defaultBorders[key];
        }

        if (border) {
          const shouldCompare =
            key === "ELEKTRISCHER_WIRKUNGSGRAD" ? elapsedTime >= 15 * 60 : true;

          if (shouldCompare) {
            const { min, max } = border;
            const minOut = min !== undefined && value < min;
            const maxOut = max !== undefined && value > max;
            if (minOut || maxOut) {
              updatedOutOfRangeState[key] = {
                min: minOut,
                max: maxOut,
              };
            }
          }
        }
      }
    });

    setOutOfRangeState(updatedOutOfRangeState);
  };

  useEffect(() => {
    if (Object.keys(firstValues).length > 0) {
      evaluateOutOfRangeState();
    }
  }, [firstValues]);

  useEffect(() => {
    if (!message || !Array.isArray(message)) return;

    const stopMessage = message.find((item) => item.stopPolling);

    if (stopMessage && stopMessage.endTime) {
      // Calculate the elapsed time using the backend-provided start time and end time
      const backendEndTime = stopMessage.endTime;
      const backendStartTime = stopMessage.startTime;
      const elapsedTime = Math.floor(
        (backendEndTime - backendStartTime) / 1000
      ); // In seconds
      console.log("Elapsed Time (seconds):", elapsedTime);

      setElapsedTime(elapsedTime); // Save the elapsed time in state (for UI)

      // You can also save this information to the backend or do further processing
      // sendElapsedTimeToBackend(backendStartTime, backendEndTime, elapsedTime);
    }

    setCurrentValues((prevValues) => {
      const updatedData = { ...prevValues };

      message.forEach((item) => {
        // Handle ERZEUGTE_ENERGIE
        if (
          item.difference === "ERZEUGTE_ENERGIE" &&
          Object.prototype.hasOwnProperty.call(item, "ERZEUGTE_ENERGIE") &&
          firstValues["ERZEUGTE_ENERGIE"] !== undefined
        ) {
          const difference = item["ERZEUGTE_ENERGIE"];
          setDifference((prevDifference) => ({
            ...prevDifference,
            ERZEUGTE_ENERGIE: difference,
          }));
        }

        // Handle ERZEUGTE_ENERGIE_HEATING
        if (
          item.difference === "ERZEUGTE_ENERGIE_HEATING" &&
          Object.prototype.hasOwnProperty.call(
            item,
            "ERZEUGTE_ENERGIE_HEATING"
          ) &&
          firstValues["ERZEUGTE_ENERGIE_HEATING"] !== undefined
        ) {
          const difference = item["ERZEUGTE_ENERGIE_HEATING"];
          setDifference((prevDifference) => ({
            ...prevDifference,
            ERZEUGTE_ENERGIE_HEATING: difference,
          }));
        }

        // Handle GAS_METER
        if (
          item["difference"] === "GAS_ZAHLER" &&
          item["GAS_ZAHLER"] !== undefined
        ) {
          const newGasMeterDifference = item["GAS_ZAHLER"];
          setGasMeterDifference((prevDifference) => ({
            ...prevDifference,
            GAS_ZAHLER: newGasMeterDifference,
          }));
          console.log("Updated GAS_METER_difference:", newGasMeterDifference);
        }

        Object.keys(item).forEach((key) => {
          // item is single message key (difference)
          if (key !== "deviceId" && key !== "difference") {
            if (!item.difference) {
              const newValue = item[key];

              console.log("Incoming key:", key, "with value:", item[key]);

              updatedData[key] = { data: newValue };
            }
          }
        });
      });

      return updatedData;
    });
  }, [message]);

  const handleStartTask = async () => {
    if (selectedDevice && isButtonDisabled) {
      setIsMachineTypeSelected(true);
      setIsRunning(true);

      try {
        const response = await startMeasure(selectedDevice, serienNummer);

        const initialData = response.initialData;

        const backendStartTime = response.startTime;

        if (backendStartTime && initialData) {
          setFirstValues(initialData);

          // Calculate how many seconds have already passed
          const initialElapsed = Math.floor(
            (Date.now() - backendStartTime) / 1000
          );
          setElapsedTime(initialElapsed); // Immediately show correct time
          startTimer(backendStartTime); // Begin ticking accurately
        } else {
          console.error("Invalid response. Timer will not start.");
          setIsRunning(false);
        }
      } catch (error) {
        console.error("Error starting measurement:", error);
        setIsRunning(false);
      }
    }
  };

  const handleStopTask = async () => {
    if (selectedDevice) {
      try {
        const response = await stopMeasure(selectedDevice);
        setLastValues(response.lastData);

        // Assuming lastData contains startTime and endTime, use them to calculate the duration
        const backendEndTime = response.endTime; // Get endTime from backend response
        const backendStartTime = response.startTime; // Get startTime from backend response

        if (backendStartTime && backendEndTime) {
          const elapsedTime = Math.floor(
            (backendEndTime - backendStartTime) / 1000
          ); // In seconds
          console.log("Polling elapsed time:", elapsedTime);
          setElapsedTime(elapsedTime);

          // Optionally, send this elapsed time to the backend for logging or persistence
          // sendElapsedTimeToBackend(backendStartTime, backendEndTime, elapsedTime);
        }
      } catch (error) {
        console.error("Error stopping measurement:", error);
      } finally {
        setIsRunning(false);
        setStopped(true);
        stopTimer(); // Stop the timer
        setIsMachineTypeSelected(false);

        if (socket.readyState === WebSocket.OPEN) {
          // WebSocket.OPEN 1 The connection is open and ready to communicate.
          console.log(
            "Before closing: WebSocket is open (readyState: " +
              socket.readyState +
              ")"
          );
          socket.close();
          console.log(
            "After closing: WebSocket is now closed (readyState: " +
              socket.readyState +
              ")"
          );
        } else {
          console.log(
            "WebSocket is not open. Current readyState: " + socket.readyState
          );
        }
      }
    }
  };

  const handleReset = () => {
    setIsRunning(false); // Stop the task if it's running
    setStopped(false); // Reset the stopped state
    // Reset selected values to defaults
    setSelectedDevice(null); // Clear selected device
    setSelectedMachineType(""); // Clear selected machine type

    // Reset other values as needed
    setFirstValues({});
    setCurrentValues({});
    setLastValues({});
    setDifference({});
    setMachineTypeBorders({});
    setGasMeterDifference({});
    setIsRunning(false); // Mark task as stopped

    stopTimer(); // Stop the timer if needed
    resetTimer();

    setOutOfRangeState({});
    setIsMachineTypeSelected(false);
    setDefaultBorders({});
    setSerienNummer(""); // Clear the Seriennummer
    setIsButtonDisabled(false); // Enable the Check Status button
    setOnStopPolling(false);

    if (socket) {
      socket.close(); // Close WebSocket connection if open
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
    handleStopTask,
    selectedMachineType,
    setSelectedMachineType,
    machineTypeBorders,
    handleMachineTypeChange,
    gasMeterDifference,
    setGasMeterDifference,
    error,
    handleReset,
    isStopped,
    setStopped,
    setMachineTypeBorders,
    outOfRangeState,
    setOutOfRangeState,
    isMachineTypeSelected,
    setIsMachineTypeSelected,
    machineTypes,
    defaultBorders,
    setDefaultBorders,
    handleInputChange,
    serienNummer,
    productStatus,
    fetchStatus,
    isButtonDisabled,
    resetTimer,
    startTimer,
    stopTimer,
    elapsedTime,
    onStopPolling,
    setOnStopPolling,
    ubertragenData,
    handleUbertragen,
  };
};

export { useDeviceMonitor };
