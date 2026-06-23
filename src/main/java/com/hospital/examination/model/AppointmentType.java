package com.hospital.examination.model;

public enum AppointmentType {
    PERSONAL("个人体检"),
    ORGANIZATION("单位体检");

    private final String label;

    AppointmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
