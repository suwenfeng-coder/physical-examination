package com.hospital.examination.repository;

import com.hospital.examination.model.CheckupPackage;
import com.hospital.examination.model.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckupPackageRepository extends JpaRepository<CheckupPackage, Long> {
    List<CheckupPackage> findAllByOrderByIdDesc();
    List<CheckupPackage> findByTypeOrderByIdDesc(PackageType type);
    List<CheckupPackage> findByEnabledTrueOrderByIdDesc();
    List<CheckupPackage> findByEnabledTrueAndTypeOrderByIdDesc(PackageType type);
}
