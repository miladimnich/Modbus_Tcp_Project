import axios from "axios";

const apiClient = axios.create({
  baseURL: "http://localhost:8080/api", // Backend API base URL
});

export const fetchDevices = async () => {
  try {
    const response = await apiClient.get("/devices");
    if (response.status === 503) {
      console.log("Devices are not ready yet. Retrying...");
      setTimeout(fetchDevices, 2000); // Retry after 2 seconds
    } else {
      return response; // Successfully received devices
    }
  } catch (error) {
    console.error("Error fetching devices:", error);
  }
};

export const startMeasure = async (deviceId, serienNummer) => {
  try {
    const response = await apiClient.post(`/devices/${deviceId}/startMeasure`, {
      serienNummer: serienNummer,
    });
    return response.data;
  } catch (error) {
    console.error("Error starting task:", error);
    throw error;
  }
};

export const stopMeasure = async (deviceId) => {
  try {
    const response = await apiClient.post(`/devices/${deviceId}/stopMeasure`);
    console.log("Stop measure response:", response);
    return response.data; // Corrected the typo here
  } catch (error) {
    console.error("Error stopping task:", error);
    throw error;
  }
};

export const fetchMaschineConfig = async (typeName) => {
  try {
    const response = await apiClient.get(`/machine-types/${typeName}`);
    return response.data;
  } catch (error) {
    console.error("Error fetching machine configuration:", error);
    throw error;
  }
};

export const fetchMaschineBordersDefault = async (deviceId) => {
  try {
    const response = await apiClient.get(`/devices/${deviceId}/borders`);
    return response.data;
  } catch (error) {
    console.error("Error fetching machine configuration:", error);
    throw error;
  }
};

// This function fetches all available machine types
export const fetchMachineTypes = async () => {
  try {
    const response = await apiClient.get(`/machine-types`);
    return response.data; // Assuming this returns an array of machine types
  } catch (error) {
    console.error("Error fetching machine types:", error);
    throw error;
  }
};

export const fetchProductStatus = async (serienNummer) => {
  try {
    const response = await apiClient.get(`/product/status/${serienNummer}`);
    return response.data; // Assuming this returns an array of machine types
  } catch (error) {
    console.error("Error fetching product status:", error);
    throw error;
  }
};

export const ubertragenData = async (deviceId) => {
  try {
    const response = await apiClient.post(`/protokolle/${deviceId}/ubertragen`);
    return response.data;
  } catch (error) {
    console.error("Error fetching machine configuration:", error);
    throw error;
  }
};
