package com.hospital.examination.service;

import com.hospital.examination.model.*;
import com.hospital.examination.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExamOrderService {
    private final ExamOrderRepository orderRepository;
    private final PatientRepository patientRepository;
    private final CheckupPackageRepository packageRepository;
    private final DoctorRepository doctorRepository;
    private final ExamResultRepository resultRepository;

    public ExamOrderService(ExamOrderRepository orderRepository,
                            PatientRepository patientRepository,
                            CheckupPackageRepository packageRepository,
                            DoctorRepository doctorRepository,
                            ExamResultRepository resultRepository) {
        this.orderRepository = orderRepository;
        this.patientRepository = patientRepository;
        this.packageRepository = packageRepository;
        this.doctorRepository = doctorRepository;
        this.resultRepository = resultRepository;
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
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("体检单不存在"));
    }

    @Transactional
    public void saveResults(Long orderId, List<Long> resultIds, List<String> values,
                            List<ResultStatus> statuses, List<String> remarks) {
        ExamOrder order = get(orderId);
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
    public void finish(Long orderId, String conclusion, String advice) {
        ExamOrder order = get(orderId);
        boolean hasPending = order.getResults().stream()
                .anyMatch(result -> result.getStatus() == ResultStatus.PENDING);
        if (hasPending) {
            throw new IllegalStateException("仍有待检查项目，暂不能完成体检");
        }
        order.setConclusion(conclusion);
        order.setAdvice(advice);
        order.setStatus(OrderStatus.COMPLETED);
    }

    @Transactional
    public void cancel(Long orderId) {
        ExamOrder order = get(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("已完成的体检单不能取消");
        }
        order.setStatus(OrderStatus.CANCELLED);
    }
}
