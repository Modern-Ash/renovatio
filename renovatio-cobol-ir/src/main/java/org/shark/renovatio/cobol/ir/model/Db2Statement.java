package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public final class Db2Statement implements CobolStatement {

    private final String sql;

    public Db2Statement(String sql) {
        this.sql = Objects.requireNonNull(sql, "sql");
    }

    public String getSql() {
        return sql;
    }
}
