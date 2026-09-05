# Configuración de runners de equivalencia

`ProcessReplayRunner` recibe una lista de argumentos ya validada y un directorio de trabajo permitido. Los comandos deben configurarse del lado servidor; nunca se aceptan comandos arbitrarios desde la webapp.

Ejemplo conceptual:

```yaml
equivalence:
  baseline:
    command: ["cobol-runner", "--program", "PERFORM-SIMPLE-NESTED"]
    workingDirectory: "/srv/renovatio/cobol-baseline"
  candidate:
    command: ["java", "-jar", "/srv/renovatio/generated/app.jar"]
    workingDirectory: "/srv/renovatio/generated"
```

Cada ejecución debe registrar el `caseId`, versión de ambos artefactos, código de salida y hashes de stdout/stderr antes de pasar al comparador y al gate de cutover.
