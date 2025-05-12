package com.example.backend.controller;

import com.example.backend.models.ValueRange;
import com.example.backend.service.MaschineTypeService;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/machine-types")
public class MaschineTypeController {
  private final MaschineTypeService machineTypeService;


  // Constructor for dependency injection
  public MaschineTypeController(MaschineTypeService machineTypeService) {
    this.machineTypeService = machineTypeService;
  }

  // Method to get all machine types
  @GetMapping
  public Set<String> getAllTypeNames() {
    return machineTypeService.getAllTypes()
        .stream()
        .filter(type -> !"DEFAULT".equalsIgnoreCase(type))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @GetMapping("/{typeName}")
  public Map<String, ValueRange> getTypeDetails(@PathVariable String typeName) {
    return machineTypeService.getMachineTypeBorders(typeName);
  }
}

