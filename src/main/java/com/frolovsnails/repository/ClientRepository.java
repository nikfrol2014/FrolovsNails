package com.frolovsnails.repository;

import com.frolovsnails.entity.Client;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT c FROM Client c WHERE c.user.id = :userId")
    Optional<Client> findByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT c FROM Client c WHERE c.user.phone = :phone")
    Optional<Client> findByUserPhone(@Param("phone") String phone);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Client c WHERE c.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}