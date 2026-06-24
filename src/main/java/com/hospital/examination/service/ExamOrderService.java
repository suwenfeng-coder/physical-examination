package com.hospital.examination.service;

import com.hospital.examination.model.*;
import com.hospital.examination.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExamOrderService {
    private final ExamOrderRepository orderRepository;
    private final PatientRepository patientRepository;
    private final CheckupPackageRepository packageRepository;
    private final DoctorRepository doctorRepository;
    private final ExamResultRepository resultRepository;
    private final ReportNotificationSender reportNotificationSender;

    public ExamOrderService(ExamOrderRepository orderRepository,
                            PatientRepository patientRepository,
                            CheckupPackageRepository packageRepository,
                            DoctorRepository doctorRepository,
                            ExamResultRepository resultRepository,
                            ReportNotificationSender reportNotificationSender) {
        this.orderRepository = orderRepository;
        this.patientRepository = patientRepository;
        this.packageRepository = packageRepository;
        this.doctorRepository = doctorRepository;
        this.resultRepository = resultRepository;
        this.reportNotificationSender = reportNotificationSender;
    }

    @Transactional
    public ExamOrder create(Long patientId, Long packageId, Long doctorId, LocalDate examDate) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("受检人不存在"));
        CheckupPackage checkupPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new EntityNotFoundException("体检套餐不存在"));

        ExamOrder order = new ExamOrder();
        order.setPatient(patient);
        order.setCheckupPackage(checkupPackage);
        order.setDoctor(doctorId == null ? null : doctorRepository.findById(doctorId).orElse(null));
        order.setExamDate(examDate);
        order.setStatus(OrderStatus.REGISTERED);
        order.setAmount(checkupPackage.getPrice());
        order.setOrderNo(nextOrderNo());

        for (CheckupItem item : checkupPackage.getItems()) {
            ExamResult result = new ExamResult();
            result.setExamOrder(order);
            result.setItem(item);
            order.getResults().add(result);
        }
        return orderRepository.save(order);
    }

    private String nextOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long count = orderRepository.countByExamDate(LocalDate.now()) + 1;
        return "PE" + date + String.format("%04d", count);
    }

    @Transactional(readOnly = true)
    public ExamOrder get(Long id) {
        ExamOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("体检单不存在"));
        order.getResults().size();
        order.getAttachments().size();
        return order;
    }

    @Transactional
    public void saveResults(Long orderId, List<Long> resultIds, List<String> values,
                            List<ResultStatus> statuses, List<String> remarks) {
        ExamOrder order = get(orderId);
        assertReportEditable(order);
        if (resultIds.size() != values.size() || resultIds.size() != statuses.size()
                || resultIds.size() != remarks.size()) {
            throw new IllegalArgumentException("检查结果表单数据不完整");
        }
        for (int i = 0; i < resultIds.size(); i++) {
            ExamResult result = resultRepository.findById(resultIds.get(i))
                    .orElseThrow(() -> new EntityNotFoundException("检查结果不存在"));
            if (!result.getExamOrder().getId().equals(orderId)) {
                throw new IllegalArgumentException("检查结果不属于当前体检单");
            }
            result.setResultValue(values.get(i));
            result.setStatus(statuses.get(i));
            result.setRemark(remarks.get(i));
        }
        if (order.getStatus() == OrderStatus.REGISTERED) {
            order.setStatus(OrderStatus.EXAMINING);
        }
    }

    @Transactional
    public void submitForReview(Long orderId, String conclusion, String advice) {
        ExamOrder order = get(orderId);
        assertReportEditable(order);
        boolean hasPending = order.getResults().stream()
                .anyMatch(result -> result.getStatus() == ResultStatus.PENDING);
        if (hasPending) {
            throw new IllegalStateException("仍有待检查项目，暂不能提交审核");
        }
        order.setConclusion(conclusion);
        order.setAdvice(advice);
        order.setReviewComment(null);
        order.setSubmittedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING_REVIEW);
    }

    @Transactional
    public void approve(Long orderId, Long reviewerId, String reviewComment) {
        ExamOrder order = get(orderId);
        if (order.getStatus() != OrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("只有待审核报告可以审核通过");
        }
        Doctor reviewer = doctorRepository.findById(reviewerId)
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new EntityNotFoundException("审核医生不存在或已停用"));
        order.setReviewer(reviewer);
        order.setReviewComment(reviewComment);
        order.setReviewedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED);
        reportNotificationSender.sendPickupReady(
                order.getPatient().getPhone(), order.getPatient().getName(), order.getOrderNo());
        order.setPickupNotifiedAt(LocalDateTime.now());
    }

    @Transactional
    public void reject(Long orderId, Long reviewerId, String reviewComment) {
        ExamOrder order = get(orderId);
        if (order.getStatus() != OrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("只有待审核报告可以退回");
        }
        if (reviewComment == null || reviewComment.isBlank()) {
            throw new IllegalArgumentException("退回报告时必须填写审核意见");
        }
        Doctor reviewer = doctorRepository.findById(reviewerId)
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new EntityNotFoundException("审核医生不存在或已停用"));
        order.setReviewer(reviewer);
        order.setReviewComment(reviewComment);
        order.setReviewedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.EXAMINING);
    }

    @Transactional
    public void cancel(Long orderId) {
        ExamOrder order = get(orderId);
        if (order.getStatus() == OrderStatus.PENDING_REVIEW || order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("已提交审核或已完成的体检单不能取消");
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    private void assertReportEditable(ExamOrder order) {
        if (order.getStatus() == OrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("报告正在审核，不能修改");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("已审核完成的报告不能修改");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("已取消的体检单不能修改");
        }
    }
}
