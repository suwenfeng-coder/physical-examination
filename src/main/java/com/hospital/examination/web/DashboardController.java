package com.hospital.examination.web;

import com.hospital.examination.model.OrderStatus;
import com.hospital.examination.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class DashboardController {
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final CheckupPackageRepository packageRepository;
    private final ExamOrderRepository orderRepository;

    public DashboardController(PatientRepository patientRepository, DoctorRepository doctorRepository,
                               CheckupPackageRepository packageRepository, ExamOrderRepository orderRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.packageRepository = packageRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("patientCount", patientRepository.count());
        model.addAttribute("doctorCount", doctorRepository.count());
        model.addAttribute("packageCount", packageRepository.count());
        model.addAttribute("todayCount", orderRepository.countByExamDate(LocalDate.now()));
        model.addAttribute("examiningCount", orderRepository.countByStatus(OrderStatus.EXAMINING));
        model.addAttribute("completedCount", orderRepository.countByStatus(OrderStatus.COMPLETED));
        model.addAttribute("recentOrders", orderRepository.findAllByOrderByExamDateDescIdDesc().stream().limit(8).toList());
        return "dashboard";
    }
}
