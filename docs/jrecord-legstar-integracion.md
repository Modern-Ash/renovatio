# Evaluación de integración: JRecord y LegStar en Renovatio

Este memo resume los cambios técnicos necesarios para aprovechar **JRecord** (Apache 2.0) en la resolución de tipos COBOL complejos y **LegStar** (LGPL) para exponer integración CICS dentro de Renovatio. Se centra en impactos sobre los módulos existentes, riesgos, beneficios y lineamientos de implementación gradual.

## 1. Contexto actual en Renovatio

- La generación de artefactos Java para COBOL se concentra en `renovatio-provider-cobol`, que ya depende del IR COBOL (`renovatio-cobol-ir`) y de las recetas OpenRewrite para producir servicios Java, plantillas Freemarker/Mustache y DTOs soportados por MapStruct.【F:renovatio-provider-cobol/pom.xml†L17-L66】
- El parser intermedio y las recetas se encargan de mapear sentencias COBOL a código Java, pero aún carecen de soporte profundo para tipos empaquetados (`COMP-3`), copybooks anidados y bindings CICS. La integración con librerías externas debe respetar la arquitectura modular y los contratos MCP descritos en la documentación principal.【F:docs/cobol-java-mapping-investigacion.md†L1-L90】【F:README.md†L1-L78】

## 2. Integrar JRecord para tipos COBOL complejos

1. **Dependencia Maven**: añadir `net.sf.JRecord:jrecord` (scope `compile`) en `renovatio-provider-cobol` para que las recetas y generadores dispongan de los parsers de copybook y runtimes de conversión. Puede declararse opcional en módulos que no procesen COBOL.【F:renovatio-provider-cobol/pom.xml†L17-L66】
2. **Ingesta de copybooks**:
   - Extender `renovatio-cobol-ir` para delegar la lectura de copybooks a `ExternalRecordBuilder`, generando metadatos (longitudes, signos, empaquetado) que alimenten el IR.
   - Guardar los esquemas en un caché (por ejemplo, Lucene ya disponible) para reutilizarlos durante la generación de DTOs.【F:renovatio-provider-cobol/pom.xml†L39-L54】
3. **Generación de DTOs**:
   - Adaptar las plantillas de JavaPoet/Freemarker para consumir la metadata JRecord y producir POJOs/records con anotaciones MapStruct coherentes.
   - Incluir utilidades de serialización/deserialización (byte array ↔ DTO) usando `AbstractLine` y `LayoutDetail` de JRecord.
4. **Recetas OpenRewrite**:
   - Crear recetas específicas (p. ej., `CobolCopybookRecipe`) que inyecten constructores y validaciones basadas en la metadata proveniente de JRecord.
   - Proveer transformadores para `MOVE`, `COMPUTE` y rutinas que requieran conversions `BigDecimal` ↔ `PackedDecimal`.
5. **Pruebas**:
   - Incorporar datasets COBOL en `examples/` y diseñar tests JUnit que comparen lectura/escritura con resultados esperados.
   - Validar la compatibilidad con archivos secuenciales, VSAM (simulados) y copybooks con `REDEFINES`.
6. **Despliegue**: JRecord es Apache 2.0, totalmente compatible con la licencia del proyecto; solo requiere atribución en los notices.

## 3. Integrar LegStar para CICS y bindings mainframe

1. **Dependencia Maven**: incluir los artefactos `org.legstar:legstar-cob2trans` y `legstar-cixsgen` en `renovatio-provider-cobol`, manteniéndolos opcionales para no afectar módulos sin CICS. Estos proveen generación de clases Java a partir de copybooks y runtimes para invocación CICS.
2. **Generación de adaptadores**:
   - Crear un pipeline dentro del módulo (o un submódulo) que ejecute el generador LegStar (`cob2trans`) sobre copybooks, produciendo interfaces Java y beans de request/response.
   - Integrar el output en las plantillas existentes para que los servicios REST/gRPC expuestos por Renovatio deleguen en los adaptadores CICS.
3. **Runtime y seguridad**:
   - Configurar beans Spring que encapsulen las llamadas CICS (via JCICS/Zowe cuando se ejecute en mainframe) y soporten credenciales externas.
   - Proveer perfiles de configuración (`application-cics.yml`) que definan endpoints, mapeos de transacción y timeouts.
4. **MCP y herramientas**:
   - Exponer nuevos tools (por ejemplo, `cicsGenerateAdapter`) en el MCP server (`renovatio-mcp-server`) que disparen el flujo LegStar y devuelvan artefactos generados.
   - Documentar los esquemas de entrada/salida en `schemas/` para permitir su invocación por agentes.
5. **Pruebas e integración continua**:
   - Añadir simuladores CICS (LegStar soporta `legstar-host`) en los tests de integración y pipelines CI.
   - Diseñar pruebas de contrato que validen la serialización/deserialización y la conectividad con entornos sandbox.
6. **Licenciamiento**: LegStar está bajo LGPL; se debe distribuir como dependencia dinámica. Es necesario mantenerlo como jar externo y documentar cómo reemplazarlo en entornos comerciales para cumplir con la licencia.

## 4. Problemas potenciales

- **Complejidad del build**: la adición de generadores externos aumenta el tiempo de Maven y puede requerir perfiles específicos (`-Pwith-jrecord`, `-Pwith-legstar`).
- **Gestión de copybooks**: mantener versiones sincronizadas entre JRecord y LegStar exige una convención de almacenamiento (por ejemplo, repositorio Git separado o artefactos empaquetados).
- **LGPL compliance**: cualquier modificación a LegStar debe redistribuirse; se recomienda aislarla en un módulo opcional para evitar obligaciones adicionales sobre el núcleo de Renovatio.
- **Curva de aprendizaje**: el equipo deberá dominar APIs detalladas (Layouts, HostBytes) y flujos CICS, lo que implica capacitación y documentación adicional.

## 5. Beneficios y virtudes

- **Cobertura de tipos avanzada**: JRecord resuelve `COMP-3`, `BINARY`, `OCCURS DEPENDING ON` y copybooks anidados, habilitando migraciones más completas.
- **Reutilización de copybooks**: ambos frameworks generan POJOs y bindings basados en copybooks existentes, reduciendo errores manuales.
- **Integración CICS real**: LegStar facilita el puente Java ↔ CICS sin tener que desarrollar runtimes propios, acelerando la modernización.
- **Alineamiento con MCP**: exponer los generadores como herramientas MCP permite que agentes automáticos utilicen los nuevos flujos dentro del ecosistema Renovatio.

## 6. Plan de adopción sugerido

1. Prototipo interno con JRecord en un programa piloto (sin LegStar) para validar conversiones de copybook y generación de DTOs.
2. Extender las recetas OpenRewrite para consumir la metadata JRecord y generar código Java funcional.
3. Aislar LegStar en un módulo `renovatio-provider-cics` opcional, integrando primero la generación de clases y luego el runtime.
4. Actualizar la documentación y los esquemas MCP, incorporando casos de prueba automatizados para copybooks complejos y flujos CICS.
5. Evaluar métricas de performance y overhead en pipelines CI antes de generalizar la dependencia.

Este roadmap permite incorporar las librerías de forma controlada, maximizando los beneficios (tipos complejos y CICS) y mitigando riesgos de licencia y complejidad operativa.
