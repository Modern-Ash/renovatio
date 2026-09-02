package org.shark.renovatio.emitter.node.config;

import org.shark.renovatio.emitter.node.DefaultNodeRenderer;
import org.shark.renovatio.emitter.node.NodeEmitter;
import org.shark.renovatio.emitter.node.prisma.PrismaStrategy;
import org.shark.renovatio.persistence.strategy.PersistenceStrategy;
import org.shark.renovatio.shared.spi.TargetEmitter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class NodeEmitterAutoConfiguration {
    @Bean
    public TargetEmitter nodeEmitter() {
        return new NodeEmitter(new DefaultNodeRenderer());
    }

    @Bean
    public PersistenceStrategy prismaStrategy() {
        return new PrismaStrategy();
    }
}
