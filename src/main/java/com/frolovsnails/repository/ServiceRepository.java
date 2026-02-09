package com.frolovsnails.repository;

import com.frolovsnails.entity.Service;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Эти запросы простые, EntityGraph не нужен
    List<Service> findByCategory(String category);
    List<Service> findByIsActiveTrue();
    List<Service> findByCategoryAndIsActiveTrue(String category);

    @Query("SELECT DISTINCT s.category FROM Service s WHERE s.isActive = true")
    List<String> findDistinctActiveCategories();

    Optional<Service> findById(@NonNull Long id);
}