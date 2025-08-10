package com.example.backend.repository;



import com.example.backend.models.ProductionProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ProductionProtocolRepository extends JpaRepository<ProductionProtocol, Integer> {
    ProductionProtocol findByDirectReference1Number(Integer directReference1Number);
}
