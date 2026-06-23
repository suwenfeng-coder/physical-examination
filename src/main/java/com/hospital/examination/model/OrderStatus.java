package com.hospital.examination.model;

public enum OrderStatus {
    REGISTERED("已登记"),
    EXAMINING("检查中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
