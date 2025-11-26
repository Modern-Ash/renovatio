# Control Break Pattern Decomposition

## Overview

This document describes Renovatio's solution to the architectural impedance mismatch between COBOL's file-processing paradigm and modern service-oriented architectures.

## The Problem: Architectural Impedance Mismatch

COBOL programs that process ISAM/sequential files with control break logic are common in legacy systems. These programs typically:

1. **Read files sequentially** in a loop
2. **Detect level breaks** when key fields change (e.g., customer ID, region code)
3. **Accumulate values** and produce subtotals at each break level
4. **Process group headers and footers** during breaks

This pattern doesn't translate well to a 1:1 migration because:
- Modern systems use databases, APIs, or queues instead of ISAM files
- File processing is interleaved with business logic
- Control flow is implicit in the READ loop structure
- Aggregations are embedded in the procedural code

## The Solution: Decomposition

Instead of translating 1 COBOL program to 1 Java program, Renovatio decomposes the control break patterns into **reusable architectural components**:

### Generated Components

| Component Type | COBOL Origin | Java Target |
|---------------|--------------|-------------|
| **Repository Interface** | FILE operations (READ, WRITE) | Data access abstraction (JPA, JDBC, REST client) |
| **Business Rules Service** | COMPUTE, MOVE, IF statements | Discrete, testable methods |
| **Aggregation Service** | Control break totals | Java Stream collectors |
| **Validation Service** | Data validation checks | Reusable validators |
| **Processing Orchestrator** | Main program flow | Service that coordinates components |

### Benefits

1. **Data Source Independence**: Business logic works with any data source (DB, file, API, queue)
2. **Testability**: Each component is unit-testable in isolation
3. **Reusability**: Business rules can be invoked from multiple contexts
4. **Parallelization**: Stream-based aggregations can be parallelized
5. **Modern Architecture**: Fits into microservices, event-driven, or batch architectures

## Using the `cobol.decompose` Tool

### MCP Tool Call

```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "tools/call",
  "params": {
    "name": "cobol.decompose",
    "arguments": {
      "workspacePath": "/path/to/cobol/programs"
    }
  }
}
```

### Response

```json
{
  "success": true,
  "message": "Decomposed 3 program(s) with 5 control break pattern(s). Generated 15 reusable component(s).",
  "data": {
    "generated": {
      "CustomerRepository.java": "...",
      "SalesBusinessRules.java": "...",
      "SalesAggregations.java": "...",
      "SalesValidator.java": "...",
      "SalesProcessingService.java": "..."
    }
  }
}
```

## Example: Sales Report COBOL Program

### Original COBOL

```cobol
       IDENTIFICATION DIVISION.
       PROGRAM-ID. SALESRPT.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-PREV-REGION     PIC X(10).
       01  WS-PREV-CUSTOMER   PIC X(10).
       01  WS-REGION-TOTAL    PIC 9(9)V99.
       01  WS-CUSTOMER-TOTAL  PIC 9(9)V99.
       01  WS-GRAND-TOTAL     PIC 9(9)V99.
       
       PROCEDURE DIVISION.
           OPEN INPUT SALES-FILE.
           READ SALES-FILE.
           MOVE REGION TO WS-PREV-REGION.
           MOVE CUSTOMER TO WS-PREV-CUSTOMER.
           
           PERFORM UNTIL END-OF-FILE
               IF REGION NOT = WS-PREV-REGION
                   PERFORM REGION-BREAK
               END-IF
               IF CUSTOMER NOT = WS-PREV-CUSTOMER
                   PERFORM CUSTOMER-BREAK
               END-IF
               ADD AMOUNT TO WS-CUSTOMER-TOTAL
               READ SALES-FILE
           END-PERFORM.
           
           PERFORM FINAL-TOTALS.
           CLOSE SALES-FILE.
           STOP RUN.
```

### Generated Java Components

#### 1. Repository Interface

```java
public interface SalesRepository {
    Stream<SalesRecord> streamAll();
    Stream<SalesRecord> streamByRegion(String region);
    List<SalesRecord> findAll();
}
```

#### 2. Aggregation Service

```java
@Component
public class SalesAggregations {
    
    public Map<String, RegionTotals> aggregateByRegion(Stream<SalesRecord> records) {
        return records.collect(
            Collectors.groupingBy(
                SalesRecord::getRegion,
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    this::computeRegionTotals
                )
            )
        );
    }
    
    public Map<String, CustomerTotals> aggregateByCustomer(Stream<SalesRecord> records) {
        return records.collect(
            Collectors.groupingBy(
                SalesRecord::getCustomer,
                LinkedHashMap::new,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    this::computeCustomerTotals
                )
            )
        );
    }
}
```

#### 3. Processing Service (Orchestrator)

```java
@Service
public class SalesProcessingService {
    
    private final SalesRepository repository;
    private final SalesAggregations aggregations;
    private final SalesBusinessRules businessRules;
    
    public ProcessingResult process() {
        // Stream-based processing replaces COBOL READ loop
        return repository.streamAll()
            .collect(Collectors.groupingBy(
                SalesRecord::getRegion,
                LinkedHashMap::new,
                Collectors.groupingBy(
                    SalesRecord::getCustomer,
                    LinkedHashMap::new,
                    Collectors.toList()
                )
            ))
            .entrySet().stream()
            .map(this::processRegion)
            .reduce(ProcessingResult::merge)
            .orElse(ProcessingResult.empty());
    }
}
```

## Pattern Detection

Renovatio detects control break patterns by analyzing:

1. **File operations**: READ in a loop, OPEN/CLOSE sequences
2. **Save variables**: Fields with SAVE-, PREV-, OLD- prefixes storing previous values
3. **Break comparisons**: IF statements comparing current field to previous value
4. **Accumulator fields**: Variables named with TOTAL-, SUM-, COUNT- patterns
5. **Break paragraphs**: Paragraphs named with BREAK, CORTE, SUBTOTAL patterns

## Configuration

No special configuration is required. The decomposition service is automatically available when the COBOL provider is enabled.

## Related Documentation

- [ARCHITECTURE.md](../../ARCHITECTURE.md) - System architecture
- [MCP-CLIENT-GUIDE.md](../../MCP-CLIENT-GUIDE.md) - MCP client integration
- [cobol-java-mapping-investigacion.md](../cobol-java-mapping-investigacion.md) - COBOL to Java mapping research
