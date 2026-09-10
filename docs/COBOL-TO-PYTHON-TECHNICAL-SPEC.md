# COBOL to Python - Especificación Técnica

## 1. Resumen Ejecutivo

Este documento proporciona la especificación técnica detallada para implementar la traducción de COBOL a Python en Renovatio. Incluye definiciones de interfaces, ejemplos de código, y patrones de diseño.

## 2. Interfaces y Contratos

### 2.1 PythonLanguageProvider

```java
package org.shark.renovatio.provider.python;

import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.BaseLanguageProvider;
import java.util.*;

/**
 * Python Language Provider - Traduce COBOL a Python
 * Reutiliza servicios de parsing COBOL existentes
 */
public class PythonLanguageProvider extends BaseLanguageProvider {
    
    private static final String LANG_PYTHON = "python";
    
    // Servicios compartidos desde renovatio-provider-cobol
    private final CobolParsingService cobolParsingService;
    private final CobolIntermediateModelService intermediateModelService;
    
    // Servicios específicos de Python
    private final PythonGenerationService pythonGenerationService;
    private final PythonTemplateService pythonTemplateService;
    
    @Override
    public String language() {
        return LANG_PYTHON;
    }
    
    @Override
    public Set<Capabilities> capabilities() {
        return EnumSet.of(
            Capabilities.ANALYZE,     // Reutiliza COBOL parsing
            Capabilities.PLAN,        // Reutiliza planning
            Capabilities.APPLY,       // Nueva generación Python
            Capabilities.STUBS,       // Genera código Python
            Capabilities.METRICS      // Reutiliza métricas
        );
    }
    
    @Override
    public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) {
        // 1. Parse COBOL (reutiliza)
        AnalyzeResult analysis = cobolParsingService.analyzeCOBOL(query, workspace);
        
        // 2. Generar código Python (nuevo)
        return Optional.of(pythonGenerationService.generatePythonCode(analysis, workspace));
    }
    
    @Override
    public List<Tool> getTools() {
        return List.of(
            new BasicTool("python.generate_from_cobol", 
                         "Generate Python code from COBOL programs", 
                         baseSchema()),
            new BasicTool("python.generate_dataclass", 
                         "Generate Python dataclass from COBOL copybook", 
                         copybookSchema()),
            new BasicTool("python.generate_sqlalchemy", 
                         "Generate SQLAlchemy models from DB2 statements", 
                         db2Schema()),
            new BasicTool("python.generate_fastapi", 
                         "Generate FastAPI endpoints from CICS transactions", 
                         cicsSchema())
        );
    }
}
```

### 2.2 PythonGenerationService

```java
package org.shark.renovatio.provider.python.service;

import org.shark.renovatio.cobol.ir.model.*;
import org.shark.renovatio.shared.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio de generación de código Python desde IR de COBOL
 * No usa JavaPoet - usa string templates directos
 */
@Service
public class PythonGenerationService {
    
    private final PythonTemplateService templateService;
    private final CobolToPythonTypeMapper typeMapper;
    
    /**
     * Genera código Python completo desde análisis COBOL
     */
    public StubResult generatePythonCode(AnalyzeResult analysis, Workspace workspace) {
        Map<String, String> generatedFiles = new HashMap<>();
        
        List<CobolProgram> programs = extractPrograms(analysis);
        
        for (CobolProgram program : programs) {
            String baseName = sanitizePythonModuleName(program.getName());
            
            // Generar dataclass
            String dataclassCode = generateDataclass(program, baseName);
            generatedFiles.put(baseName + "_model.py", dataclassCode);
            
            // Generar funciones
            String functionsCode = generateFunctions(program, baseName);
            generatedFiles.put(baseName + "_service.py", functionsCode);
            
            // Generar validaciones
            String validationCode = generateValidations(program, baseName);
            generatedFiles.put(baseName + "_validation.py", validationCode);
            
            // Si tiene CICS, generar FastAPI
            if (program.hasCicsCommands()) {
                String apiCode = generateFastAPIEndpoints(program, baseName);
                generatedFiles.put(baseName + "_api.py", apiCode);
            }
            
            // Si tiene DB2, generar SQLAlchemy
            if (program.hasDb2Statements()) {
                String modelCode = generateSQLAlchemyModels(program, baseName);
                generatedFiles.put(baseName + "_orm.py", modelCode);
            }
        }
        
        // Escribir archivos al disco
        writeGeneratedFilesToDisk(generatedFiles, workspace);
        
        StubResult result = new StubResult(true, 
            "Generated " + generatedFiles.size() + " Python files");
        result.setGeneratedCode(generatedFiles);
        return result;
    }
    
    /**
     * Genera dataclass Python desde WORKING-STORAGE
     */
    private String generateDataclass(CobolProgram program, String baseName) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("moduleName", baseName);
        templateData.put("className", toPascalCase(baseName));
        templateData.put("fields", mapFieldsToDataclass(program.getDataItems()));
        templateData.put("cobolSource", program.getSourcePath());
        
        return templateService.generateFromTemplate("python_dataclass.ftl", templateData);
    }
    
    /**
     * Mapea campos COBOL a atributos Python con type hints
     */
    private List<Map<String, Object>> mapFieldsToDataclass(List<CobolDataItem> dataItems) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        for (CobolDataItem item : dataItems) {
            Map<String, Object> field = new HashMap<>();
            field.put("name", toSnakeCase(item.getName()));
            field.put("type", typeMapper.mapToPythonType(item));
            field.put("cobolName", item.getName());
            field.put("picture", item.getPicture());
            field.put("level", item.getLevel());
            field.put("optional", false);
            
            fields.add(field);
        }
        
        return fields;
    }
    
    /**
     * Genera funciones Python desde PROCEDURE DIVISION
     */
    private String generateFunctions(CobolProgram program, String baseName) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("moduleName", baseName);
        templateData.put("className", toPascalCase(baseName));
        templateData.put("functions", mapProceduresToFunctions(program));
        
        return templateService.generateFromTemplate("python_functions.ftl", templateData);
    }
    
    // ... métodos auxiliares
}
```

### 2.3 CobolToPythonTypeMapper

```java
package org.shark.renovatio.provider.python.mapper;

import org.shark.renovatio.cobol.ir.model.CobolDataItem;
import org.springframework.stereotype.Component;

/**
 * Mapea tipos COBOL a tipos Python con type hints
 */
@Component
public class CobolToPythonTypeMapper {
    
    /**
     * Mapea un CobolDataItem a un tipo Python
     */
    public String mapToPythonType(CobolDataItem item) {
        String picture = item.getPicture();
        if (picture == null || picture.isEmpty()) {
            return "str";
        }
        
        picture = picture.toUpperCase().trim();
        
        // Numéricos enteros
        if (picture.matches("9+") || picture.matches("9\\(\\d+\\)")) {
            return "int";
        }
        
        // Numéricos con decimales
        if (picture.contains("V") || picture.contains(".")) {
            return "Decimal";
        }
        
        // Numéricos con signo
        if (picture.startsWith("S") && picture.contains("9")) {
            if (picture.contains("V")) {
                return "Decimal";
            }
            return "int";
        }
        
        // Alphanumeric
        if (picture.startsWith("X") || picture.startsWith("A")) {
            return "str";
        }
        
        // Computacionales
        if (picture.contains("COMP")) {
            if (picture.contains("V") || picture.contains("COMP-3")) {
                return "Decimal";
            }
            return "int";
        }
        
        // Default
        return "str";
    }
    
    /**
     * Obtiene el import necesario para el tipo
     */
    public String getImportForType(String pythonType) {
        return switch (pythonType) {
            case "Decimal" -> "from decimal import Decimal";
            case "date" -> "from datetime import date";
            case "datetime" -> "from datetime import datetime";
            case "Optional" -> "from typing import Optional";
            default -> null;
        };
    }
    
    /**
     * Determina si el campo debe ser Optional
     */
    public boolean isOptional(CobolDataItem item) {
        // En COBOL, la mayoría de campos no son opcionales
        // Pero podemos inferir basado en el nivel 88 o VALUE clauses
        return item.hasLevel88Items() || item.hasValueClause();
    }
}
```

### 2.4 PythonTemplateService

```java
package org.shark.renovatio.provider.python.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Map;

/**
 * Servicio de templates para generación de código Python
 * Usa Freemarker (igual que Java provider)
 */
@Service
public class PythonTemplateService {
    
    private final Configuration freemarkerConfig;
    
    public PythonTemplateService() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates");
        freemarkerConfig.setDefaultEncoding("UTF-8");
    }
    
    /**
     * Genera código desde un template
     */
    public String generateFromTemplate(String templateName, Map<String, Object> data) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(data, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Template generation failed: " + templateName, e);
        }
    }
}
```

## 3. Templates Freemarker

### 3.1 Template: python_dataclass.ftl

```python
"""
${className} - Generated from COBOL program
Source: ${cobolSource}

This module was automatically generated by Renovatio.
Do not edit manually - regenerate from source COBOL.
"""
from dataclasses import dataclass
<#list imports as import>
${import}
</#list>

@dataclass
class ${className}:
    """
    Data model generated from COBOL WORKING-STORAGE SECTION
    
    Original COBOL structure has been translated to Python dataclass
    with appropriate type hints for type safety.
    """
    
<#list fields as field>
    ${field.name}: ${field.type}  # COBOL: ${field.level} ${field.cobolName} PIC ${field.picture}
</#list>

    def validate(self) -> bool:
        """
        Validate all fields according to COBOL constraints
        
        Returns:
            bool: True if all validations pass, False otherwise
        """
<#list validations as validation>
        ${validation}
</#list>
        return True
    
    def to_dict(self) -> dict:
        """Convert to dictionary for serialization"""
        return {
<#list fields as field>
            '${field.name}': self.${field.name},
</#list>
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> '${className}':
        """Create instance from dictionary"""
        return cls(
<#list fields as field>
            ${field.name}=data.get('${field.name}'),
</#list>
        )
```

### 3.2 Template: python_functions.ftl

```python
"""
${moduleName}_service - Business logic from COBOL PROCEDURE DIVISION
Source: ${cobolSource}

This module contains the translated business logic from the
original COBOL program.
"""
from typing import Optional
from .${moduleName}_model import ${className}

<#list functions as func>
def ${func.name}(${func.parameters}) -> ${func.returnType}:
    """
    ${func.description}
    
    Original COBOL: ${func.cobolParagraph}
    
    Args:
<#list func.args as arg>
        ${arg.name}: ${arg.description}
</#list>
    
    Returns:
        ${func.returnDescription}
    """
    # TODO: Implement business logic from COBOL
<#list func.statements as stmt>
    ${stmt}
</#list>
    pass

</#list>

class ${className}Service:
    """Service class encapsulating all business operations"""
    
    def __init__(self):
        """Initialize service"""
        pass
    
<#list methods as method>
    def ${method.name}(self, input_data: ${className}) -> ${className}:
        """
        ${method.description}
        
        Args:
            input_data: Input data structure
            
        Returns:
            Processed output data structure
        """
        # Validate input
        if not input_data.validate():
            raise ValueError("Input validation failed")
        
        # Process
        output = ${className}(**input_data.to_dict())
        
        # TODO: Implement COBOL business logic
<#list method.logic as line>
        ${line}
</#list>
        
        return output
</#list>
```

### 3.3 Template: python_fastapi.ftl

```python
"""
${moduleName}_api - FastAPI endpoints from COBOL CICS transactions
Source: ${cobolSource}

This module exposes the COBOL business logic as modern REST API endpoints.
"""
from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel
from .${moduleName}_model import ${className}
from .${moduleName}_service import ${className}Service

router = APIRouter(prefix="/${moduleName}", tags=["${className}"])
service = ${className}Service()

# Pydantic models for API
class ${className}Request(BaseModel):
    """API request model"""
<#list fields as field>
    ${field.name}: ${field.type}
</#list>

class ${className}Response(BaseModel):
    """API response model"""
<#list fields as field>
    ${field.name}: ${field.type}
</#list>

<#list endpoints as endpoint>
@router.${endpoint.method}("${endpoint.path}")
async def ${endpoint.function_name}(request: ${className}Request) -> ${className}Response:
    """
    ${endpoint.description}
    
    Original CICS Transaction: ${endpoint.cicsTransaction}
    """
    try:
        # Convert request to domain model
        input_data = ${className}(**request.dict())
        
        # Process
        output_data = service.${endpoint.serviceMethod}(input_data)
        
        # Convert to response
        return ${className}Response(**output_data.to_dict())
        
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error"
        )
</#list>
```

## 4. Ejemplos de Traducción

### 4.1 Ejemplo Completo: Customer Processing

**Input COBOL:**
```cobol
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CUSTPROC.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  CUSTOMER-REC.
           05  CUST-ID            PIC 9(8).
           05  CUST-NAME          PIC X(50).
           05  CUST-BALANCE       PIC S9(9)V99 COMP-3.
           05  CUST-STATUS        PIC X(1).
               88  STATUS-ACTIVE  VALUE 'A'.
               88  STATUS-CLOSED  VALUE 'C'.
       
       PROCEDURE DIVISION.
       MAIN-PROCESS.
           MOVE 12345678 TO CUST-ID.
           MOVE 'JOHN DOE' TO CUST-NAME.
           MOVE 1500.50 TO CUST-BALANCE.
           SET STATUS-ACTIVE TO TRUE.
           
           PERFORM VALIDATE-CUSTOMER.
           IF CUST-BALANCE > 1000
              DISPLAY 'High value customer'
           END-IF.
           STOP RUN.
       
       VALIDATE-CUSTOMER.
           IF CUST-ID = ZERO
              DISPLAY 'Invalid customer ID'
           END-IF.
```

**Output Python (custproc_model.py):**
```python
"""
CustomerRec - Generated from COBOL program CUSTPROC
Source: CUSTPROC.cbl
"""
from dataclasses import dataclass
from decimal import Decimal
from enum import Enum

class CustomerStatus(str, Enum):
    """COBOL 88-level STATUS conditions"""
    ACTIVE = 'A'  # STATUS-ACTIVE
    CLOSED = 'C'  # STATUS-CLOSED

@dataclass
class CustomerRec:
    """Data model from WORKING-STORAGE SECTION"""
    
    cust_id: int  # COBOL: 05 CUST-ID PIC 9(8)
    cust_name: str  # COBOL: 05 CUST-NAME PIC X(50)
    cust_balance: Decimal  # COBOL: 05 CUST-BALANCE PIC S9(9)V99 COMP-3
    cust_status: str  # COBOL: 05 CUST-STATUS PIC X(1)
    
    def validate(self) -> bool:
        """Validate fields according to COBOL constraints"""
        if self.cust_id is None or self.cust_id == 0:
            return False
        if not self.cust_name or len(self.cust_name) > 50:
            return False
        if self.cust_balance is None:
            return False
        if self.cust_status not in ['A', 'C']:
            return False
        return True
    
    def is_active(self) -> bool:
        """Check if STATUS-ACTIVE (88-level)"""
        return self.cust_status == CustomerStatus.ACTIVE
    
    def is_closed(self) -> bool:
        """Check if STATUS-CLOSED (88-level)"""
        return self.cust_status == CustomerStatus.CLOSED
```

**Output Python (custproc_service.py):**
```python
"""
custproc_service - Business logic from PROCEDURE DIVISION
"""
from decimal import Decimal
from .custproc_model import CustomerRec, CustomerStatus

def validate_customer(customer: CustomerRec) -> bool:
    """
    Validate customer data
    Original COBOL: VALIDATE-CUSTOMER paragraph
    """
    if customer.cust_id == 0:
        print('Invalid customer ID')
        return False
    return True

class CustprocService:
    """Service class for customer processing"""
    
    def main_process(self) -> CustomerRec:
        """
        Main processing logic
        Original COBOL: MAIN-PROCESS paragraph
        """
        # Create customer record
        customer = CustomerRec(
            cust_id=12345678,
            cust_name='JOHN DOE',
            cust_balance=Decimal('1500.50'),
            cust_status=CustomerStatus.ACTIVE
        )
        
        # Validate
        if not validate_customer(customer):
            raise ValueError('Customer validation failed')
        
        # Check balance
        if customer.cust_balance > 1000:
            print('High value customer')
        
        return customer
```

## 5. Configuración Maven

### 5.1 pom.xml del Módulo Python

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.shark.renovatio</groupId>
        <artifactId>renovatio-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    
    <artifactId>renovatio-provider-python</artifactId>
    <name>Renovatio Python Provider</name>
    <description>Python code generation from COBOL sources</description>
    
    <dependencies>
        <!-- Internal modules (SHARED) -->
        <dependency>
            <groupId>org.shark.renovatio</groupId>
            <artifactId>renovatio-shared</artifactId>
        </dependency>
        <dependency>
            <groupId>org.shark.renovatio</groupId>
            <artifactId>renovatio-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.shark.renovatio</groupId>
            <artifactId>renovatio-provider-cobol</artifactId>
        </dependency>
        <dependency>
            <groupId>org.shark.renovatio</groupId>
            <artifactId>renovatio-cobol-ir</artifactId>
        </dependency>
        
        <!-- Template engine (SHARED) -->
        <dependency>
            <groupId>org.freemarker</groupId>
            <artifactId>freemarker</artifactId>
        </dependency>
        
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

## 6. Checklist de Implementación

### Fase 1: Setup ✅
- [ ] Crear módulo Maven `renovatio-provider-python`
- [ ] Configurar estructura de directorios
- [ ] Añadir dependencias básicas
- [ ] Configurar Spring Boot auto-configuration

### Fase 2: Servicios Core ✅
- [ ] Implementar `PythonLanguageProvider`
- [ ] Implementar `PythonGenerationService`
- [ ] Implementar `CobolToPythonTypeMapper`
- [ ] Implementar `PythonTemplateService`

### Fase 3: Templates ✅
- [ ] Crear `python_dataclass.ftl`
- [ ] Crear `python_functions.ftl`
- [ ] Crear `python_validation.ftl`
- [ ] Crear `python_fastapi.ftl`
- [ ] Crear `python_sqlalchemy.ftl`

### Fase 4: Tests ✅
- [ ] Unit tests para TypeMapper
- [ ] Unit tests para GenerationService
- [ ] Integration tests para traducción completa
- [ ] Tests de validación de Python generado

### Fase 5: Integración ✅
- [ ] Registrar provider en MCP server
- [ ] Añadir herramientas MCP
- [ ] Actualizar documentación
- [ ] Crear ejemplos

---

**Documento Técnico:** Versión 1.0  
**Fecha:** 2025-11-26  
**Estado:** Diseño Aprobado para Implementación
