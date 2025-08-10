import axios from 'axios';
import { toast } from 'react-toastify';


// API configuration and endpoint constants
const apiClient = axios.create({
  baseURL: "http://localhost:8080/api",  // Backend API base URL

});

const API_ENDPOINTS = {
  testStations: '/testStations',
  machineTypes: '/machine-types',
  productStatus: '/product/status',
  protocols: '/protocols',
  gas: '/gas',
};

// Utility function for retrying API requests
const retryRequest = async (fn, retryCount = 0, maxRetries = 5, retryDelay = 2000) => {
  try {
    return await fn();
  } catch (error) {
    if (retryCount < maxRetries) {
      console.log(`Retrying request... (${retryCount + 1}/${maxRetries})`);
      await new Promise(resolve => setTimeout(resolve, retryDelay)); // Wait for 2 seconds
      return retryRequest(fn, retryCount + 1, maxRetries, retryDelay); // Retry
    }
    throw error; // Throw the error after max retries
  }
};

// Fetch devices
export const fetchTestStations = async () => {
  return retryRequest(async () => {
    try {
      const response = await apiClient.get(API_ENDPOINTS.testStations);
      console.log('Response status:', response.status);
      console.log('Response headers:', response.headers);
      if (response.data && Array.isArray(response.data)) {
        console.log('Fetched test stations:', response.data);
        return response.data;
      }
    } catch (error) {
      console.error('Error fetching test stations:', error);
      throw error; // re-throw so retryRequest can retry or caller can handle it
    }
  });
};



export const startMeasure = async (testStationId) => {
  try {
    await apiClient.post(`${API_ENDPOINTS.testStations}/${testStationId}/startMeasure`, {
    });
  } catch (error) {
    console.error('Error starting task:', error);
    throw error; // You may handle errors globally with a toast or user-friendly message
  }
};


export const stopMeasure = async (testStationId) => {
  try {
    await apiClient.post(`${API_ENDPOINTS.testStations}/${testStationId}/stopMeasure`);
  } catch (error) {
    console.error('Error stopping task:', error);
    throw error; // Handle or propagate the error as needed
  }
};

export const setAutoStopDuration = async (minutes) => {
  try {
    console.log("Calling backend with minutes=", minutes);
    const response = await apiClient.post(`${API_ENDPOINTS.gas}/setAutoStopDuration`, null, {
      params: { minutes }
    });
    console.log("Success:", response.data);
  } catch (error) {
    throw error; // Optional: only if you want to block next steps
  }
}


// Fetch machine config
export const fetchMaschineConfig = async (typeName) => {
  try {
    const response = await apiClient.get(`${API_ENDPOINTS.machineTypes}/${typeName}`);
    return response.data;
  } catch (error) {
    console.error("Error fetching machine configuration:", error);
    toast.error("Error fetching machine configuration.");
    throw error; // You can also return a default value or an empty object if appropriate
  }
};

// Fetch machine borders for a device
export const fetchMaschineBordersDefault = async () => {
  try {
    const response = await apiClient.get(`${API_ENDPOINTS.testStations}/borders`);
    return response.data;
  } catch (error) {
    console.error("Error fetching machine borders:", error);
    throw error;
  }
};

// Fetch all available machine types
export const fetchMachineTypes = async () => {
  try {
    const response = await apiClient.get(API_ENDPOINTS.machineTypes);
    return response.data;
  } catch (error) {
    console.error("Error fetching machine types:", error);
    throw error;  // Handle this error appropriately
  }
};

// Fetch product status for a given serienNummer
export const fetchProductStatus = async (serialNumber) => {
  try {
    const response = await apiClient.get(`${API_ENDPOINTS.productStatus}/${serialNumber}`);
    return response.data;
  } catch (error) {
    console.error("Error fetching product status:", error);
    throw error;
  }
};

// Transmit data for a device
export const submit = async () => {
  try {
    const response = await apiClient.post(`${API_ENDPOINTS.protocols}/submit`);
    return response.data;
  } catch (error) {
       if (error.response?.status === 404) {
      throw new Error("Kein passendes Protokoll gefunden."); // Handle specific case
    }
    throw error;
  }
};
