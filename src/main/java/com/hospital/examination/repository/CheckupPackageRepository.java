package com.hospital.examination.repository;

import com.hospital.examination.model.CheckupPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckupPackageRepository extends JpaRepository<CheckupPackage, Long> {
    List<CheckupPackage> findAllByOrderByIdDesc();
    List<CheckupPackage> findByEnabledTrueOrderByIdDesc();
}
