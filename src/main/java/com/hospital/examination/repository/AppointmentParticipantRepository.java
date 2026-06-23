package com.hospital.examination.repository;

import com.hospital.examination.model.AppointmentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentParticipantRepository extends JpaRepository<AppointmentParticipant, Long> {
}
