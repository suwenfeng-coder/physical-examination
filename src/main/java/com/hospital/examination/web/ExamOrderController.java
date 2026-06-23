package com.hospital.examination.web;

import com.hospital.examination.model.OrderStatus;
import com.hospital.examination.model.ResultStatus;
import com.hospital.examination.repository.*;
import com.hospital.examination.service.ExamOrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class ExamOrderController {
    private final ExamOrderRepository orderRepository;
    private final PatientRepository patientRepository;
    private final CheckupPackageRepository packageRepository;
    private final DoctorRepository doctorRepository;
    private final ExamOrderService orderService;

    public ExamOrderController(ExamOrderRepository orderRepository, PatientRepository patientRepository,
                               CheckupPackageRepository packageRepository, DoctorRepository doctorRepository,
                               ExamOrderService orderService) {
        this.orderRepository = orderRepository;
        this.patientRepository = patientRepository;
        this.packageRepository = packageRepository;
        this.doctorRepository = doctorRepository;
        this.orderService = orderService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) OrderStatus status, Model model) {
        if (!keyword.isBlank()) {
            model.addAttribute("orders", orderRepository.search(keyword));
        } else if (status != null) {
            model.addAttribute("orders", orderRepository.findByStatusOrderByExamDateDescIdDesc(status));
        } else {
            model.addAttribute("orders", orderRepository.findAllByOrderByExamDateDescIdDesc());
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        return "orders/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("patients", patientRepository.findAll());
        model.addAttribute("packages", packageRepository.findByEnabledTrueOrderByIdDesc());
        model.addAttribute("doctors", doctorRepository.findByEnabledTrueOrderByDepartmentAscNameAsc());
        model.addAttribute("today", LocalDate.now());
        return "orders/form";
    }

    @PostMapping
    public String create(@RequestParam Long patientId, @RequestParam Long packageId,
                         @RequestParam(required = false) Long doctorId,
                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate examDate,
                         RedirectAttributes redirectAttributes) {
        var order = orderService.create(patientId, packageId, doctorId, examDate);
        redirectAttributes.addFlashAttribute("success", "体检登记成功，单号：" + order.getOrderNo());
        return "redirect:/orders/" + order.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.get(id));
        model.addAttribute("resultStatuses", ResultStatus.values());
        return "orders/detail";
    }

    @PostMapping("/{id}/results")
    public String saveResults(@PathVariable Long id,
                              @RequestParam List<Long> resultIds,
                              @RequestParam List<String> resultValues,
                              @RequestParam List<ResultStatus> resultStatuses,
                              @RequestParam List<String> remarks,
                              RedirectAttributes redirectAttributes) {
        orderService.saveResults(id, resultIds, resultValues, resultStatuses, remarks);
        redirectAttributes.addFlashAttribute("success", "检查结果已保存");
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/finish")
    public String finish(@PathVariable Long id, @RequestParam String conclusion,
                         @RequestParam String advice, RedirectAttributes redirectAttributes) {
        try {
            orderService.finish(id, conclusion, advice);
            redirectAttributes.addFlashAttribute("success", "体检报告已完成");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancel(id);
            redirectAttributes.addFlashAttribute("success", "体检单已取消");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @GetMapping("/{id}/report")
    public String report(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.get(id));
        return "orders/report";
    }
}
