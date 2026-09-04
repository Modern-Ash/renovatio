package org.shark.renovatio.emitter.node;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.DocumentationSettings;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.emission.TranslationDocumentation;
import org.shark.renovatio.emitter.node.catalog.NodeIdiomCatalog;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DefaultNodeRenderer implements NodeArtifactRenderer {
    private final NodeIdiomCatalog idiomCatalog = new NodeIdiomCatalog();

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
        files.put("package.json", generatePackageJson());
        files.put("tsconfig.json", generateTsConfig());
        files.put("docs/node-idioms.md", generateIdiomReport());
        if (profile.persistence() != null
                && profile.persistence().defaultStrategy() == MigrationProfile.PersistenceStrategy.PRISMA) {
            files.put("prisma/schema.prisma", generatePrismaSchema(model));
            files.put("prisma/seed.ts", generatePrismaSeed());
        }
        return EmittedArtifacts.fromUtf8(files);
    }

    private String generateIdiomReport() {
        StringBuilder report = new StringBuilder("# Node idiom mapping\n\n");
        idiomCatalog.allIdioms().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> report.append("- `").append(entry.getKey()).append("`: ")
                        .append(entry.getValue()).append('\n'));
        if (!idiomCatalog.missingPatterns().isEmpty()) {
            report.append("\n## Manual action items\n\n");
            idiomCatalog.missingPatterns().stream().sorted()
                    .forEach(pattern -> report.append("- Review unsupported construct `")
                            .append(pattern).append("`\n"));
        }
        return report.toString();
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
                    import type { Request, Response } from 'express';

                    export function %sController(_request: Request, response: Response): void {
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
                import express from 'express';

                const app = express();
                app.use(express.json());

                app.get('/health', (_req, res) => {
                  res.json({ status: 'ok' });
                });

                const port = process.env.PORT || 3000;
                app.listen(port, () => {
                  console.log(`Server running on port ${port}`);
                });
                """;
    }

    private String generatePackageJson() {
        return """
                {
                  "name": "renovatio-node-app",
                  "version": "1.0.0",
                  "main": "dist/main.js",
                  "scripts": {
                    "build": "tsc",
                    "start": "node dist/main.js"
                  },
                  "dependencies": {
                    "express": "^4.18.2"
                  },
                  "devDependencies": {
                    "typescript": "^5.3.3",
                    "@types/express": "^4.17.21"
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
                    "esModuleInterop": true,
                    "skipLibCheck": true,
                    "forceConsistentCasingInFileNames": true
                  },
                  "include": ["src"]
                }
                """;
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
