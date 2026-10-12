package dev.lumi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LumiEnglishApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumiEnglishApplication.class, args);
    }
}
