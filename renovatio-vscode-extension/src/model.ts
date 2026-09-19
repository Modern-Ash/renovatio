import type { DiagramEdgeVM, DiagramEvent, DiagramModel, DiagramNodeVM } from '@renovatio/diagram-canvas/lib/browser';

export type DocumentKind = 'domain' | 'persistence' | 'architecture' | 'diagram';
export type ArchitectureStyle = 'LAYERED_MVC' | 'HEXAGONAL';

export interface ParsedDiagramDocument {
    kind: DocumentKind;
    raw: Record<string, any>;
    model: DiagramModel;
}

type RenovatioDiagramEvent = DiagramEvent | {
    type: 'layoutChanged';
    positions: Record<string, { x: number; y: number }>;
} | {
    type: 'architectureStyleChanged';
    style: ArchitectureStyle;
};

const LAYER_NODE_PREFIX = 'architecture-layer:';
const MVC_LAYERS = ['controller', 'service', 'model', 'persistence'];
const HEXAGONAL_LAYERS = ['inbound-adapter', 'inbound-port', 'application', 'domain', 'outbound-port', 'outbound-adapter'];
const DEFAULT_LAYERS = MVC_LAYERS;
const PACKAGE_WIDTH = 300;
const PACKAGE_HEADER_HEIGHT = 48;
const PACKAGE_PADDING = 18;
const COMPONENT_WIDTH = PACKAGE_WIDTH - PACKAGE_PADDING * 2;
const COMPONENT_HEIGHT = 104;
const COMPONENT_GAP = 14;
const PACKAGE_GAP = 48;

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
    } else if (event.type === 'edgeReconnected') {
        applyEdgeReconnected(parsed.kind, raw, event.id, event.source, event.target);
    } else if (event.type === 'edgeLabelChanged') {
        applyEdgeLabelChanged(parsed.kind, raw, event.id, event.label);
    } else if (event.type === 'edgesDeleted') {
        applyEdgesDeleted(parsed.kind, raw, event.ids);
    } else if (event.type === 'architectureStyleChanged') {
        applyArchitectureStyleChanged(parsed.kind, raw, event.style);
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
    if (raw.diagramKind === 'persistence' || raw.projection === 'persistence' || fileName.includes('persistence')) {
        return 'persistence';
    }
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
    if (kind === 'domain' || kind === 'persistence') {
        return domainToDiagram(raw);
    }
    if (kind === 'architecture') {
        return architectureToDiagram(raw);
    }
    return plainDiagram(raw);
}

function normalizeConceptName(value: unknown): string {
    return String(value ?? "").trim().toUpperCase().replace(/[^A-Z0-9]/g, "");
}

function domainDisplayName(value: unknown, kind: unknown): string {
    const raw = String(value ?? "").trim();
    const normalizedKind = String(kind ?? "").toUpperCase();
    if (!raw || normalizedKind === "USE_CASE" || normalizedKind === "DOMAIN_SERVICE" || normalizedKind === "SERVICE") {
        return raw;
    }
    const cleaned = raw
        .replace(/^DB2\s+table\s+/i, "")
        .replace(/^file\s+record\s+/i, "")
        .replace(/^(FD|WS|LS)-/i, "")
        .replace(/-(FD|WS|LS)$/i, "")
        .replace(/-(FILE|RECORD|REC|TABLE|DATA|IN|OUT|INPUT|OUTPUT)$/ig, "")
        .replace(/^(FILE|RECORD|REC|TABLE|DATA|IN|OUT|INPUT|OUTPUT)-/ig, "")
        .replace(/-{2,}/g, "-")
        .replace(/^-|-$/g, "")
        .trim();
    return titleCaseDomainName(cleaned || raw);
}

function titleCaseDomainName(value: string): string {
    return value
        .split(/[-_\s]+/)
        .filter(Boolean)
        .map(token => {
            const lower = token.toLowerCase();
            if (/^[A-Z0-9]{2,}$/.test(token) && token.length <= 4) return token;
            return lower.charAt(0).toUpperCase() + lower.slice(1);
        })
        .join(" ");
}

function mergeNodeProperties(props: any[]): any[] {
    const seen = new Map<string, any>();
    for (const p of props) {
        if (!p || !p.name) continue;
        const key = String(p.name).trim().toUpperCase();
        if (!seen.has(key)) {
            seen.set(key, { ...p });
        } else {
            const existing = seen.get(key);
            if (p.isKey) existing.isKey = true;
            if (p.isForeignKey) existing.isForeignKey = true;
            if (!existing.type && p.type) existing.type = p.type;
        }
    }
    return Array.from(seen.values());
}

function domainToDiagram(raw: Record<string, any>): DiagramModel {
    const excluded = new Map((raw.excludedNodeIds ?? []).map((value: any) => [String(value.id), value.reason]));
    const aliasMap = new Map<string, string>();
    const bySemanticKey = new Map<string, any>();

    const canonicalRawNodes: any[] = [];
    for (const rawNode of safeArray(raw.nodes)) {
        if (!rawNode?.id) continue;
        const kind = String(rawNode.kind ?? "ENTITY").toUpperCase();
        if (kind === "AGGREGATE" || kind === "EXTERNAL_SYSTEM") continue;
        const name = String(rawNode.name ?? rawNode.label ?? rawNode.id).trim();
        const isSharedConcept = kind === "REPOSITORY" || kind === "ENTITY" || kind === "VALUE_OBJECT";
        const semanticKey = isSharedConcept ? `${kind}:${normalizeConceptName(name)}` : `id:${rawNode.id}`;

        const existing = bySemanticKey.get(semanticKey);
        if (!existing) {
            const copy = { ...rawNode, name, properties: safeArray(rawNode.properties) };
            bySemanticKey.set(semanticKey, copy);
            aliasMap.set(String(rawNode.id), String(copy.id));
            canonicalRawNodes.push(copy);
        } else {
            aliasMap.set(String(rawNode.id), String(existing.id));
            existing.properties = mergeNodeProperties([...existing.properties, ...safeArray(rawNode.properties)]);
            if (rawNode.tableName && !existing.tableName) existing.tableName = rawNode.tableName;
            if (rawNode.sourceDataset && !existing.sourceDataset) existing.sourceDataset = rawNode.sourceDataset;
            if (typeof rawNode.confidence === "number") {
                existing.confidence = Math.max(Number(existing.confidence ?? 0), rawNode.confidence);
            }
        }
    }

    const foreignKeys = new Map<string, Set<string>>();
    safeArray(raw.relations).forEach((relation: any) => {
        const property = relation.foreignKey?.property;
        const fromId = aliasMap.get(String(relation.fromId)) ?? String(relation.fromId);
        if (!property || !fromId) return;
        const set = foreignKeys.get(fromId) ?? new Set<string>();
        set.add(String(property));
        foreignKeys.set(fromId, set);
    });

    const nodes: DiagramNodeVM[] = canonicalRawNodes.map((node: any, index: number) => {
        const id = String(node.id ?? `domain-node:${index}`);
        const nodeForeignKeys = foreignKeys.get(id) ?? new Set<string>();
        return {
            id,
            kind: String(node.kind ?? "ENTITY"),
            label: domainDisplayName(node.name ?? node.label ?? id, node.kind),
            group: String(node.kind ?? "ENTITY"),
            x: numberOrUndefined(raw.layout?.[id]?.x ?? node.x),
            y: numberOrUndefined(raw.layout?.[id]?.y ?? node.y),
            data: {
                diagramKind: raw.diagramKind ?? raw.projection,
                properties: safeArray(node.properties).map((property: any) => ({
                    ...property,
                    isForeignKey: nodeForeignKeys.has(String(property.name))
                })),
                tableName: node.tableName,
                sourceDataset: node.sourceDataset,
                confidence: node.confidence,
                origin: node.origin,
                physicalName: node.physicalName ?? node.name,
                excluded: excluded.has(id),
                exclusionReason: excluded.get(id) ?? ""
            }
        };
    });

    const seenEdges = new Set<string>();
    const edges: DiagramEdgeVM[] = [];
    safeArray(raw.relations).forEach((relation: any, index: number) => {
        const source = aliasMap.get(String(relation.fromId)) ?? String(relation.fromId);
        const target = aliasMap.get(String(relation.toId)) ?? String(relation.toId);
        if (!source || !target || source === target) return;
        const edgeKey = `${source}->${target}:${relation.kind ?? ""}`;
        if (seenEdges.has(edgeKey)) return;
        seenEdges.add(edgeKey);

        edges.push({
            id: String(relation.id ?? `domain-relation:${index}`),
            source,
            target,
            kind: String(relation.kind ?? "ASSOCIATES_WITH"),
            label: relation.label
                ? String(relation.label)
                : relation.foreignKey?.property
                    ? String(relation.foreignKey.property)
                    : undefined,
            data: {
                associationLabel: relation.foreignKey
                    ? `${relation.foreignKey.property} -> ${relation.foreignKey.referencesProperty}`
                    : relation.kind,
                sourceCardinality: relation.sourceCardinality,
                targetCardinality: relation.targetCardinality,
                foreignKey: relation.foreignKey
            }
        });
    });

    return { nodes, edges };
}

function architectureToDiagram(raw: Record<string, any>): DiagramModel {
    const profile = raw.profile ?? {};
    const style = architectureStyle(profile.style ?? raw.style);
    const canvas = safeArray(raw.canvas);
    const rules = safeArray(raw.dependencyRules ?? profile.dependencyRules);
    const diagnostics = safeArray(raw.dependencyDiagnostics);
    const layers = collectLayers(canvas, rules, profile, style);
    const layerSlots = new Map<string, number>();
    const excluded = new Map(safeArray(profile.excludedNodeIds ?? raw.excludedNodeIds).map((value: any) => [String(value.id), value.reason]));

    // architecturePackagePosition used to hand back fixed coordinates per
    // layer (a hexagon-shaped layout for HEXAGONAL, a single fixed row for
    // MVC) regardless of how tall a package actually grew — fine for a
    // handful of components, but once the native canvas's component caps
    // were removed (#280), a layer with dozens of components dwarfs its
    // fixed slot and overlaps its neighbors, reading as "muy disperso."
    // Packages fill a 3-column grid instead, each column tracking its own
    // cumulative Y so a tall package only pushes down its own column —
    // the same fix already applied to the Workbench's own architecture
    // mapper for the identical symptom.
    const PACKAGE_COLUMNS = 3;
    const columnCursorY = new Array(PACKAGE_COLUMNS).fill(24);
    const layerNodes: DiagramNodeVM[] = layers.map((layer, index) => {
        const id = layerNodeId(layer);
        const count = canvas.filter((node: any) => architectureVisualLayer(node, style) === layer).length;
        const height = PACKAGE_HEADER_HEIGHT + PACKAGE_PADDING
            + Math.max(count, 1) * COMPONENT_HEIGHT + Math.max(count - 1, 0) * COMPONENT_GAP
            + PACKAGE_PADDING;
        const column = index % PACKAGE_COLUMNS;
        const defaultX = 24 + column * (PACKAGE_WIDTH + PACKAGE_GAP);
        const defaultY = columnCursorY[column];
        const node: DiagramNodeVM = {
            id,
            kind: 'ARCHITECTURE_LAYER',
            label: layer,
            group: layer,
            x: numberOrUndefined(profile.layout?.[id]?.x ?? raw.layout?.[id]?.x) ?? defaultX,
            y: numberOrUndefined(profile.layout?.[id]?.y ?? raw.layout?.[id]?.y) ?? defaultY,
            width: PACKAGE_WIDTH,
            height,
            data: {
                architectureStyle: style,
                layer,
                layerRole: architectureLayerRole(layer, style),
                packageName: profile.packageRoots?.[layer] ?? profile.packageRoots?.base ?? '',
                className: profile.suffixes?.[layer] ?? 'Layer',
                componentCount: count,
                diagnostics: diagnostics.filter((diagnostic: any) => diagnostic.fromLayer === layer || diagnostic.toLayer === layer)
            }
        };
        columnCursorY[column] += height + PACKAGE_GAP;
        return node;
    });

    const componentNodes: DiagramNodeVM[] = canvas.map((node: any, index: number) => {
        const id = String(node.id ?? `architecture-node:${index}`);
        const layer = architectureVisualLayer(node, style);
        const slot = layerSlots.get(layer) ?? 0;
        layerSlots.set(layer, slot + 1);
        const stored = {
            x: numberOrUndefined(profile.layout?.[id]?.x ?? raw.layout?.[id]?.x),
            y: numberOrUndefined(profile.layout?.[id]?.y ?? raw.layout?.[id]?.y)
        };
        const storedLooksRelative = stored.x !== undefined && stored.y !== undefined
            && stored.x >= 0 && stored.x <= PACKAGE_WIDTH - COMPONENT_WIDTH
            && stored.y >= PACKAGE_HEADER_HEIGHT && stored.y <= 2000;
        return {
            id,
            kind: String(node.kind ?? 'COMPONENT'),
            label: String(node.label ?? node.className ?? id),
            group: layer,
            parentId: layerNodeId(layer),
            width: COMPONENT_WIDTH,
            height: COMPONENT_HEIGHT,
            x: storedLooksRelative ? stored.x : PACKAGE_PADDING,
            y: storedLooksRelative ? stored.y : PACKAGE_HEADER_HEIGHT + PACKAGE_PADDING + slot * (COMPONENT_HEIGHT + COMPONENT_GAP),
            data: {
                layer,
                packageName: node.packageName,
                className: node.className,
                componentId: node.componentId,
                architectureStyle: style,
                layerRole: architectureLayerRole(layer, style),
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
    if (kind === 'domain' || kind === 'persistence') {
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
    if (kind !== 'domain' && kind !== 'persistence') {
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

function applyEdgeReconnected(kind: DocumentKind, raw: Record<string, any>, id: string, source: string, target: string): void {
    if (kind !== 'domain' && kind !== 'persistence') {
        return;
    }
    const relation = safeArray(raw.relations).find((value: any) => String(value.id) === id);
    if (!relation) {
        return;
    }
    relation.fromId = source;
    relation.toId = target;
}

function applyEdgeLabelChanged(kind: DocumentKind, raw: Record<string, any>, id: string, label: string): void {
    if (kind !== 'domain' && kind !== 'persistence') {
        return;
    }
    const relation = safeArray(raw.relations).find((value: any) => String(value.id) === id);
    if (!relation) {
        return;
    }
    relation.label = label;
}

function applyEdgesDeleted(kind: DocumentKind, raw: Record<string, any>, ids: string[]): void {
    if (kind !== 'domain' && kind !== 'persistence') {
        return;
    }
    const idSet = new Set(ids);
    raw.relations = safeArray(raw.relations).filter((relation: any) => !idSet.has(String(relation.id)));
}

function applyArchitectureStyleChanged(kind: DocumentKind, raw: Record<string, any>, style: ArchitectureStyle): void {
    if (kind !== 'architecture') {
        return;
    }
    const next = architectureStyle(style);
    raw.profile = raw.profile ?? {};
    const basePackage = inferBasePackage(raw.profile.packageRoots);
    raw.profile.style = next;
    raw.profile.packageRoots = architecturePackageRoots(next, basePackage);
    raw.profile.suffixes = architectureSuffixes(next);
    raw.profile.dependencyRules = architectureDependencyRules(next);
    delete raw.profile.layout;
}

function collectLayers(canvas: any[], rules: any[], profile: Record<string, any>, style: ArchitectureStyle): string[] {
    const preferred = architectureLayerOrder(style);
    const discovered = Array.from(new Set([
        ...canvas.map(node => architectureVisualLayer(node, style)),
        ...rules.flatMap(rule => [rule.fromLayer, rule.toLayer]),
        ...Object.keys(profile.packageRoots ?? {}),
        ...Object.keys(profile.suffixes ?? {}),
        ...preferred
    ].filter(Boolean).map(String)));
    return [
        ...preferred.filter(layer => discovered.includes(layer)),
        ...discovered.filter(layer => !preferred.includes(layer))
    ];
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

function architectureStyle(value: unknown): ArchitectureStyle {
    return value === 'HEXAGONAL' ? 'HEXAGONAL' : 'LAYERED_MVC';
}

function architectureLayerOrder(style: ArchitectureStyle): string[] {
    return style === 'HEXAGONAL'
        ? HEXAGONAL_LAYERS
        : MVC_LAYERS;
}


function architectureLayerRole(layer: string, style: ArchitectureStyle): string {
    if (style === 'HEXAGONAL') {
        const roles: Record<string, string> = {
            'inbound-adapter': 'Driving adapter',
            'inbound-port': 'Inbound port',
            application: 'Use case / application service',
            domain: 'Domain model',
            'outbound-port': 'Outbound port',
            'outbound-adapter': 'Driven adapter'
        };
        return roles[layer] ?? layer;
    }
    const roles: Record<string, string> = {
        controller: 'Controller layer',
        service: 'Service layer',
        model: 'Model layer',
        persistence: 'Persistence layer'
    };
    return roles[layer] ?? layer;
}

function inferBasePackage(packageRoots: Record<string, string> | undefined): string {
    const roots = Object.values(packageRoots ?? {}).filter(Boolean).map(String);
    const first = roots[0] ?? 'com.example.modernized';
    return first
        .replace(/\.(adapter\.(in|out\.persistence)|port\.(in|out)|application|domain|controller|service|model|persistence)$/, '')
        .replace(/\.(adapter|out)$/, '');
}

function architecturePackageRoots(style: ArchitectureStyle, basePackage: string): Record<string, string> {
    if (style === 'HEXAGONAL') {
        return {
            'inbound-adapter': `${basePackage}.adapter.in`,
            'inbound-port': `${basePackage}.port.in`,
            application: `${basePackage}.application`,
            domain: `${basePackage}.domain`,
            'outbound-port': `${basePackage}.port.out`,
            'outbound-adapter': `${basePackage}.adapter.out.persistence`
        };
    }
    return {
        controller: `${basePackage}.controller`,
        service: `${basePackage}.service`,
        model: `${basePackage}.model`,
        persistence: `${basePackage}.persistence`
    };
}

function architectureSuffixes(style: ArchitectureStyle): Record<string, string> {
    return style === 'HEXAGONAL'
        ? {
            'inbound-adapter': 'Controller',
            'inbound-port': 'Port',
            application: 'UseCase',
            domain: '',
            'outbound-port': 'Port',
            'outbound-adapter': 'Adapter'
        }
        : { controller: 'Controller', service: 'Service', model: '', persistence: 'Repository' };
}

function architectureDependencyRules(style: ArchitectureStyle): Array<{ fromLayer: string; toLayer: string; allowed: boolean; reason: string }> {
    if (style === 'HEXAGONAL') {
        return [
            { fromLayer: 'inbound-adapter', toLayer: 'inbound-port', allowed: true, reason: 'Driving adapters call inbound ports.' },
            { fromLayer: 'inbound-port', toLayer: 'application', allowed: true, reason: 'Inbound ports expose application use cases.' },
            { fromLayer: 'application', toLayer: 'domain', allowed: true, reason: 'Use cases coordinate domain behavior.' },
            { fromLayer: 'application', toLayer: 'outbound-port', allowed: true, reason: 'Application core depends on outbound ports.' },
            { fromLayer: 'outbound-adapter', toLayer: 'outbound-port', allowed: true, reason: 'Driven adapters implement outbound ports.' },
            { fromLayer: 'domain', toLayer: 'inbound-adapter', allowed: false, reason: 'Domain must not depend on driving adapters.' },
            { fromLayer: 'domain', toLayer: 'outbound-adapter', allowed: false, reason: 'Domain must not depend on driven adapters.' },
            { fromLayer: 'application', toLayer: 'outbound-adapter', allowed: false, reason: 'Application core talks to ports, not adapter implementations.' }
        ];
    }
    return [
        { fromLayer: 'controller', toLayer: 'service', allowed: true, reason: 'Controllers call services.' },
        { fromLayer: 'service', toLayer: 'model', allowed: true, reason: 'Services own model orchestration.' },
        { fromLayer: 'service', toLayer: 'persistence', allowed: true, reason: 'Services call persistence repositories.' },
        { fromLayer: 'model', toLayer: 'controller', allowed: false, reason: 'Model must not depend on controllers.' },
        { fromLayer: 'persistence', toLayer: 'controller', allowed: false, reason: 'Persistence must not depend on controllers.' }
    ];
}

function architectureVisualLayer(node: any, style: ArchitectureStyle): string {
    const rawLayer = String(node?.layer ?? '').toLowerCase();
    if (style !== 'HEXAGONAL') {
        if (rawLayer === 'application') return 'service';
        if (rawLayer === 'domain') return 'model';
        if (rawLayer === 'outbound-adapter') return 'persistence';
        if (rawLayer === 'inbound-adapter') return 'controller';
        return rawLayer || 'model';
    }
    if (HEXAGONAL_LAYERS.includes(rawLayer)) {
        return rawLayer;
    }
    const kind = String(node?.kind ?? '').toUpperCase();
    const text = `${node?.label ?? ''} ${node?.className ?? ''} ${node?.packageName ?? ''} ${node?.componentId ?? ''}`.toLowerCase();
    if (kind === 'INBOUND_PORT' || text.includes('inbound port') || text.includes('.port.in')) return 'inbound-port';
    if (kind === 'OUTBOUND_PORT' || text.includes('outbound port') || text.includes('.port.out')) return 'outbound-port';
    if (kind === 'USE_CASE' || kind === 'SERVICE' || rawLayer === 'service' || rawLayer === 'application') return 'application';
    if (kind === 'ENTITY' || kind === 'VALUE' || kind === 'VALUE_OBJECT' || rawLayer === 'model' || rawLayer === 'domain') return 'domain';
    if (kind === 'ADAPTER' || kind === 'COMPONENT') {
        if (rawLayer === 'controller' || text.includes('controller') || text.includes('inbound') || text.includes('.adapter.in')) return 'inbound-adapter';
        return 'outbound-adapter';
    }
    if (rawLayer === 'persistence' || text.includes('repository') || text.includes('persistence')) return 'outbound-adapter';
    return 'domain';
}
