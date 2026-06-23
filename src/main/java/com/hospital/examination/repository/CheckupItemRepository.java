package com.hospital.examination.repository;

import com.hospital.examination.model.CheckupItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckupItemRepository extends JpaRepository<CheckupItem, Long> {
    List<CheckupItem> findAllByOrderByDepartmentAscNameAsc();
}
