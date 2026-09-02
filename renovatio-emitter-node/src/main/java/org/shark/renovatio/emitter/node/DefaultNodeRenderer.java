package org.shark.renovatio.emitter.node;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultNodeRenderer implements NodeArtifactRenderer {
    @Override
    public EmittedArtifacts render(TargetModel model, MigrationProfile profile) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("src/main.ts", generateMain(model));
        files.put("package.json", generatePackageJson(model));
        files.put("tsconfig.json", generateTsConfig());
        return EmittedArtifacts.fromUtf8(files);
    }

    private String generateMain(TargetModel model) {
        String programId = model.semanticProgram().programId();
        return """
                import express from 'express';

                const app = express();
                app.use(express.json());

                app.get('/health', (_req, res) => {
                  res.json({ status: 'ok', program: '%s' });
                });

                const port = process.env.PORT || 3000;
                app.listen(port, () => {
                  console.log(`Server running on port ${port}`);
                });
                """.formatted(programId);
    }

    private String generatePackageJson(TargetModel model) {
        String programId = model.semanticProgram().programId();
        return """
                {
                  "name": "%s",
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
                """.formatted(programId.toLowerCase());
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
}
