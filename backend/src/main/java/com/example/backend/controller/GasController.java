package com.example.backend.controller;

 import com.example.backend.service.gas.GasService;
 import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gas")
public class GasController {

    private final GasService gasService;

    public GasController(GasService gasService) {
        this.gasService = gasService;
    }

    @PostMapping("/setAutoStopDuration")
    public ResponseEntity<String> setAutoStopDuration(@RequestParam long minutes) {
        if (minutes <= 0 || minutes > 1440) { //1440 24 hours
            return ResponseEntity.badRequest().body("Invalid duration (1 to 1440 minutes allowed)");
        }
        gasService.setAutoStopDurationMinutes(minutes);
        return ResponseEntity.ok("Auto-stop duration set to " + minutes + " minutes.");
    }
}
