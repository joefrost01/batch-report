package com.demo.batchreport.service;

import com.demo.batchreport.config.Config;
import com.demo.batchreport.config.ExpectedScenariosConfig;
import com.demo.batchreport.config.StyleConfig;
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

            // Add simulated data if no real data exists (for demo purposes)
            if (batchRecords.isEmpty()) {
                batchRecords = generateSimulatedBatchRecords(batchDate);
            }

            List<BatchSummary> summaryData = generateSummaryDataWithExpectations(batchRecords);
            List<ScenarioDetail> scenarioDetails = generateScenarioDetails(batchRecords);
            List<BatchStatusCount> statusCounts = findStatusCountsForLast120Days(batchDate);
            List<BackdatedScenario> backdatedScenarios = findRecentlyLoadedBackdatedScenarios(batchDate);

            File chartFile = generateStatusChart(statusCounts);

            // Use email-optimized HTML instead of regular HTML
            String htmlContent = buildEmailOptimizedReport(batchDate, summaryData, scenarioDetails, statusCounts, backdatedScenarios);

            sendEmail(buildSubject(batchDate), htmlContent, chartFile);

            // Cleanup
            Files.deleteIfExists(chartFile.toPath());

            log.info("Batch report sent successfully for date: {}", batchDate);

        } catch (Exception e) {
            log.error("Failed to send batch report for date: {}", batchDate, e);
            throw new RuntimeException("Batch report generation failed", e);
        }
    }

// Add these methods to your BatchReportService class

    /**
     * Generate email-optimized HTML that works better in email clients
     */
    public String generateEmailOptimizedHtml(LocalDate batchDate, List<BatchRecord> batchRecords,
                                             List<BatchStatusCount> statusCounts) {
        // Add simulated data if needed
        if (batchRecords.isEmpty()) {
            batchRecords = generateSimulatedBatchRecords(batchDate);
        }
        if (statusCounts.isEmpty()) {
            statusCounts = generateSimulatedStatusCounts(batchDate);
        }

        List<BatchSummary> summaryData = generateSummaryDataWithExpectations(batchRecords);
        List<ScenarioDetail> scenarioDetails = generateScenarioDetails(batchRecords);
        List<BackdatedScenario> backdatedScenarios = findRecentlyLoadedBackdatedScenarios(batchDate);

        return buildEmailOptimizedReport(batchDate, summaryData, scenarioDetails, statusCounts, backdatedScenarios);
    }

    private String buildEmailOptimizedReport(LocalDate batchDate, List<BatchSummary> summaryData,
                                             List<ScenarioDetail> scenarioDetails, List<BatchStatusCount> statusCounts,
                                             List<BackdatedScenario> backdatedScenarios) {

        String formattedDate = batchDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        // Calculate statistics
        long totalLoaded = summaryData.stream().mapToLong(BatchSummary::getLoadCount).sum();
        long totalExpected = summaryData.stream().mapToLong(BatchSummary::getExpectedCount).sum();
        long completeSummaries = summaryData.stream().mapToLong(s -> s.isComplete() ? 1 : 0).sum();
        long missingScenarios = scenarioDetails.stream().mapToLong(s -> s.getStatus() == ScenarioDetail.ScenarioStatus.MISSING ? 1 : 0).sum();

        double completionRate = totalExpected > 0 ? (double) totalLoaded / totalExpected * 100 : 0;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <!--[if gte mso 9]>\n")
                .append("    <xml>\n")
                .append("        <o:OfficeDocumentSettings>\n")
                .append("            <o:AllowPNG/>\n")
                .append("            <o:PixelsPerInch>96</o:PixelsPerInch>\n")
                .append("        </o:OfficeDocumentSettings>\n")
                .append("    </xml>\n")
                .append("    <![endif]-->\n")
                .append(getEmailOptimizedStyles())
                .append("</head>\n")
                .append("<body style=\"margin: 0; padding: 0; background-color: white !important; background: white !important; font-family: Arial, sans-serif;\" bgcolor=\"white\" class=\"darkmode-bg\">\n")

                // Use table-based layout for email compatibility - make it wider
                .append("    <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"background-color: white !important; background: white !important;\" bgcolor=\"white\">\n")
                .append("        <tr>\n")
                .append("            <td align=\"center\" style=\"padding: 20px; background-color: white !important;\" bgcolor=\"white\">\n")
                .append("                <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"900\" class=\"main-table\" style=\"max-width: 95%; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\" bgcolor=\"white\">\n")

                // Header with gradient background (email-safe)
                .append(buildEmailHeader(formattedDate))

                // Overview stats
                .append(buildEmailOverviewStats(totalLoaded, totalExpected, completionRate, completeSummaries, missingScenarios))

                // Summary section (always visible)
                .append(buildEmailSummarySection(summaryData))

                // Chart section with inline image
                .append(buildEmailChartSection())

                // Details section (first 20 items to avoid email length issues)
                .append(buildEmailDetailsSection(scenarioDetails.stream().limit(20).collect(Collectors.toList())))

                // Backdated scenarios
                .append(buildEmailBackdatedSection(backdatedScenarios))

                // Footer
                .append(buildEmailFooter(timestamp))

                .append("                </table>\n")
                .append("            </td>\n")
                .append("        </tr>\n")
                .append("    </table>\n")
                .append("</body>\n")
                .append("</html>");

        return html.toString();
    }

    private String getEmailOptimizedStyles() {
        return "    <style type=\"text/css\">\n" +
                "        #outlook a { padding: 0; }\n" +
                "        \n" +
                "        /* Email client resets */\n" +
                "        .ReadMsgBody { width: 100%; background-color: white !important; }\n" +
                "        .ExternalClass { width: 100%; background-color: white !important; }\n" +
                "        .ExternalClass, .ExternalClass p, .ExternalClass span, .ExternalClass font, .ExternalClass td, .ExternalClass div { line-height: 100%; background-color: white !important; }\n" +
                "        body, table, td, p, a, li, blockquote { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }\n" +
                "        table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }\n" +
                "        img { -ms-interpolation-mode: bicubic; }\n" +
                "        \n" +
                "        /* Outlook specific */\n" +
                "        .outlook-table { width: 100% !important; }\n" +
                "        \n" +
                "        /* Status colors - exempt from background override */\n" +
                "        .status-success { color: #4caf50 !important; font-weight: bold; background-color: transparent !important; }\n" +
                "        .status-warning { color: #ff9800 !important; font-weight: bold; background-color: transparent !important; }\n" +
                "        .status-danger { color: #f44336 !important; font-weight: bold; background-color: transparent !important; }\n" +
                "        .status-info { color: #00A693 !important; font-weight: bold; background-color: transparent !important; }\n" +
                "        \n" +
                "        /* Exempt header and stats from white background override */\n" +
                "        .header-gradient { background: linear-gradient(135deg, #006A4E 0%, #00A693 100%) !important; }\n" +
                "        .stats-bg { background-color: #e8f5f1 !important; }\n" +
                "        .chart-bg { background-color: #fafafa !important; }\n" +
                "        .footer-bg { background-color: #f5f5f5 !important; }\n" +
                "        .info-box-bg { background-color: #e8f5f1 !important; }\n" +
                "        .warning-box-bg { background-color: #fff3cd !important; }\n" +
                "        \n" +
                "        /* Mobile responsive */\n" +
                "        @media only screen and (max-width: 600px) {\n" +
                "            .container { width: 100% !important; }\n" +
                "            .content { padding: 10px !important; }\n" +
                "            .main-table { width: 100% !important; }\n" +
                "        }\n" +
                "        \n" +
                "        /* Ensure tables expand properly */\n" +
                "        .main-table { width: 900px; max-width: 95%; }\n" +
                "        .content-table { width: 100%; }\n" +
                "    </style>\n";
    }

    private String buildEmailHeader(String formattedDate) {
        return "                    <!-- Header with enhanced gradient and better text visibility -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"background: linear-gradient(135deg, #006A4E 0%, #00A693 100%); padding: 30px; text-align: center;\" class=\"header-gradient\">\n" +
                "                                        <h1 style=\"background: transparent;margin: 0; font-size: 32px; font-weight: 400; color: #ffffff !important; font-family: 'Segoe UI', Arial, sans-serif; text-shadow: 0 2px 4px rgba(0,0,0,0.4); letter-spacing: -0.5px;\">📊 Surveillance Data Load Report</h1>\n" +
                "                                        <div style=\"background: transparent;font-size: 20px; color: #ffffff !important; margin-top: 12px; font-family: 'Segoe UI', Arial, sans-serif; font-weight: 300; text-shadow: 0 1px 2px rgba(0,0,0,0.3);\">" + formattedDate + "</div>\n" +
                "                        </td>\n" +
                "                    </tr>\n";
    }

    private String buildEmailOverviewStats(long totalLoaded, long totalExpected, double completionRate,
                                           long completeSummaries, long missingScenarios) {
        return "                    <!-- Overview Stats -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"background-color: #e8f5f1 !important; padding: 20px;\" class=\"stats-bg\" bgcolor=\"#e8f5f1\">\n" +
                "                            <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" class=\"content-table\">\n" +
                "                                <tr>\n" +
                "                                    <td align=\"center\" style=\"padding: 10px; width: 16.66%;\">\n" +
                "                                        <div style=\"font-size: 28px; font-weight: bold; color: #006A4E; font-family: Arial, sans-serif;\">" + String.format("%,d", totalLoaded) + "</div>\n" +
                "                                        <div style=\"font-size: 11px; color: #666; text-transform: uppercase; font-family: Arial, sans-serif; letter-spacing: 0.5px;\">SCENARIOS LOADED</div>\n" +
                "                                    </td>\n" +
                "                                    <td align=\"center\" style=\"padding: 10px; width: 16.66%;\">\n" +
                "                                        <div style=\"font-size: 28px; font-weight: bold; color: #006A4E; font-family: Arial, sans-serif;\">" + String.format("%,d", totalExpected) + "</div>\n" +
                "                                        <div style=\"font-size: 11px; color: #666; text-transform: uppercase; font-family: Arial, sans-serif; letter-spacing: 0.5px;\">EXPECTED SCENARIOS</div>\n" +
                "                                    </td>\n" +
                "                                    <td align=\"center\" style=\"padding: 10px; width: 16.66%;\">\n" +
                "                                        <div style=\"font-size: 28px; font-weight: bold; color: #006A4E; font-family: Arial, sans-serif;\">" + String.format("%.1f%%", completionRate) + "</div>\n" +
                "                                        <div style=\"font-size: 11px; color: #666; text-transform: uppercase; font-family: Arial, sans-serif; letter-spacing: 0.5px;\">COMPLETION RATE</div>\n" +
                "                                    </td>\n" +
                "                                    <td align=\"center\" style=\"padding: 10px; width: 16.66%;\">\n" +
                "                                        <div style=\"font-size: 28px; font-weight: bold; color: #006A4E; font-family: Arial, sans-serif;\">" + String.format("%,d", completeSummaries) + "</div>\n" +
                "                                        <div style=\"font-size: 11px; color: #666; text-transform: uppercase; font-family: Arial, sans-serif; letter-spacing: 0.5px;\">COMPLETE GROUPS</div>\n" +
                "                                    </td>\n" +
                "                                    <td align=\"center\" style=\"padding: 10px; width: 16.66%;\">\n" +
                "                                        <div style=\"font-size: 28px; font-weight: bold; color: #006A4E; font-family: Arial, sans-serif;\">" + String.format("%,d", missingScenarios) + "</div>\n" +
                "                                        <div style=\"font-size: 11px; color: #666; text-transform: uppercase; font-family: Arial, sans-serif; letter-spacing: 0.5px;\">MISSING SCENARIOS</div>\n" +
                "                                    </td>\n" +
                "                                    <td align=\"center\" style=\"padding: 10px; width: 16.66%;\">\n" +
                "                                        <div style=\"font-size: 28px; font-weight: bold; color: #006A4E; font-family: Arial, sans-serif;\">📋</div>\n" +
                "                                        <div style=\"font-size: 11px; color: #666; text-transform: uppercase; font-family: Arial, sans-serif; letter-spacing: 0.5px;\">REPORT STATUS</div>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </table>\n" +
                "                        </td>\n" +
                "                    </tr>\n";
    }

    private String buildEmailSummarySection(List<BatchSummary> summaryData) {
        StringBuilder section = new StringBuilder();
        section.append("                    <!-- Load Summary -->\n")
                .append("                    <tr>\n")
                .append("                        <td style=\"padding: 30px; border-bottom: 1px solid #e0e0e0;\">\n")
                .append("                            <h2 style=\"color: #006A4E; margin: 0 0 20px 0; font-size: 20px; font-family: Arial, sans-serif;\">📋 Load Summary</h2>\n");

        if (summaryData.isEmpty()) {
            section.append("                            <div style=\"text-align: center; padding: 40px; color: #666; font-style: italic; font-family: Arial, sans-serif;\">No data loaded for this batch date</div>\n");
        } else {
            section.append("                            <table cellpadding=\"8\" cellspacing=\"0\" border=\"1\" width=\"100%\" class=\"data-table\" style=\"border-collapse: collapse; border: 1px solid #ddd; font-family: Arial, sans-serif; background-color: white !important;\">\n")
                    .append("                                <thead>\n")
                    .append("                                    <tr>\n")
                    .append("                                        <th style=\"background-color: #006A4E !important; color: white !important; padding: 12px; text-align: left; font-size: 13px; font-weight: 600; border: 1px solid #004d37;\">Asset Class</th>\n")
                    .append("                                        <th style=\"background-color: #006A4E !important; color: white !important; padding: 12px; text-align: left; font-size: 13px; font-weight: 600; border: 1px solid #004d37;\">Product</th>\n")
                    .append("                                        <th style=\"background-color: #006A4E !important; color: white !important; padding: 12px; text-align: left; font-size: 13px; font-weight: 600; border: 1px solid #004d37;\">Entity</th>\n")
                    .append("                                        <th style=\"background-color: #006A4E !important; color: white !important; padding: 12px; text-align: right; font-size: 13px; font-weight: 600; border: 1px solid #004d37;\">Loaded</th>\n")
                    .append("                                        <th style=\"background-color: #006A4E !important; color: white !important; padding: 12px; text-align: right; font-size: 13px; font-weight: 600; border: 1px solid #004d37;\">Expected</th>\n")
                    .append("                                        <th style=\"background-color: #006A4E !important; color: white !important; padding: 12px; text-align: right; font-size: 13px; font-weight: 600; border: 1px solid #004d37;\">Status</th>\n")
                    .append("                                    </tr>\n")
                    .append("                                </thead>\n")
                    .append("                                <tbody>\n");

            boolean isEven = false;
            for (BatchSummary summary : summaryData) {
                String statusIcon = getStatusIcon(summary.getStatus());
                String statusClass = "status-" + summary.getStatus().getCssClass();
                String rowBg = isEven ? "#f8f9fa" : "white";

                section.append("                                    <tr>\n")
                        .append("                                        <td style=\"padding: 14px 12px; border: 1px solid #e0e0e0; font-size: 14px; background-color: ").append(rowBg).append(" !important;\">").append(escapeHtml(summary.getAssetClass())).append("</td>\n")
                        .append("                                        <td style=\"padding: 14px 12px; border: 1px solid #e0e0e0; font-size: 14px; background-color: ").append(rowBg).append(" !important;\">").append(escapeHtml(summary.getProduct())).append("</td>\n")
                        .append("                                        <td style=\"padding: 14px 12px; border: 1px solid #e0e0e0; font-size: 14px; background-color: ").append(rowBg).append(" !important;\">").append(escapeHtml(summary.getEntity())).append("</td>\n")
                        .append("                                        <td style=\"padding: 14px 12px; border: 1px solid #e0e0e0; font-size: 14px; text-align: right; font-weight: 600; color: #006A4E; background-color: ").append(rowBg).append(" !important; font-family: 'Segoe UI', Arial, monospace;\">").append(String.format("%,d", summary.getLoadCount())).append("</td>\n")
                        .append("                                        <td style=\"padding: 14px 12px; border: 1px solid #e0e0e0; font-size: 14px; text-align: right; font-weight: 600; color: #006A4E; background-color: ").append(rowBg).append(" !important; font-family: 'Segoe UI', Arial, monospace;\">").append(String.format("%,d", summary.getExpectedCount())).append("</td>\n")
                        .append("                                        <td style=\"padding: 14px 12px; border: 1px solid #e0e0e0; font-size: 14px; text-align: right; background-color: ").append(rowBg).append(" !important; font-weight: 700;\" class=\"").append(statusClass).append("\">")
                        .append(statusIcon).append(" ").append(summary.getStatus().getDisplayName()).append("</td>\n")
                        .append("                                    </tr>\n");
                isEven = !isEven;
            }

            section.append("                                </tbody>\n")
                    .append("                            </table>\n");
        }

        section.append("                        </td>\n")
                .append("                    </tr>\n");

        return section.toString();
    }

    private String buildEmailChartSection() {
        return "                    <!-- Chart Section -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding: 30px; text-align: center; background-color: #fafafa !important; border-bottom: 1px solid #e0e0e0;\" class=\"chart-bg\" bgcolor=\"#fafafa\">\n" +
                "                            <h2 style=\"color: #006A4E; margin: 0 0 20px 0; font-size: 20px; font-family: Arial, sans-serif;\">📈 120-Day Load Status Trend</h2>\n" +
                "                            <img src=\"cid:statusChart\" alt=\"Batch Status Chart\" style=\"max-width: 100%; height: auto; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.15);\"/>\n" +
                "                            <p style=\"font-size: 12px; color: #666; margin-top: 15px; font-family: Arial, sans-serif;\">\n" +
                "                                Green: Successfully loaded batches | Red: Missing/failed batches\n" +
                "                            </p>\n" +
                "                        </td>\n" +
                "                    </tr>\n";
    }

    private String buildEmailDetailsSection(List<ScenarioDetail> scenarioDetails) {
        StringBuilder section = new StringBuilder();
        section.append("                    <!-- Scenario Details (Top 20) -->\n")
                .append("                    <tr>\n")
                .append("                        <td style=\"padding: 30px; border-bottom: 1px solid #e0e0e0;\">\n")
                .append("                            <h2 style=\"color: #006A4E; margin: 0 0 20px 0; font-size: 20px; font-family: Arial, sans-serif;\">📄 Key Scenario Details</h2>\n");

        if (scenarioDetails.isEmpty()) {
            section.append("                            <div style=\"text-align: center; padding: 40px; color: #666; font-style: italic; font-family: Arial, sans-serif;\">No scenario details available</div>\n");
        } else {
            // Show only missing and unexpected scenarios for relevance
            List<ScenarioDetail> relevantDetails = scenarioDetails.stream()
//                    .filter(detail -> detail.getStatus() == ScenarioDetail.ScenarioStatus.MISSING ||
//                            detail.getStatus() == ScenarioDetail.ScenarioStatus.UNEXPECTED)
                    .collect(Collectors.toList());

            if (relevantDetails.isEmpty()) {
                section.append("                            <div style=\"text-align: center; padding: 20px; color: #4caf50; font-weight: bold; font-family: Arial, sans-serif;\">✅ All expected scenarios loaded successfully!</div>\n");
            } else {
                section.append("                            <div style=\"background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin-bottom: 20px; font-family: Arial, sans-serif;\">\n")
                        .append("                                <strong>Attention Required:</strong> Please note, some scenarios are missing:\n")
                        .append("                            </div>\n")
                        .append("                            <table cellpadding=\"8\" cellspacing=\"0\" border=\"1\" width=\"100%\" style=\"border-collapse: collapse; border: 1px solid #ddd; font-family: Arial, sans-serif;\">\n")
                        .append("                                <thead>\n")
                        .append("                                    <tr style=\"background-color: #006A4E; color: white;\">\n")
                        .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Asset Class</th>\n")
                        .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Product</th>\n")
                        .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Entity</th>\n")
                        .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Scenario</th>\n")
                        .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Status</th>\n")
                        .append("                                    </tr>\n")
                        .append("                                </thead>\n")
                        .append("                                <tbody>\n");

                boolean isEven = false;
                for (ScenarioDetail detail : relevantDetails) {
                    String statusIcon = getScenarioStatusIcon(detail.getStatus());
                    String statusClass = "status-" + detail.getStatus().getCssClass();
                    String rowBg = isEven ? "#f8f9fa" : "white";

                    section.append("                                    <tr style=\"background-color: ").append(rowBg).append(";\">\n")
                            .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\">").append(escapeHtml(detail.getAssetClass())).append("</td>\n")
                            .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\">").append(escapeHtml(detail.getProduct())).append("</td>\n")
                            .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\">").append(escapeHtml(detail.getEntity())).append("</td>\n")
                            .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\">").append(escapeHtml(detail.getScenario())).append("</td>\n")
                            .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\" class=\"").append(statusClass).append("\">")
                            .append(statusIcon).append(" ").append(detail.getStatus().getDisplayName()).append("</td>\n")
                            .append("                                    </tr>\n");
                    isEven = !isEven;
                }

                section.append("                                </tbody>\n")
                        .append("                            </table>\n");
            }
        }

        section.append("                        </td>\n")
                .append("                    </tr>\n");

        return section.toString();
    }

    private String buildEmailBackdatedSection(List<BackdatedScenario> backdatedScenarios) {
        StringBuilder section = new StringBuilder();
        section.append("                    <!-- Backdated Scenarios -->\n")
                .append("                    <tr>\n")
                .append("                        <td style=\"padding: 30px; border-bottom: 1px solid #e0e0e0;\">\n")
                .append("                            <h2 style=\"color: #006A4E; margin: 0 0 20px 0; font-size: 20px; font-family: Arial, sans-serif;\">🔄 Recently Loaded Backdated Scenarios</h2>\n");

        if (backdatedScenarios.isEmpty()) {
            section.append("                            <div style=\"text-align: center; padding: 20px; color: #666; font-style: italic; font-family: Arial, sans-serif;\">No backdated scenarios loaded in the last 7 days</div>\n");
        } else {
            section.append("                            <div style=\"background-color: #e8f5f1; border-left: 4px solid #00A693; padding: 15px; margin-bottom: 20px; font-family: Arial, sans-serif;\">\n")
                    .append("                                <strong>Note:</strong> These scenarios have batch dates older than today but were loaded recently. ")
                    .append("This typically indicates catch-up processing or delayed data delivery.\n")
                    .append("                            </div>\n")
                    .append("                            <table cellpadding=\"8\" cellspacing=\"0\" border=\"1\" width=\"100%\" style=\"border-collapse: collapse; border: 1px solid #ddd; font-family: Arial, sans-serif;\">\n")
                    .append("                                <thead>\n")
                    .append("                                    <tr style=\"background-color: #006A4E; color: white;\">\n")
                    .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Asset Class</th>\n")
                    .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Product</th>\n")
                    .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Scenario</th>\n")
                    .append("                                        <th style=\"padding: 12px; text-align: left; font-size: 12px;\">Batch Date</th>\n")
                    .append("                                        <th style=\"padding: 12px; text-align: right; font-size: 12px;\">Days Late</th>\n")
                    .append("                                    </tr>\n")
                    .append("                                </thead>\n")
                    .append("                                <tbody>\n");

            boolean isEven = false;
            for (BackdatedScenario backdated : backdatedScenarios) {
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(backdated.getBatchDate(), backdated.getLoadedDate());
                String lateness = daysLate > 7 ? "status-danger" : (daysLate > 3 ? "status-warning" : "status-info");
                String rowBg = isEven ? "#f8f9fa" : "white";

                section.append("                                    <tr style=\"background-color: ").append(rowBg).append(";\">\n")
                        .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\">").append(escapeHtml(backdated.getAssetClass())).append("</td>\n")
                        .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\">").append(escapeHtml(backdated.getProduct())).append("</td>\n")
                        .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\">").append(escapeHtml(backdated.getScenario())).append("</td>\n")
                        .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px;\">").append(backdated.getBatchDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("</td>\n")
                        .append("                                        <td style=\"padding: 12px; border-bottom: 1px solid #e0e0e0; font-size: 14px; text-align: right; font-weight: 600;\" class=\"").append(lateness).append("\">").append(daysLate).append("</td>\n")
                        .append("                                    </tr>\n");
                isEven = !isEven;
            }

            section.append("                                </tbody>\n")
                    .append("                            </table>\n");
        }

        section.append("                        </td>\n")
                .append("                    </tr>\n");

        return section.toString();
    }

    private String buildEmailFooter(String timestamp) {
        return "                    <!-- Footer -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"background-color: #f5f5f5; padding: 20px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #e0e0e0; font-family: Arial, sans-serif;\">\n" +
                "                            <p style=\"margin: 0 0 10px 0;\">Report generated on " + timestamp + " | Trade Surveillance</p>\n" +
                "                            <p style=\"margin: 0;\">For questions or issues, please contact the Trade Surveillance dev team via the Teams channel.</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n";
    }

    /**
     * Generate simulated batch records for demo purposes when no real data exists
     */
    private List<BatchRecord> generateSimulatedBatchRecords(LocalDate batchDate) {
        Random random = new Random(batchDate.toEpochDay()); // Consistent seed for same date

        List<BatchRecord> simulatedRecords = new ArrayList<>();
        List<ExpectedScenariosConfig.ExpectedScenario> allExpected = ExpectedScenariosConfig.getAllExpectedScenarios();

        // Randomly load 75-90% of expected scenarios
        double loadRate = 0.75 + (random.nextDouble() * 0.15);

        for (ExpectedScenariosConfig.ExpectedScenario expected : allExpected) {
            if (random.nextDouble() < loadRate) {
                simulatedRecords.add(new BatchRecord(
                    null,
                    expected.getAssetClass(),
                    expected.getProduct(),
                    expected.getScenario(),
                    expected.getEntity(),
                    batchDate
                ));
            }
        }

        log.info("Generated {} simulated batch records for date: {}", simulatedRecords.size(), batchDate);
        return simulatedRecords;
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

        // If no real data, generate simulated data for the chart
        if (countsByDate.isEmpty()) {
            return generateSimulatedStatusCounts(endDate);
        }

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

    /**
     * Generate simulated status counts for demo purposes
     */
    private List<BatchStatusCount> generateSimulatedStatusCounts(LocalDate endDate) {
        List<BatchStatusCount> statusCounts = new ArrayList<>();
        Random random = new Random(endDate.toEpochDay());

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

    /**
     * Find scenarios that were loaded recently but have batch dates older than the current batch date
     * This helps analysts identify backdated data that was loaded after initial processing
     */
    public List<BackdatedScenario> findRecentlyLoadedBackdatedScenarios(LocalDate currentBatchDate) {
        // Look for records created in the last 7 days but with batch dates older than current
        LocalDate lookbackDate = currentBatchDate.minusDays(7);

        // This would typically use a created_date column, but for demo purposes we'll simulate
        // In production, you'd add a created_date/loaded_date timestamp column to BatchRecord
        List<BatchRecord> allRecentRecords = batchQueryRepository.findAllByBatchDateBetween(lookbackDate, currentBatchDate.minusDays(1));

        // If no real data, generate simulated backdated scenarios
        if (allRecentRecords.isEmpty()) {
            return generateSimulatedBackdatedScenarios(currentBatchDate);
        }

        // Simulate backdated scenarios (in production, filter by created_date > currentBatchDate.minusDays(7))
        return allRecentRecords.stream()
                .filter(record -> record.getBatchDate().isBefore(currentBatchDate.minusDays(1)))
                .map(record -> new BackdatedScenario(
                        record.getAssetClass(),
                        record.getProduct(),
                        record.getScenario(),
                        record.getEntity(),
                        record.getBatchDate(),
                        LocalDate.now() // In production, use actual loaded_date from database
                ))
                .sorted((a, b) -> b.getLoadedDate().compareTo(a.getLoadedDate())) // Most recent first
                .limit(50) // Limit to prevent overly long reports
                .collect(Collectors.toList());
    }

    /**
     * Generate simulated backdated scenarios for demo purposes
     */
    private List<BackdatedScenario> generateSimulatedBackdatedScenarios(LocalDate currentBatchDate) {
        List<BackdatedScenario> backdatedScenarios = new ArrayList<>();
        Random random = new Random(currentBatchDate.toEpochDay());

        // Generate 3-8 backdated scenarios
        int count = 3 + random.nextInt(6);

        String[] assetClasses = {"Equity", "Fixed Income", "Derivatives"};
        String[] products = {"US Large Cap", "Corporate Bonds", "Interest Rate"};
        String[] entities = {"Entity A", "Entity B", "Entity C"};
        String[] scenarios = {"Base", "Stress", "Adverse"};

        for (int i = 0; i < count; i++) {
            // Create scenarios 2-10 days old that were "loaded" 1-3 days ago
            LocalDate batchDate = currentBatchDate.minusDays(2 + random.nextInt(9));
            LocalDate loadedDate = currentBatchDate.minusDays(1 + random.nextInt(3));

            backdatedScenarios.add(new BackdatedScenario(
                assetClasses[random.nextInt(assetClasses.length)],
                products[random.nextInt(products.length)],
                scenarios[random.nextInt(scenarios.length)],
                entities[random.nextInt(entities.length)],
                batchDate,
                loadedDate
            ));
        }

        return backdatedScenarios.stream()
                .sorted((a, b) -> b.getLoadedDate().compareTo(a.getLoadedDate()))
                .collect(Collectors.toList());
    }

    public String generateBatchReportHtml(LocalDate batchDate, List<BatchRecord> batchRecords, List<BatchStatusCount> statusCounts) {
        // Add simulated data if needed
        if (batchRecords.isEmpty()) {
            batchRecords = generateSimulatedBatchRecords(batchDate);
        }
        if (statusCounts.isEmpty()) {
            statusCounts = generateSimulatedStatusCounts(batchDate);
        }

        List<BatchSummary> summaryData = generateSummaryDataWithExpectations(batchRecords);
        List<ScenarioDetail> scenarioDetails = generateScenarioDetails(batchRecords);
        List<BackdatedScenario> backdatedScenarios = findRecentlyLoadedBackdatedScenarios(batchDate);
        return buildBatchReportEmail(batchDate, summaryData, scenarioDetails, statusCounts, backdatedScenarios);
    }

    private String buildBatchReportEmail(LocalDate batchDate, List<BatchSummary> summaryData,
                                         List<ScenarioDetail> scenarioDetails, List<BatchStatusCount> statusCounts,
                                         List<BackdatedScenario> backdatedScenarios) {

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
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <style>\n")
                .append(StyleConfig.getStyles())
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <div class=\"container\">\n")
                .append("        <div class=\"header\">\n")
                .append("            <h1>📊 Surveillance Data Load Report</h1>\n")
                .append("            <div class=\"batch-date\">").append(formattedDate).append("</div>\n")
                .append("        </div>\n")
                .append("        \n")
                .append(buildNavigationSection())
                .append("        \n")
                .append(buildOverviewStatsSection(totalLoaded, totalExpected, completeSummaries, loadedScenarios, missingScenarios, uniqueAssetClasses, uniqueProducts, uniqueEntities))
                .append("        \n")
                .append(buildSummarySection(summaryData))
                .append("        \n")
                .append(buildChartSection())
                .append("        \n")
                .append(buildDetailSection(scenarioDetails))
                .append("        \n")
                .append(buildBackdatedScenariosSection(backdatedScenarios))
                .append("        \n")
                .append("        <div class=\"footer\">\n")
                .append("            <p>Report generated on ").append(timestamp).append(" | Trade Surveillance</p>\n")
                .append("            <p>For questions or issues, please contact the Trade Surveillance dev team via the Teams channel.</p>\n")
                .append("        </div>\n")
                .append("    </div>\n")

                // Add JavaScript at the bottom for better compatibility
                .append("    <script>\n")
                .append("        function toggleSection(contentId) {\n")
                .append("            var content = document.getElementById(contentId);\n")
                .append("            var header = content.previousElementSibling;\n")
                .append("            var indicator = header.querySelector('.toggle-indicator');\n")
                .append("            \n")
                .append("            if (content.classList.contains('collapsed')) {\n")
                .append("                content.classList.remove('collapsed');\n")
                .append("                content.style.maxHeight = 'none';\n")
                .append("                if (indicator) indicator.innerHTML = '▼';\n")
                .append("            } else {\n")
                .append("                content.classList.add('collapsed');\n")
                .append("                content.style.maxHeight = '0';\n")
                .append("                if (indicator) indicator.innerHTML = '▶';\n")
                .append("            }\n")
                .append("        }\n")
                .append("    </script>\n")
                .append("</body>\n")
                .append("</html>");

        return html.toString();
    }

    private String buildNavigationSection() {
        return "<div class=\"navigation\">\n" +
                "    <h3>📍 Quick Navigation</h3>\n" +
                "    <div class=\"nav-links\">\n" +
                "        <a href=\"#overview\" class=\"nav-link\">📊 Overview</a>\n" +
                "        <a href=\"#summary\" class=\"nav-link\">📋 Load Summary</a>\n" +
                "        <a href=\"#chart\" class=\"nav-link\">📈 Status Trend</a>\n" +
                "        <a href=\"#details\" class=\"nav-link\">📄 Scenario Details</a>\n" +
                "        <a href=\"#backdated\" class=\"nav-link\">🔄 Recent Backdated</a>\n" +
                "    </div>\n" +
                "    <div class=\"collapsible-note\">\n" +
                "        <small>💡 Tip: Click section headers to expand/collapse content</small>\n" +
                "    </div>\n" +
                "</div>\n";
    }

    private String buildOverviewStatsSection(long totalLoaded, long totalExpected, long completeSummaries,
                                             long loadedScenarios, long missingScenarios,
                                             int uniqueAssetClasses, int uniqueProducts, int uniqueEntities) {
        double completionRate = totalExpected > 0 ? (double) totalLoaded / totalExpected * 100 : 0;

        return "<div id=\"overview\" class=\"stats-overview\">\n" +
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
            return "<div id=\"summary\" class=\"section collapsible\">\n" +
                    "    <h2 class=\"collapsible-header\" onclick=\"toggleSection('summary-content')\">📋 Load Summary <span class=\"toggle-indicator\">▼</span></h2>\n" +
                    "    <div id=\"summary-content\" class=\"collapsible-content\">\n" +
                    "        <div class=\"empty-state\">No data loaded for this batch date</div>\n" +
                    "    </div>\n" +
                    "</div>\n";
        }

        StringBuilder section = new StringBuilder();
        section.append("<div id=\"summary\" class=\"section collapsible\">\n")
                .append("    <h2 class=\"collapsible-header\" onclick=\"toggleSection('summary-content')\">📋 Load Summary <span class=\"toggle-indicator\">▼</span></h2>\n")
                .append("    <div id=\"summary-content\" class=\"collapsible-content\">\n")
                .append("        <div class=\"table-container\">\n")
                .append("            <table>\n")
                .append("                <thead>\n")
                .append("                    <tr>\n")
                .append("                        <th>Asset Class</th>\n")
                .append("                        <th>Product</th>\n")
                .append("                        <th>Entity</th>\n")
                .append("                        <th>Loaded</th>\n")
                .append("                        <th>Expected</th>\n")
                .append("                        <th>Completion</th>\n")
                .append("                        <th>Status</th>\n")
                .append("                    </tr>\n")
                .append("                </thead>\n")
                .append("                <tbody>\n");

        for (BatchSummary summary : summaryData) {
            String statusIcon = getStatusIcon(summary.getStatus());
            String statusClass = "status-" + summary.getStatus().getCssClass();

            section.append("                    <tr>\n")
                    .append("                        <td>").append(escapeHtml(summary.getAssetClass())).append("</td>\n")
                    .append("                        <td>").append(escapeHtml(summary.getProduct())).append("</td>\n")
                    .append("                        <td>").append(escapeHtml(summary.getEntity())).append("</td>\n")
                    .append("                        <td class=\"number-cell\">").append(String.format("%,d", summary.getLoadCount())).append("</td>\n")
                    .append("                        <td class=\"number-cell\">").append(String.format("%,d", summary.getExpectedCount())).append("</td>\n")
                    .append("                        <td class=\"number-cell\">").append(summary.getCompletionPercentage()).append("</td>\n")
                    .append("                        <td class=\"").append(statusClass).append("\">")
                    .append(statusIcon).append(" ").append(summary.getStatus().getDisplayName()).append("</td>\n")
                    .append("                    </tr>\n");
        }

        section.append("                </tbody>\n")
                .append("            </table>\n")
                .append("        </div>\n")
                .append("    </div>\n")
                .append("</div>\n");

        return section.toString();
    }

    private String buildDetailSection(List<ScenarioDetail> scenarioDetails) {
        if (scenarioDetails.isEmpty()) {
            return "<div id=\"details\" class=\"section collapsible\">\n" +
                    "    <h2 class=\"collapsible-header\" onclick=\"toggleSection('details-content')\">📄 Scenario Details <span class=\"toggle-indicator\">▶</span></h2>\n" +
                    "    <div id=\"details-content\" class=\"collapsible-content collapsed\">\n" +
                    "        <div class=\"empty-state\">No scenario details available</div>\n" +
                    "    </div>\n" +
                    "</div>\n";
        }

        StringBuilder section = new StringBuilder();
        section.append("<div id=\"details\" class=\"section collapsible\">\n")
                .append("    <h2 class=\"collapsible-header\" onclick=\"toggleSection('details-content')\">📄 Scenario Details <span class=\"toggle-indicator\">▶</span></h2>\n")
                .append("    <div id=\"details-content\" class=\"collapsible-content collapsed\" style=\"max-height: 0;\">\n") // Start collapsed for long tables
                .append("        <div class=\"table-container\">\n")
                .append("            <table>\n")
                .append("                <thead>\n")
                .append("                    <tr>\n")
                .append("                        <th>Asset Class</th>\n")
                .append("                        <th>Product</th>\n")
                .append("                        <th>Entity</th>\n")
                .append("                        <th>Scenario</th>\n")
                .append("                        <th>Status</th>\n")
                .append("                    </tr>\n")
                .append("                </thead>\n")
                .append("                <tbody>\n");

        for (ScenarioDetail detail : scenarioDetails) {
            String statusIcon = getScenarioStatusIcon(detail.getStatus());
            String statusClass = "status-" + detail.getStatus().getCssClass();

            section.append("                    <tr>\n")
                    .append("                        <td>").append(escapeHtml(detail.getAssetClass())).append("</td>\n")
                    .append("                        <td>").append(escapeHtml(detail.getProduct())).append("</td>\n")
                    .append("                        <td>").append(escapeHtml(detail.getEntity())).append("</td>\n")
                    .append("                        <td>").append(escapeHtml(detail.getScenario())).append("</td>\n")
                    .append("                        <td class=\"").append(statusClass).append("\">")
                    .append(statusIcon).append(" ").append(detail.getStatus().getDisplayName()).append("</td>\n")
                    .append("                    </tr>\n");
        }

        section.append("                </tbody>\n")
                .append("            </table>\n")
                .append("        </div>\n")
                .append("    </div>\n")
                .append("</div>\n");

        return section.toString();
    }

    private String buildBackdatedScenariosSection(List<BackdatedScenario> backdatedScenarios) {
        StringBuilder section = new StringBuilder();
        section.append("<div id=\"backdated\" class=\"section collapsible\">\n")
                .append("    <h2 class=\"collapsible-header\" onclick=\"toggleSection('backdated-content')\">🔄 Recently Loaded Backdated Scenarios <span class=\"toggle-indicator\">▼</span></h2>\n")
                .append("    <div id=\"backdated-content\" class=\"collapsible-content\">\n");

        if (backdatedScenarios.isEmpty()) {
            section.append("        <div class=\"empty-state\">No backdated scenarios loaded in the last 7 days</div>\n");
        } else {
            section.append("        <div class=\"info-box\">\n")
                    .append("            <p><strong>Note:</strong> These scenarios have batch dates older than today but were loaded recently. ")
                    .append("This typically indicates catch-up processing or delayed data delivery.</p>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"table-container\">\n")
                    .append("            <table>\n")
                    .append("                <thead>\n")
                    .append("                    <tr>\n")
                    .append("                        <th>Asset Class</th>\n")
                    .append("                        <th>Product</th>\n")
                    .append("                        <th>Entity</th>\n")
                    .append("                        <th>Scenario</th>\n")
                    .append("                        <th>Batch Date</th>\n")
                    .append("                        <th>Loaded Date</th>\n")
                    .append("                        <th>Days Late</th>\n")
                    .append("                    </tr>\n")
                    .append("                </thead>\n")
                    .append("                <tbody>\n");

            for (BackdatedScenario backdated : backdatedScenarios) {
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(backdated.getBatchDate(), backdated.getLoadedDate());
                String lateness = daysLate > 7 ? "status-danger" : (daysLate > 3 ? "status-warning" : "status-info");

                section.append("                    <tr>\n")
                        .append("                        <td>").append(escapeHtml(backdated.getAssetClass())).append("</td>\n")
                        .append("                        <td>").append(escapeHtml(backdated.getProduct())).append("</td>\n")
                        .append("                        <td>").append(escapeHtml(backdated.getEntity())).append("</td>\n")
                        .append("                        <td>").append(escapeHtml(backdated.getScenario())).append("</td>\n")
                        .append("                        <td>").append(backdated.getBatchDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("</td>\n")
                        .append("                        <td>").append(backdated.getLoadedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("</td>\n")
                        .append("                        <td class=\"number-cell ").append(lateness).append("\">").append(daysLate).append("</td>\n")
                        .append("                    </tr>\n");
            }

            section.append("                </tbody>\n")
                    .append("            </table>\n")
                    .append("        </div>\n");
        }

        section.append("    </div>\n")
                .append("</div>\n");

        return section.toString();
    }

    private String buildChartSection() {
        return "<div id=\"chart\" class=\"chart-section collapsible\">\n" +
                "    <h2 class=\"collapsible-header\" onclick=\"toggleSection('chart-content')\">📈 120-Day Load Status Trend <span class=\"toggle-indicator\">▼</span></h2>\n" +
                "    <div id=\"chart-content\" class=\"collapsible-content\">\n" +
                "        <img src=\"cid:statusChart\" alt=\"Batch Status Chart\"/>\n" +
                "        <p style=\"font-size: 12px; color: #666; margin-top: 15px;\">\n" +
                "            Green: Successfully loaded batches | Red: Missing/failed batches\n" +
                "        </p>\n" +
                "    </div>\n" +
                "</div>\n";
    }

    private String getStatusIcon(BatchSummary.CompletionStatus status) {
        switch (status) {
            case COMPLETE: return "✅";
            case INCOMPLETE: return "❌";
            case EXCESS: return "⚠️";
            default: return "❓";
        }
    }

    private String getScenarioStatusIcon(ScenarioDetail.ScenarioStatus status) {
        switch (status) {
            case LOADED: return "✅";
            case MISSING: return "❌";
            case UNEXPECTED: return "⚠️";
            default: return "➖";
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

        if (chartFile != null) {
            helper.addInline("statusChart", chartFile);
        }

        mailSender.send(message);
    }

    // Keep all other existing private methods...
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
}