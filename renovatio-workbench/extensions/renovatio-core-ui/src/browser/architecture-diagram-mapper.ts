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

// UML package-containment sizing. A layer renders as a package box wide
// enough for one column of component cards, tall enough for however many
// components it holds — real nesting (parentId), not a "membership" edge.
// A first attempt drew an edge from layer to each component; a real UML
// package diagram never links a package to its own members with an arrow,
// it draws them *inside* the package's border, so that approach was wrong
// and is replaced here with actual parent/child containment.
const PACKAGE_WIDTH = 260;
const PACKAGE_HEADER_HEIGHT = 44;
const PACKAGE_PADDING = 16;
const COMPONENT_WIDTH = PACKAGE_WIDTH - PACKAGE_PADDING * 2;
const COMPONENT_HEIGHT = 90;
const COMPONENT_GAP = 12;
const PACKAGE_GAP = 40;
const PACKAGE_COLUMNS = 3;

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

    // Packages fill a grid (PACKAGE_COLUMNS wide) instead of one ever-growing
    // vertical column — a single-column stack made the diagram creep far
    // down the page as layers accumulated, even though nothing was wrong
    // horizontally. Each column tracks its own cursorY so a tall package in
    // one column doesn't push unrelated columns down.
    const columnCursorY = new Array(PACKAGE_COLUMNS).fill(24);
    const layerHeights = new Map<string, number>();
    const layerNodes: DiagramNodeVM[] = layers.map((layer, index) => {
        const count = architecture.canvas.filter(node => node.layer === layer).length;
        const height = PACKAGE_HEADER_HEIGHT + PACKAGE_PADDING
            + Math.max(count, 1) * COMPONENT_HEIGHT + Math.max(count - 1, 0) * COMPONENT_GAP
            + PACKAGE_PADDING;
        const column = index % PACKAGE_COLUMNS;
        const node: DiagramNodeVM = {
            id: layerNodeId(layer),
            kind: 'ARCHITECTURE_LAYER',
            label: layer,
            group: layer,
            x: 24 + column * (PACKAGE_WIDTH + PACKAGE_GAP),
            y: columnCursorY[column],
            width: PACKAGE_WIDTH,
            height,
            data: {
                layer,
                packageName: architecture.profile.packageRoots[layer] ?? architecture.profile.packageRoots.base ?? '',
                className: architecture.profile.suffixes[layer] ?? 'Layer',
                componentCount: count,
                pendingSuggestionCount: pendingSuggestionCounts.get(layer) ?? pendingSuggestionCounts.get(layerNodeId(layer)) ?? 0,
                diagnostics: architecture.dependencyDiagnostics.filter(diagnostic => diagnostic.fromLayer === layer || diagnostic.toLayer === layer)
            }
        };
        columnCursorY[column] += height + PACKAGE_GAP;
        layerHeights.set(layer, height);
        return node;
    });

    const componentNodes: DiagramNodeVM[] = architecture.canvas.map(node => {
        const slot = nodesByLayer.get(node.layer) ?? 0;
        nodesByLayer.set(node.layer, slot + 1);
        // Contained nodes position relative to their package's own origin
        // (React Flow semantics for parentId), not the canvas origin —
        // profile.layout positions (from dragging) are relative-to-parent
        // too once a node has been dragged inside its package. A saved
        // position can predate that relative-to-parent convention (or just
        // be stale after re-layout shrank the package) and land far outside
        // the package box — reported bug: one component flying off far
        // below the diagram, unconstrained, edges stretching off-canvas.
        // Clamp to the package's own bounds rather than trust it blindly.
        const position = architecture.profile.layout?.[node.id];
        const packageHeight = layerHeights.get(node.layer) ?? Number.POSITIVE_INFINITY;
        const maxX = PACKAGE_WIDTH - PACKAGE_PADDING - COMPONENT_WIDTH;
        const maxY = packageHeight - PACKAGE_PADDING - COMPONENT_HEIGHT;
        const clampedX = position?.x !== undefined ? Math.min(Math.max(position.x, PACKAGE_PADDING), Math.max(maxX, PACKAGE_PADDING)) : undefined;
        const clampedY = position?.y !== undefined ? Math.min(Math.max(position.y, PACKAGE_HEADER_HEIGHT), Math.max(maxY, PACKAGE_HEADER_HEIGHT)) : undefined;
        return {
            id: node.id,
            kind: node.kind,
            label: node.label,
            group: node.layer,
            parentId: layerNodeId(node.layer),
            width: COMPONENT_WIDTH,
            height: COMPONENT_HEIGHT,
            x: clampedX ?? PACKAGE_PADDING,
            y: clampedY ?? PACKAGE_HEADER_HEIGHT + PACKAGE_PADDING + slot * (COMPONENT_HEIGHT + COMPONENT_GAP),
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
    // Layer (package) nodes must precede their contained components so
    // React Flow can resolve parentId on first render.
    return { nodes: [...layerNodes, ...componentNodes], edges };
}
