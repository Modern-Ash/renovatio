package org.shark.renovatio.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataAccessDto {
    private String id;
    private String kind;
    private String resourceReference;
    private double confidence;
    private String suggestedStrategy;
    private String currentStrategy;
    private KeyShapeDto keyShape;
    private RecordShapeDto recordShape;
    private List<String> discriminatorValues;

    public DataAccessDto() {}

    public DataAccessDto(String id, String kind, String resourceReference, double confidence,
                         String suggestedStrategy, String currentStrategy,
                         KeyShapeDto keyShape, RecordShapeDto recordShape,
                         List<String> discriminatorValues) {
        this.id = id;
        this.kind = kind;
        this.resourceReference = resourceReference;
        this.confidence = confidence;
        this.suggestedStrategy = suggestedStrategy;
        this.currentStrategy = currentStrategy;
        this.keyShape = keyShape;
        this.recordShape = recordShape;
        this.discriminatorValues = discriminatorValues;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getResourceReference() { return resourceReference; }
    public void setResourceReference(String resourceReference) { this.resourceReference = resourceReference; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getSuggestedStrategy() { return suggestedStrategy; }
    public void setSuggestedStrategy(String suggestedStrategy) { this.suggestedStrategy = suggestedStrategy; }
    public String getCurrentStrategy() { return currentStrategy; }
    public void setCurrentStrategy(String currentStrategy) { this.currentStrategy = currentStrategy; }
    public KeyShapeDto getKeyShape() { return keyShape; }
    public void setKeyShape(KeyShapeDto keyShape) { this.keyShape = keyShape; }
    public RecordShapeDto getRecordShape() { return recordShape; }
    public void setRecordShape(RecordShapeDto recordShape) { this.recordShape = recordShape; }
    public List<String> getDiscriminatorValues() { return discriminatorValues; }
    public void setDiscriminatorValues(List<String> discriminatorValues) { this.discriminatorValues = discriminatorValues; }

    public static class KeyShapeDto {
        private List<String> fields;

        public KeyShapeDto() {}
        public KeyShapeDto(List<String> fields) { this.fields = fields; }
        public List<String> getFields() { return fields; }
        public void setFields(List<String> fields) { this.fields = fields; }
        public boolean isNone() { return fields == null || fields.isEmpty(); }
    }

    public static class RecordShapeDto {
        private String fdName;
        private String table;
        private List<String> columns;

        public RecordShapeDto() {}
        public RecordShapeDto(String fdName, String table, List<String> columns) {
            this.fdName = fdName;
            this.table = table;
            this.columns = columns;
        }
        public String getFdName() { return fdName; }
        public void setFdName(String fdName) { this.fdName = fdName; }
        public String getTable() { return table; }
        public void setTable(String table) { this.table = table; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
    }
}
