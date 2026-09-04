package org.shark.renovatio.api.config;

import org.shark.renovatio.decisions.DecisionPolicyRepository;
import org.shark.renovatio.decisions.FileDecisionPolicyRepository;
import org.shark.renovatio.profile.FileProfileTemplateRepository;
import org.shark.renovatio.profile.ProfileTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class ReusableAssetsConfig {
    @Bean
    ProfileTemplateRepository profileTemplateRepository(
            @Value("${renovatio.reusable-assets.root:${user.home}/.renovatio}") String root) {
        return new FileProfileTemplateRepository(Path.of(root).resolve("profiles"));
    }

    @Bean
    DecisionPolicyRepository decisionPolicyRepository(
            @Value("${renovatio.reusable-assets.root:${user.home}/.renovatio}") String root) {
        return new FileDecisionPolicyRepository(Path.of(root).resolve("policies"));
    }
}
