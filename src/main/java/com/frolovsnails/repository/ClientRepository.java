package com.frolovsnails.repository;

import com.frolovsnails.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByUserId(Long userId);
    Optional<Client> findByUserPhone(String phone);
    boolean existsByUserId(Long userId);
}