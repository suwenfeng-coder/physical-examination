package com.hospital.examination.repository;

import com.hospital.examination.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByNameContainingIgnoreCaseOrPhoneContainingOrderByIdDesc(String name, String phone);
}
