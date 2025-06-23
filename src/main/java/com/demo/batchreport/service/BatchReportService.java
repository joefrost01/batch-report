package com.demo.batchreport.service;

import com.demo.batchreport.config.Config;
import com.demo.batchreport.domain.BatchRecord;
import com.demo.batchreport.domain.BatchStatusCount;
import com.demo.batchreport.domain.BatchSummary;
import com.demo.batchreport.repository.BatchQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchReportService {

    private final JavaMailSender mailSender;
    private final Config config;
    private final BatchQueryRepository batchQueryRepository;

    public void sendBatchReport(LocalDate batchDate) {
        try {
            List<BatchRecord> batchRecords = batchQueryRepository.findAllByBatchDate(batchDate);
            List<BatchSummary> summaryData = generateSummaryData(batchRecords);
            List<BatchStatusCount> statusCounts = findStatusCountsForLast120Days(batchDate);

            File chartFile = generateStatusChart(statusCounts);
            String htmlContent = buildBatchReportEmail(batchDate, summaryData, batchRecords, statusCounts);

            sendEmail(buildSubject(batchDate), htmlContent, chartFile);

            // Cleanup
            Files.deleteIfExists(chartFile.toPath());

            log.info("Batch report sent successfully for date: {}", batchDate);

        } catch (Exception e) {
            log.error("Failed to send batch report for date: {}", batchDate, e);
            throw new RuntimeException("Batch report generation failed", e);
        }
    }

    public List<BatchStatusCount> findStatusCountsForLast120Days(LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(119);
        List<BatchRecord> records = batchQueryRepository.findAllByBatchDateBetween(startDate, endDate);

        // Group by date and count
        Map<LocalDate, Long> countsByDate = records.stream()
                .collect(Collectors.groupingBy(
                        BatchRecord::getBatchDate,
                        Collectors.counting()
                ));

        // Generate full 120-day range
        List<BatchStatusCount> statusCounts = new ArrayList<>();
        for (int i = 119; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            Long loadedCount = countsByDate.getOrDefault(date, 0L);
            Long missingCount = loadedCount == 0 ? 1L : 0L;

            statusCounts.add(new BatchStatusCount(date, loadedCount, missingCount));
        }

        return statusCounts;
    }

    public String generateBatchReportHtml(LocalDate batchDate, List<BatchRecord> batchRecords, List<BatchStatusCount> statusCounts) {
        List<BatchSummary> summaryData = generateSummaryData(batchRecords);
        return buildBatchReportEmail(batchDate, summaryData, batchRecords, statusCounts);
    }

    private List<BatchSummary> generateSummaryData(List<BatchRecord> batchRecords) {
        Map<String, Long> summaryMap = batchRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> String.format("%s|%s|%s",
                                record.getAssetClass(),
                                record.getProduct(),
                                record.getEntity()),
                        Collectors.counting()
                ));

        return summaryMap.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    return new BatchSummary(parts[0], parts[1], parts[2], entry.getValue());
                })
                .sorted((a, b) -> {
                    int assetClassCompare = a.getAssetClass().compareTo(b.getAssetClass());
                    if (assetClassCompare != 0) return assetClassCompare;

                    int productCompare = a.getProduct().compareTo(b.getProduct());
                    if (productCompare != 0) return productCompare;

                    return a.getEntity().compareTo(b.getEntity());
                })
                .collect(Collectors.toList());
    }

    private File generateStatusChart(List<BatchStatusCount> statusCounts) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        statusCounts.stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .forEach(count -> {
                    String dateLabel = count.getDate().format(DateTimeFormatter.ofPattern("MM/dd"));
                    dataset.addValue(count.getLoadedCount(), "Loaded", dateLabel);
                    dataset.addValue(count.getMissingCount(), "Missing", dateLabel);
                });

        JFreeChart chart = ChartFactory.createStackedBarChart(
                "Batch Load Status - Last 120 Days",
                "Date",
                "Count",
                dataset
        );

        styleStatusChart(chart);

        Path tempFile = Files.createTempFile("batch-status-chart", ".png");
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

    private String buildBatchReportEmail(LocalDate batchDate, List<BatchSummary> summaryData,
                                         List<BatchRecord> batchRecords, List<BatchStatusCount> statusCounts) {

        String formattedDate = batchDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        long totalLoaded = summaryData.stream().mapToLong(BatchSummary::getLoadCount).sum();
        int uniqueAssetClasses = (int) summaryData.stream().map(BatchSummary::getAssetClass).distinct().count();
        int uniqueProducts = (int) summaryData.stream().map(BatchSummary::getProduct).distinct().count();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <style>\n")
                .append("        body {\n")
                .append("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n")
                .append("            line-height: 1.6;\n")
                .append("            color: #333;\n")
                .append("            max-width: 1200px;\n")
                .append("            margin: 0 auto;\n")
                .append("            padding: 20px;\n")
                .append("            background-color: #f8f9fa;\n")
                .append("        }\n")
                .append("        .container {\n")
                .append("            background-color: white;\n")
                .append("            border-radius: 8px;\n")
                .append("            box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n")
                .append("            overflow: hidden;\n")
                .append("        }\n")
                .append("        .header {\n")
                .append("            background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%);\n")
                .append("            color: white;\n")
                .append("            padding: 30px;\n")
                .append("            text-align: center;\n")
                .append("        }\n")
                .append("        .header h1 {\n")
                .append("            margin: 0;\n")
                .append("            font-size: 28px;\n")
                .append("            font-weight: 300;\n")
                .append("        }\n")
                .append("        .header .batch-date {\n")
                .append("            font-size: 18px;\n")
                .append("            opacity: 0.9;\n")
                .append("            margin-top: 10px;\n")
                .append("        }\n")
                .append("        .stats-overview {\n")
                .append("            display: flex;\n")
                .append("            justify-content: space-around;\n")
                .append("            padding: 20px;\n")
                .append("            background-color: #e3f2fd;\n")
                .append("            border-bottom: 1px solid #bbdefb;\n")
                .append("        }\n")
                .append("        .stat-item {\n")
                .append("            text-align: center;\n")
                .append("            padding: 10px;\n")
                .append("        }\n")
                .append("        .stat-number {\n")
                .append("            font-size: 24px;\n")
                .append("            font-weight: bold;\n")
                .append("            color: #1976d2;\n")
                .append("            display: block;\n")
                .append("        }\n")
                .append("        .stat-label {\n")
                .append("            font-size: 12px;\n")
                .append("            color: #666;\n")
                .append("            text-transform: uppercase;\n")
                .append("            letter-spacing: 0.5px;\n")
                .append("        }\n")
                .append("        .section {\n")
                .append("            padding: 30px;\n")
                .append("            border-bottom: 1px solid #e0e0e0;\n")
                .append("        }\n")
                .append("        .section h2 {\n")
                .append("            color: #1976d2;\n")
                .append("            margin-top: 0;\n")
                .append("            margin-bottom: 20px;\n")
                .append("            font-size: 20px;\n")
                .append("            border-bottom: 2px solid #1976d2;\n")
                .append("            padding-bottom: 10px;\n")
                .append("        }\n")
                .append("        .table-container {\n")
                .append("            overflow-x: auto;\n")
                .append("            margin: 20px 0;\n")
                .append("        }\n")
                .append("        table {\n")
                .append("            width: 100%;\n")
                .append("            border-collapse: collapse;\n")
                .append("            background-color: white;\n")
                .append("            box-shadow: 0 1px 3px rgba(0,0,0,0.12);\n")
                .append("            border-radius: 6px;\n")
                .append("            overflow: hidden;\n")
                .append("        }\n")
                .append("        th {\n")
                .append("            background-color: #1976d2;\n")
                .append("            color: white;\n")
                .append("            padding: 15px 12px;\n")
                .append("            text-align: left;\n")
                .append("            font-weight: 600;\n")
                .append("            font-size: 14px;\n")
                .append("            text-transform: uppercase;\n")
                .append("            letter-spacing: 0.5px;\n")
                .append("        }\n")
                .append("        td {\n")
                .append("            padding: 12px;\n")
                .append("            border-bottom: 1px solid #e0e0e0;\n")
                .append("            font-size: 14px;\n")
                .append("        }\n")
                .append("        tr:nth-child(even) {\n")
                .append("            background-color: #f8f9fa;\n")
                .append("        }\n")
                .append("        tr:hover {\n")
                .append("            background-color: #e3f2fd;\n")
                .append("        }\n")
                .append("        .number-cell {\n")
                .append("            text-align: right;\n")
                .append("            font-weight: 600;\n")
                .append("            color: #1976d2;\n")
                .append("        }\n")
                .append("        .chart-section {\n")
                .append("            text-align: center;\n")
                .append("            padding: 30px;\n")
                .append("            background-color: #fafafa;\n")
                .append("        }\n")
                .append("        .chart-section h2 {\n")
                .append("            color: #333;\n")
                .append("            margin-bottom: 20px;\n")
                .append("        }\n")
                .append("        .chart-section img {\n")
                .append("            max-width: 100%;\n")
                .append("            height: auto;\n")
                .append("            border-radius: 6px;\n")
                .append("            box-shadow: 0 2px 8px rgba(0,0,0,0.15);\n")
                .append("        }\n")
                .append("        .empty-state {\n")
                .append("            text-align: center;\n")
                .append("            padding: 40px;\n")
                .append("            color: #666;\n")
                .append("            font-style: italic;\n")
                .append("        }\n")
                .append("        .footer {\n")
                .append("            background-color: #f5f5f5;\n")
                .append("            padding: 20px 30px;\n")
                .append("            text-align: center;\n")
                .append("            font-size: 12px;\n")
                .append("            color: #666;\n")
                .append("            border-top: 1px solid #e0e0e0;\n")
                .append("        }\n")
                .append("        @media (max-width: 768px) {\n")
                .append("            .stats-overview {\n")
                .append("                flex-direction: column;\n")
                .append("            }\n")
                .append("            .stat-item {\n")
                .append("                margin: 5px 0;\n")
                .append("            }\n")
                .append("            th, td {\n")
                .append("                padding: 8px 6px;\n")
                .append("                font-size: 12px;\n")
                .append("            }\n")
                .append("        }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <div class=\"container\">\n")
                .append("        <div class=\"header\">\n")
                .append("            <h1>📊 Surveillance Data Load Report</h1>\n")
                .append("            <div class=\"batch-date\">").append(formattedDate).append("</div>\n")
                .append("        </div>\n")
                .append("        \n")
                .append(buildSummarySection(summaryData))
                .append("        \n")
                .append(buildChartSection())
                .append("        \n")
                .append(buildDetailSection(batchRecords))
                .append("        \n")
                .append("        <div class=\"footer\">\n")
                .append("            <p>Report generated on ").append(timestamp).append(" | Trade Surveillance</p>\n")
                .append("            <p>For questions or issues, please contact the Trade Surveillance dev team via the Teams channel.</p>\n")
                .append("        </div>\n")
                .append("    </div>\n")
                .append("</body>\n")
                .append("</html>");

        return html.toString();
    }

    private String buildSummarySection(List<BatchSummary> summaryData) {
        if (summaryData.isEmpty()) {
            return "<div class=\"section\">\n" +
                    "    <h2>📋 Load Summary</h2>\n" +
                    "    <div class=\"empty-state\">No data loaded for this batch date</div>\n" +
                    "</div>\n";
        }

        StringBuilder section = new StringBuilder();
        section.append("<div class=\"section\">\n")
                .append("    <h2>📋 Load Summary</h2>\n")
                .append("    <div class=\"table-container\">\n")
                .append("        <table>\n")
                .append("            <thead>\n")
                .append("                <tr>\n")
                .append("                    <th>Asset Class</th>\n")
                .append("                    <th>Product</th>\n")
                .append("                    <th>Entity</th>\n")
                .append("                    <th>Load Count</th>\n")
                .append("                </tr>\n")
                .append("            </thead>\n")
                .append("            <tbody>\n");

        for (BatchSummary summary : summaryData) {
            section.append("                <tr>\n")
                    .append("                    <td>").append(escapeHtml(summary.getAssetClass())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(summary.getProduct())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(summary.getEntity())).append("</td>\n")
                    .append("                    <td class=\"number-cell\">").append(String.format("%,d", summary.getLoadCount())).append("</td>\n")
                    .append("                </tr>\n");
        }

        section.append("            </tbody>\n")
                .append("        </table>\n")
                .append("    </div>\n")
                .append("</div>\n");

        return section.toString();
    }

    private String buildDetailSection(List<BatchRecord> batchRecords) {
        if (batchRecords.isEmpty()) {
            return "<div class=\"section\">\n" +
                    "    <h2>📄 Detailed Records</h2>\n" +
                    "    <div class=\"empty-state\">No detailed records available</div>\n" +
                    "</div>\n";
        }

        StringBuilder section = new StringBuilder();
        section.append("<div class=\"section\">\n")
                .append("    <h2>📄 Detailed Records</h2>\n")
                .append("    <div class=\"table-container\">\n")
                .append("        <table>\n")
                .append("            <thead>\n")
                .append("                <tr>\n")
                .append("                    <th>Asset Class</th>\n")
                .append("                    <th>Product</th>\n")
                .append("                    <th>Scenario</th>\n")
                .append("                    <th>Entity</th>\n")
                .append("                    <th>Batch Date</th>\n")
                .append("                </tr>\n")
                .append("            </thead>\n")
                .append("            <tbody>\n");

        for (BatchRecord record : batchRecords) {
            section.append("                <tr>\n")
                    .append("                    <td>").append(escapeHtml(record.getAssetClass())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(record.getProduct())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(record.getScenario())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(record.getEntity())).append("</td>\n")
                    .append("                    <td>").append(record.getBatchDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("</td>\n")
                    .append("                </tr>\n");
        }

        section.append("            </tbody>\n")
                .append("        </table>\n")
                .append("    </div>\n")
                .append("</div>\n");

        return section.toString();
    }

    private String buildChartSection() {
        return "<div class=\"chart-section\">\n" +
                "    <h2>📈 120-Day Load Status Trend</h2>\n" +
                "    <img src=\"cid:statusChart\" alt=\"Batch Status Chart\"/>\n" +
                "    <p style=\"font-size: 12px; color: #666; margin-top: 15px;\">\n" +
                "        Green: Successfully loaded batches | Red: Missing/failed batches\n" +
                "    </p>\n" +
                "</div>\n";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String buildSubject(LocalDate batchDate) {
        return String.format("Batch Load Report - %s",
                batchDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    private void sendEmail(String subject, String htmlContent, File chartFile) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(config.getFromAddress(), config.getFromName());
        helper.setTo(config.getRecipients().toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // Attach chart
        if (chartFile != null) {
            helper.addInline("statusChart", chartFile);
        }

        mailSender.send(message);
    }
}

