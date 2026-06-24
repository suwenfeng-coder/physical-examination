package com.hospital.examination.web;

import com.hospital.examination.model.ExamOrder;
import com.hospital.examination.model.OrderStatus;
import com.hospital.examination.repository.ExamOrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ExamReportController {
    private final ExamOrderRepository orderRepository;

    public ExamReportController(ExamOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) OrderStatus status,
                       Model model) {
        List<ExamOrder> reports;
        if (!keyword.isBlank()) {
            reports = orderRepository.search(keyword);
            if (status != null) {
                reports = reports.stream().filter(order -> order.getStatus() == status).toList();
            }
        } else if (status != null) {
            reports = orderRepository.findByStatusOrderByExamDateDescIdDesc(status);
        } else {
            reports = orderRepository.findAllByOrderByExamDateDescIdDesc();
        }

        model.addAttribute("reports", reports);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", Arrays.stream(OrderStatus.values())
                .filter(value -> value != OrderStatus.CANCELLED)
                .toList());
        model.addAttribute("editingCount",
                orderRepository.countByStatus(OrderStatus.REGISTERED)
                        + orderRepository.countByStatus(OrderStatus.EXAMINING));
        model.addAttribute("pendingReviewCount", orderRepository.countByStatus(OrderStatus.PENDING_REVIEW));
        model.addAttribute("completedCount", orderRepository.countByStatus(OrderStatus.COMPLETED));
        return "reports/list";
    }
}
