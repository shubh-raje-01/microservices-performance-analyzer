package com.analyzer.service_registry.repository;

import com.analyzer.service_registry.model.Service;
import com.analyzer.service_registry.model.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, String> {

    Optional<Service> findByName(String name);

    List<Service> findByStatus(ServiceStatus status);

    List<Service> findByNameContainingIgnoreCase(String name);

    @Query("SELECT s FROM Service s WHERE s.disabledAt IS NULL AND s.status != 'DISABLED'")
    List<Service> findAllEnabled();

    @Query("SELECT s FROM Service s WHERE s.disabledAt IS NULL AND s.status != 'DISABLED'")
    Page<Service> findAllEnabled(Pageable pageable);

    @Query("""
        SELECT s FROM Service s
        WHERE s.updatedAt < :threshold
          AND s.disabledAt IS NULL
          AND s.status != 'DISABLED'
    """)
    List<Service> findStaleServices(@Param("threshold") Instant threshold);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);

    @Query("SELECT COUNT(s) FROM Service s WHERE s.status = :status AND s.disabledAt IS NULL")
    long countByStatus(@Param("status") ServiceStatus status);
}
