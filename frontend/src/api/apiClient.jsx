import axios from "axios";

const apiClient = axios.create({
    baseURL: "http://localhost:8080/api",  // Backend API base URL
});

// Fetch devices from the backend
export const fetchDevices = async () => {
  // eslint-disable-next-line no-useless-catch
  try {
    const response = await apiClient.get('/devices'); // Endpoint to fetch devices
    return response;
  } catch (error) {
    throw error; // Handle error if the request fails
  }
};


export const getCurrentValue = async (deviceId) => {
  try {
    const response = await apiClient.post(`/devices/${deviceId}`);
    return response;  // Corrected the typo here
  } catch (error) {
    console.error('Error starting task:', error);
    throw error;
  }
};

export const startMeasure = async (deviceId) => {
  try {
    const response = await apiClient.post(`/devices/${deviceId}/startMeasure`);
    return response.data;  // Corrected the typo here
  } catch (error) {
    console.error('Error starting task:', error);
    throw error;
  }
};

export const stopMeasure = async (deviceId) => {
  try {
    const response = await apiClient.post(`/devices/${deviceId}/stopMeasure`);
    return response.data;  // Corrected the typo here
  } catch (error) {
    console.error('Error stopping task:', error);
    throw error;
  }
};
