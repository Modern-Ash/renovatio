package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.ActionItemDto;
import org.shark.renovatio.api.entity.ActionItemEntity;
import org.shark.renovatio.api.repository.ActionItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActionItemService {
    private final ActionItemRepository actionItemRepo;

    public ActionItemService(ActionItemRepository actionItemRepo) {
        this.actionItemRepo = actionItemRepo;
    }

    public List<ActionItemDto> getActionItems(String projectId) {
        return actionItemRepo.findByProjectId(projectId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<ActionItemDto> updateStatus(String id, String status) {
        return actionItemRepo.findById(id).map(entity -> {
            entity.setReviewStatus(status);
            entity.setReviewedAt(LocalDateTime.now());
            actionItemRepo.save(entity);
            return toDto(entity);
        });
    }

    private ActionItemDto toDto(ActionItemEntity entity) {
        return ActionItemDto.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .runId(entity.getRunId())
                .severity(entity.getSeverity())
                .reason(entity.getReason())
                .requiredHumanAction(entity.getRequiredHumanAction())
                .acceptanceCondition(entity.getAcceptanceCondition())
                .reviewStatus(entity.getReviewStatus())
                .createdAt(entity.getCreatedAt())
                .reviewedAt(entity.getReviewedAt())
                .build();
    }
}
