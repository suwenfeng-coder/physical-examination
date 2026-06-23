package com.hospital.examination.repository;

import com.hospital.examination.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    @Query("select d from Doctor d left join d.department dept order by coalesce(dept.name, d.legacyDepartment), d.name")
    List<Doctor> findAllOrdered();

    @Query("select d from Doctor d left join d.department dept where d.enabled = true order by coalesce(dept.name, d.legacyDepartment), d.name")
    List<Doctor> findEnabledOrdered();
}
