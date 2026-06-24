package com.hospital.examination.repository;

import com.hospital.examination.model.Appointment;
import com.hospital.examination.model.AppointmentStatus;
import com.hospital.examination.model.AppointmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByOrderByAppointmentDateDescIdDesc();
    List<Appointment> findByTypeOrderByAppointmentDateDescIdDesc(AppointmentType type);
    List<Appointment> findByStatusOrderByAppointmentDateDescIdDesc(AppointmentStatus status);
    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("""
            select distinct a from Appointment a
            left join a.organization organization
            left join a.participants participant
            left join participant.patient patient
            where (:keyword = ''
                   or lower(a.appointmentNo) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(organization.name, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(patient.name, '')) like lower(concat('%', :keyword, '%'))
                   or coalesce(patient.phone, '') like concat('%', :keyword, '%'))
              and (:type is null or a.type = :type)
              and (:status is null or a.status = :status)
            order by a.appointmentDate desc, a.id desc
            """)
    List<Appointment> searchWithFilters(@Param("keyword") String keyword,
                                        @Param("type") AppointmentType type,
                                        @Param("status") AppointmentStatus status);

    @Query("""
            select distinct a from Appointment a
            left join a.organization organization
            left join a.participants participant
            left join participant.patient patient
            where lower(a.appointmentNo) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(organization.name, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(patient.name, '')) like lower(concat('%', :keyword, '%'))
               or coalesce(patient.phone, '') like concat('%', :keyword, '%')
            order by a.appointmentDate desc, a.id desc
            """)
    List<Appointment> search(@Param("keyword") String keyword);
}
