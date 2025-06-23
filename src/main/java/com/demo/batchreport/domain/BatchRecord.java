package com.demo.batchreport.domain;

// BatchRecord.java
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class BatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String assetClass;
    private String product;
    private String scenario;
    private String entity;
    private LocalDate batchDate;
}
