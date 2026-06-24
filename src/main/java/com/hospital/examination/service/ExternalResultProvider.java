package com.hospital.examination.service;

import java.util.List;

/**
 * 外部科室系统接入点。实现类负责鉴权、查询及把外部数据转换为统一结果。
 */
public interface ExternalResultProvider {
    String providerCode();

    List<ExternalExamResult> fetchResults(String orderNo);
}
