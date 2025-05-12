package com.example.backend.repository;

import com.example.backend.models.Werksprotokolle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WerksprotokolleRepository extends JpaRepository<Werksprotokolle, Integer> {
  Werksprotokolle findByDirektBezug1Nr(Integer direktBezug1Nr);
}
