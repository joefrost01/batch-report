package com.demo.batchreport.service;

import com.demo.batchreport.config.Config;
import com.demo.batchreport.config.ExpectedScenariosConfig;
import com.demo.batchreport.domain.*;
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
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.demo.batchreport.config.StyleConfig.getStyles;

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
            List<BatchSummary> summaryData = generateSummaryDataWithExpectations(batchRecords);
            List<ScenarioDetail> scenarioDetails = generateScenarioDetails(batchRecords);
            List<BatchStatusCount> statusCounts = findStatusCountsForLast120Days(batchDate);

            File chartFile = generateStatusChart(statusCounts);
            String htmlContent = buildBatchReportEmail(batchDate, summaryData, scenarioDetails, statusCounts);

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
        List<BatchSummary> summaryData = generateSummaryDataWithExpectations(batchRecords);
        List<ScenarioDetail> scenarioDetails = generateScenarioDetails(batchRecords);
        return buildBatchReportEmail(batchDate, summaryData, scenarioDetails, statusCounts);
    }

    private List<BatchSummary> generateSummaryDataWithExpectations(List<BatchRecord> batchRecords) {
        // Group loaded records by asset class, product, and entity
        Map<String, Long> loadedCounts = batchRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> String.format("%s|%s|%s",
                                record.getAssetClass(),
                                record.getProduct(),
                                record.getEntity()),
                        Collectors.counting()
                ));

        // Generate summary for all expected combinations
        List<BatchSummary> summaries = new ArrayList<>();

        Map<String, List<ExpectedScenariosConfig.ExpectedScenario>> scenariosByGroup =
                ExpectedScenariosConfig.getScenariosByGroup();

        for (Map.Entry<String, List<ExpectedScenariosConfig.ExpectedScenario>> entry : scenariosByGroup.entrySet()) {
            String groupKey = entry.getKey();
            String[] parts = groupKey.split("\\|");
            String assetClass = parts[0];
            String product = parts[1];
            String entity = parts[2];

            long expectedCount = entry.getValue().size();
            long loadedCount = loadedCounts.getOrDefault(groupKey, 0L);

            BatchSummary.CompletionStatus status = BatchSummary.CompletionStatus.fromCounts(
                    loadedCount, expectedCount);

            summaries.add(new BatchSummary(assetClass, product, entity,
                    loadedCount, expectedCount, status));
        }

        // Add any unexpected combinations that were loaded but not in expected config
        for (Map.Entry<String, Long> entry : loadedCounts.entrySet()) {
            String groupKey = entry.getKey();
            String[] parts = groupKey.split("\\|");
            String assetClass = parts[0];
            String product = parts[1];
            String entity = parts[2];

            // Check if this combination is expected
            boolean isExpected = scenariosByGroup.containsKey(groupKey);

            if (!isExpected) {
                summaries.add(new BatchSummary(assetClass, product, entity,
                        entry.getValue(), 0L, BatchSummary.CompletionStatus.UNKNOWN));
            }
        }

        return summaries.stream()
                .sorted((a, b) -> {
                    int assetClassCompare = a.getAssetClass().compareTo(b.getAssetClass());
                    if (assetClassCompare != 0) return assetClassCompare;

                    int productCompare = a.getProduct().compareTo(b.getProduct());
                    if (productCompare != 0) return productCompare;

                    return a.getEntity().compareTo(b.getEntity());
                })
                .collect(Collectors.toList());
    }

    private List<ScenarioDetail> generateScenarioDetails(List<BatchRecord> batchRecords) {
        // Create a set of loaded scenarios
        Set<String> loadedScenarios = batchRecords.stream()
                .map(record -> String.format("%s|%s|%s|%s",
                        record.getAssetClass(),
                        record.getProduct(),
                        record.getEntity(),
                        record.getScenario()))
                .collect(Collectors.toSet());

        List<ScenarioDetail> details = new ArrayList<>();

        // Add all expected scenarios
        for (ExpectedScenariosConfig.ExpectedScenario expected : ExpectedScenariosConfig.getAllExpectedScenarios()) {
            String fullKey = expected.getFullKey();
            boolean isLoaded = loadedScenarios.contains(fullKey);

            details.add(new ScenarioDetail(
                    expected.getAssetClass(),
                    expected.getProduct(),
                    expected.getScenario(),
                    expected.getEntity(),
                    isLoaded,
                    true // isExpected = true for all from config
            ));
        }

        // Add any unexpected scenarios that were loaded
        for (BatchRecord record : batchRecords) {
            boolean isExpected = ExpectedScenariosConfig.isScenarioExpected(
                    record.getAssetClass(),
                    record.getProduct(),
                    record.getEntity(),
                    record.getScenario()
            );

            if (!isExpected) {
                details.add(new ScenarioDetail(
                        record.getAssetClass(),
                        record.getProduct(),
                        record.getScenario(),
                        record.getEntity(),
                        true, // isLoaded = true
                        false // isExpected = false
                ));
            }
        }

        return details.stream()
                .sorted((a, b) -> {
                    int assetClassCompare = a.getAssetClass().compareTo(b.getAssetClass());
                    if (assetClassCompare != 0) return assetClassCompare;

                    int productCompare = a.getProduct().compareTo(b.getProduct());
                    if (productCompare != 0) return productCompare;

                    int entityCompare = a.getEntity().compareTo(b.getEntity());
                    if (entityCompare != 0) return entityCompare;

                    return a.getScenario().compareTo(b.getScenario());
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
                                         List<ScenarioDetail> scenarioDetails, List<BatchStatusCount> statusCounts) {

        String formattedDate = batchDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        // Calculate statistics
        long totalLoaded = summaryData.stream().mapToLong(BatchSummary::getLoadCount).sum();
        long totalExpected = summaryData.stream().mapToLong(BatchSummary::getExpectedCount).sum();
        long completeSummaries = summaryData.stream().mapToLong(s -> s.isComplete() ? 1 : 0).sum();
        long loadedScenarios = scenarioDetails.stream().mapToLong(s -> s.isLoaded() ? 1 : 0).sum();
        long missingScenarios = scenarioDetails.stream().mapToLong(s -> s.getStatus() == ScenarioDetail.ScenarioStatus.MISSING ? 1 : 0).sum();

        int uniqueAssetClasses = (int) summaryData.stream().map(BatchSummary::getAssetClass).distinct().count();
        int uniqueProducts = (int) summaryData.stream().map(BatchSummary::getProduct).distinct().count();
        int uniqueEntities = (int) summaryData.stream().map(BatchSummary::getEntity).distinct().count();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <style>\n")
                .append(getStyles())
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <div class=\"container\">\n")
                .append("        <div class=\"header\">\n")
                .append("            <h1>📊 Surveillance Data Load Report</h1>\n")
                .append("            <div class=\"batch-date\">").append(formattedDate).append("</div>\n")
                .append("        </div>\n")
                .append("        \n")
                .append(buildOverviewStatsSection(totalLoaded, totalExpected, completeSummaries, loadedScenarios, missingScenarios, uniqueAssetClasses, uniqueProducts, uniqueEntities))
                .append("        \n")
                .append(buildSummarySection(summaryData))
                .append("        \n")
                .append(buildChartSection())
                .append("        \n")
                .append(buildDetailSection(scenarioDetails))
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

    private String buildOverviewStatsSection(long totalLoaded, long totalExpected, long completeSummaries,
                                             long loadedScenarios, long missingScenarios,
                                             int uniqueAssetClasses, int uniqueProducts, int uniqueEntities) {
        double completionRate = totalExpected > 0 ? (double) totalLoaded / totalExpected * 100 : 0;

        return "<div class=\"stats-overview\">\n" +
                "    <div class=\"stat-item\">\n" +
                "        <span class=\"stat-number\">" + String.format("%,d", totalLoaded) + "</span>\n" +
                "        <span class=\"stat-label\">Scenarios Loaded</span>\n" +
                "    </div>\n" +
                "    <div class=\"stat-item\">\n" +
                "        <span class=\"stat-number\">" + String.format("%,d", totalExpected) + "</span>\n" +
                "        <span class=\"stat-label\">Expected Scenarios</span>\n" +
                "    </div>\n" +
                "    <div class=\"stat-item\">\n" +
                "        <span class=\"stat-number\">" + String.format("%.1f%%", completionRate) + "</span>\n" +
                "        <span class=\"stat-label\">Completion Rate</span>\n" +
                "    </div>\n" +
                "    <div class=\"stat-item\">\n" +
                "        <span class=\"stat-number\">" + String.format("%,d", completeSummaries) + "</span>\n" +
                "        <span class=\"stat-label\">Complete Groups</span>\n" +
                "    </div>\n" +
                "    <div class=\"stat-item\">\n" +
                "        <span class=\"stat-number\">" + String.format("%,d", missingScenarios) + "</span>\n" +
                "        <span class=\"stat-label\">Missing Scenarios</span>\n" +
                "    </div>\n" +
                "    <div class=\"stat-item\">\n" +
                "        <span class=\"stat-number\">" + uniqueAssetClasses + "</span>\n" +
                "        <span class=\"stat-label\">Asset Classes</span>\n" +
                "    </div>\n" +
                "</div>\n";
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
                .append("                    <th>Loaded</th>\n")
                .append("                    <th>Expected</th>\n")
                .append("                    <th>Completion</th>\n")
                .append("                    <th>Status</th>\n")
                .append("                </tr>\n")
                .append("            </thead>\n")
                .append("            <tbody>\n");

        for (BatchSummary summary : summaryData) {
            String statusIcon = getStatusIcon(summary.getStatus());
            String statusClass = "status-" + summary.getStatus().getCssClass();

            section.append("                <tr>\n")
                    .append("                    <td>").append(escapeHtml(summary.getAssetClass())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(summary.getProduct())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(summary.getEntity())).append("</td>\n")
                    .append("                    <td class=\"number-cell\">").append(String.format("%,d", summary.getLoadCount())).append("</td>\n")
                    .append("                    <td class=\"number-cell\">").append(String.format("%,d", summary.getExpectedCount())).append("</td>\n")
                    .append("                    <td class=\"number-cell\">").append(summary.getCompletionPercentage()).append("</td>\n")
                    .append("                    <td class=\"").append(statusClass).append("\">")
                    .append(statusIcon).append(" ").append(summary.getStatus().getDisplayName()).append("</td>\n")
                    .append("                </tr>\n");
        }

        section.append("            </tbody>\n")
                .append("        </table>\n")
                .append("    </div>\n")
                .append("</div>\n");

        return section.toString();
    }

    private String buildDetailSection(List<ScenarioDetail> scenarioDetails) {
        if (scenarioDetails.isEmpty()) {
            return "<div class=\"section\">\n" +
                    "    <h2>📄 Scenario Details</h2>\n" +
                    "    <div class=\"empty-state\">No scenario details available</div>\n" +
                    "</div>\n";
        }

        StringBuilder section = new StringBuilder();
        section.append("<div class=\"section\">\n")
                .append("    <h2>📄 Scenario Details</h2>\n")
                .append("    <div class=\"table-container\">\n")
                .append("        <table>\n")
                .append("            <thead>\n")
                .append("                <tr>\n")
                .append("                    <th>Asset Class</th>\n")
                .append("                    <th>Product</th>\n")
                .append("                    <th>Entity</th>\n")
                .append("                    <th>Scenario</th>\n")
                .append("                    <th>Status</th>\n")
                .append("                </tr>\n")
                .append("            </thead>\n")
                .append("            <tbody>\n");

        for (ScenarioDetail detail : scenarioDetails) {
            String statusIcon = getScenarioStatusIcon(detail.getStatus());
            String statusClass = "status-" + detail.getStatus().getCssClass();

            section.append("                <tr>\n")
                    .append("                    <td>").append(escapeHtml(detail.getAssetClass())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(detail.getProduct())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(detail.getEntity())).append("</td>\n")
                    .append("                    <td>").append(escapeHtml(detail.getScenario())).append("</td>\n")
                    .append("                    <td class=\"").append(statusClass).append("\">")
                    .append(statusIcon).append(" ").append(detail.getStatus().getDisplayName()).append("</td>\n")
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

    private String getStatusIcon(BatchSummary.CompletionStatus status) {
        switch (status) {
            case COMPLETE:
                return "✅";
            case INCOMPLETE:
                return "❌";
            case EXCESS:
                return "⚠️";
            default:
                return "❓";
        }
    }

    private String getScenarioStatusIcon(ScenarioDetail.ScenarioStatus status) {
        switch (status) {
            case LOADED:
                return "✅";
            case MISSING:
                return "❌";
            case UNEXPECTED:
                return "⚠️";
            default:
                return "➖";
        }
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