package com.alwaysmoveforward.configurationmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ConfigurationManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationManagerApplication.class, args);
    }
}

