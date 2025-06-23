package com.demo.batchreport.repository;

import com.demo.batchreport.domain.BatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BatchQueryRepository extends JpaRepository<BatchRecord, Long> {
    List<BatchRecord> findAllByBatchDate(LocalDate loadDate);


    List<BatchRecord> findAllByBatchDateBetween(LocalDate startDate, LocalDate endDate);

}
