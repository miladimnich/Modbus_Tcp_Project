package com.example.backend.service.machine;

import com.example.backend.models.ValueRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class MachineTypeService {

    private final Map<String, Map<String, ValueRange>> machineTypes;

    // Constructor that takes the JSON file resource and loads it into machineTypes map
    public MachineTypeService(@Value("classpath:machine-types.json") Resource resource) throws IOException {
        // Check if the resource exists
        if (!resource.exists()) {
            throw new IOException("Resource 'machine-types.json' not found");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<Map<String, Map<String, ValueRange>>> typeRef = new TypeReference<>() {};
        // Log or debug the resource URI for better visibility
        log.info("Loaded machine-types.json from URI: {}", resource.getURI());

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
    public Map<String, ValueRange> getDefaultValues() {
        return machineTypes.getOrDefault("DEFAULT", Collections.emptyMap());
    }
}
