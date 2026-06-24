package com.hospital.examination.service;

import com.hospital.examination.model.ExamOrder;
import com.hospital.examination.model.OrderStatus;
import com.hospital.examination.model.ReportAttachment;
import com.hospital.examination.repository.ExamOrderRepository;
import com.hospital.examination.repository.ReportAttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportAttachmentService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf");

    private final ReportAttachmentRepository repository;
    private final ExamOrderRepository orderRepository;
    private final Path uploadDir;

    public ReportAttachmentService(ReportAttachmentRepository repository,
                                   ExamOrderRepository orderRepository,
                                   @Value("${app.report.upload-dir:runtime-assets/report-uploads}") String uploadDir) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public ReportAttachment store(ExamOrder order, MultipartFile file) {
        order = orderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("体检单不存在"));
        assertEditable(order);
        validate(file);
        try {
            Files.createDirectories(uploadDir);
            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename() == null ? "影像附件" : file.getOriginalFilename());
            String extension = extensionOf(originalName);
            String storageName = UUID.randomUUID() + extension;
            Path target = uploadDir.resolve(storageName).normalize();
            if (!target.getParent().equals(uploadDir)) {
                throw new IllegalArgumentException("附件名称不合法");
            }
            file.transferTo(target);

            ReportAttachment attachment = new ReportAttachment();
            attachment.setExamOrder(order);
            attachment.setOriginalName(originalName);
            attachment.setStorageName(storageName);
            attachment.setContentType(file.getContentType().toLowerCase(Locale.ROOT));
            attachment.setSize(file.getSize());
            order.getAttachments().add(attachment);
            return repository.save(attachment);
        } catch (IOException ex) {
            throw new IllegalStateException("影像附件保存失败", ex);
        }
    }

    @Transactional(readOnly = true)
    public ReportAttachment get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("附件不存在"));
    }

    public Resource load(ReportAttachment attachment) {
        try {
            Path file = uploadDir.resolve(attachment.getStorageName()).normalize();
            if (!file.getParent().equals(uploadDir)) {
                throw new IllegalArgumentException("附件路径不合法");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("附件文件不存在");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalStateException("附件读取失败", ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择需要上传的影像或 PDF 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("单个附件不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、GIF、WebP 影像或 PDF 文件");
        }
    }

    private void assertEditable(ExamOrder order) {
        if (order.getStatus() == OrderStatus.PENDING_REVIEW
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("当前报告状态不允许上传附件");
        }
    }

    private String extensionOf(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index).toLowerCase(Locale.ROOT);
    }
}
