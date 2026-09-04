package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.api.entity.ProjectDomainModelEntity;
import org.shark.renovatio.api.repository.ProjectDomainModelRepository;
import org.shark.renovatio.domain.model.DomainModel;
import org.springframework.stereotype.Service;

@Service
public class ProjectDomainModelService {
    private final ProjectDomainModelRepository repository; private final ObjectMapper mapper;
    public ProjectDomainModelService(ProjectDomainModelRepository repository, ObjectMapper mapper) { this.repository = repository; this.mapper = mapper; }
    public DomainModel save(String projectId, DomainModel model, boolean confirm) {
        try {
            String json = mapper.writeValueAsString(model);
            ProjectDomainModelEntity entity = repository.findById(projectId).orElseGet(() -> new ProjectDomainModelEntity(projectId, model.schemaVersion(), model.canonicalHash(), json));
            entity.replace(model.canonicalHash(), json); if (confirm) entity.confirm(); repository.save(entity); return model;
        } catch (Exception error) { throw new IllegalStateException("Unable to persist DomainModel", error); }
    }
    public DomainModel get(String projectId) {
        try {
            var entity = repository.findById(projectId).orElse(null);
            return entity == null ? null : mapper.readValue(entity.getModelJson(), DomainModel.class);
        }
        catch (Exception error) { throw new IllegalStateException("Unable to read DomainModel", error); }
    }
}
