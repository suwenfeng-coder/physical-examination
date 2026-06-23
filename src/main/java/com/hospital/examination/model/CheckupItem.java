package com.hospital.examination.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Entity
@Table(name = "checkup_items")
public class CheckupItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "项目名称不能为空")
    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "department", nullable = false, length = 50)
    private String legacyDepartment = "";

    @Column(length = 50)
    private String unit;

    @Column(length = 100)
    private String referenceRange;

    @DecimalMin(value = "0.00", message = "价格不能小于0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(length = 300)
    private String description;

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
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
