package com.wtoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.wtoffice.ai.AiSettings;

@SpringBootApplication
@EnableConfigurationProperties(AiSettings.class)
public class WtOfficeApplication {
    public static void main(String[] args) {
        SpringApplication.run(WtOfficeApplication.class, args);
    }
}
