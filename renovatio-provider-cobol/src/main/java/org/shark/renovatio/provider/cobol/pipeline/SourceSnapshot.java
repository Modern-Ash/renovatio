package org.shark.renovatio.provider.cobol.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Stable digest of all migration inputs in a reference fixture. */
final class SourceSnapshot {
    private SourceSnapshot() {
    }

    static String capture(Path fixtureDir) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<Path> inputs;
            try (var paths = Files.walk(fixtureDir)) {
                inputs = paths.filter(Files::isRegularFile)
                    .filter(SourceSnapshot::isInput)
                    .sorted((left, right) -> relative(fixtureDir, left)
                        .compareTo(relative(fixtureDir, right)))
                    .toList();
            }
            for (Path input : inputs) {
                digest.update(relative(fixtureDir, input).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Files.readAllBytes(input));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to snapshot fixture " + fixtureDir, exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean isInput(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".cbl") || name.endsWith(".cob") || name.endsWith(".cobol")
            || name.endsWith(".cpy") || name.equals("decisions.json");
    }

    private static String relative(Path root, Path path) {
        return root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize())
            .toString().replace('\\', '/');
    }
}
