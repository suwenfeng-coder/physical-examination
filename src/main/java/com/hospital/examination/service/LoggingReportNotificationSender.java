package com.hospital.examination.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "logging", matchIfMissing = true)
public class LoggingReportNotificationSender implements ReportNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingReportNotificationSender.class);

    @Override
    public void sendPickupReady(String phone, String patientName, String orderNo) {
        log.info("[开发短信] 手机号={}，内容={}您好，体检报告（{}）已审核完成，请到医院领取。",
                mask(phone), patientName, orderNo);
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "未知";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
