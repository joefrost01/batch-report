package com.demo.batchreport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "email")
public class Config {
    String fromAddress;
    String fromName;
    List<String> recipients;
}
