package com.example.backend.repository;

import com.example.backend.models.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<ProductStatus, Integer> {
  ProductStatus findBySerienNummer(Integer serienNummer);
}

