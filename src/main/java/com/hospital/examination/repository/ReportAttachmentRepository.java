package com.hospital.examination.repository;

import com.hospital.examination.model.ReportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportAttachmentRepository extends JpaRepository<ReportAttachment, Long> {
}
