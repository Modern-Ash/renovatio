package org.shark.renovatio.api.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="llm_evaluations")
public class LlmEvaluationEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String projectId; @Column(nullable=false) private String datasetId;
 private int total; private int accepted; private int schemaFailures; private int provenanceFailures; private double acceptanceRate; private boolean passes; @Column(nullable=false) private Instant createdAt;
 protected LlmEvaluationEntity() {}
 public LlmEvaluationEntity(String projectId, String datasetId, int total, int accepted, int schemaFailures, int provenanceFailures, double rate, boolean passes) { this.projectId=projectId; this.datasetId=datasetId; this.total=total; this.accepted=accepted; this.schemaFailures=schemaFailures; this.provenanceFailures=provenanceFailures; this.acceptanceRate=rate; this.passes=passes; }
 @PrePersist void touch(){createdAt=Instant.now();}
 public String getDatasetId(){return datasetId;} public int getTotal(){return total;} public int getAccepted(){return accepted;} public double getAcceptanceRate(){return acceptanceRate;} public boolean isPasses(){return passes;} public Instant getCreatedAt(){return createdAt;}
}
