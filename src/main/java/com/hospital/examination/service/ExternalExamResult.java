package com.hospital.examination.service;

import com.hospital.examination.model.ResultStatus;

public record ExternalExamResult(
        String itemCode,
        String itemName,
        String resultValue,
        ResultStatus status,
        String remark
) {
}
