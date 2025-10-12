package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;
import java.util.Optional;

public record CobolDataItem(String name, String picture, int level, Integer occurs,
                            String redefines, String javaType) {

    public CobolDataItem(String name, String picture, int level, Integer occurs, String redefines, String javaType) {
        this.name = Objects.requireNonNull(name, "name");
        this.picture = picture;
        this.level = level;
        this.occurs = occurs;
        this.redefines = redefines;
        this.javaType = javaType == null ? "String" : javaType;
    }

    // Bean-style getters for JUnit expectations
    public String getName() { return name; }
    public String getPicture() { return picture; }
    public int getLevel() { return level; }
    public String getJavaType() { return javaType; }

    public Optional<Integer> getOccurs() {
        return Optional.ofNullable(occurs);
    }

    public Optional<String> getRedefines() {
        return Optional.ofNullable(redefines);
    }
}
