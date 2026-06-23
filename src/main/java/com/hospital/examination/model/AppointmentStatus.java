package com.hospital.examination.model;

public enum AppointmentStatus {
    BOOKED("已预约"),
    CANCELLED("已取消");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
