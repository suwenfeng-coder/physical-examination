package com.hospital.examination.service;

import com.hospital.examination.model.ExamOrder;
import com.hospital.examination.model.ExamResult;
import com.hospital.examination.model.OrderStatus;
import com.hospital.examination.repository.ExamOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ExternalResultSyncService {
    private final List<ExternalResultProvider> providers;
    private final ExamOrderRepository orderRepository;

    public ExternalResultSyncService(List<ExternalResultProvider> providers,
                                     ExamOrderRepository orderRepository) {
        this.providers = providers;
        this.orderRepository = orderRepository;
    }

    public boolean isConfigured() {
        return !providers.isEmpty();
    }

    @Transactional
    public int sync(ExamOrder order) {
        order = orderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("体检单不存在"));
        if (!isConfigured()) {
            throw new IllegalStateException("尚未配置外部科室结果接口");
        }
        if (order.getStatus() == OrderStatus.PENDING_REVIEW
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("当前报告状态不允许同步检查结果");
        }

        int updated = 0;
        for (ExternalResultProvider provider : providers) {
            for (ExternalExamResult external : provider.fetchResults(order.getOrderNo())) {
                ExamResult target = findTarget(order, external);
                if (target == null) {
                    continue;
                }
                target.setResultValue(external.resultValue());
                if (external.status() != null) {
                    target.setStatus(external.status());
                }
                target.setRemark(external.remark());
                updated++;
            }
        }
        if (updated > 0 && order.getStatus() == OrderStatus.REGISTERED) {
            order.setStatus(OrderStatus.EXAMINING);
        }
        return updated;
    }

    private ExamResult findTarget(ExamOrder order, ExternalExamResult external) {
        return order.getResults().stream()
                .filter(result -> matches(result, external))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(ExamResult result, ExternalExamResult external) {
        if (external.itemCode() != null && !external.itemCode().isBlank()) {
            return Objects.equals(result.getItem().getExternalCode(), external.itemCode());
        }
        return external.itemName() != null
                && external.itemName().equalsIgnoreCase(result.getItem().getName());
    }
}
