import type { DiagramEdgeVM, DiagramModel, DiagramNodeVM } from '@renovatio/diagram-canvas/lib/browser';
import type { ArchitectureDiagnostic, WorkbenchArchitecture } from './renovatio-shell-widget';

export interface ArchitectureDiagramSuggestion {
    targetId?: string;
    status: string;
}

const LAYER_NODE_PREFIX = 'architecture-layer:';
const DEFAULT_LAYERS = ['controller', 'service', 'model'] as const;

function layerNodeId(layer: string): string {
    return `${LAYER_NODE_PREFIX}${layer}`;
}

function collectLayers(architecture: WorkbenchArchitecture): string[] {
    return Array.from(new Set([
        ...architecture.canvas.map(node => node.layer),
        ...architecture.manifest.map(entry => entry.layer),
        ...architecture.dependencyRules.flatMap(rule => [rule.fromLayer, rule.toLayer]),
        ...Object.keys(architecture.profile.packageRoots),
        ...Object.keys(architecture.profile.suffixes),
        ...DEFAULT_LAYERS
    ].filter(Boolean)));
}

function diagnosticsFor(
    diagnostics: ArchitectureDiagnostic[],
    fromLayer: string,
    toLayer: string
): ArchitectureDiagnostic[] {
    return diagnostics.filter(diagnostic => diagnostic.fromLayer === fromLayer && diagnostic.toLayer === toLayer);
}

export function architectureToDiagram(
    architecture: WorkbenchArchitecture,
    suggestions: readonly ArchitectureDiagramSuggestion[] = []
): DiagramModel {
    const layers = collectLayers(architecture);
    const nodesByLayer = new Map<string, number>();
    const pendingSuggestionCounts = new Map<string, number>();
    suggestions
        .filter(suggestion => !['CONFIRMED', 'confirmed', 'OVERRIDDEN', 'overridden'].includes(suggestion.status))
        .forEach(suggestion => {
            if (!suggestion.targetId) return;
            pendingSuggestionCounts.set(suggestion.targetId, (pendingSuggestionCounts.get(suggestion.targetId) ?? 0) + 1);
        });
    const layerNodes: DiagramNodeVM[] = layers.map((layer, index) => {
        const count = architecture.canvas.filter(node => node.layer === layer).length;
        return {
            id: layerNodeId(layer),
            kind: 'ARCHITECTURE_LAYER',
            label: layer,
            group: layer,
            x: 24,
            y: index * 190,
            data: {
                layer,
                packageName: architecture.profile.packageRoots[layer] ?? architecture.profile.packageRoots.base ?? '',
                className: architecture.profile.suffixes[layer] ?? 'Layer',
                componentCount: count,
                pendingSuggestionCount: pendingSuggestionCounts.get(layer) ?? pendingSuggestionCounts.get(layerNodeId(layer)) ?? 0,
                diagnostics: architecture.dependencyDiagnostics.filter(diagnostic => diagnostic.fromLayer === layer || diagnostic.toLayer === layer)
            }
        };
    });
    const componentNodes: DiagramNodeVM[] = architecture.canvas.map(node => {
        const layerIndex = Math.max(0, layers.indexOf(node.layer));
        const layerSlot = nodesByLayer.get(node.layer) ?? 0;
        nodesByLayer.set(node.layer, layerSlot + 1);
        const position = architecture.profile.layout?.[node.id];
        return {
            id: node.id,
            kind: node.kind,
            label: node.label,
            group: node.layer,
            x: position?.x ?? 300 + (layerSlot % 3) * 230,
            y: position?.y ?? layerIndex * 190 + Math.floor(layerSlot / 3) * 92,
            data: {
                layer: node.layer,
                packageName: node.packageName,
                className: node.className,
                componentId: node.componentId,
                pendingSuggestionCount: pendingSuggestionCounts.get(node.id) ?? pendingSuggestionCounts.get(node.componentId) ?? pendingSuggestionCounts.get(node.layer) ?? 0,
                excluded: node.excluded ?? architecture.profile.excludedNodeIds?.some(value => value.id === node.id) ?? false,
                exclusionReason: node.exclusionReason ?? architecture.profile.excludedNodeIds?.find(value => value.id === node.id)?.reason ?? '',
                diagnostics: architecture.dependencyDiagnostics.filter(diagnostic => diagnostic.fromLayer === node.layer || diagnostic.toLayer === node.layer)
            }
        };
    });
    const edges: DiagramEdgeVM[] = architecture.dependencyRules.map((rule, index) => {
        const diagnostics = diagnosticsFor(architecture.dependencyDiagnostics, rule.fromLayer, rule.toLayer);
        return {
            id: `architecture-rule:${rule.fromLayer}:${rule.toLayer}:${index}`,
            source: layerNodeId(rule.fromLayer),
            target: layerNodeId(rule.toLayer),
            kind: rule.allowed ? 'ALLOWED_DEPENDENCY' : 'DENIED_DEPENDENCY',
            label: rule.reason,
            data: {
                allowed: rule.allowed,
                fromLayer: rule.fromLayer,
                toLayer: rule.toLayer,
                diagnostics,
                diagnosticMessage: diagnostics.map(diagnostic => diagnostic.message).join(' | ')
            }
        };
    });
    return { nodes: [...layerNodes, ...componentNodes], edges };
}
