package org.shark.renovatio.cobol.ir.model;

import org.shark.renovatio.cobol.runtime.PicType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CobolDataItem(String name, String picture, int level, Integer occurs,
                            String redefines, String javaType, PicType picType,
                            List<Level88Condition> level88Conditions) {

    public CobolDataItem(String name, String picture, int level, Integer occurs, String redefines, String javaType) {
        this(name, picture, level, occurs, redefines, javaType, null, List.of());
    }

    public CobolDataItem {
        name = Objects.requireNonNull(name, "name");
        javaType = javaType == null ? "String" : javaType;
        level88Conditions = List.copyOf(level88Conditions == null ? List.of() : level88Conditions);
    }

    // Bean-style getters for JUnit expectations
    public String getName() { return name; }
    public String getPicture() { return picture; }
    public int getLevel() { return level; }
    public String getJavaType() { return javaType; }
    public PicType getPicType() { return picType; }
    public List<Level88Condition> getLevel88Conditions() { return level88Conditions; }

    public Optional<Integer> getOccurs() {
        return Optional.ofNullable(occurs);
    }

    public Optional<String> getRedefines() {
        return Optional.ofNullable(redefines);
    }
}
