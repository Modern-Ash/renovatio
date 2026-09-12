import type { DiagramEdgeVM, DiagramModel, DiagramNodeVM } from '@renovatio/diagram-canvas/lib/browser';
import type { DomainModel } from './renovatio-shell-widget';

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
    layout: Readonly<Record<string, { x: number; y: number }>> = {}
): DiagramModel {
    const nodes: DiagramNodeVM[] = model.nodes.map(node => ({
        id: node.id,
        kind: node.kind,
        label: node.name,
        group: node.kind,
        x: layout[node.id]?.x,
        y: layout[node.id]?.y,
        data: {
            properties: node.properties,
            confidence: node.confidence,
            origin: node.origin
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
            targetCardinality: relation.targetCardinality
        }
    }));
    return { nodes, edges };
}
