package com.example.backend.controller;

import com.example.backend.models.ProductionProtocol;
import com.example.backend.service.production.ProductionProtocolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/protocols")
public class ProductionProtocolController {


    private final ProductionProtocolService productionProtocolService;

    public ProductionProtocolController(ProductionProtocolService productionProtocolService) {
        this.productionProtocolService = productionProtocolService;
    }

    @PostMapping("/submit")
    public ResponseEntity<ProductionProtocol> submit() {
        ProductionProtocol protocol = productionProtocolService.recordData();
        if (protocol == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(null); // or use ResponseEntity.of(Optional.empty());
        }
        return ResponseEntity.ok(protocol);
    }
}
