package com.demo.batchreport.controller;

import com.demo.batchreport.domain.BatchRecord;
import com.demo.batchreport.domain.BatchStatusCount;
import com.demo.batchreport.service.BatchReportService;
import lombok.RequiredArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class BatchReportController {

    private final BatchReportService batchReportService;

    @PostMapping("/send-batch-report")
    public ResponseEntity<String> sendBatchReport(@RequestParam LocalDate batchDate) {
        batchReportService.sendBatchReport(batchDate);
        return ResponseEntity.ok("Batch report sent successfully for " + batchDate);
    }

    @GetMapping("/preview-batch-report")
    public ResponseEntity<String> previewBatchReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate batchDate) {

        try {
            // Create more realistic mock data
            List<BatchRecord> mockData = Arrays.asList(
                    new BatchRecord(null, "Equity", "US Large Cap", "Base", "Entity A", batchDate),
                    new BatchRecord(null, "Fixed Income", "Corporate Bonds", "Stress", "Entity B", batchDate)
            );

            // Create mock status data for the last 120 days
            List<BatchStatusCount> mockStatus = generateMockStatusData(batchDate);

            // Generate the actual chart file
            File chartFile = generateMockChart(mockStatus);

            // Convert chart to base64 for inline display in HTML
            String base64Chart = convertChartToBase64(chartFile);

            String html = batchReportService.generateBatchReportHtml(batchDate, mockData, mockStatus);

            // Replace the cid:statusChart with base64 data URL for preview
            html = html.replace("src=\"cid:statusChart\"",
                    "src=\"data:image/png;base64," + base64Chart + "\"");

            // Cleanup
            Files.deleteIfExists(chartFile.toPath());

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating preview: " + e.getMessage());
        }
    }

    private List<BatchStatusCount> generateMockStatusData(LocalDate endDate) {
        List<BatchStatusCount> statusCounts = new ArrayList<>();
        Random random = new Random();

        for (int i = 119; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);

            // Simulate realistic data - most days have loads, some are missing
            long loadedCount = random.nextInt(60) + 40; // 40-99 loads
            long missingCount = random.nextDouble() < 0.15 ? 1 : 0; // 15% chance of missing

            if (missingCount == 1) {
                loadedCount = 0; // If missing, no loads
            }

            statusCounts.add(new BatchStatusCount(date, loadedCount, missingCount));
        }

        return statusCounts;
    }

    private File generateMockChart(List<BatchStatusCount> statusCounts) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Take every 10th day to avoid overcrowded chart
        for (int i = 0; i < statusCounts.size(); i += 10) {
            BatchStatusCount count = statusCounts.get(i);
            String dateLabel = count.getDate().format(DateTimeFormatter.ofPattern("MM/dd"));
            dataset.addValue(count.getLoadedCount(), "Loaded", dateLabel);
            dataset.addValue(count.getMissingCount(), "Missing", dateLabel);
        }

        JFreeChart chart = ChartFactory.createStackedBarChart(
                "Batch Load Status - Last 120 Days",
                "Date",
                "Count",
                dataset
        );

        // Apply the same styling as your service
        styleStatusChart(chart);

        Path tempFile = Files.createTempFile("preview-chart", ".png");
        ChartUtils.saveChartAsPNG(tempFile.toFile(), chart, 800, 400);

        return tempFile.toFile();
    }

    private void styleStatusChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(76, 175, 80)); // Green for loaded
        renderer.setSeriesPaint(1, new Color(244, 67, 54)); // Red for missing
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
    }

    private String convertChartToBase64(File chartFile) throws IOException {
        byte[] fileContent = Files.readAllBytes(chartFile.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }
}
