package com.hospital.examination.repository;

import com.hospital.examination.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findAllByOrderByDepartmentAscNameAsc();
    List<Doctor> findByEnabledTrueOrderByDepartmentAscNameAsc();
}
