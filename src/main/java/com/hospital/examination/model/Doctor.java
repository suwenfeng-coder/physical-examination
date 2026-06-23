package com.hospital.examination.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "医生姓名不能为空")
    @Column(nullable = false, length = 40)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "department", nullable = false, length = 50)
    private String legacyDepartment = "";

    @Column(length = 40)
    private String title;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    @PrePersist
    @PreUpdate
    void syncLegacyDepartment() {
        if (department != null) {
            legacyDepartment = department.getName();
        }
    }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) {
        this.department = department;
        if (department != null) {
            this.legacyDepartment = department.getName();
        }
    }
    public String getLegacyDepartment() { return legacyDepartment; }
    public void setLegacyDepartment(String legacyDepartment) { this.legacyDepartment = legacyDepartment; }
    public String getDepartmentName() {
        return department == null ? legacyDepartment : department.getName();
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
