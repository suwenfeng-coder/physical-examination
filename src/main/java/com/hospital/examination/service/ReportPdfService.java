package com.hospital.examination.service;

import com.hospital.examination.model.ExamOrder;
import com.hospital.examination.model.ExamResult;
import com.hospital.examination.repository.ExamOrderRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportPdfService {
    private final String configuredFontPath;
    private final ExamOrderRepository orderRepository;

    public ReportPdfService(@Value("${app.report.pdf-font-path:}") String configuredFontPath,
                            ExamOrderRepository orderRepository) {
        this.configuredFontPath = configuredFontPath;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public byte[] create(ExamOrder order) {
        order = orderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("体检单不存在"));
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 42, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            BaseFont baseFont = loadChineseFont();
            Font title = new Font(baseFont, 20, Font.BOLD, new Color(8, 127, 120));
            Font heading = new Font(baseFont, 13, Font.BOLD);
            Font normal = new Font(baseFont, 10);
            Font small = new Font(baseFont, 9, Font.NORMAL, Color.DARK_GRAY);

            Paragraph reportTitle = new Paragraph("健康体检报告", title);
            reportTitle.setAlignment(Element.ALIGN_CENTER);
            reportTitle.setSpacingAfter(18);
            document.add(reportTitle);

            PdfPTable patient = new PdfPTable(3);
            patient.setWidthPercentage(100);
            addInfo(patient, "姓名：" + order.getPatient().getName(), normal);
            addInfo(patient, "性别：" + order.getPatient().getGender().getLabel(), normal);
            addInfo(patient, "年龄：" + order.getPatient().getAge() + " 岁", normal);
            addInfo(patient, "体检编号：" + order.getOrderNo(), normal);
            addInfo(patient, "体检日期：" + order.getExamDate().format(DateTimeFormatter.ISO_DATE), normal);
            addInfo(patient, "联系电话：" + order.getPatient().getPhone(), normal);
            document.add(patient);

            Paragraph resultHeading = new Paragraph("检查结果", heading);
            resultHeading.setSpacingBefore(18);
            resultHeading.setSpacingAfter(8);
            document.add(resultHeading);

            PdfPTable results = new PdfPTable(new float[]{0.6f, 1.6f, 2.5f, 1.4f, 1.0f});
            results.setWidthPercentage(100);
            for (String header : List.of("序号", "检查项目", "检查结果", "参考范围", "判定")) {
                PdfPCell cell = new PdfPCell(new Phrase(header, normal));
                cell.setBackgroundColor(new Color(235, 245, 243));
                cell.setPadding(6);
                results.addCell(cell);
            }
            int index = 1;
            for (ExamResult result : order.getResults()) {
                addCell(results, String.valueOf(index++), normal);
                addCell(results, result.getItem().getName(), normal);
                String value = blankToDash(result.getResultValue());
                if (result.getItem().getUnit() != null && !result.getItem().getUnit().isBlank()) {
                    value += " " + result.getItem().getUnit();
                }
                if (result.getRemark() != null && !result.getRemark().isBlank()) {
                    value += "\n备注：" + result.getRemark();
                }
                addCell(results, value, normal);
                addCell(results, blankToDash(result.getItem().getReferenceRange()), normal);
                addCell(results, result.getStatus().getLabel(), normal);
            }
            document.add(results);

            addSection(document, "总检结论", order.getConclusion(), heading, normal);
            addSection(document, "健康建议", order.getAdvice(), heading, normal);

            Paragraph audit = new Paragraph(
                    "审核医生：" + (order.getReviewer() == null ? "-" : order.getReviewer().getName())
                            + "    审核时间：" + (order.getReviewedAt() == null ? "-"
                            : order.getReviewedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                    small);
            audit.setSpacingBefore(24);
            document.add(audit);

            Paragraph notice = new Paragraph("本报告仅对本次体检负责，异常结果请结合临床并遵医嘱复查。", small);
            notice.setAlignment(Element.ALIGN_CENTER);
            notice.setSpacingBefore(28);
            document.add(notice);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("PDF 体检报告生成失败，请检查中文字体配置", ex);
        }
    }

    private BaseFont loadChineseFont() throws Exception {
        for (String candidate : List.of(
                configuredFontPath,
                "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc")) {
            if (candidate != null && !candidate.isBlank() && Files.exists(Path.of(candidate))) {
                String fontPath = candidate.endsWith(".ttc") ? candidate + ",0" : candidate;
                return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        throw new IllegalStateException("未找到可用中文字体，请配置 app.report.pdf-font-path");
    }

    private void addInfo(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(new Color(246, 249, 248));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addSection(Document document, String title, String content, Font heading, Font normal)
            throws DocumentException {
        Paragraph sectionTitle = new Paragraph(title, heading);
        sectionTitle.setSpacingBefore(16);
        sectionTitle.setSpacingAfter(6);
        document.add(sectionTitle);
        Paragraph paragraph = new Paragraph(blankToDash(content), normal);
        paragraph.setLeading(18);
        document.add(paragraph);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
