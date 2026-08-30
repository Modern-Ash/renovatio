# data-model.md — Entidades para Migración COBOL→Python

Basado en `specs/1-cobol-python-migration/spec.md`.

## Entidades claves

### COBOLProgram
- id: string (path o identificador único)
- name: string
- copybooks: [CopybookRef]
- procedures: [Procedure]
- records: [Record]
- io_definitions: [IODefinition]
- metadata: { source_encoding, line_length, author }

### Copybook
- id: string
- name: string
- fields: [Field]
- metadata: { path, referenced_by }

### Record / Field
- name: string
- offset: int (byte offset in fixed layout)
- length: int
- type: enum (ALPHANUMERIC, NUMERIC, COMP-3, PACKED, BINARY)
- scale: int (decimales)
- picture: string (COBOL PICTURE)
- redefines: optional string

### Procedure
- name: string
- params: [Param]
- body_ir: object (IR for actions: MOVE, PERFORM, IF, READ, WRITE)

### IODefinition
- name: string
- type: enum (SEQUENTIAL_FILE, VSAM, DB2_TABLE, MQ)
- record_layout: Record
- access_mode: enum (SEQUENTIAL, RANDOM)

### MigrationPlan (metadatos de la conversión)
- feature_branch: string
- target_python_version: string
- mapping_rules: [MappingRule]
- exceptions: [ManualActionItem]

### MappingRule
- cobol_construct: string
- python_pattern: string
- notes: string

### ManualActionItem
- id: string
- location: { program_id, line }
- reason: string
- recommended_action: string

## Validations / Rules derivadas
- Fields COMP-3 deben mapearse a Decimal.  
- Records with REDEFINES deben generar tests que validen offsets y overlaps.  
- IODefinitions with DB2 must be flagged for adapter creation.



