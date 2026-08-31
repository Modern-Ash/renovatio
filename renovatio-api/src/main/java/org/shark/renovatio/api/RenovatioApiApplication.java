package org.shark.renovatio.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
    "org.shark.renovatio.core",
    "org.shark.renovatio.shared",
    "org.shark.renovatio.provider.java",
    "org.shark.renovatio.provider.cobol",
    "org.shark.renovatio.api"
})
@EnableAsync
public class RenovatioApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RenovatioApiApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public OpenRewriteRunner openRewriteRunner() {
        return new OpenRewriteRunner();
    }
}
