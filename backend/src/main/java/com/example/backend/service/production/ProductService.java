package com.example.backend.service.production;

import com.example.backend.models.ProductStatus;
import com.example.backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

  private final ProductRepository productRepository;

  public ProductService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public Short getProductStatusBySerialNumber(Integer serialNumber) {
    ProductStatus status = productRepository.findBySerialNumber(serialNumber);

    if (status == null) {
      throw new EntityNotFoundException("No product found for serialNumber: " + serialNumber);
    }
    return status.getProductStatus();
  }

}
