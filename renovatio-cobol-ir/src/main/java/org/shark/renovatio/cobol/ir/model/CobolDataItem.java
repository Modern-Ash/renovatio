package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;
import java.util.Optional;

public final class CobolDataItem {

    private final String name;
    private final String picture;
    private final int level;
    private final Integer occurs;
    private final String redefines;
    private final String javaType;

    public CobolDataItem(String name, String picture, int level, Integer occurs, String redefines, String javaType) {
        this.name = Objects.requireNonNull(name, "name");
        this.picture = picture;
        this.level = level;
        this.occurs = occurs;
        this.redefines = redefines;
        this.javaType = javaType == null ? "String" : javaType;
    }

    public String getName() {
        return name;
    }

    public String getPicture() {
        return picture;
    }

    public int getLevel() {
        return level;
    }

    public Optional<Integer> getOccurs() {
        return Optional.ofNullable(occurs);
    }

    public Optional<String> getRedefines() {
        return Optional.ofNullable(redefines);
    }

    public String getJavaType() {
        return javaType;
    }
}
