package org.shark.renovatio.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
    "org.shark.renovatio.core",
    "org.shark.renovatio.shared",
    "org.shark.renovatio.provider.cobol",
    "org.shark.renovatio.api"
})
@EnableAsync
public class RenovatioApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RenovatioApiApplication.class, args);
    }
}
