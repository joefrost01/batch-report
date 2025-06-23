package com.demo.batchreport.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchSummary {
    private String assetClass;
    private String product;
    private String entity;
    private Long loadCount;
}
