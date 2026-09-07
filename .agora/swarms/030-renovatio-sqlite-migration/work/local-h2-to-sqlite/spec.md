# SQLite local migration

Replace the local H2 runtime configuration with SQLite while retaining H2 files as rollback. The migration must copy existing data, never overwrite an existing target, and validate row counts before cutover.
