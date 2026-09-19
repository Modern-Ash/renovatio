import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Handle, Position, type NodeProps } from '@xyflow/react';
import {
    DiagramCanvas,
    type DiagramEdgeVM,
    type DiagramEvent,
    type DiagramModel,
    type DiagramNodeVM
} from '@renovatio/diagram-canvas/lib/browser';
import './webview.css';

declare const acquireVsCodeApi: () => {
    postMessage(message: unknown): void;
    getState?(): { edgeType?: EdgeLineType } | undefined;
    setState?(state: { edgeType?: EdgeLineType }): void;
};

type DocumentKind = 'domain' | 'persistence' | 'architecture' | 'diagram';
type ArchitectureStyle = 'LAYERED_MVC' | 'HEXAGONAL';

interface WebviewState {
    documentKind: DocumentKind;
    model: DiagramModel;
    ready: boolean;
    error?: string;
}

const vscode = acquireVsCodeApi();

/**
 * Issue #270 gap fix: this webview used to reimplement its own SVG diagram
 * renderer (custom node cards, hand-rolled edge routing) instead of the
 * shared @renovatio/diagram-canvas component the Theia Workbench uses
 * (#265/#267) — meaning "reuse the same component" from the original issue
 * was never actually true. It now renders through the real DiagramCanvas
 * (React Flow), same as the Workbench. Two things from the old renderer are
 * intentionally NOT carried over:
 *  - the curved/orthogonal/straight line-type toggle (React Flow's default
 *    bezier edges don't support per-instance routing styles without a
 *    custom edge type; not worth a bespoke edge renderer just for this)
 *  - custom multi-track orthogonal edge routing for parallel edges
 * Auto layout (grouped by layer/kind, issue's `autoArrange`) is kept,
 * since DiagramCanvas only provides a bare grid fallback for missing
 * positions, not a domain-aware one.
 */
type EdgeLineType = 'straight' | 'default' | 'step' | 'smoothstep';
type WebviewDiagramEvent = DiagramEvent | {
    type: 'layoutChanged';
    positions: Record<string, { x: number; y: number }>;
} | {
    type: 'architectureStyleChanged';
    style: ArchitectureStyle;
};

function App(): React.ReactElement {
    const [state, setState] = useState<WebviewState>({
        documentKind: 'diagram',
        model: { nodes: [], edges: [] },
        ready: false
    });
    const pendingPositions = useRef<Record<string, { x: number; y: number }>>({});
    const saveTimer = useRef<number | undefined>(undefined);
    // Line style applies to every edge in whichever diagram is open — kept
    // as its own piece of state (not per-document) since it's a viewing
    // preference, not something that belongs in the saved artifact.
    const [edgeType, setEdgeTypeState] = useState<EdgeLineType>(() => vscode.getState?.()?.edgeType ?? 'smoothstep');

    useEffect(() => {
        const listener = (event: MessageEvent): void => {
            if (event.data?.type === 'setModel') {
                const documentKind: DocumentKind = event.data.documentKind;
                const model: DiagramModel = withPendingPositions(event.data.model, pendingPositions.current);
                const positions = autoArrange(model.nodes, model.edges, documentKind);
                setState({
                    documentKind,
                    model: event.data.hasSavedLayout
                        ? withMissingLayout(model, positions)
                        : withLayout(model, positions),
                    ready: true
                });
            }
        };
        const errorListener = (event: ErrorEvent): void => {
            if (isResizeObserverLoopMessage(event.message)) {
                event.preventDefault();
                return;
            }
            setState(current => ({ ...current, ready: true, error: event.message }));
            vscode.postMessage({ type: 'error', message: event.message });
        };
        const rejectionListener = (event: PromiseRejectionEvent): void => {
            const detail = String(event.reason);
            if (isResizeObserverLoopMessage(detail)) {
                event.preventDefault();
                return;
            }
            setState(current => ({ ...current, ready: true, error: detail }));
            vscode.postMessage({ type: 'error', message: detail });
        };
        window.addEventListener('message', listener);
        window.addEventListener('error', errorListener);
        window.addEventListener('unhandledrejection', rejectionListener);
        vscode.postMessage({ type: 'ready' });
        return () => {
            window.removeEventListener('message', listener);
            window.removeEventListener('error', errorListener);
            window.removeEventListener('unhandledrejection', rejectionListener);
            if (saveTimer.current !== undefined) {
                window.clearTimeout(saveTimer.current);
            }
        };
    }, []);

    const postDiagramEvent = useCallback((event: WebviewDiagramEvent) => {
        vscode.postMessage({ type: 'diagramEvent', event });
    }, []);

    const flushPendingLayout = useCallback(() => {
        const positions = pendingPositions.current;
        if (!Object.keys(positions).length) return;
        pendingPositions.current = {};
        if (saveTimer.current !== undefined) {
            window.clearTimeout(saveTimer.current);
            saveTimer.current = undefined;
        }
        postDiagramEvent({ type: 'layoutChanged', positions });
    }, [postDiagramEvent]);

    const discardPendingLayoutSave = useCallback(() => {
        pendingPositions.current = {};
        if (saveTimer.current !== undefined) {
            window.clearTimeout(saveTimer.current);
            saveTimer.current = undefined;
        }
    }, []);

    const scheduleLayoutSave = useCallback(() => {
        if (saveTimer.current !== undefined) {
            window.clearTimeout(saveTimer.current);
        }
        saveTimer.current = window.setTimeout(() => {
            flushPendingLayout();
        }, 450);
    }, [flushPendingLayout]);

    const handleEvent = useCallback((event: DiagramEvent) => {
        if (event.type === 'nodeMoved') {
            pendingPositions.current = {
                ...pendingPositions.current,
                [event.id]: { x: event.x, y: event.y }
            };
            setState(current => ({
                ...current,
                model: moveNodeInModel(current.model, event.id, event.x, event.y)
            }));
            scheduleLayoutSave();
            return;
        }
        flushPendingLayout();
        postDiagramEvent(event);
    }, [flushPendingLayout, postDiagramEvent, scheduleLayoutSave]);

    const handleArchitectureStyleChange = useCallback((event: React.ChangeEvent<HTMLSelectElement>) => {
        flushPendingLayout();
        postDiagramEvent({ type: 'architectureStyleChanged', style: event.target.value as ArchitectureStyle });
    }, [flushPendingLayout, postDiagramEvent]);

    const setEdgeType = useCallback((next: EdgeLineType) => {
        setEdgeTypeState(next);
        vscode.setState?.({ edgeType: next });
    }, []);

    const runAutoLayout = useCallback(() => {
        discardPendingLayoutSave();
        const positions = autoArrange(state.model.nodes, state.model.edges, state.documentKind);
        setState(current => ({ ...current, model: withLayout(current.model, positions) }));
        postDiagramEvent({ type: 'layoutChanged', positions });
    }, [discardPendingLayoutSave, postDiagramEvent, state.documentKind, state.model.edges, state.model.nodes]);

    const nodeTypes = useMemo(() => ({ renovatio: RenovatioDiagramNode }), []);
    const architectureStyle = architectureStyleFromModel(state.model);

    return (
        <main className='renovatio-vscode-editor'>
            <div className='renovatio-vscode-toolbar'>
                <strong>{diagramTitle(state.documentKind)}</strong>
                <span>{state.model.nodes.length} nodes</span>
                <span>{state.model.edges.length} edges</span>
                <div className='toolbar-actions'>
                    {state.documentKind === 'architecture' && <label className='toolbar-field'>Style
                        <select value={architectureStyle} onChange={handleArchitectureStyleChange}>
                            <option value='LAYERED_MVC'>MVC</option>
                            <option value='HEXAGONAL'>Hexagonal</option>
                        </select>
                    </label>}
                    <label className='toolbar-field'>Lines
                        <select value={edgeType} onChange={event => setEdgeType(event.target.value as EdgeLineType)}>
                            <option value='straight'>Straight</option>
                            <option value='default'>Curved</option>
                            <option value='smoothstep'>Step</option>
                        </select>
                    </label>
                    <button type='button' onClick={runAutoLayout}>Auto layout</button>
                </div>
            </div>
            <section className='renovatio-vscode-canvas'>
                {state.error ? (
                    <div className='renovatio-vscode-message is-error'>
                        <strong>Diagram renderer failed</strong>
                        <code>{state.error}</code>
                    </div>
                ) : !state.ready ? (
                    <div className='renovatio-vscode-message'>Loading diagram...</div>
                ) : state.model.nodes.length === 0 ? (
                    <div className='renovatio-vscode-message'>No diagram nodes found in this artifact.</div>
                ) : (
                    <DiagramCanvas
                        model={state.model}
                        onEvent={handleEvent}
                        nodeTypes={nodeTypes}
                        nodeTypeFor={() => 'renovatio'}
                        edgeStyleFor={edgeStyle}
                        edgeType={edgeType}
                        enablePrune
                        pruneLabel='Exclude selected'
                        className='renovatio-vscode-flow'
                    />
                )}
            </section>
        </main>
    );
}

function edgeStyle(edge: DiagramEdgeVM): React.CSSProperties {
    if (edge.data?.allowed === false) return { stroke: '#e5484d' };
    if (edge.data?.foreignKey) {
        return { stroke: 'var(--vscode-charts-blue, #3794ff)', strokeWidth: 1.6 };
    }
    if (edge.data?.sourceCardinality || edge.data?.targetCardinality) {
        return { stroke: 'rgba(180, 190, 200, 0.62)', strokeWidth: 1.2 };
    }
    return {};
}

function moveNodeInModel(model: DiagramModel, id: string, x: number, y: number): DiagramModel {
    return {
        ...model,
        nodes: model.nodes.map(node => node.id === id ? { ...node, x, y } : node)
    };
}

function withPendingPositions(model: DiagramModel, positions: Record<string, { x: number; y: number }>): DiagramModel {
    if (!Object.keys(positions).length) {
        return model;
    }
    return {
        ...model,
        nodes: model.nodes.map(node => {
            const position = positions[node.id];
            return position ? { ...node, x: position.x, y: position.y } : node;
        })
    };
}

function RenovatioDiagramNode(props: NodeProps): React.ReactElement {
    const data = (props.data ?? {}) as Record<string, any>;
    const rawProperties = Array.isArray(data.properties) ? data.properties : [];
    const properties = rawProperties.slice(0, 5);
    const isArchitectureLayer = data.kind === 'ARCHITECTURE_LAYER';
    const isArchitectureComponent = data.layer && !isArchitectureLayer;
    const isPersistenceTable = data.diagramKind === 'persistence' || data.kind === 'TABLE';
    const isDomainClass = !isArchitectureLayer && !isArchitectureComponent && Array.isArray(data.properties);
    const stereotype = isArchitectureLayer ? 'package' : String(data.kind ?? '').toLowerCase().replace(/_/g, ' ');
    const title = isArchitectureLayer ? String(data.packageName || data.label || '') : String(data.className || data.label || '');
    const subtitle = isArchitectureLayer
        ? String(data.layerRole || data.label || '')
        : String(data.layerRole || data.tableName || data.sourceDataset || data.packageName || data.group || data.layer || '');
    return (
        <article className={`renovatio-vscode-node ${isArchitectureLayer ? 'is-package' : ''} ${isArchitectureComponent ? 'is-architecture-class' : ''} ${isDomainClass ? 'is-domain-class' : ''} ${isPersistenceTable ? 'is-persistence-table' : ''} ${props.selected ? 'selected' : ''} ${data.excluded ? 'excluded' : ''}`}>
            <Handle type='target' position={Position.Left} />
            <Handle type='source' position={Position.Right} />
            <header className='node-title'>
                <span>{title}</span>
                <small>«{stereotype}»</small>
            </header>
            <div className='node-body'>
                {properties.length === 0 && <div className='muted'>{subtitle || data.componentId || 'No fields'}</div>}
                {properties.length > 0 && subtitle && <div className='muted'>{subtitle}</div>}
                {properties.map((property: any) => (
                    <div className='property-row' key={`${property.name}:${property.type}`}>
                        <span><strong>{property.isKey ? 'PK' : property.isForeignKey ? 'FK' : ''}</strong>{property.columnName ?? property.name}{property.required ? '*' : ''}</span>
                        <code>{property.type ?? 'unknown'}</code>
                    </div>
                ))}
                {rawProperties.length > properties.length && <div className='muted'>+{rawProperties.length - properties.length} attributes</div>}
            </div>
            {data.excluded && <div className='node-note'>{String(data.exclusionReason ?? 'Excluded')}</div>}
        </article>
    );
}

/** Domain-aware auto layout: lanes nodes by kind/layer in a sensible reading
 * order, ordering within a lane by connectivity (most-connected first).
 * Kept from the pre-#270-fix renderer — DiagramCanvas itself only fills in
 * a bare grid for nodes with no position, it has no concept of "layer". */
function autoArrange(nodes: DiagramNodeVM[], edges: DiagramEdgeVM[], kind: DocumentKind): Record<string, { x: number; y: number }> {
    if (kind === 'architecture') {
        return autoArrangeArchitecture(nodes, edges);
    }
    if (kind === 'persistence') {
        return autoArrangePersistence(nodes, edges);
    }
    if (kind === 'domain') {
        return autoArrangeDomain(nodes, edges);
    }
    const COLUMN_GAP = 430;
    const ROW_GAP = 170;
    const groups = orderedGroups(nodes, kind);
    const groupByNode = new Map(nodes.map(node => [node.id, groupKey(node, kind)]));
    const connected = new Map<string, Set<string>>();
    for (const node of nodes) connected.set(node.id, new Set());
    for (const edge of edges) {
        connected.get(edge.source)?.add(edge.target);
        connected.get(edge.target)?.add(edge.source);
    }
    const orderedNodes = [...nodes].sort((left, right) => {
        const groupDiff = groups.indexOf(groupByNode.get(left.id) ?? '') - groups.indexOf(groupByNode.get(right.id) ?? '');
        if (groupDiff !== 0) return groupDiff;
        const degreeDiff = (connected.get(right.id)?.size ?? 0) - (connected.get(left.id)?.size ?? 0);
        if (degreeDiff !== 0) return degreeDiff;
        return String(left.label || left.id).localeCompare(String(right.label || right.id));
    });
    const lanes = new Map(groups.map(group => [group, [] as DiagramNodeVM[]]));
    for (const node of orderedNodes) {
        const group = groupByNode.get(node.id) ?? groups[groups.length - 1] ?? 'model';
        lanes.get(group)?.push(node);
    }
    const result: Record<string, { x: number; y: number }> = {};
    groups.forEach((group, column) => {
        const lane = lanes.get(group) ?? [];
        lane.forEach((node, index) => {
            result[node.id] = { x: 72 + column * COLUMN_GAP, y: 64 + index * ROW_GAP };
        });
    });
    return result;
}

function autoArrangeDomain(nodes: DiagramNodeVM[], edges: DiagramEdgeVM[]): Record<string, { x: number; y: number }> {
    const COLUMN_GAP = 360;
    const ROW_GAP = 165;
    // A kind with many nodes (e.g. dozens of VALUE_OBJECTs) used to stack
    // as one column that ran far down the page. Each kind lane now wraps
    // into its own sub-columns after MAX_LANE_ROWS, staying compact instead
    // of racing downward.
    const MAX_LANE_ROWS = 8;
    const LANE_SUBCOLUMN_GAP = COLUMN_GAP;
    const columns = ['USE_CASE', 'DOMAIN_SERVICE', 'AGGREGATE', 'ENTITY', 'VALUE_OBJECT', 'REPOSITORY', 'EXTERNAL_SYSTEM', 'EVENT'];
    const columnFor = (node: DiagramNodeVM): number => {
        const kind = String(node.kind ?? '').toUpperCase();
        if (kind === 'USE_CASE') return 0;
        if (kind === 'DOMAIN_SERVICE' || kind === 'SERVICE') return 1;
        if (kind === 'AGGREGATE') return 2;
        if (kind === 'ENTITY' || kind === 'VALUE_OBJECT' || kind === 'BOUNDED_CONTEXT') return 3;
        if (kind === 'REPOSITORY') return 4;
        if (kind === 'EXTERNAL_SYSTEM') return 5;
        if (kind === 'EVENT') return 2;
        return 3;
    };
    const connected = new Map<string, number>();
    nodes.forEach(node => connected.set(node.id, 0));
    edges.forEach(edge => {
        connected.set(edge.source, (connected.get(edge.source) ?? 0) + 1);
        connected.set(edge.target, (connected.get(edge.target) ?? 0) + 1);
    });
    // Nodes with no relation at all would otherwise sit inline in their
    // kind's lane, breaking up the connected ones and making the relation
    // lines harder to follow. Pull them out of the lanes entirely and give
    // them their own block off to the side, grouped together.
    const isolated = nodes.filter(node => (connected.get(node.id) ?? 0) === 0);
    const isolatedIds = new Set(isolated.map(node => node.id));
    const lanes = new Map<number, DiagramNodeVM[]>();
    nodes.forEach(node => {
        if (isolatedIds.has(node.id)) return;
        const column = columnFor(node);
        lanes.set(column, [...(lanes.get(column) ?? []), node]);
    });
    const result: Record<string, { x: number; y: number }> = {};
    let cursorX = 72;
    for (let column = 0; column < columns.length; column += 1) {
        const lane = lanes.get(column);
        if (!lane || !lane.length) continue;
        const eventOffset = columns[column] === 'EVENT' ? 120 : 0;
        lane.sort((left, right) => (connected.get(right.id) ?? 0) - (connected.get(left.id) ?? 0)
            || columns.indexOf(String(left.kind ?? '').toUpperCase()) - columns.indexOf(String(right.kind ?? '').toUpperCase())
            || String(left.label || left.id).localeCompare(String(right.label || right.id)));
        lane.forEach((node, index) => {
            const subColumn = Math.floor(index / MAX_LANE_ROWS);
            const row = index % MAX_LANE_ROWS;
            result[node.id] = { x: cursorX + subColumn * LANE_SUBCOLUMN_GAP, y: 72 + eventOffset + row * ROW_GAP };
        });
        const subColumnCount = Math.ceil(lane.length / MAX_LANE_ROWS);
        cursorX += subColumnCount * LANE_SUBCOLUMN_GAP;
    }
    if (isolated.length) {
        // A full extra lane's worth of gap marks this as its own separate
        // group rather than one more column of the relation graph.
        cursorX += LANE_SUBCOLUMN_GAP;
        isolated
            .sort((left, right) => String(left.kind ?? '').localeCompare(String(right.kind ?? ''))
                || String(left.label || left.id).localeCompare(String(right.label || right.id)))
            .forEach((node, index) => {
                const subColumn = Math.floor(index / MAX_LANE_ROWS);
                const row = index % MAX_LANE_ROWS;
                result[node.id] = { x: cursorX + subColumn * LANE_SUBCOLUMN_GAP, y: 72 + row * ROW_GAP };
            });
    }
    return result;
}

function autoArrangePersistence(nodes: DiagramNodeVM[], edges: DiagramEdgeVM[]): Record<string, { x: number; y: number }> {
    const COLUMN_GAP = 320;
    const NODE_GAP = 72;
    const START_X = 80;
    const START_Y = 80;
    const connectedEdges = edges.filter(edge =>
        nodes.some(node => node.id === edge.source) && nodes.some(node => node.id === edge.target));
    if (!connectedEdges.length) {
        return autoArrangePersistenceGrid(nodes);
    }

    const incoming = new Map(nodes.map(node => [node.id, [] as DiagramEdgeVM[]]));
    const outgoing = new Map(nodes.map(node => [node.id, [] as DiagramEdgeVM[]]));
    connectedEdges.forEach(edge => {
        incoming.get(edge.target)?.push(edge);
        outgoing.get(edge.source)?.push(edge);
    });

    const hub = [...nodes].sort((left, right) => {
        const leftDegree = (incoming.get(left.id)?.length ?? 0) + (outgoing.get(left.id)?.length ?? 0);
        const rightDegree = (incoming.get(right.id)?.length ?? 0) + (outgoing.get(right.id)?.length ?? 0);
        if (rightDegree !== leftDegree) return rightDegree - leftDegree;
        return String(left.label || left.id).localeCompare(String(right.label || right.id));
    })[0];
    if (hub) {
        return autoArrangePersistenceAroundHub(nodes, connectedEdges, incoming, outgoing, hub);
    }

    const columns = new Map<number, DiagramNodeVM[]>();
    nodes.forEach(node => {
        const incomingCount = incoming.get(node.id)?.length ?? 0;
        const outgoingCount = outgoing.get(node.id)?.length ?? 0;
        const column = outgoingCount > 0 && incomingCount > 0
            ? 1
            : incomingCount > 0
                ? 2
                : 0;
        columns.set(column, [...(columns.get(column) ?? []), node]);
    });

    const result: Record<string, { x: number; y: number }> = {};
    [...columns.entries()].sort(([left], [right]) => left - right).forEach(([column, lane]) => {
        const ordered = lane.sort((left, right) => {
            const leftIncomingY = averageIncomingY(left.id, incoming, result);
            const rightIncomingY = averageIncomingY(right.id, incoming, result);
            if (leftIncomingY !== rightIncomingY) return leftIncomingY - rightIncomingY;
            const degreeDiff = ((incoming.get(right.id)?.length ?? 0) + (outgoing.get(right.id)?.length ?? 0))
                - ((incoming.get(left.id)?.length ?? 0) + (outgoing.get(left.id)?.length ?? 0));
            if (degreeDiff !== 0) return degreeDiff;
            return String(left.label || left.id).localeCompare(String(right.label || right.id));
        });
        let cursorY = START_Y;
        ordered.forEach(node => {
            result[node.id] = { x: START_X + column * COLUMN_GAP, y: cursorY };
            cursorY += estimatedPersistenceNodeHeight(node) + NODE_GAP;
        });
    });
    avoidRelationNodeOverlaps(nodes, connectedEdges, result);
    return result;
}

function autoArrangePersistenceAroundHub(
    nodes: DiagramNodeVM[],
    edges: DiagramEdgeVM[],
    incoming: Map<string, DiagramEdgeVM[]>,
    outgoing: Map<string, DiagramEdgeVM[]>,
    hub: DiagramNodeVM
): Record<string, { x: number; y: number }> {
    const LEFT_X = 80;
    const HUB_X = 540;
    const RIGHT_X = 900;
    const TOP_Y = 80;
    const NODE_GAP = 84;
    const result: Record<string, { x: number; y: number }> = {};

    const dependents = uniqueNodesForIds((incoming.get(hub.id) ?? []).map(edge => edge.source), nodes)
        .sort((left, right) => String(left.label || left.id).localeCompare(String(right.label || right.id)));
    const references = uniqueNodesForIds((outgoing.get(hub.id) ?? []).map(edge => edge.target), nodes)
        .sort((left, right) => String(left.label || left.id).localeCompare(String(right.label || right.id)));
    const placed = new Set<string>([hub.id, ...dependents.map(node => node.id), ...references.map(node => node.id)]);
    const remaining = nodes.filter(node => !placed.has(node.id))
        .sort((left, right) => String(left.label || left.id).localeCompare(String(right.label || right.id)));

    const leftLane = [...dependents, ...remaining.filter(node => (outgoing.get(node.id)?.length ?? 0) > 0)];
    const rightLane = references;
    const centerLane = [hub, ...remaining.filter(node => (outgoing.get(node.id)?.length ?? 0) === 0)];

    layoutLane(leftLane, LEFT_X, TOP_Y, NODE_GAP, result);
    const leftHeight = laneHeight(leftLane, NODE_GAP);
    const hubY = Math.max(TOP_Y, TOP_Y + Math.max(0, (leftHeight - estimatedPersistenceNodeHeight(hub)) / 2));
    result[hub.id] = { x: HUB_X, y: hubY };
    layoutLane(centerLane.filter(node => node.id !== hub.id), HUB_X, hubY + estimatedPersistenceNodeHeight(hub) + NODE_GAP, NODE_GAP, result);
    layoutLane(rightLane, RIGHT_X, hubY, NODE_GAP, result);

    avoidRelationNodeOverlaps(nodes, edges, result);
    return result;
}

function layoutLane(
    lane: DiagramNodeVM[],
    x: number,
    startY: number,
    gap: number,
    result: Record<string, { x: number; y: number }>
): void {
    let cursorY = startY;
    lane.forEach(node => {
        result[node.id] = { x, y: cursorY };
        cursorY += estimatedPersistenceNodeHeight(node) + gap;
    });
}

function laneHeight(lane: DiagramNodeVM[], gap: number): number {
    if (!lane.length) return 0;
    return lane.reduce((sum, node) => sum + estimatedPersistenceNodeHeight(node), 0) + (lane.length - 1) * gap;
}

function uniqueNodesForIds(ids: string[], nodes: DiagramNodeVM[]): DiagramNodeVM[] {
    const byId = new Map(nodes.map(node => [node.id, node]));
    return Array.from(new Set(ids)).map(id => byId.get(id)).filter((node): node is DiagramNodeVM => Boolean(node));
}

function autoArrangePersistenceGrid(nodes: DiagramNodeVM[]): Record<string, { x: number; y: number }> {
    const COLUMN_GAP = 430;
    const NODE_GAP = 72;
    const columnHeights = [80, 80, 80];
    const result: Record<string, { x: number; y: number }> = {};
    [...nodes]
        .sort((left, right) => String(left.label || left.id).localeCompare(String(right.label || right.id)))
        .forEach((node, index) => {
            const column = index % 3;
            result[node.id] = { x: 80 + column * COLUMN_GAP, y: columnHeights[column] };
            columnHeights[column] += estimatedPersistenceNodeHeight(node) + NODE_GAP;
        });
    return result;
}

function estimatedPersistenceNodeHeight(node: DiagramNodeVM): number {
    const properties = Array.isArray(node.data?.properties) ? node.data.properties : [];
    const visibleProperties = Math.min(properties.length, 5);
    const hasMore = properties.length > visibleProperties;
    return 72 + visibleProperties * 24 + (hasMore ? 22 : 0);
}

function avoidRelationNodeOverlaps(
    nodes: DiagramNodeVM[],
    edges: DiagramEdgeVM[],
    positions: Record<string, { x: number; y: number }>
): void {
    const byId = new Map(nodes.map(node => [node.id, node]));
    const NODE_WIDTH = 280;
    const CLEARANCE = 22;
    for (let pass = 0; pass < 8; pass += 1) {
        let moved = false;
        for (const edge of edges) {
            const source = byId.get(edge.source);
            const target = byId.get(edge.target);
            const sourcePosition = positions[edge.source];
            const targetPosition = positions[edge.target];
            if (!source || !target || !sourcePosition || !targetPosition) continue;
            const sourceHeight = estimatedPersistenceNodeHeight(source);
            const targetHeight = estimatedPersistenceNodeHeight(target);
            const line = {
                x1: sourcePosition.x + NODE_WIDTH,
                y1: sourcePosition.y + sourceHeight / 2,
                x2: targetPosition.x,
                y2: targetPosition.y + targetHeight / 2
            };
            for (const node of nodes) {
                if (node.id === edge.source || node.id === edge.target) continue;
                const position = positions[node.id];
                if (!position) continue;
                const height = estimatedPersistenceNodeHeight(node);
                const rect = {
                    left: position.x - CLEARANCE,
                    right: position.x + NODE_WIDTH + CLEARANCE,
                    top: position.y - CLEARANCE,
                    bottom: position.y + height + CLEARANCE
                };
                if (!lineIntersectsRect(line, rect)) continue;
                positions[node.id] = { x: position.x, y: rect.bottom + CLEARANCE };
                moved = true;
            }
        }
        if (!moved) return;
    }
}

function lineIntersectsRect(
    line: { x1: number; y1: number; x2: number; y2: number },
    rect: { left: number; right: number; top: number; bottom: number }
): boolean {
    if (line.x1 < rect.left && line.x2 < rect.left) return false;
    if (line.x1 > rect.right && line.x2 > rect.right) return false;
    if (line.y1 < rect.top && line.y2 < rect.top) return false;
    if (line.y1 > rect.bottom && line.y2 > rect.bottom) return false;
    if (pointInRect(line.x1, line.y1, rect) || pointInRect(line.x2, line.y2, rect)) return true;
    return segmentsIntersect(line.x1, line.y1, line.x2, line.y2, rect.left, rect.top, rect.right, rect.top)
        || segmentsIntersect(line.x1, line.y1, line.x2, line.y2, rect.right, rect.top, rect.right, rect.bottom)
        || segmentsIntersect(line.x1, line.y1, line.x2, line.y2, rect.right, rect.bottom, rect.left, rect.bottom)
        || segmentsIntersect(line.x1, line.y1, line.x2, line.y2, rect.left, rect.bottom, rect.left, rect.top);
}

function pointInRect(
    x: number,
    y: number,
    rect: { left: number; right: number; top: number; bottom: number }
): boolean {
    return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
}

function segmentsIntersect(
    ax: number,
    ay: number,
    bx: number,
    by: number,
    cx: number,
    cy: number,
    dx: number,
    dy: number
): boolean {
    const direction = (px: number, py: number, qx: number, qy: number, rx: number, ry: number): number =>
        (rx - px) * (qy - py) - (qx - px) * (ry - py);
    const d1 = direction(cx, cy, dx, dy, ax, ay);
    const d2 = direction(cx, cy, dx, dy, bx, by);
    const d3 = direction(ax, ay, bx, by, cx, cy);
    const d4 = direction(ax, ay, bx, by, dx, dy);
    return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
        && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
}

function averageIncomingY(
    nodeId: string,
    incoming: Map<string, DiagramEdgeVM[]>,
    positions: Record<string, { x: number; y: number }>
): number {
    const ys = (incoming.get(nodeId) ?? [])
        .map(edge => positions[edge.source]?.y)
        .filter((value): value is number => typeof value === 'number');
    if (!ys.length) return Number.MAX_SAFE_INTEGER;
    return ys.reduce((sum, value) => sum + value, 0) / ys.length;
}

function diagramTitle(kind: DocumentKind): string {
    if (kind === 'architecture') return 'Architecture Diagram';
    if (kind === 'persistence') return 'Persistence DER';
    return 'Domain Diagram';
}

function autoArrangeArchitecture(nodes: DiagramNodeVM[], edges: DiagramEdgeVM[]): Record<string, { x: number; y: number }> {
    const style = architectureStyleFromModel({ nodes, edges: [] });
    const packages = nodes.filter(node => node.kind === 'ARCHITECTURE_LAYER');
    const packageOrder = orderedArchitectureGroups(packages.length ? packages : nodes, style);
    const packageByGroup = new Map(packages.map(node => [String(node.group ?? node.id).toLowerCase(), node]));
    const result: Record<string, { x: number; y: number }> = {};
    packageOrder.forEach((group, index) => {
        const packageNode = packageByGroup.get(group);
        if (!packageNode) return;
        result[packageNode.id] = architecturePackagePosition(group, index, style);
        const childTop = style === 'HEXAGONAL' && group === 'service' ? 64 : 66;
        const children = nodes
            .filter(node => node.parentId === packageNode.id)
            .sort((left, right) => String(left.label || left.id).localeCompare(String(right.label || right.id)));
        children.forEach((node, index) => {
            result[node.id] = { x: 18, y: childTop + index * 118 };
        });
    });
    avoidArchitecturePackageRelationOverlaps(packages, edges, result);
    return result;
}

function avoidArchitecturePackageRelationOverlaps(
    packages: DiagramNodeVM[],
    edges: DiagramEdgeVM[],
    positions: Record<string, { x: number; y: number }>
): void {
    const byId = new Map(packages.map(node => [node.id, node]));
    const CLEARANCE = 34;
    for (let pass = 0; pass < 8; pass += 1) {
        let moved = false;
        for (const edge of edges) {
            const source = byId.get(edge.source);
            const target = byId.get(edge.target);
            const sourcePosition = positions[edge.source];
            const targetPosition = positions[edge.target];
            if (!source || !target || !sourcePosition || !targetPosition) continue;
            const sourceWidth = source.width ?? 300;
            const targetHeight = target.height ?? 180;
            const sourceHeight = source.height ?? 180;
            const line = {
                x1: sourcePosition.x + sourceWidth,
                y1: sourcePosition.y + sourceHeight / 2,
                x2: targetPosition.x,
                y2: targetPosition.y + targetHeight / 2
            };
            for (const node of packages) {
                if (node.id === edge.source || node.id === edge.target) continue;
                const position = positions[node.id];
                if (!position) continue;
                const width = node.width ?? 300;
                const height = node.height ?? 180;
                const rect = {
                    left: position.x - CLEARANCE,
                    right: position.x + width + CLEARANCE,
                    top: position.y - CLEARANCE,
                    bottom: position.y + height + CLEARANCE
                };
                if (!lineIntersectsRect(line, rect)) continue;
                positions[node.id] = { x: position.x, y: rect.bottom + CLEARANCE };
                moved = true;
            }
        }
        if (!moved) return;
    }
}

function withLayout(model: DiagramModel, positions: Record<string, { x: number; y: number }>): DiagramModel {
    return { nodes: model.nodes.map(node => ({ ...node, x: positions[node.id]?.x ?? node.x, y: positions[node.id]?.y ?? node.y })), edges: model.edges };
}

function withMissingLayout(model: DiagramModel, positions: Record<string, { x: number; y: number }>): DiagramModel {
    return {
        nodes: model.nodes.map(node => {
            if (typeof node.x === 'number' && typeof node.y === 'number') return node;
            return { ...node, x: positions[node.id]?.x ?? node.x, y: positions[node.id]?.y ?? node.y };
        }),
        edges: model.edges
    };
}

function orderedGroups(nodes: DiagramNodeVM[], kind: DocumentKind): string[] {
    if (kind === 'architecture') {
        return orderedArchitectureGroups(nodes, architectureStyleFromModel({ nodes, edges: [] }));
    }
    const preferred = ['USE_CASE', 'SERVICE', 'AGGREGATE', 'ENTITY', 'VALUE_OBJECT', 'REPOSITORY'];
    const existing = unique(nodes.map(node => groupKey(node, kind)));
    return [...preferred.filter(group => existing.includes(group)), ...existing.filter(group => !preferred.includes(group))];
}

function orderedArchitectureGroups(nodes: DiagramNodeVM[], style: ArchitectureStyle): string[] {
    const preferred = style === 'HEXAGONAL'
        ? ['inbound-adapter', 'inbound-port', 'application', 'domain', 'outbound-port', 'outbound-adapter']
        : ['controller', 'service', 'model', 'persistence'];
    const existing = unique(nodes.map(node => groupKey(node, 'architecture')));
    return [...preferred.filter(group => existing.includes(group)), ...existing.filter(group => !preferred.includes(group))];
}

function architectureStyleFromModel(model: DiagramModel): ArchitectureStyle {
    const value = model.nodes.find(node => node.kind === 'ARCHITECTURE_LAYER')?.data?.architectureStyle;
    return value === 'HEXAGONAL' ? 'HEXAGONAL' : 'LAYERED_MVC';
}

function architecturePackagePosition(layer: string, index: number, style: ArchitectureStyle): { x: number; y: number } {
    if (style === 'HEXAGONAL') {
        const positions: Record<string, { x: number; y: number }> = {
            'inbound-adapter': { x: 80, y: 360 },
            'inbound-port': { x: 500, y: 360 },
            application: { x: 920, y: 120 },
            domain: { x: 920, y: 600 },
            'outbound-port': { x: 1340, y: 360 },
            'outbound-adapter': { x: 1760, y: 360 }
        };
        return positions[layer] ?? { x: 80 + (index % 3) * 420, y: 780 + Math.floor(index / 3) * 320 };
    }
    return { x: 80 + index * 420, y: 120 };
}

function groupKey(node: DiagramNodeVM, kind: DocumentKind): string {
    if (kind === 'architecture') {
        return String((node.data as any)?.layer ?? node.group ?? node.kind ?? 'model').toLowerCase();
    }
    return String(node.kind ?? node.group ?? 'NODE').toUpperCase();
}

function unique(values: string[]): string[] {
    return Array.from(new Set(values.filter(Boolean)));
}

function isResizeObserverLoopMessage(message: unknown): boolean {
    return String(message ?? '').includes('ResizeObserver loop completed with undelivered notifications');
}

createRoot(document.getElementById('root')!).render(<App />);
