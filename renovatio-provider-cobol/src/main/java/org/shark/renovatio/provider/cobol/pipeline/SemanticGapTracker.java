package org.shark.renovatio.provider.cobol.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks semantic gaps detected during COBOL-to-Java translation.
 * Collects action items and determines if they block the pipeline.
 */
public class SemanticGapTracker {
    
    private final List<ActionItem> actionItems = new ArrayList<>();
    
    /**
     * Record a semantic gap.
     * 
     * @param actionItem The action item to record
     */
    public void recordGap(ActionItem actionItem) {
        actionItems.add(actionItem);
    }
    
    /**
     * Record multiple semantic gaps.
     * 
     * @param gaps List of action items to record
     */
    public void recordGaps(List<ActionItem> gaps) {
        actionItems.addAll(gaps);
    }
    
    /**
     * Get all recorded action items.
     * 
     * @return List of action items
     */
    public List<ActionItem> getActionItems() {
        return List.copyOf(actionItems);
    }
    
    /**
     * Get action items filtered by severity.
     * 
     * @param severity Severity level to filter by
     * @return List of action items with specified severity
     */
    public List<ActionItem> getActionItems(ActionItem.Severity severity) {
        return actionItems.stream()
            .filter(item -> item.severity() == severity)
            .toList();
    }
    
    /**
     * Check if there are any blocking gaps.
     * 
     * @return true if there are blocking gaps
     */
    public boolean hasBlockingGaps() {
        return actionItems.stream()
            .anyMatch(ActionItem::isBlocking);
    }
    
    /**
     * Get count of gaps by severity.
     * 
     * @param severity Severity level to count
     * @return Number of gaps with specified severity
     */
    public long countGaps(ActionItem.Severity severity) {
        return actionItems.stream()
            .filter(item -> item.severity() == severity)
            .count();
    }
    
    /**
     * Get total number of recorded gaps.
     * 
     * @return Total gap count
     */
    public int getTotalCount() {
        return actionItems.size();
    }
    
    /**
     * Clear all recorded action items.
     */
    public void clear() {
        actionItems.clear();
    }
    
    /**
     * Generate a summary report of all gaps.
     * 
     * @return Summary string
     */
    public String generateSummary() {
        long blocking = countGaps(ActionItem.Severity.BLOCKING);
        long warnings = countGaps(ActionItem.Severity.WARNING);
        long info = countGaps(ActionItem.Severity.INFO);
        
        return String.format(
            "Semantic Gaps Summary: %d total (%d blocking, %d warnings, %d info)",
            getTotalCount(), blocking, warnings, info
        );
    }
}
