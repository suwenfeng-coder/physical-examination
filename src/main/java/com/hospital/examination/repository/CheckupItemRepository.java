package com.hospital.examination.repository;

import com.hospital.examination.model.CheckupItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CheckupItemRepository extends JpaRepository<CheckupItem, Long> {
    @Query("select i from CheckupItem i left join i.department dept order by coalesce(dept.name, i.legacyDepartment), i.name")
    List<CheckupItem> findAllOrdered();
}
