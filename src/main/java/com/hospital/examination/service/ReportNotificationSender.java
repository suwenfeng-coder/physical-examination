package com.hospital.examination.service;

public interface ReportNotificationSender {
    void sendPickupReady(String phone, String patientName, String orderNo);
}
