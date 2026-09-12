import React, { useCallback, useEffect, useMemo, useState } from 'react';
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

declare const acquireVsCodeApi: () => { postMessage(message: unknown): void };

type DocumentKind = 'domain' | 'architecture' | 'diagram';

interface WebviewState {
    documentKind: DocumentKind;
    model: DiagramModel;
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
function App(): React.ReactElement {
    const [state, setState] = useState<WebviewState>({
        documentKind: 'diagram',
        model: { nodes: [], edges: [] }
    });

    useEffect(() => {
        const listener = (event: MessageEvent): void => {
            if (event.data?.type === 'setModel') {
                const documentKind: DocumentKind = event.data.documentKind;
                const model: DiagramModel = event.data.model;
                setState({ documentKind, model: withLayout(model, autoArrange(model.nodes, model.edges, documentKind)) });
            }
        };
        const errorListener = (event: ErrorEvent): void => {
            vscode.postMessage({ type: 'error', message: event.message });
        };
        const rejectionListener = (event: PromiseRejectionEvent): void => {
            vscode.postMessage({ type: 'error', message: String(event.reason) });
        };
        window.addEventListener('message', listener);
        window.addEventListener('error', errorListener);
        window.addEventListener('unhandledrejection', rejectionListener);
        vscode.postMessage({ type: 'ready' });
        return () => {
            window.removeEventListener('message', listener);
            window.removeEventListener('error', errorListener);
            window.removeEventListener('unhandledrejection', rejectionListener);
        };
    }, []);

    const handleEvent = useCallback((event: DiagramEvent) => {
        vscode.postMessage({ type: 'diagramEvent', event });
    }, []);

    const runAutoLayout = useCallback(() => {
        const positions = autoArrange(state.model.nodes, state.model.edges, state.documentKind);
        setState(current => ({ ...current, model: withLayout(current.model, positions) }));
        vscode.postMessage({ type: 'diagramEvent', event: { type: 'layoutChanged', positions } });
    }, [state.documentKind, state.model.edges, state.model.nodes]);

    const nodeTypes = useMemo(() => ({ renovatio: RenovatioDiagramNode }), []);

    return (
        <main className='renovatio-vscode-editor'>
            <div className='renovatio-vscode-toolbar'>
                <strong>{state.documentKind === 'architecture' ? 'Architecture' : 'Domain'} Diagram</strong>
                <span>{state.model.nodes.length} nodes</span>
                <span>{state.model.edges.length} edges</span>
                <div className='toolbar-actions'>
                    <button type='button' onClick={runAutoLayout}>Auto layout</button>
                </div>
            </div>
            <section className='renovatio-vscode-canvas'>
                <DiagramCanvas
                    model={state.model}
                    onEvent={handleEvent}
                    nodeTypes={nodeTypes}
                    nodeTypeFor={() => 'renovatio'}
                    edgeStyleFor={edgeStyle}
                    enablePrune
                    pruneLabel='Exclude selected'
                    className='renovatio-vscode-flow'
                />
            </section>
        </main>
    );
}

function edgeStyle(edge: DiagramEdgeVM): React.CSSProperties {
    if (edge.data?.allowed === false) return { stroke: '#e5484d' };
    return {};
}

function RenovatioDiagramNode(props: NodeProps): React.ReactElement {
    const data = (props.data ?? {}) as Record<string, any>;
    const properties = Array.isArray(data.properties) ? data.properties.slice(0, 6) : [];
    return (
        <article className={`renovatio-vscode-node ${props.selected ? 'selected' : ''} ${data.excluded ? 'excluded' : ''}`}>
            <Handle type='target' position={Position.Left} />
            <Handle type='source' position={Position.Right} />
            <header className='node-title'>
                <span>{String(data.label ?? '')}</span>
                <small>{String(data.kind ?? '')}</small>
            </header>
            <div className='node-body'>
                {properties.length === 0 && <div className='muted'>{data.group || data.layer || 'No fields'}</div>}
                {properties.map((property: any) => (
                    <div className='property-row' key={`${property.name}:${property.type}`}>
                        <span>{property.isKey ? 'PK ' : ''}{property.name}{property.required ? '*' : ''}</span>
                        <code>{property.type ?? 'unknown'}</code>
                    </div>
                ))}
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
    const COLUMN_GAP = 320;
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

function withLayout(model: DiagramModel, positions: Record<string, { x: number; y: number }>): DiagramModel {
    return { nodes: model.nodes.map(node => ({ ...node, x: positions[node.id]?.x ?? node.x, y: positions[node.id]?.y ?? node.y })), edges: model.edges };
}

function orderedGroups(nodes: DiagramNodeVM[], kind: DocumentKind): string[] {
    if (kind === 'architecture') {
        const preferred = ['controller', 'service', 'application', 'model', 'domain', 'persistence', 'infrastructure'];
        const existing = unique(nodes.map(node => groupKey(node, kind)));
        return [...preferred.filter(group => existing.includes(group)), ...existing.filter(group => !preferred.includes(group))];
    }
    const preferred = ['USE_CASE', 'SERVICE', 'AGGREGATE', 'ENTITY', 'VALUE_OBJECT', 'REPOSITORY'];
    const existing = unique(nodes.map(node => groupKey(node, kind)));
    return [...preferred.filter(group => existing.includes(group)), ...existing.filter(group => !preferred.includes(group))];
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

createRoot(document.getElementById('root')!).render(<App />);
