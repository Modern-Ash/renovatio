package org.shark.renovatio.emitter.node.prisma;

import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.persistence.strategy.PersistenceArtifacts;
import org.shark.renovatio.persistence.strategy.PersistenceStrategy;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.Objects;

public final class PrismaStrategy implements PersistenceStrategy {
    @Override
    public boolean supports(DataAccessClassification classification, MigrationProfile.Language target) {
        return target == MigrationProfile.Language.NODE
                && classification.kind() != DataAccessKind.RESIDUAL;
    }

    @Override
    public PersistenceArtifacts emit(DataAccessClassification classification,
                                     MigrationProfiles.EffectiveProfile profile) {
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(profile, "profile");
        String entityName = classifyName(classification.id());

        String entitySource = """
                // Auto-generated Prisma schema for %s
                // DataAccessKind: %s

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
                """.formatted(entityName, classification.kind(), entityName);

        String repositorySource = """
                import { PrismaClient } from '@prisma/client';

                const prisma = new PrismaClient();

                export class %sRepository {
                  async findAll() {
                    return prisma.%s.findMany();
                  }

                  async findById(id: number) {
                    return prisma.%s.findUnique({ where: { id } });
                  }

                  async create(data: { data: string }) {
                    return prisma.%s.create({ data });
                  }
                }
                """.formatted(entityName, entityName.toLowerCase(), entityName.toLowerCase(), entityName.toLowerCase());

        return new PersistenceArtifacts(
                entityName,
                entitySource,
                entityName + "Repository",
                repositorySource,
                "",
                java.util.List.of()
        );
    }

    private String classifyName(String classificationId) {
        String name = classificationId.replaceAll("[^a-zA-Z0-9]", "");
        if (name.isEmpty() || !Character.isUpperCase(name.charAt(0))) {
            name = "Entity" + name;
        }
        return name;
    }
}
