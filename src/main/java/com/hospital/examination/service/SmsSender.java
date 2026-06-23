package com.hospital.examination.service;

import com.hospital.examination.model.SmsPurpose;

public interface SmsSender {
    void send(String phone, String code, SmsPurpose purpose);
}
