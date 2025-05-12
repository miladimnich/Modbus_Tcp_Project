package com.example.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

@Service
public class FileService {
  @Value("classpath:bounds.txt")
  private Resource boundsFile;
  public String readBoundsFile() throws IOException {
    //return new String(Files.readAllBytes(Paths.get(boundsFile.getURI())));
    // Read file as String
    String content = new String(Files.readAllBytes(Paths.get(boundsFile.getURI())));

    // Print to console for inspection
    System.out.println("File Content: " + content);

    return content;

  }

}
