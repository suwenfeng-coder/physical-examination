package com.hospital.examination.repository;

import com.hospital.examination.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByCreditCode(String creditCode);
    Optional<Organization> findFirstByNameIgnoreCase(String name);
}
