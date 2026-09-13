package org.modernash.renovatio.api.config;

import org.modernash.renovatio.decisions.DecisionPolicyRepository;
import org.modernash.renovatio.decisions.FileDecisionPolicyRepository;
import org.modernash.renovatio.profile.FileProfileTemplateRepository;
import org.modernash.renovatio.profile.ProfileTemplateRepository;
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
