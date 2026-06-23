package com.hospital.examination.repository;

import com.hospital.examination.model.SmsPurpose;
import com.hospital.examination.model.SmsVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmsVerificationCodeRepository extends JpaRepository<SmsVerificationCode, Long> {
    Optional<SmsVerificationCode> findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String phone, SmsPurpose purpose);
}
