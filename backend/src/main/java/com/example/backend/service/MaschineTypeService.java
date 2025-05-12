package com.example.backend.service;

import com.example.backend.models.ValueRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class MaschineTypeService {
  private final Map<String, Map<String, ValueRange>> machineTypes;

  // Constructor that takes the JSON file resource and loads it into machineTypes map
  public MaschineTypeService(@Value("classpath:maschine-types.json") Resource resource) throws IOException {
    // Check if the resource exists
    if (!resource.exists()) {
      throw new IOException("Resource 'maschine-types.json' not found");
    }

    ObjectMapper objectMapper = new ObjectMapper();
    TypeReference<Map<String, Map<String, ValueRange>>> typeRef = new TypeReference<>() {};
    // Log or debug the resource URI for better visibility
    System.out.println("Resource URI: " + resource.getURI());

    // Read the resource into machineTypes
    this.machineTypes = objectMapper.readValue(resource.getInputStream(), typeRef);
  }


  // Get all machine types
  public Set<String> getAllTypes() {
    return machineTypes.keySet();
  }

  // Fetch borders (same as machineType in your case)
  public Map<String, ValueRange> getMachineTypeBorders(String typeName) {
    return machineTypes.getOrDefault(typeName, new HashMap<>());
  }
  // Fetch default values (from the "DEFAULT" section of your JSON)
  public Map<String, ValueRange> getDefaultValues(int deviceId) {
    return machineTypes.getOrDefault("DEFAULT", Collections.emptyMap());
  }
}
