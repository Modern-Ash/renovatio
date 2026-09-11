package org.shark.renovatio.emitter.node;

import org.shark.renovatio.emitter.node.catalog.NodeIdiomCatalog;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.DocumentationSettings;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.emission.TranslationDocumentation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DefaultNodeRenderer implements NodeArtifactRenderer {

    @Override
    public EmittedArtifacts render(TargetModel model, MigrationProfile profile) {
        Map<String, String> files = new LinkedHashMap<>();
        model.targetStructure().artifactPaths().stream()
                .filter(path -> path.endsWith(".ts"))
                .forEach(path -> {
                    String content = generateProgramArtifact(model, path);
                    if (DocumentationSettings.enabled(profile)) {
                        content = TranslationDocumentation.tsdoc(model) + content;
                    }
                    files.put(path, content);
                });
        files.put("src/main.ts", generateMain());
        files.put("src/health.ts", generateHealth());
        files.put("src/main.test.ts", generateSmokeTest());
        files.put("package.json", generatePackageJson());
        files.put("package-lock.json", generatePackageLock());
        files.put("tsconfig.json", generateTsConfig());
        if (profile.persistence() != null
                && profile.persistence().defaultStrategy() == MigrationProfile.PersistenceStrategy.PRISMA) {
            files.put("prisma/schema.prisma", generatePrismaSchema(model));
            files.put("prisma/seed.ts", generatePrismaSeed());
        }
        files.put("docs/node-idioms.md", generateNodeIdiomsReport());
        return EmittedArtifacts.fromUtf8(files);
    }

    private String generatePrismaSchema(TargetModel model) {
        return """
                generator client {
                  provider = "prisma-client-js"
                }

                datasource db {
                  provider = "postgresql"
                  url      = env("DATABASE_URL")
                }

                model %s {
                  id   Int    @id @default(autoincrement())
                  data String
                }
                """.formatted(typeName(model.semanticProgram().programId()));
    }

    private String generatePrismaSeed() {
        return """
                import { PrismaClient } from '@prisma/client';

                const prisma = new PrismaClient();
                await prisma.$disconnect();
                """;
    }

    private String generateProgramArtifact(TargetModel model, String path) {
        String programId = model.semanticProgram().programId();
        String typeName = typeName(programId);
        String literal = stringLiteral(programId);
        if (path.endsWith(".service.ts")) {
            return """
                    export class %sService {
                      readonly programId = '%s';

                      execute(input: unknown): unknown {
                        return input;
                      }
                    }
                    """.formatted(typeName, literal);
        }
        if (path.endsWith(".entity.ts")) {
            return """
                    export interface %sEntity {
                      readonly programId: '%s';
                    }
                    """.formatted(typeName, literal);
        }
        if (path.endsWith(".repository.ts")) {
            return """
                    export interface %sRepository {
                      findById(id: string): Promise<{ readonly programId: string } | null>;
                    }
                    """.formatted(typeName);
        }
        if (path.endsWith(".controller.ts")) {
            return """
                    export interface HttpResponse {
                      json(body: unknown): void;
                    }

                    export function %sController(response: HttpResponse): void {
                      response.json({ program: '%s' });
                    }
                    """.formatted(variableName(typeName), literal);
        }
        return """
                export const %sProgram = {
                  programId: '%s'
                } as const;
                """.formatted(variableName(typeName), literal);
    }

    private String generateMain() {
        return """
                import { createServer } from 'node:http';
                import { healthResponse } from './health';

                const server = createServer((request, response) => {
                  if (request.url === '/health') {
                    response.writeHead(200, { 'content-type': 'application/json' });
                    response.end(healthResponse());
                    return;
                  }
                  response.writeHead(404, { 'content-type': 'application/json' });
                  response.end(JSON.stringify({ error: 'not_found' }));
                });

                const port = Number(process.env.PORT || 3000);
                server.listen(port, () => {
                  console.log(`Server running on port ${port}`);
                });
                """;
    }

    private String generateHealth() {
        return """
                export function healthResponse(): string {
                  return JSON.stringify({ status: 'ok' });
                }
                """;
    }

    private String generateSmokeTest() {
        return """
                import { strict as assert } from 'node:assert';
                import test from 'node:test';
                import { healthResponse } from './health';

                test('health response is stable', () => {
                  assert.deepEqual(JSON.parse(healthResponse()), { status: 'ok' });
                });
                """;
    }

    private String generatePackageJson() {
        return """
                {
                  "name": "renovatio-node-app",
                  "version": "1.0.0",
                  "private": true,
                  "packageManager": "npm@10.8.2",
                  "main": "dist/main.js",
                  "scripts": {
                    "build": "tsc",
                    "lint": "tsc --noEmit",
                    "test": "npm run build && node --test dist/main.test.js",
                    "start": "node dist/main.js"
                  },
                  "devDependencies": {
                    "@types/node": "20.14.10",
                    "typescript": "5.5.4"
                  }
                }
                """;
    }

    private String generatePackageLock() {
        return """
                {
                  "name": "renovatio-node-app",
                  "version": "1.0.0",
                  "lockfileVersion": 3,
                  "requires": true,
                  "packages": {
                    "": {
                      "name": "renovatio-node-app",
                      "version": "1.0.0",
                      "devDependencies": {
                        "@types/node": "20.14.10",
                        "typescript": "5.5.4"
                      }
                    }
                  }
                }
                """;
    }

    private String generateTsConfig() {
        return """
                {
                  "compilerOptions": {
                    "target": "ES2022",
                    "module": "commonjs",
                    "outDir": "dist",
                    "rootDir": "src",
                    "strict": true,
                    "moduleResolution": "node",
                    "skipLibCheck": true,
                    "forceConsistentCasingInFileNames": true
                  },
                  "include": ["src"]
                }
                """;
    }

    private String generateNodeIdiomsReport() {
        NodeIdiomCatalog catalog = new NodeIdiomCatalog();
        StringBuilder sb = new StringBuilder();
        sb.append("# COBOL to TypeScript Idiom Mappings\n\n");
        sb.append("Generated by renovatio-emitter-node — deterministic shared report.\n\n");
        sb.append("| COBOL Construct | TypeScript Equivalent |\n");
        sb.append("|-----------------|----------------------|\n");
        catalog.allIdioms().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n"));
        if (!catalog.missingPatterns().isEmpty()) {
            sb.append("\n## Unmapped Patterns\n\n");
            catalog.missingPatterns().forEach(p -> sb.append("- ").append(p).append("\n"));
        }
        return sb.toString();
    }

    private String typeName(String programId) {
        StringBuilder result = new StringBuilder();
        for (String part : programId.split("[^A-Za-z0-9]+")) {
            if (part.isEmpty()) continue;
            String lower = part.toLowerCase(Locale.ROOT);
            result.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        if (result.isEmpty()) result.append("Program");
        if (Character.isDigit(result.charAt(0))) result.insert(0, "Program");
        return result.toString();
    }

    private String variableName(String typeName) {
        return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
    }

    private String stringLiteral(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}
