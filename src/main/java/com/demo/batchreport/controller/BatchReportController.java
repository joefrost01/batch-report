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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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

    // Add these methods to your BatchReportController

    @GetMapping("/email-optimized-preview")
    public ResponseEntity<String> emailOptimizedPreview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate batchDate) {

        try {
            // Create mock data for preview
            List<BatchRecord> mockData = generateMockData(batchDate);
            List<BatchStatusCount> mockStatus = generateMockStatusData(batchDate);

            // Generate email-optimized HTML
            String html = batchReportService.generateEmailOptimizedHtml(batchDate, mockData, mockStatus);

            // Generate and embed chart as base64
            File chartFile = generateMockChart(mockStatus);
            String base64Chart = convertChartToBase64(chartFile);

            // Replace chart reference with base64 data URL
            html = html.replace("src=\"cid:statusChart\"",
                    "src=\"data:image/png;base64," + base64Chart + "\"");

            // Cleanup
            Files.deleteIfExists(chartFile.toPath());

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating email-optimized preview: " + e.getMessage());
        }
    }

    @GetMapping("/generate-eml-file")
    public ResponseEntity<byte[]> generateProperEmlFile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate batchDate) {

        try {
            // Create mock data
            List<BatchRecord> mockData = generateMockData(batchDate);
            List<BatchStatusCount> mockStatus = generateMockStatusData(batchDate);

            // Generate email-optimized HTML
            String htmlContent = batchReportService.generateEmailOptimizedHtml(batchDate, mockData, mockStatus);

            // Generate chart and encode as base64
            File chartFile = generateMockChart(mockStatus);
            byte[] chartBytes = Files.readAllBytes(chartFile.toPath());
            String base64Chart = Base64.getEncoder().encodeToString(chartBytes);

            // Create proper EML with embedded image
            String emlContent = createProperEmlContent(htmlContent, base64Chart, batchDate);

            // Cleanup
            Files.deleteIfExists(chartFile.toPath());

            String fileName = "batch-report-" + batchDate.toString() + ".eml";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + fileName)
                    .header("Content-Type", "message/rfc822")
                    .body(emlContent.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Error generating EML: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private String createProperEmlContent(String htmlContent, String base64Chart, LocalDate batchDate) {
        String boundary = "----=_NextPart_" + System.currentTimeMillis();
        String imageBoundary = "----=_NextPart_Image_" + System.currentTimeMillis();

        StringBuilder eml = new StringBuilder();

        // Email headers
        eml.append("From: reports@company.com\r\n");
        eml.append("To: test@company.com\r\n");
        eml.append("Subject: Batch Load Report - ").append(batchDate.toString()).append("\r\n");
        eml.append("Date: ").append(formatEmailDate()).append("\r\n");
        eml.append("MIME-Version: 1.0\r\n");
        eml.append("Content-Type: multipart/related; boundary=\"").append(boundary).append("\"\r\n");
        eml.append("\r\n");

        // HTML content part
        eml.append("--").append(boundary).append("\r\n");
        eml.append("Content-Type: text/html; charset=UTF-8\r\n");
        eml.append("Content-Transfer-Encoding: quoted-printable\r\n");
        eml.append("\r\n");

        // Replace the chart reference to use Content-ID
        String htmlWithCid = htmlContent.replace("src=\"cid:statusChart\"", "src=\"cid:chart@company.com\"");
        eml.append(encodeQuotedPrintable(htmlWithCid));
        eml.append("\r\n\r\n");

        // Chart image part
        eml.append("--").append(boundary).append("\r\n");
        eml.append("Content-Type: image/png\r\n");
        eml.append("Content-Transfer-Encoding: base64\r\n");
        eml.append("Content-ID: <chart@company.com>\r\n");
        eml.append("Content-Disposition: inline; filename=\"chart.png\"\r\n");
        eml.append("\r\n");

        // Split base64 into 76-character lines (RFC requirement)
        for (int i = 0; i < base64Chart.length(); i += 76) {
            int end = Math.min(i + 76, base64Chart.length());
            eml.append(base64Chart.substring(i, end)).append("\r\n");
        }

        // End boundary
        eml.append("--").append(boundary).append("--\r\n");

        return eml.toString();
    }

    private String encodeQuotedPrintable(String text) {
        // Simple quoted-printable encoding for HTML
        StringBuilder encoded = new StringBuilder();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        int lineLength = 0;
        for (byte b : bytes) {
            if (b == '\n') {
                encoded.append("\r\n");
                lineLength = 0;
            } else if (b == '\r') {
                // Skip standalone CR
            } else if (b >= 33 && b <= 126 && b != '=') {
                encoded.append((char) b);
                lineLength++;
            } else {
                encoded.append(String.format("=%02X", b & 0xFF));
                lineLength += 3;
            }

            // Soft line break for long lines
            if (lineLength >= 70) {
                encoded.append("=\r\n");
                lineLength = 0;
            }
        }

        return encoded.toString();
    }

    private String formatEmailDate() {
        return ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private List<BatchRecord> generateMockData(LocalDate batchDate) {
        return Arrays.asList(
                new BatchRecord(null, "Equity", "US Large Cap", "Base", "Entity A", batchDate),
                new BatchRecord(null, "Equity", "US Large Cap", "Stress", "Entity A", batchDate),
                new BatchRecord(null, "Equity", "US Large Cap", "Adverse", "Entity A", batchDate),
                new BatchRecord(null, "Fixed Income", "Corporate Bonds", "Base", "Entity B", batchDate),
                new BatchRecord(null, "Fixed Income", "Corporate Bonds", "Stress", "Entity B", batchDate),
                new BatchRecord(null, "Derivatives", "Interest Rate", "Base", "Entity A", batchDate),
                new BatchRecord(null, "Cash", "Money Market", "Base", "Entity C", batchDate)
        );
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
