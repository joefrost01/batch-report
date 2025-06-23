package com.demo.batchreport.service;

import com.demo.batchreport.config.Config;
import com.demo.batchreport.domain.BatchRecord;
import com.demo.batchreport.domain.BatchStatusCount;
import com.demo.batchreport.repository.BatchQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchReportServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Config config;

    @Mock
    private BatchQueryRepository batchQueryRepository;

    private BatchReportService batchReportService;

    @BeforeEach
    void setUp() {
        batchReportService = new BatchReportService(mailSender, config, batchQueryRepository);
    }

    @Test
    void shouldGenerateHtmlReportWithSampleData() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 12, 15);

        List<BatchRecord> mockBatchRecords = Arrays.asList(
                new BatchRecord(null,"Equity", "US Large Cap", "Base", "Entity A", testDate),
                new BatchRecord(null,"Equity", "US Large Cap", "Stress", "Entity A", testDate),
                new BatchRecord(null,"Equity", "Emerging Markets", "Base", "Entity B", testDate),
                new BatchRecord(null,"Fixed Income", "Corporate Bonds", "Base", "Entity A", testDate),
                new BatchRecord(null,"Fixed Income", "Government Bonds", "Base", "Entity C", testDate)
        );

        List<BatchStatusCount> mockStatusCounts = Arrays.asList(
                new BatchStatusCount(testDate.minusDays(2), 45L, 5L),
                new BatchStatusCount(testDate.minusDays(1), 48L, 2L),
                new BatchStatusCount(testDate, 50L, 0L)
        );

        // When
        String htmlContent = batchReportService.generateBatchReportHtml(testDate, mockBatchRecords, mockStatusCounts);

        // Then
        assertThat(htmlContent).isNotNull();
        assertThat(htmlContent).contains("Batch Load Report");
        assertThat(htmlContent).contains("December 15, 2024");

        // Check summary data is present
        assertThat(htmlContent).contains("Load Summary");
        assertThat(htmlContent).contains("Equity");
        assertThat(htmlContent).contains("Fixed Income");
        assertThat(htmlContent).contains("Entity A");
        assertThat(htmlContent).contains("Entity B");
        assertThat(htmlContent).contains("Entity C");

        // Check detailed records table
        assertThat(htmlContent).contains("Detailed Records");
        assertThat(htmlContent).contains("US Large Cap");
        assertThat(htmlContent).contains("Emerging Markets");
        assertThat(htmlContent).contains("Corporate Bonds");
        assertThat(htmlContent).contains("Government Bonds");
        assertThat(htmlContent).contains("Base");
        assertThat(htmlContent).contains("Stress");

        // Check statistics
        assertThat(htmlContent).contains("5"); // Total records
        assertThat(htmlContent).contains("2"); // Asset classes (Equity, Fixed Income)
        assertThat(htmlContent).contains("4"); // Products

        // Check chart section
        assertThat(htmlContent).contains("120-Day Load Status Trend");
        assertThat(htmlContent).contains("cid:statusChart");
    }

    @Test
    void shouldGenerateHtmlReportWithEmptyData() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 12, 15);
        List<BatchRecord> emptyBatchRecords = Arrays.asList();
        List<BatchStatusCount> emptyStatusCounts = Arrays.asList();

        // When
        String htmlContent = batchReportService.generateBatchReportHtml(testDate, emptyBatchRecords, emptyStatusCounts);

        // Then
        assertThat(htmlContent).isNotNull();
        assertThat(htmlContent).contains("Batch Load Report");
        assertThat(htmlContent).contains("December 15, 2024");
        assertThat(htmlContent).contains("No data loaded for this batch date");
        assertThat(htmlContent).contains("No detailed records available");

        // Check empty state statistics
        assertThat(htmlContent).contains("<span class=\"stat-number\">0</span>");
    }

    @Test
    void shouldHandleSpecialCharactersInData() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 12, 15);

        List<BatchRecord> mockBatchRecords = Arrays.asList(
                new BatchRecord(null,"Equity & Derivatives", "US <Large> Cap", "Base \"Test\"", "Entity A&B", testDate)
        );

        List<BatchStatusCount> mockStatusCounts = Arrays.asList();

        // When
        String htmlContent = batchReportService.generateBatchReportHtml(testDate, mockBatchRecords, mockStatusCounts);

        // Then
        assertThat(htmlContent).contains("Equity &amp; Derivatives");
        assertThat(htmlContent).contains("US &lt;Large&gt; Cap");
        assertThat(htmlContent).contains("Base &quot;Test&quot;");
        assertThat(htmlContent).contains("Entity A&amp;B");
    }

    @Test
    void shouldGroupSummaryDataCorrectly() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 12, 15);

        List<BatchRecord> mockBatchRecords = Arrays.asList(
                new BatchRecord(null,"Equity", "US Large Cap", "Base", "Entity A", testDate),
                new BatchRecord(null,"Equity", "US Large Cap", "Stress", "Entity A", testDate),
                new BatchRecord(null,"Equity", "US Large Cap", "Base", "Entity B", testDate),
                new BatchRecord(null,"Fixed Income", "Corporate Bonds", "Base", "Entity A", testDate)
        );

        List<BatchStatusCount> mockStatusCounts = Arrays.asList();

        // When
        String htmlContent = batchReportService.generateBatchReportHtml(testDate, mockBatchRecords, mockStatusCounts);

        // Then
        // Should have 3 summary rows: Equity/US Large Cap/Entity A (2), Equity/US Large Cap/Entity B (1), Fixed Income/Corporate Bonds/Entity A (1)
        assertThat(htmlContent).contains("class=\"number-cell\">2</td>"); // Entity A has 2 records for Equity/US Large Cap
        assertThat(htmlContent).contains("class=\"number-cell\">1</td>"); // Other combinations have 1 record each
    }

    @Test
    void shouldFormatDatesCorrectly() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 12, 15);

        List<BatchRecord> mockBatchRecords = Arrays.asList(
                new BatchRecord(null,"Equity", "US Large Cap", "Base", "Entity A", testDate)
        );

        List<BatchStatusCount> mockStatusCounts = Arrays.asList();

        // When
        String htmlContent = batchReportService.generateBatchReportHtml(testDate, mockBatchRecords, mockStatusCounts);

        // Then
        assertThat(htmlContent).contains("December 15, 2024"); // Header date format
        assertThat(htmlContent).contains("2024-12-15"); // Table date format
    }
}

