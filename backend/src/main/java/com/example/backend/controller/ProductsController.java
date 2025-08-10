package com.example.backend.controller;


import com.example.backend.config.MeasurementSessionRegistry;
import com.example.backend.service.production.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/product")
public class ProductsController {
    private final ProductService productService;
    private final MeasurementSessionRegistry measurementSessionRegistry;

    public ProductsController(ProductService productService, MeasurementSessionRegistry measurementSessionRegistry) {
        this.productService = productService;
        this.measurementSessionRegistry = measurementSessionRegistry;
    }

    @GetMapping("/status/{serialNumber}")
    public ResponseEntity<Short> getProductStatus(@PathVariable Integer serialNumber) {
        Short status = productService.getProductStatusBySerialNumber(serialNumber);

        measurementSessionRegistry.registerSerialNumber(serialNumber);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
