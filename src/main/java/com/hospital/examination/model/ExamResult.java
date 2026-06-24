package com.hospital.examination.model;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_results")
public class ExamResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private ExamOrder examOrder;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private CheckupItem item;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String resultValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultStatus status = ResultStatus.PENDING;

    @Column(length = 2000)
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ExamOrder getExamOrder() { return examOrder; }
    public void setExamOrder(ExamOrder examOrder) { this.examOrder = examOrder; }
    public CheckupItem getItem() { return item; }
    public void setItem(CheckupItem item) { this.item = item; }
    public String getResultValue() { return resultValue; }
    public void setResultValue(String resultValue) { this.resultValue = resultValue; }
    public ResultStatus getStatus() { return status; }
    public void setStatus(ResultStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
