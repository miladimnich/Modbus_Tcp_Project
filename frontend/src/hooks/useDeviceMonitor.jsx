import { useState, useEffect, useRef } from 'react';
import useWebSocket from "./useWebSocket";
import { fetchTestStations, startMeasure, fetchMaschineConfig, fetchMachineTypes, fetchMaschineBordersDefault, fetchProductStatus, submit, stopMeasure, setAutoStopDuration } from '../api/apiClient';
import { toast } from "react-toastify";


const useDeviceMonitor = () => {
  const [testStations, setTestStations] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const { socket, message, isOpen, error } = useWebSocket(selectedDevice);
  const [currentValues, setCurrentValues] = useState({});
  const [firstValues, setFirstValues] = useState({});
  const [lastValues, setLastValues] = useState({});
  const [isRunning, setIsRunning] = useState(false);
  const [difference, setDifference] = useState({});
  const [gasMeterDifference, setGasMeterDifference] = useState({});
  const [selectedMachineType, setSelectedMachineType] = useState(''); // Initially no machine type selected
  const [machineTypeBorders, setMachineTypeBorders] = useState({}); // Default min/max for ASV_20
  const [machineTypes, setMachineTypes] = useState([]);
  const [isMachineTypeSelected, setIsMachineTypeSelected] = useState(false);
  const previousValuesRef = useRef({});

  const [serialNumber, setSerialNumber] = useState("");
  const [productStatus, setProductStatus] = useState(null);


  const [outOfRangeState, setOutOfRangeState] = useState({});
  const [defaultBorders, setDefaultBorders] = useState({});
  const [isButtonDisabled, setIsButtonDisabled] = useState(false); // Initially, the button is not disabled

  // Timer-related states
  const [elapsedTime, setElapsedTime] = useState(0); // Track elapsed time
  const intervalIdRef = useRef(null);
  // const [timerStarted, setTimerStarted] = useState(false);


  const [isStopped, setStopped] = useState(false);  // Track if the task has been started
  const [isReset, setIsReset] = useState(false);
  const [hasEvaluatedAfterFirstDiff, setHasEvaluatedAfterFirstDiff] = useState(false);

  const [autoStopMinutes, setAutoStopMinutes] = useState(120);
  const [backendStartTime, setBackendStartTime] = useState({});


  useEffect(() => {
    if (!isOpen) {
      console.log("WebSocket closed, stopping timer");
      setIsRunning(false);
      setStopped(true);
      stopTimer();
      //  setIsMachineTypeSelected(false);
    }
  }, [isOpen]);  // always called in every render


  useEffect(() => {
    console.log("Starting to fetch devices...");
    fetchTestStations()
      .then((testStations) => {
        console.log("Fetched devices:", testStations); // ✅ devices is already the data
        setTestStations(testStations);
      })
      .catch((error) => {
        console.error("Error fetching devices:", error);
      });
  }, []);



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
    setSelectedMachineType(event.target.value);  // Update selected machine type
  };

  const handleInputChange = (event) => {
    setSerialNumber(event.target.value);
  };

  const handleUbertragen = async () => {
    try {
      const response = await submit(selectedDevice); // Assuming selectedDevice is the deviceId
      toast.success(response.message || "Daten erfolgreich übertragen");  // Show success message if available
    } catch (error) {
      toast.error("Fehler beim Übertragen der Daten");  // Show error message
    }
  };


  const fetchStatus = async () => {

    if (!serialNumber) return;
    try {
      const data = await fetchProductStatus(serialNumber);
      setProductStatus(data);
      // Check if the product status is 70
      if (data === 20) {
        toast.success(`Produktstatus: Testbetrieb ${data}`);
        setIsButtonDisabled(true); // Disable the button if status is 70
      } else {
        toast.warn(`Falscher Produktstatus ${data}`);
      }
    } catch (error) {
      console.error("Fehler beim Abrufen des Status", error);
      setProductStatus(null);
      toast.error("Fehler beim Abrufen des Status.");
    }
  };



  const handleDeviceChange = async (event) => {
    const testStationId = event.target.value;
    setSelectedDevice(testStationId);

    if (!isRunning) {
      resetTimer();
    }
    // Fetch default borders
    try {
      const defaultBorders = await fetchMaschineBordersDefault();
      console.log("Setting defaultBorders to:", defaultBorders);
      setDefaultBorders(defaultBorders);
      console.log("After setDefaultBorders:", defaultBorders);
    } catch (error) {
      console.error("Failed to fetch borders for device:", error);
    }
  };

  const startTimer = (backendStartTime) => {
    if (intervalIdRef.current) {
      console.warn("Tried to start timer, but one is already running!");
    }

    console.log('Starting interval timer at:', new Date().toISOString());
    // Update immediately to avoid delay
    const now = Date.now();
    let elapsed = Math.floor((now - backendStartTime) / 1000) - 1;
    if (elapsed < 0) elapsed = 0;
    setElapsedTime(elapsed);

    intervalIdRef.current = setInterval(() => {
      const now = Date.now();
      const elapsed = Math.floor((now - backendStartTime) / 1000);
      console.log('Timer tick:', elapsed, 'seconds elapsed');
      setElapsedTime(elapsed);
    }, 1000);
    console.log('Interval started with ID:', intervalIdRef.current);
  };

  const stopTimer = () => {
    console.log('Stopping timer, intervalId:', intervalIdRef.current);
    if (intervalIdRef.current) {
      clearInterval(intervalIdRef.current);
      console.log("Interval stopped. No further ticks should happen.");
      intervalIdRef.current = null;
    }
  };

  const resetTimer = () => {
    stopTimer();
    setElapsedTime(0);
  };


  useEffect(() => {
    return () => {
      if (intervalIdRef.current) {
        console.log("Cleaning up interval on unmount");
        clearInterval(intervalIdRef.current);
        intervalIdRef.current = null;
      }
    };
  }, []);



  const evaluateOutOfRangeState = () => {
    setOutOfRangeState(prevState => {
      console.log("prevState is:", prevState); // 👈 See what React actually passes
      const newState = { ...prevState }; // Start from existing out-of-range marks

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
              key === "ELECTRICAL_EFFICIENCY" || key === "THERMAL_EFFICIENCY" || key === "OVERALL_EFFICIENCY" ? elapsedTime >= 20 * 60 : true;
            if (shouldCompare) {
              const { min, max } = border;
              const minOut = min !== undefined && value < min;
              const maxOut = max !== undefined && value > max;

              // Only add to newState if value is out of range and key not already present
              if ((minOut || maxOut) && !newState[key]) {
                newState[key] = {
                  min: minOut,
                  max: maxOut,
                };
              }
            }
          }
        }
      });

      return newState; // Preserve previous keys, add new ones only
    });
  };


  useEffect(() => {
    if (Object.keys(firstValues).length > 0) {
      evaluateOutOfRangeState();
    }
  }, [currentValues]);


  useEffect(() => {
    if (!message) return;
    console.log("🟡 Incoming WebSocket batch:", message);

    const batch = Array.isArray(message) ? message : [message];

    const {
      initialDataFound,
      startTimeFound,
      lastDataFound,
      endTimeFound
    } = extractTimestampsFromBatch(batch);


    updateDifferencesFromBatch(batch);
    updateCurrentValuesFromBatch(batch);


    if (initialDataFound) setFirstValues(initialDataFound);

    if (lastDataFound && Object.keys(lastDataFound).length > 0) {
      setLastValues(lastDataFound);
      toast.success("Messung beendet");
    }


    if (startTimeFound) {
      setBackendStartTime(startTimeFound);
      console.log(`Start time found : startTimeFound seconds`);
      startTimer(startTimeFound);
    }

    if (endTimeFound) {
      const finalElapsed = Math.floor((endTimeFound - backendStartTime) / 1000);
      setElapsedTime(finalElapsed); // ✅ Ensure accurate final time in UI
      stopTimer();
      setIsRunning(false);
      setStopped(true);
      //setIsMachineTypeSelected(false);
    }

  }, [message]);




  function extractTimestampsFromBatch(batch) {
    let initialDataFound = null;
    let startTimeFound = null;
    let lastDataFound = null;
    let endTimeFound = null;

    batch.forEach(item => {


      if (!initialDataFound && item.initialData) {
        initialDataFound = item.initialData;
        console.log('Setting firstValues from handleStartTask', initialDataFound);
      }


      if (!startTimeFound && item.startTime) startTimeFound = item.startTime;
      if (!lastDataFound && item.lastData) lastDataFound = item.lastData;
      if (!endTimeFound && item.endTime) endTimeFound = item.endTime;
    });

    return { initialDataFound, startTimeFound, lastDataFound, endTimeFound };
  }


  function updateDifferencesFromBatch(batch) {
    batch.forEach(item => {
      const { difference } = item;

      if (difference === "GENERATED_ENERGY" && item.GENERATED_ENERGY !== undefined && firstValues["GENERATED_ENERGY"] !== undefined) {
        setDifference(prev => ({
          ...prev,
          GENERATED_ENERGY: item.GENERATED_ENERGY
        }));
      }

      if (difference === "GENERATED_ENERGY_HEATING" && item.GENERATED_ENERGY_HEATING !== undefined && firstValues["GENERATED_ENERGY_HEATING"] !== undefined) {
        setDifference(prev => ({
          ...prev,
          GENERATED_ENERGY_HEATING: item.GENERATED_ENERGY_HEATING
        }));
      }

      if (difference === "GAS_METER" && item.GAS_METER !== undefined) {
        setGasMeterDifference(prev => ({
          ...prev,
          GAS_METER: item.GAS_METER
        }));   
      /*  if (!hasEvaluatedAfterFirstDiff) {
          console.log("Received first GAS_METER difference");
          setHasEvaluatedAfterFirstDiff(true);
        }*/
      }

    });
  }

  function updateCurrentValuesFromBatch(batch) {
    const updatedValues = {};
    let hasChange = false;

    batch.forEach(item => {//item - GENERATED_ENERGY ="362578.03"testStationId =1
      Object.keys(item).forEach(key => {//key GENERATED_ENERGY
        if (key !== "testStationId" &&
          key !== "difference" &&
          key !== "initialData" &&
          key !== "lastData" &&
          key !== "startTime" &&
          key !== "endTime") {
          if (!item.difference && item[key] !== undefined) {
            const newVal = item[key]; // convert to number to normalize
            const oldVal = previousValuesRef.current[key];

            // If key not present before OR value changed, update
            if (oldVal === undefined || oldVal !== newVal) {
              console.log(`🔄 Updating key: ${key} | Old: ${oldVal} → New: ${newVal}`);
              previousValuesRef.current[key] = newVal;
              updatedValues[key] = { data: newVal };
              hasChange = true;
            }
          }
        }
      });
    });

    if (hasChange) {
      setCurrentValues(prev => ({ ...prev, ...updatedValues }));
    }
  }



  const durationOptions = [
    { value: 60, label: "1 Stunde" },
    { value: 90, label: "1.5 Stunde" },
    { value: 120, label: "2 Stunde" },
    { value: 180, label: "3 Stunde" },
    { value: 240, label: "4 Stunde" },
    { value: 300, label: "5 Stunde" },
  ];



  const handleAutoStopMinutes = (event) => {
    setAutoStopMinutes(Number(event.target.value));
  };



  const handleStartTask = async () => {
    if (!selectedDevice || !isButtonDisabled || !isOpen) return; // double check logic here

    try {
      await setAutoStopDuration(Number(autoStopMinutes)); // <- trigger your endpoint here
      toast.success(`Auto-Stopp auf ${autoStopMinutes} Minuten eingestellt`);

      setIsMachineTypeSelected(true);
      setIsRunning(true);
      setIsReset(true); // Mark that Start was triggered

      await startMeasure(selectedDevice);
    } catch (error) {
      console.error("Error starting measurement:", error);
      toast.error("Messung konnte nicht gestartet werden.");
      setIsRunning(false);
    }
  };



  const handleStopTask = async () => {
    if (!selectedDevice) return;
    try {
      await stopMeasure(selectedDevice);  // just trigger stop, no need to parse response
    } catch (error) {
      console.error("Error stopping measurement:", error);
      toast.error("Messung konnte nicht gestoppt werden. Bitte versuchen Sie es erneut.");
    } finally {
      setIsRunning(false);
      setStopped(true);
      stopTimer();
      
      //setIsMachineTypeSelected(false);
    }
  };



  const handleReset = () => {
    setIsRunning(false);  // Stop the task if it's running
    setStopped(true);  // Reset the stopped state
    // Reset selected values to defaults
    setSelectedDevice(null);  // Clear selected device
    setSelectedMachineType('');  // Clear selected machine type

    // Reset other values as needed
    setFirstValues({});
    setCurrentValues({});
    setLastValues({});
    setDifference({});
    setMachineTypeBorders({});
    setGasMeterDifference({});
    setIsReset(false); // Reset this as well

    stopTimer();  // Stop the timer if needed
    resetTimer();

    setOutOfRangeState({});
    setIsMachineTypeSelected(false);
    setDefaultBorders({});
    setSerialNumber("");  // Clear the Seriennummer
    setIsButtonDisabled(false);  // Enable the Check Status button

    // **Reset backend start/end times!**
    setBackendStartTime(null);
    setAutoStopMinutes(120); // reset to default
    previousValuesRef.current = {};

    if (socket) {
      console.log("socket is :", socket);
      socket.close(); // Close WebSocket connection if open
    }

  };


  return {
    testStations,
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
    serialNumber,
    productStatus,
    fetchStatus,
    isButtonDisabled,
    resetTimer,
    startTimer,
    stopTimer,
    elapsedTime,
    intervalIdRef,




    submit,
    handleUbertragen,
    isReset,
    hasEvaluatedAfterFirstDiff,
    autoStopMinutes,
    handleAutoStopMinutes,
    durationOptions


  };
};

export { useDeviceMonitor };




