package com.example.backend.service;

import com.example.backend.models.ProductStatus;
import com.example.backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

  @Autowired
  ProductRepository productRepository;

  public Short getProductStatusBySerienNummer(Integer serienNummer) {
    ProductStatus status = productRepository.findBySerienNummer(serienNummer);

    if (status == null) {
      throw new EntityNotFoundException("No product found for SerienNummer: " + serienNummer);
    }
    return status.getProduktStatus();
  }

}
