package com.demo.batchreport.config;

public class StyleConfig {

    public static String getStyles() {
        return "        body {\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333;\n" +
                "            max-width: 1200px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "            background-color: #f8f9fa;\n" +
                "        }\n" +
                "        .container {\n" +
                "            background-color: white;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        .header {\n" +
                "            background: linear-gradient(135deg, #006A4E 0%, #00A693 100%);\n" +
                "            color: white;\n" +
                "            padding: 30px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .header h1 {\n" +
                "            margin: 0;\n" +
                "            font-size: 28px;\n" +
                "            font-weight: 300;\n" +
                "        }\n" +
                "        .header .batch-date {\n" +
                "            font-size: 18px;\n" +
                "            opacity: 0.9;\n" +
                "            margin-top: 10px;\n" +
                "        }\n" +
                "        .navigation {\n" +
                "            background-color: #f0f9f6;\n" +
                "            padding: 20px 30px;\n" +
                "            border-bottom: 1px solid #00A693;\n" +
                "        }\n" +
                "        .navigation h3 {\n" +
                "            margin: 0 0 15px 0;\n" +
                "            color: #006A4E;\n" +
                "            font-size: 16px;\n" +
                "        }\n" +
                "        .nav-links {\n" +
                "            display: flex;\n" +
                "            flex-wrap: wrap;\n" +
                "            gap: 15px;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .nav-link {\n" +
                "            color: #006A4E;\n" +
                "            text-decoration: none;\n" +
                "            padding: 8px 16px;\n" +
                "            border: 1px solid #00A693;\n" +
                "            border-radius: 20px;\n" +
                "            background-color: white;\n" +
                "            font-size: 13px;\n" +
                "            font-weight: 500;\n" +
                "            transition: all 0.2s ease;\n" +
                "        }\n" +
                "        .nav-link:hover {\n" +
                "            background-color: #00A693;\n" +
                "            color: white;\n" +
                "        }\n" +
                "        .collapsible-note {\n" +
                "            font-style: italic;\n" +
                "            color: #666;\n" +
                "        }\n" +
                "        .stats-overview {\n" +
                "            display: flex;\n" +
                "            justify-content: space-around;\n" +
                "            padding: 20px;\n" +
                "            background-color: #e8f5f1;\n" +
                "            border-bottom: 1px solid #b3d9cc;\n" +
                "        }\n" +
                "        .stat-item {\n" +
                "            text-align: center;\n" +
                "            padding: 10px;\n" +
                "        }\n" +
                "        .stat-number {\n" +
                "            font-size: 24px;\n" +
                "            font-weight: bold;\n" +
                "            color: #006A4E;\n" +
                "            display: block;\n" +
                "        }\n" +
                "        .stat-label {\n" +
                "            font-size: 12px;\n" +
                "            color: #666;\n" +
                "            text-transform: uppercase;\n" +
                "            letter-spacing: 0.5px;\n" +
                "        }\n" +
                "        .section {\n" +
                "            border-bottom: 1px solid #e0e0e0;\n" +
                "        }\n" +
                "        .collapsible-header {\n" +
                "            color: #006A4E;\n" +
                "            margin: 0;\n" +
                "            padding: 20px 30px;\n" +
                "            font-size: 20px;\n" +
                "            border-bottom: 2px solid #00A693;\n" +
                "            cursor: pointer;\n" +
                "            user-select: none;\n" +
                "            background-color: #f8f9fa;\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "        }\n" +
                "        .collapsible-header:hover {\n" +
                "            background-color: #f0f9f6;\n" +
                "        }\n" +
                "        .toggle-indicator {\n" +
                "            font-size: 14px;\n" +
                "            transition: transform 0.2s ease;\n" +
                "        }\n" +
                "        .collapsible-content {\n" +
                "            padding: 30px;\n" +
                "            overflow: hidden;\n" +
                "            transition: max-height 0.3s ease;\n" +
                "        }\n" +
                "        .collapsible-content.collapsed {\n" +
                "            max-height: 0 !important;\n" +
                "            padding: 0 30px;\n" +
                "        }\n" +
                "        .table-container {\n" +
                "            overflow-x: auto;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        table {\n" +
                "            width: 100%;\n" +
                "            border-collapse: collapse;\n" +
                "            background-color: white;\n" +
                "            box-shadow: 0 1px 3px rgba(0,0,0,0.12);\n" +
                "            border-radius: 6px;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        th {\n" +
                "            background-color: #006A4E;\n" +
                "            color: white;\n" +
                "            padding: 15px 12px;\n" +
                "            text-align: left;\n" +
                "            font-weight: 600;\n" +
                "            font-size: 14px;\n" +
                "            text-transform: uppercase;\n" +
                "            letter-spacing: 0.5px;\n" +
                "        }\n" +
                "        td {\n" +
                "            padding: 12px;\n" +
                "            border-bottom: 1px solid #e0e0e0;\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        tr:nth-child(even) {\n" +
                "            background-color: #f8f9fa;\n" +
                "        }\n" +
                "        tr:hover {\n" +
                "            background-color: #e8f5f1;\n" +
                "        }\n" +
                "        .number-cell {\n" +
                "            text-align: right;\n" +
                "            font-weight: 600;\n" +
                "            color: #006A4E;\n" +
                "        }\n" +
                "        .chart-section {\n" +
                "            text-align: center;\n" +
                "            background-color: #fafafa;\n" +
                "        }\n" +
                "        .chart-section h2 {\n" +
                "            color: #333;\n" +
                "        }\n" +
                "        .chart-section img {\n" +
                "            max-width: 100%;\n" +
                "            height: auto;\n" +
                "            border-radius: 6px;\n" +
                "            box-shadow: 0 2px 8px rgba(0,0,0,0.15);\n" +
                "        }\n" +
                "        .empty-state {\n" +
                "            text-align: center;\n" +
                "            padding: 40px;\n" +
                "            color: #666;\n" +
                "            font-style: italic;\n" +
                "        }\n" +
                "        .info-box {\n" +
                "            background-color: #e8f5f1;\n" +
                "            border-left: 4px solid #00A693;\n" +
                "            padding: 15px 20px;\n" +
                "            margin: 20px 0;\n" +
                "            border-radius: 0 6px 6px 0;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            background-color: #f5f5f5;\n" +
                "            padding: 20px 30px;\n" +
                "            text-align: center;\n" +
                "            font-size: 12px;\n" +
                "            color: #666;\n" +
                "            border-top: 1px solid #e0e0e0;\n" +
                "        }\n" +
                "        .status-success {\n" +
                "            color: #4caf50;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .status-warning {\n" +
                "            color: #ff9800;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .status-danger {\n" +
                "            color: #f44336;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .status-info {\n" +
                "            color: #00A693;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        .status-neutral {\n" +
                "            color: #666;\n" +
                "            font-weight: bold;\n" +
                "        }\n" +
                "        @media (max-width: 768px) {\n" +
                "            .stats-overview {\n" +
                "                flex-direction: column;\n" +
                "            }\n" +
                "            .stat-item {\n" +
                "                margin: 5px 0;\n" +
                "            }\n" +
                "            .nav-links {\n" +
                "                flex-direction: column;\n" +
                "            }\n" +
                "            th, td {\n" +
                "                padding: 8px 6px;\n" +
                "                font-size: 12px;\n" +
                "            }\n" +
                "        }";
    }
}