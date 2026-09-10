package org.shark.renovatio.provider.cobol.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of MavenBuildService using Maven CLI.
 */
public class MavenBuildServiceImpl implements MavenBuildService {
    
    private static final Logger log = LoggerFactory.getLogger(MavenBuildServiceImpl.class);
    private static final long BUILD_TIMEOUT_MINUTES = 5;
    
    @Override
    public BuildResult compile(Path sourceDir, String classpath) {
        log.info("Compiling Java sources in: {}", sourceDir);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Create a temporary pom.xml for compilation
            Path pomFile = createTemporaryPom(sourceDir, classpath);
            
            // Execute Maven compile
            ProcessBuilder processBuilder = new ProcessBuilder(
                "mvn", "compile", "-q", "-f", pomFile.toString()
            );
            processBuilder.directory(sourceDir.toFile());
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            boolean finished = process.waitFor(BUILD_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            long compilationTime = System.currentTimeMillis() - startTime;
            
            if (!finished) {
                process.destroyForcibly();
                return BuildResult.failure(-1, "Build timed out", "Build exceeded timeout");
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Compilation successful in {}ms", compilationTime);
                return BuildResult.success(output.toString(), compilationTime);
            } else {
                log.error("Compilation failed with exit code: {}", exitCode);
                return BuildResult.failure(exitCode, output.toString(), errors.toString());
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("Error during compilation", e);
            return BuildResult.failure(-1, null, e.getMessage());
        }
    }
    
    @Override
    public BuildResult test(Path testDir, String classpath) {
        log.info("Running tests in: {}", testDir);
        
        try {
            long startTime = System.currentTimeMillis();
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                "mvn", "test", "-q"
            );
            processBuilder.directory(testDir.toFile());
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            boolean finished = process.waitFor(BUILD_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            long testTime = System.currentTimeMillis() - startTime;
            
            if (!finished) {
                process.destroyForcibly();
                return BuildResult.failure(-1, "Tests timed out", "Tests exceeded timeout");
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Tests passed in {}ms", testTime);
                return BuildResult.success(output.toString(), testTime);
            } else {
                log.error("Tests failed with exit code: {}", exitCode);
                return BuildResult.failure(exitCode, output.toString(), null);
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("Error during test execution", e);
            return BuildResult.failure(-1, null, e.getMessage());
        }
    }
    
    private Path createTemporaryPom(Path sourceDir, String classpath) throws IOException {
        // Create directory if it doesn't exist
        if (!Files.exists(sourceDir)) {
            Files.createDirectories(sourceDir);
        }
        
        Path pomFile = sourceDir.resolve("pom.xml");
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
            
                <groupId>org.shark.renovatio.generated</groupId>
                <artifactId>cobol-generated</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>
            
                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
            
                <dependencies>
                    <dependency>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.30</version>
                        <scope>provided</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                        <version>3.2.5</version>
                    </dependency>
                    <dependency>
                        <groupId>jakarta.persistence</groupId>
                        <artifactId>jakarta.persistence-api</artifactId>
                        <version>3.1.0</version>
                    </dependency>
                </dependencies>
            
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.11.0</version>
                            <configuration>
                                <source>17</source>
                                <target>17</target>
                                <annotationProcessorPaths>
                                    <path>
                                        <groupId>org.projectlombok</groupId>
                                        <artifactId>lombok</artifactId>
                                        <version>1.18.30</version>
                                    </path>
                                </annotationProcessorPaths>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;
        
        Files.writeString(pomFile, pomContent);
        return pomFile;
    }
}
