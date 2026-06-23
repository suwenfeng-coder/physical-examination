package com.hospital.examination.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "package_templates")
public class PackageTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "模板名称不能为空")
    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @NotNull(message = "请选择模板类型")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PackageType type = PackageType.PERSONAL;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "package_template_items",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id"))
    @OrderBy("id asc")
    private Set<CheckupItem> items = new LinkedHashSet<>();

    public BigDecimal getGuidePrice() {
        return items.stream()
                .map(CheckupItem::getPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PackageType getType() { return type; }
    public void setType(PackageType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<CheckupItem> getItems() { return items; }
    public void setItems(Set<CheckupItem> items) { this.items = items; }
}
