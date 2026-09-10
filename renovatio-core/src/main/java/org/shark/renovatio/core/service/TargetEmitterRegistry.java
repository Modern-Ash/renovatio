package org.shark.renovatio.core.service;

import org.shark.renovatio.shared.spi.TargetEmitter;
import org.springframework.stereotype.Service;

import java.util.Collection;

/** @deprecated Use the application-neutral shared emission registry. */
@Deprecated(forRemoval = true)
@Service
public class TargetEmitterRegistry extends org.shark.renovatio.shared.emission.TargetEmitterRegistry {
    public TargetEmitterRegistry(Collection<TargetEmitter> candidates) {
        super(candidates);
    }
}
