# Migration plan

Use a JDBC migrator to create SQLite schema and primary keys, copy rows, convert only temporal fields and CLOB JSON payloads, then validate counts and API startup.
