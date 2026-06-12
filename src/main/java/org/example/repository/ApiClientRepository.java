package org.example.repository;

import org.example.entity.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiClientRepository extends JpaRepository<ApiClient, Long> {

    Optional<ApiClient> findByApiKeyAndIsActiveTrue(String apiKey);

    List<ApiClient> findByIsActiveTrue();

    boolean existsByApiKey(String apiKey);
}
