package com.hospital.examination.repository;

import com.hospital.examination.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByOrderBySortOrderAscNameAsc();
    List<Department> findByEnabledTrueAndNameContainingIgnoreCaseOrderBySortOrderAscNameAsc(String keyword);
    Optional<Department> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
