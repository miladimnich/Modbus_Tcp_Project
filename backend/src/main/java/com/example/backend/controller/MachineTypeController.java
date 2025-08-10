package com.example.backend.controller;


import com.example.backend.models.ValueRange;
import com.example.backend.service.machine.MachineTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/machine-types")
public class MachineTypeController {
    private final MachineTypeService machineTypeService;

    public MachineTypeController(MachineTypeService machineTypeService) {
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
    public ResponseEntity<Map<String, ValueRange>> getTypeDetails(@PathVariable String typeName) {
        Map<String, ValueRange> details = machineTypeService.getMachineTypeBorders(typeName);
        if (details == null || details.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }
}
