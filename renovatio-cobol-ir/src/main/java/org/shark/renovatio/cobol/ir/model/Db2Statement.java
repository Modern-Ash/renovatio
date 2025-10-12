package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record Db2Statement(String sql) implements CobolStatement {

    public Db2Statement(String sql) {
        this.sql = Objects.requireNonNull(sql, "sql");
    }
}
