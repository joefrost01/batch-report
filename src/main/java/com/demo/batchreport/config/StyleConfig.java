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
                "            background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%);\n" +
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
                "        .stats-overview {\n" +
                "            display: flex;\n" +
                "            justify-content: space-around;\n" +
                "            padding: 20px;\n" +
                "            background-color: #e3f2fd;\n" +
                "            border-bottom: 1px solid #bbdefb;\n" +
                "        }\n" +
                "        .stat-item {\n" +
                "            text-align: center;\n" +
                "            padding: 10px;\n" +
                "        }\n" +
                "        .stat-number {\n" +
                "            font-size: 24px;\n" +
                "            font-weight: bold;\n" +
                "            color: #1976d2;\n" +
                "            display: block;\n" +
                "        }\n" +
                "        .stat-label {\n" +
                "            font-size: 12px;\n" +
                "            color: #666;\n" +
                "            text-transform: uppercase;\n" +
                "            letter-spacing: 0.5px;\n" +
                "        }\n" +
                "        .section {\n" +
                "            padding: 30px;\n" +
                "            border-bottom: 1px solid #e0e0e0;\n" +
                "        }\n" +
                "        .section h2 {\n" +
                "            color: #1976d2;\n" +
                "            margin-top: 0;\n" +
                "            margin-bottom: 20px;\n" +
                "            font-size: 20px;\n" +
                "            border-bottom: 2px solid #1976d2;\n" +
                "            padding-bottom: 10px;\n" +
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
                "            background-color: #1976d2;\n" +
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
                "            background-color: #e3f2fd;\n" +
                "        }\n" +
                "        .number-cell {\n" +
                "            text-align: right;\n" +
                "            font-weight: 600;\n" +
                "            color: #1976d2;\n" +
                "        }\n" +
                "        .chart-section {\n" +
                "            text-align: center;\n" +
                "            padding: 30px;\n" +
                "            background-color: #fafafa;\n" +
                "        }\n" +
                "        .chart-section h2 {\n" +
                "            color: #333;\n" +
                "            margin-bottom: 20px;\n" +
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
                "            color: #2196f3;\n" +
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
                "            th, td {\n" +
                "                padding: 8px 6px;\n" +
                "                font-size: 12px;\n" +
                "            }\n" +
                "        }\n";
    }

}
