package com.hospital.examination.model;

public enum ResultStatus {
    PENDING("待检查"),
    NORMAL("正常"),
    ABNORMAL("异常");

    private final String label;

    ResultStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
