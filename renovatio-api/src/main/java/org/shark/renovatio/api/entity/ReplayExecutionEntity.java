package org.shark.renovatio.api.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="replay_executions")
public class ReplayExecutionEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String projectId; @Column(nullable=false) private String caseId;
 @Lob @Column(nullable=false) private String fixtureJson; @Column(nullable=false) private boolean equivalent; @Column(nullable=false) private Instant createdAt;
 protected ReplayExecutionEntity(){}
 public ReplayExecutionEntity(String projectId,String caseId,String fixtureJson,boolean equivalent){this.projectId=projectId;this.caseId=caseId;this.fixtureJson=fixtureJson;this.equivalent=equivalent;}
 @PrePersist void touch(){createdAt=Instant.now();}
 public String getCaseId(){return caseId;} public boolean isEquivalent(){return equivalent;} public Instant getCreatedAt(){return createdAt;}
}
