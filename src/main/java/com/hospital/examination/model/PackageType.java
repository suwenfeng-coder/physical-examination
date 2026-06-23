package com.hospital.examination.model;

public enum PackageType {
    PERSONAL("个人套餐"),
    ORGANIZATION("单位套餐");

    private final String label;

    PackageType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
