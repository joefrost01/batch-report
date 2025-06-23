package com.demo.batchreport.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusCount {
    private LocalDate date;
    private Long loadedCount;
    private Long missingCount;
}