package com.hospital.examination.repository;

import com.hospital.examination.model.ExamOrder;
import com.hospital.examination.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExamOrderRepository extends JpaRepository<ExamOrder, Long> {
    List<ExamOrder> findAllByOrderByExamDateDescIdDesc();
    List<ExamOrder> findByStatusOrderByExamDateDescIdDesc(OrderStatus status);
    long countByExamDate(LocalDate date);
    long countByStatus(OrderStatus status);

    @Query("""
            select o from ExamOrder o
            where lower(o.orderNo) like lower(concat('%', :keyword, '%'))
               or lower(o.patient.name) like lower(concat('%', :keyword, '%'))
               or o.patient.phone like concat('%', :keyword, '%')
            order by o.examDate desc, o.id desc
            """)
    List<ExamOrder> search(@Param("keyword") String keyword);
}
