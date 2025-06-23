# Batch Report Service

A Spring Boot service for generating professional surveillance data load reports with email delivery and web preview capabilities.

## Features

- **Email Reports**: Styled HTML emails with embedded charts and data tables
- **Web Preview**: Real-time HTML preview for rapid development without sending emails
- **Chart Generation**: 120-day load status trends using JFreeChart
- **Responsive Design**: Professional styling that works across email clients including Outlook
- **Data Visualization**: Summary statistics, detailed tables, and trend analysis
- **Template Support**: Configurable email templates and styling

## Screenshots

![Batch Report Preview](docs/batch-report-preview.png)

## Quick Start

### Prerequisites

- Java 8+
- Maven 3.6+
- SMTP server access (for email sending)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/your-username/batch-report-service.git
cd batch-report-service
```

2. Configure application settings:
```yaml
# application.yml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

email:
  recipients:
    - manager@company.com
    - ops-team@company.com
  from-address: surveillance@company.com
  from-name: "Trade Surveillance"
```

3. Set environment variables:
```bash
export EMAIL_USERNAME=your-smtp-username
export EMAIL_PASSWORD=your-smtp-password
```

4. Run the application:
```bash
mvn spring-boot:run
```

## Usage

### Web Preview (Development)

Preview reports without sending emails:
```
GET http://localhost:8080/preview-batch-report?batchDate=2024-12-15
```

### Send Email Report

Send actual email report:
```
POST http://localhost:8080/send-batch-report?batchDate=2024-12-15
```

### API Examples

```bash
# Preview report
curl "http://localhost:8080/preview-batch-report?batchDate=2024-12-15"

# Send email report
curl -X POST "http://localhost:8080/send-batch-report?batchDate=2024-12-15"
```

## Architecture

### Key Components

- **BatchReportService**: Core service handling report generation and email sending
- **BatchQueryRepository**: Data access layer with JPA repository methods
- **BatchReportController**: REST endpoints for web preview and email sending
- **Config**: Email configuration using Spring Boot `@ConfigurationProperties`

### Data Models

```java
// Core batch record
@Data
public class BatchRecord {
    private String assetClass;
    private String product;  
    private String scenario;
    private String entity;
    private LocalDate batchDate;
}

// Summary statistics
@Data  
public class BatchSummary {
    private String assetClass;
    private String product;
    private String entity;
    private Long loadCount;
}

// Status trend data
@Data
public class BatchStatusCount {
    private LocalDate date;
    private Long loadedCount; 
    private Long missingCount;
}
```

### Database Schema

```sql
CREATE TABLE batch_record (
    id BIGINT PRIMARY KEY,
    asset_class VARCHAR(255),
    batch_date DATE,
    entity VARCHAR(255), 
    product VARCHAR(255),
    scenario VARCHAR(255)
);
```

## Configuration

### Email Settings

Configure SMTP settings in `application.yml`:

```yaml
spring:
  mail:
    host: your-smtp-server.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

email:
  recipients:
    - recipient1@company.com
    - recipient2@company.com  
  from-address: reports@company.com
  from-name: "Surveillance Reports"
```

### Database Configuration

H2 in-memory database (default):
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: password
  h2:
    console:
      enabled: true
      path: /h2-console
```

Production database:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/surveillance
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
```

## Development

### Running Tests

```bash
mvn test
```

### Key Test Classes

- `BatchReportServiceTest`: Tests HTML generation with mock data
- Repository tests for data access layer
- Integration tests for email functionality

### Adding New Features

1. **Repository Layer**: Add queries to `BatchQueryRepository`
2. **Service Layer**: Extend `BatchReportService` with new report logic
3. **Controller Layer**: Add endpoints to `BatchReportController`
4. **Templates**: Modify HTML generation methods for styling changes

### Preview Development Workflow

1. Make changes to HTML generation code
2. Visit preview URL: `http://localhost:8080/preview-batch-report?batchDate=2024-12-15`
3. Refresh browser to see changes instantly
4. No email sending required during development

## Dependencies

### Core Dependencies

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Email Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- JPA/Database -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- H2 Database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Chart Generation -->
<dependency>
    <groupId>org.jfree</groupId>
    <artifactId>jfreechart</artifactId>
    <version>1.5.3</version>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

## Email Client Compatibility

The generated HTML emails are tested and work with:

- ✅ Outlook 2016/2019/365
- ✅ Gmail web/mobile
- ✅ Apple Mail
- ✅ Thunderbird
- ✅ Mobile email clients (iOS/Android)

### Design Features

- **Responsive**: Adapts to mobile screens
- **Professional Styling**: Corporate-grade appearance
- **Chart Integration**: Embedded PNG charts for broad compatibility
- **Data Tables**: Clean, sortable presentation
- **Accessibility**: Proper contrast and semantic markup

## Roadmap

### Planned Features

- [ ] **Excel Export**: Generate detailed spreadsheet attachments
- [ ] **Alert Thresholds**: Configurable warnings for missing batches
- [ ] **Filtering**: Asset class and entity-specific reports
- [ ] **Scheduling**: Automated daily/weekly report generation
- [ ] **Dashboard**: Web UI for historical report viewing
- [ ] **API Documentation**: OpenAPI/Swagger integration
- [ ] **Metrics**: Prometheus/Micrometer monitoring
- [ ] **Multi-tenancy**: Support for multiple surveillance teams

### Potential Enhancements

- Real-time batch monitoring dashboard
- Slack/Teams integration for alerts
- Custom chart types (line charts, pie charts)
- Report templates for different asset classes
- Historical trend analysis and predictions
- Integration with surveillance workflow systems

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Make changes and add tests
4. Commit changes: `git commit -am 'Add new feature'`
5. Push to branch: `git push origin feature/new-feature`
6. Submit a pull request

### Coding Standards

- Follow existing code style and naming conventions
- Add unit tests for new functionality
- Update documentation for API changes
- Use meaningful commit messages
- Ensure Java 8+ compatibility

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


## Acknowledgments

- Built with Spring Boot for enterprise reliability
- JFreeChart for professional chart generation
