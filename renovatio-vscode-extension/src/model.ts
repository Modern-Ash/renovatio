import type { DiagramEdgeVM, DiagramEvent, DiagramModel, DiagramNodeVM } from '@renovatio/diagram-canvas/lib/browser';

export type DocumentKind = 'domain' | 'architecture' | 'diagram';

export interface ParsedDiagramDocument {
    kind: DocumentKind;
    raw: Record<string, any>;
    model: DiagramModel;
}

type RenovatioDiagramEvent = DiagramEvent | {
    type: 'layoutChanged';
    positions: Record<string, { x: number; y: number }>;
};

const LAYER_NODE_PREFIX = 'architecture-layer:';
const DEFAULT_LAYERS = ['controller', 'service', 'model'];

export function parseDiagramDocument(text: string, fileName: string): ParsedDiagramDocument {
    const raw = parseJsonObject(text);
    const kind = detectKind(raw, fileName);
    return { kind, raw, model: toDiagramModel(kind, raw) };
}

export function applyDiagramEvent(
    parsed: ParsedDiagramDocument,
    event: RenovatioDiagramEvent
): ParsedDiagramDocument {
    const raw = structuredClone(parsed.raw);
    if (event.type === 'nodeMoved') {
        applyNodeMoved(parsed.kind, raw, event.id, event.x, event.y);
    } else if (event.type === 'layoutChanged') {
        applyLayoutChanged(parsed.kind, raw, event.positions);
    } else if (event.type === 'nodesPruned') {
        applyNodesPruned(parsed.kind, raw, event.ids);
    } else if (event.type === 'edgeCreated') {
        applyEdgeCreated(parsed.kind, raw, event.source, event.target);
    }
    return { kind: parsed.kind, raw, model: toDiagramModel(parsed.kind, raw) };
}

export function formatDiagramDocument(raw: Record<string, any>): string {
    return `${JSON.stringify(raw, null, 2)}\n`;
}

function parseJsonObject(text: string): Record<string, any> {
    if (!text.trim()) {
        return {};
    }
    const parsed = JSON.parse(text);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        throw new Error('Renovatio diagram files must contain a JSON object.');
    }
    return parsed;
}

function detectKind(raw: Record<string, any>, fileName: string): DocumentKind {
    if (fileName.endsWith('.renovatio-domain.json')) {
        return 'domain';
    }
    if (fileName.endsWith('.renovatio-arch.json')) {
        return 'architecture';
    }
    if (Array.isArray(raw.nodes) && Array.isArray(raw.edges)) {
        return 'diagram';
    }
    if (Array.isArray(raw.nodes) && Array.isArray(raw.relations)) {
        return 'domain';
    }
    if (Array.isArray(raw.canvas) || raw.profile?.dependencyRules) {
        return 'architecture';
    }
    return 'diagram';
}

function toDiagramModel(kind: DocumentKind, raw: Record<string, any>): DiagramModel {
    if (kind === 'domain') {
        return domainToDiagram(raw);
    }
    if (kind === 'architecture') {
        return architectureToDiagram(raw);
    }
    return plainDiagram(raw);
}

function domainToDiagram(raw: Record<string, any>): DiagramModel {
    const excluded = new Map((raw.excludedNodeIds ?? []).map((value: any) => [String(value.id), value.reason]));
    const nodes: DiagramNodeVM[] = safeArray(raw.nodes).map((node: any, index: number) => {
        const id = String(node.id ?? `domain-node:${index}`);
        return {
            id,
            kind: String(node.kind ?? 'ENTITY'),
            label: String(node.name ?? node.label ?? id),
            group: String(node.kind ?? 'ENTITY'),
            x: numberOrUndefined(raw.layout?.[id]?.x ?? node.x),
            y: numberOrUndefined(raw.layout?.[id]?.y ?? node.y),
            data: {
                properties: safeArray(node.properties),
                confidence: node.confidence,
                origin: node.origin,
                excluded: excluded.has(id),
                exclusionReason: excluded.get(id) ?? ''
            }
        };
    });
    const edges: DiagramEdgeVM[] = safeArray(raw.relations).map((relation: any, index: number) => ({
        id: String(relation.id ?? `domain-relation:${index}`),
        source: String(relation.fromId),
        target: String(relation.toId),
        kind: String(relation.kind ?? 'ASSOCIATES_WITH'),
        label: `${relation.kind ?? 'ASSOCIATES_WITH'} (${relation.sourceCardinality ?? 'ONE'} -> ${relation.targetCardinality ?? 'ONE'})`,
        data: {
            sourceCardinality: relation.sourceCardinality,
            targetCardinality: relation.targetCardinality,
            foreignKey: relation.foreignKey
        }
    })).filter(edge => edge.source && edge.target);
    return { nodes, edges };
}

function architectureToDiagram(raw: Record<string, any>): DiagramModel {
    const profile = raw.profile ?? {};
    const canvas = safeArray(raw.canvas);
    const rules = safeArray(raw.dependencyRules ?? profile.dependencyRules);
    const diagnostics = safeArray(raw.dependencyDiagnostics);
    const layers = collectLayers(canvas, rules, profile);
    const layerSlots = new Map<string, number>();
    const excluded = new Map(safeArray(profile.excludedNodeIds ?? raw.excludedNodeIds).map((value: any) => [String(value.id), value.reason]));

    const layerNodes: DiagramNodeVM[] = layers.map((layer, index) => {
        const id = layerNodeId(layer);
        return {
            id,
            kind: 'ARCHITECTURE_LAYER',
            label: layer,
            group: layer,
            x: numberOrUndefined(profile.layout?.[id]?.x ?? raw.layout?.[id]?.x) ?? 24,
            y: numberOrUndefined(profile.layout?.[id]?.y ?? raw.layout?.[id]?.y) ?? index * 190,
            data: {
                layer,
                packageName: profile.packageRoots?.[layer] ?? profile.packageRoots?.base ?? '',
                className: profile.suffixes?.[layer] ?? 'Layer',
                componentCount: canvas.filter((node: any) => node.layer === layer).length,
                diagnostics: diagnostics.filter((diagnostic: any) => diagnostic.fromLayer === layer || diagnostic.toLayer === layer)
            }
        };
    });

    const componentNodes: DiagramNodeVM[] = canvas.map((node: any, index: number) => {
        const id = String(node.id ?? `architecture-node:${index}`);
        const layer = String(node.layer ?? 'model');
        const layerIndex = Math.max(0, layers.indexOf(layer));
        const slot = layerSlots.get(layer) ?? 0;
        layerSlots.set(layer, slot + 1);
        return {
            id,
            kind: String(node.kind ?? 'COMPONENT'),
            label: String(node.label ?? node.className ?? id),
            group: layer,
            x: numberOrUndefined(profile.layout?.[id]?.x ?? raw.layout?.[id]?.x) ?? 300 + (slot % 3) * 230,
            y: numberOrUndefined(profile.layout?.[id]?.y ?? raw.layout?.[id]?.y) ?? layerIndex * 190 + Math.floor(slot / 3) * 92,
            data: {
                layer,
                packageName: node.packageName,
                className: node.className,
                componentId: node.componentId,
                excluded: excluded.has(id),
                exclusionReason: excluded.get(id) ?? '',
                diagnostics: diagnostics.filter((diagnostic: any) => diagnostic.fromLayer === layer || diagnostic.toLayer === layer)
            }
        };
    });

    const edges: DiagramEdgeVM[] = rules.map((rule: any, index: number) => ({
        id: `architecture-rule:${rule.fromLayer}:${rule.toLayer}:${index}`,
        source: layerNodeId(String(rule.fromLayer)),
        target: layerNodeId(String(rule.toLayer)),
        kind: rule.allowed ? 'ALLOWED_DEPENDENCY' : 'DENIED_DEPENDENCY',
        label: String(rule.reason ?? ''),
        data: {
            allowed: Boolean(rule.allowed),
            fromLayer: rule.fromLayer,
            toLayer: rule.toLayer
        }
    })).filter(edge => edge.source && edge.target);

    return { nodes: [...layerNodes, ...componentNodes], edges };
}

function plainDiagram(raw: Record<string, any>): DiagramModel {
    return {
        nodes: safeArray(raw.nodes).map((node: any, index: number) => ({
            id: String(node.id ?? `node:${index}`),
            kind: String(node.kind ?? 'NODE'),
            label: String(node.label ?? node.id ?? `Node ${index + 1}`),
            group: node.group,
            x: numberOrUndefined(node.x),
            y: numberOrUndefined(node.y),
            data: node.data
        })),
        edges: safeArray(raw.edges).map((edge: any, index: number) => ({
            id: String(edge.id ?? `edge:${index}`),
            source: String(edge.source),
            target: String(edge.target),
            kind: String(edge.kind ?? 'EDGE'),
            label: edge.label,
            data: edge.data
        })).filter(edge => edge.source && edge.target)
    };
}

function applyNodeMoved(kind: DocumentKind, raw: Record<string, any>, id: string, x: number, y: number): void {
    if (id.startsWith(LAYER_NODE_PREFIX) && kind !== 'architecture') {
        return;
    }
    if (kind === 'domain') {
        raw.layout = raw.layout ?? {};
        raw.layout[id] = { x, y };
        return;
    }
    if (kind === 'architecture') {
        raw.profile = raw.profile ?? {};
        raw.profile.layout = raw.profile.layout ?? {};
        raw.profile.layout[id] = { x, y };
        return;
    }
    const node = safeArray(raw.nodes).find((candidate: any) => candidate.id === id);
    if (node) {
        node.x = x;
        node.y = y;
    }
}

function applyLayoutChanged(kind: DocumentKind, raw: Record<string, any>, positions: Record<string, { x: number; y: number }>): void {
    for (const [id, position] of Object.entries(positions ?? {})) {
        const x = numberOrUndefined(position?.x);
        const y = numberOrUndefined(position?.y);
        if (x === undefined || y === undefined) {
            continue;
        }
        applyNodeMoved(kind, raw, id, x, y);
    }
}

function applyNodesPruned(kind: DocumentKind, raw: Record<string, any>, ids: string[]): void {
    const prunableIds = ids.filter(id => !id.startsWith(LAYER_NODE_PREFIX));
    if (!prunableIds.length) {
        return;
    }
    const target = kind === 'architecture' ? (raw.profile = raw.profile ?? {}) : raw;
    target.excludedNodeIds = safeArray(target.excludedNodeIds);
    const existing = new Set(target.excludedNodeIds.map((value: any) => String(value.id)));
    for (const id of prunableIds) {
        if (!existing.has(id)) {
            target.excludedNodeIds.push({ id, reason: 'Excluded from VS Code custom editor' });
        }
    }
}

function applyEdgeCreated(kind: DocumentKind, raw: Record<string, any>, source: string, target: string): void {
    if (kind !== 'domain') {
        return;
    }
    raw.relations = safeArray(raw.relations);
    raw.relations.push({
        id: `relation:${source}:${target}:${Date.now()}`,
        fromId: source,
        toId: target,
        kind: 'ASSOCIATES_WITH',
        sourceCardinality: 'ONE',
        targetCardinality: 'ONE'
    });
}

function collectLayers(canvas: any[], rules: any[], profile: Record<string, any>): string[] {
    return Array.from(new Set([
        ...canvas.map(node => node.layer),
        ...rules.flatMap(rule => [rule.fromLayer, rule.toLayer]),
        ...Object.keys(profile.packageRoots ?? {}),
        ...Object.keys(profile.suffixes ?? {}),
        ...DEFAULT_LAYERS
    ].filter(Boolean).map(String)));
}

function layerNodeId(layer: string): string {
    return `${LAYER_NODE_PREFIX}${layer}`;
}

function safeArray(value: unknown): any[] {
    return Array.isArray(value) ? value : [];
}

function numberOrUndefined(value: unknown): number | undefined {
    return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}
