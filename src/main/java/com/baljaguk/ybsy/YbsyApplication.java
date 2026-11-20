package com.baljaguk.ybsy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class YbsyApplication {

    public static void main(String[] args) {
        SpringApplication.run(YbsyApplication.class, args);
    }
}
