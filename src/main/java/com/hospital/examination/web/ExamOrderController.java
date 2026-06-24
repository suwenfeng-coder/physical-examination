package com.hospital.examination.web;

import com.hospital.examination.model.OrderStatus;
import com.hospital.examination.model.ResultStatus;
import com.hospital.examination.repository.*;
import com.hospital.examination.service.ExternalResultSyncService;
import com.hospital.examination.service.ExamOrderService;
import com.hospital.examination.service.ExamOrderPrinter;
import com.hospital.examination.service.ReportAttachmentService;
import com.hospital.examination.service.ReportPdfService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
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
    private final ReportAttachmentService attachmentService;
    private final ReportPdfService pdfService;
    private final ExternalResultSyncService externalResultSyncService;
    private final ExamOrderPrinter examOrderPrinter;

    public ExamOrderController(ExamOrderRepository orderRepository, PatientRepository patientRepository,
                               CheckupPackageRepository packageRepository, DoctorRepository doctorRepository,
                               ExamOrderService orderService,
                               ReportAttachmentService attachmentService,
                               ReportPdfService pdfService,
                               ExternalResultSyncService externalResultSyncService,
                               ExamOrderPrinter examOrderPrinter) {
        this.orderRepository = orderRepository;
        this.patientRepository = patientRepository;
        this.packageRepository = packageRepository;
        this.doctorRepository = doctorRepository;
        this.orderService = orderService;
        this.attachmentService = attachmentService;
        this.pdfService = pdfService;
        this.externalResultSyncService = externalResultSyncService;
        this.examOrderPrinter = examOrderPrinter;
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
        model.addAttribute("doctors", doctorRepository.findEnabledOrdered());
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
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        model.addAttribute("order", orderService.get(id));
        model.addAttribute("resultStatuses", ResultStatus.values());
        model.addAttribute("reviewers", doctorRepository.findEnabledOrdered());
        model.addAttribute("externalSyncConfigured", externalResultSyncService.isConfigured());
        model.addAttribute("canReview", canReview(session));
        return "orders/detail";
    }

    @PostMapping("/{id}/results")
    public String saveResults(@PathVariable Long id,
                              @RequestParam List<Long> resultIds,
                              @RequestParam List<String> resultValues,
                              @RequestParam List<ResultStatus> resultStatuses,
                              @RequestParam List<String> remarks,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.saveResults(id, resultIds, resultValues, resultStatuses, remarks);
            redirectAttributes.addFlashAttribute("success", "检查结果已保存");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/submit-review")
    public String submitReview(@PathVariable Long id, @RequestParam String conclusion,
                               @RequestParam String advice, RedirectAttributes redirectAttributes) {
        try {
            orderService.submitForReview(id, conclusion, advice);
            redirectAttributes.addFlashAttribute("success", "体检报告已提交审核");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam Long reviewerId,
                          @RequestParam(defaultValue = "") String reviewComment,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        try {
            requireReviewPermission(session);
            orderService.approve(id, reviewerId, reviewComment);
            redirectAttributes.addFlashAttribute("success", "审核完成，已发送报告领取短信");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam Long reviewerId,
                         @RequestParam String reviewComment,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            requireReviewPermission(session);
            orderService.reject(id, reviewerId, reviewComment);
            redirectAttributes.addFlashAttribute("success", "报告已退回修改");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/attachments")
    public String uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            attachmentService.store(orderService.get(id), file);
            redirectAttributes.addFlashAttribute("success", "影像附件已上传");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @GetMapping("/{orderId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long orderId,
                                                       @PathVariable Long attachmentId) {
        var attachment = attachmentService.get(attachmentId);
        if (!attachment.getExamOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("附件不属于当前体检报告");
        }
        Resource resource = attachmentService.load(attachment);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(attachment.getOriginalName(), StandardCharsets.UTF_8)
                        .build().toString())
                .contentLength(attachment.getSize())
                .body(resource);
    }

    @PostMapping("/{id}/sync-results")
    public String syncResults(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            int count = externalResultSyncService.sync(orderService.get(id));
            redirectAttributes.addFlashAttribute("success", "外部检查结果同步完成，更新 " + count + " 项");
        } catch (IllegalArgumentException | IllegalStateException ex) {
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

    @GetMapping("/{id}/print-form")
    public String printForm(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.get(id));
        return "orders/print-form";
    }

    @PostMapping("/{id}/print")
    public String print(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        var order = orderService.get(id);
        try {
            examOrderPrinter.print(order);
            redirectAttributes.addFlashAttribute("success", "已提交打印任务：" + order.getOrderNo());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", "提交打印失败：" + ex.getMessage());
        }
        return "redirect:/orders/" + id + "/print-form";
    }

    @GetMapping("/{id}/report")
    public String report(@PathVariable Long id, Model model) {
        var order = orderService.get(id);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            return "redirect:/orders/" + id;
        }
        model.addAttribute("order", order);
        return "orders/report";
    }

    @GetMapping("/{id}/report.pdf")
    public ResponseEntity<byte[]> reportPdf(@PathVariable Long id) {
        var order = orderService.get(id);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("报告审核完成后才可导出 PDF");
        }
        byte[] pdf = pdfService.create(order);
        String filename = order.getOrderNo() + "-" + order.getPatient().getName() + "-体检报告.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentLength(pdf.length)
                .body(pdf);
    }

    private boolean canReview(HttpSession session) {
        Object role = session.getAttribute("LOGIN_ROLE");
        return "ADMIN".equals(role) || "DOCTOR".equals(role);
    }

    private void requireReviewPermission(HttpSession session) {
        if (!canReview(session)) {
            throw new IllegalStateException("当前账号没有体检报告审核权限");
        }
    }
}
