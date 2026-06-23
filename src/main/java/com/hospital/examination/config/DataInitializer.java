package com.hospital.examination.config;

import com.hospital.examination.model.*;
import com.hospital.examination.repository.CheckupItemRepository;
import com.hospital.examination.repository.CheckupPackageRepository;
import com.hospital.examination.repository.DepartmentRepository;
import com.hospital.examination.repository.DoctorRepository;
import com.hospital.examination.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private final DoctorRepository doctorRepository;
    private final CheckupItemRepository itemRepository;
    private final CheckupPackageRepository packageRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:123456}")
    private String adminPassword;

    public DataInitializer(DoctorRepository doctorRepository, CheckupItemRepository itemRepository,
                           CheckupPackageRepository packageRepository,
                           UserAccountRepository userAccountRepository,
                           PasswordEncoder passwordEncoder,
                           DepartmentRepository departmentRepository) {
        this.doctorRepository = doctorRepository;
        this.itemRepository = itemRepository;
        this.packageRepository = packageRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userAccountRepository.existsByUsername(adminUsername)) {
            UserAccount admin = new UserAccount();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userAccountRepository.save(admin);
        }
        initializeDepartments();
        if (doctorRepository.count() == 0) {
            doctorRepository.saveAll(List.of(
                    doctor("张明", department("内科"), "主任医师", "13800000001"),
                    doctor("李华", department("医学影像科"), "副主任医师", "13800000002"),
                    doctor("王芳", department("检验科"), "主治医师", "13800000003")
            ));
        }
        if (itemRepository.count() == 0) {
            itemRepository.saveAll(List.of(
                    item("一般检查", department("内科"), "", "身高、体重、血压", "20"),
                    item("血常规", department("检验科"), "", "血细胞分析", "45"),
                    item("肝功能", department("检验科"), "", "ALT、AST等", "80"),
                    item("肾功能", department("检验科"), "", "肌酐、尿素氮等", "70"),
                    item("空腹血糖", department("检验科"), "mmol/L", "3.9-6.1", "20"),
                    item("心电图", department("功能科"), "", "十二导联心电图", "50"),
                    item("胸部DR", department("医学影像科"), "", "胸部正位片", "100"),
                    item("腹部彩超", department("超声科"), "", "肝胆胰脾肾", "160")
            ));
        }
        migrateLegacyDepartments();
        if (packageRepository.count() == 0) {
            List<CheckupItem> items = itemRepository.findAllOrdered();
            CheckupPackage basic = new CheckupPackage();
            basic.setName("基础健康套餐");
            basic.setDescription("适合常规年度健康筛查");
            basic.setPrice(new BigDecimal("299.00"));
            basic.setItems(new LinkedHashSet<>(items.stream().limit(5).toList()));
            packageRepository.save(basic);

            CheckupPackage comprehensive = new CheckupPackage();
            comprehensive.setName("全面健康套餐");
            comprehensive.setDescription("覆盖内科、检验、影像和功能检查");
            comprehensive.setPrice(new BigDecimal("599.00"));
            comprehensive.setItems(new LinkedHashSet<>(items));
            packageRepository.save(comprehensive);
        }
    }

    private void initializeDepartments() {
        List<String> names = List.of("内科", "检验科", "医学影像科", "功能科", "超声科");
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (!departmentRepository.existsByName(name)) {
                Department department = new Department();
                department.setName(name);
                department.setCode("DEPT" + String.format("%03d", i + 1));
                department.setSortOrder((i + 1) * 10);
                departmentRepository.save(department);
            }
        }
    }

    private void migrateLegacyDepartments() {
        for (Doctor doctor : doctorRepository.findAll()) {
            if (doctor.getDepartment() == null && doctor.getLegacyDepartment() != null
                    && !doctor.getLegacyDepartment().isBlank()) {
                doctor.setDepartment(findOrCreateDepartment(doctor.getLegacyDepartment()));
            }
        }
        for (CheckupItem item : itemRepository.findAll()) {
            if (item.getDepartment() == null && item.getLegacyDepartment() != null
                    && !item.getLegacyDepartment().isBlank()) {
                item.setDepartment(findOrCreateDepartment(item.getLegacyDepartment()));
            }
        }
    }

    private Department findOrCreateDepartment(String name) {
        return departmentRepository.findByName(name).orElseGet(() -> {
            Department department = new Department();
            department.setName(name);
            department.setSortOrder(100);
            return departmentRepository.save(department);
        });
    }

    private Department department(String name) {
        return departmentRepository.findByName(name).orElseThrow();
    }

    private Doctor doctor(String name, Department department, String title, String phone) {
        Doctor doctor = new Doctor();
        doctor.setName(name);
        doctor.setDepartment(department);
        doctor.setTitle(title);
        doctor.setPhone(phone);
        return doctor;
    }

    private CheckupItem item(String name, Department department, String unit,
                             String referenceRange, String price) {
        CheckupItem item = new CheckupItem();
        item.setName(name);
        item.setDepartment(department);
        item.setUnit(unit);
        item.setReferenceRange(referenceRange);
        item.setPrice(new BigDecimal(price));
        return item;
    }
}
