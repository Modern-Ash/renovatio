import type { DiagramEdgeVM, DiagramModel, DiagramNodeVM } from '@renovatio/diagram-canvas/lib/browser';
import type { DomainModel } from './renovatio-shell-widget';

export interface DomainDiagramSuggestion {
    targetType: string;
    targetId: string;
    status: string;
}

/**
 * Maps the real DomainModel (nodes/relations, business-domain shaped) to the
 * canvas-agnostic DiagramModel view-model. This is the only place that
 * knows both shapes — @renovatio/diagram-canvas itself never imports
 * DomainModel (see issue #264's contract tests), and the reverse mapping
 * (DiagramEvent -> a DomainModel mutation) lives in
 * RenovatioShellWidget#handleDomainDiagramEvent, not here, so this stays a
 * pure, independently testable function.
 */
export function domainModelToDiagram(
    model: DomainModel,
    layout: Readonly<Record<string, { x: number; y: number }>> = {},
    suggestions: readonly DomainDiagramSuggestion[] = []
): DiagramModel {
    const excluded = new Map((model.excludedNodeIds ?? []).map(value => [value.id, value.reason]));
    const pendingSuggestionCounts = new Map<string, number>();
    suggestions
        .filter(suggestion => suggestion.targetType === 'node' && suggestion.status === 'pending')
        .forEach(suggestion => pendingSuggestionCounts.set(suggestion.targetId, (pendingSuggestionCounts.get(suggestion.targetId) ?? 0) + 1));
    const nodes: DiagramNodeVM[] = model.nodes.map(node => ({
        id: node.id,
        kind: node.kind,
        label: node.name,
        group: node.kind,
        x: model.layout?.[node.id]?.x ?? layout[node.id]?.x,
        y: model.layout?.[node.id]?.y ?? layout[node.id]?.y,
        data: {
            properties: node.properties,
            tableName: node.tableName,
            sourceDataset: node.sourceDataset,
            confidence: node.confidence,
            origin: node.origin,
            pendingSuggestionCount: pendingSuggestionCounts.get(node.id) ?? 0,
            excluded: excluded.has(node.id),
            exclusionReason: excluded.get(node.id) ?? ''
        }
    }));
    const edges: DiagramEdgeVM[] = model.relations.map(relation => ({
        id: relation.id,
        source: relation.fromId,
        target: relation.toId,
        kind: relation.kind,
        label: `${relation.kind} (${relation.sourceCardinality} → ${relation.targetCardinality})`,
        data: {
            sourceCardinality: relation.sourceCardinality,
            targetCardinality: relation.targetCardinality,
            foreignKey: relation.foreignKey
        }
    }));
    return { nodes, edges };
}
