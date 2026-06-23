package com.hospital.examination.service;

import java.util.List;

public class AppointmentImportException extends RuntimeException {
    private final List<String> errors;

    public AppointmentImportException(List<String> errors) {
        super(String.join("；", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
