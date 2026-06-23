package com.hospital.examination.repository;

import com.hospital.examination.model.PackageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageTemplateRepository extends JpaRepository<PackageTemplate, Long> {
    List<PackageTemplate> findAllByOrderByIdDesc();
    List<PackageTemplate> findByEnabledTrueOrderByIdDesc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
