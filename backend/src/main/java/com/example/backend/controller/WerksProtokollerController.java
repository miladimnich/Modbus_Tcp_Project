package com.example.backend.controller;

import com.example.backend.models.Werksprotokolle;
import com.example.backend.service.WerksprotokolleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protokolle")
public class WerksProtokollerController {

  @Autowired
  private WerksprotokolleService werksprotokolleService;

  @PostMapping("{deviceId}/ubertragen")
  public ResponseEntity<Werksprotokolle> ubertragen(@PathVariable int deviceId) {
    return ResponseEntity.ok(werksprotokolleService.recordData(deviceId));
  }
}

