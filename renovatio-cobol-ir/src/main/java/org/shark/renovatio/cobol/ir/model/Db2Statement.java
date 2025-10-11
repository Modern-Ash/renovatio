package org.shark.renovatio.cobol.ir.model;

import lombok.Value;

import java.util.Objects;

@Value
public final class Db2Statement implements CobolStatement {

    String sql;

    public Db2Statement(String sql) {
        this.sql = Objects.requireNonNull(sql, "sql");
    }
}
