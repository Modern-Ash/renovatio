import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ReactFlow,
    Background,
    Controls,
    MarkerType,
    MiniMap,
    addEdge,
    applyNodeChanges,
    type Connection,
    type Edge,
    type Node,
    type NodeChange,
    type OnConnect,
    type OnNodesChange
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { DiagramEdgeVM, DiagramEvent, DiagramModel, DiagramNodeVM } from '../common/diagram-protocol';

export interface DiagramCanvasProps {
    model: DiagramModel;
    onEvent: (event: DiagramEvent) => void;
    /** Optional custom React Flow node types, keyed by DiagramNodeVM.kind
     * (or any grouping the host chooses to encode into `type` via
     * `nodeTypeFor`). Domain UML (#265), DER (#266) and Architecture (#267)
     * each register their own here — this package ships no node types of
     * its own beyond React Flow's built-in default. */
    nodeTypes?: Record<string, React.ComponentType<any>>;
    /** Maps a DiagramNodeVM to a React Flow node `type` key (defaults to
     * always returning 'default', i.e. React Flow's built-in box). */
    nodeTypeFor?: (node: DiagramNodeVM) => string;
    /** Maps a DiagramEdgeVM to inline React Flow edge style (stroke color,
     * dash pattern, ...). Kept generic on purpose — this package has no
     * opinion on what an edge "kind" like ALLOWED/DENIED or ASSOCIATES_WITH
     * means; the host (#265, #267) supplies the mapping. Defaults to no
     * override (React Flow's default edge look). */
    edgeStyleFor?: (edge: DiagramEdgeVM) => React.CSSProperties;
    /** Maps a DiagramEdgeVM to a `url(#markerId)` reference for each end of
     * the edge (e.g. ER crow's-foot cardinality glyphs for #266, or nothing
     * for a plain UML association). The referenced `<marker>` elements must
     * be supplied by the host via `defs` — this package draws no notation
     * of its own, it only wires the reference through to React Flow. */
    edgeMarkerFor?: (edge: DiagramEdgeVM) => { markerStart?: string; markerEnd?: string };
    /** Host-supplied `<marker>`/other SVG defs (e.g. ER cardinality glyphs),
     * rendered once in a zero-size <svg> so `edgeMarkerFor`'s `url(#id)`
     * references resolve. SVG marker lookups are document-wide, so this
     * defs block does not need to live inside React Flow's own <svg>. */
    defs?: React.ReactNode;
    /** Shows a generic prune affordance for selected nodes. The host receives
     * `nodesPruned` and decides whether to soft-exclude, delete, or ignore. */
    enablePrune?: boolean;
    pruneLabel?: string;
    className?: string;
}

const DEFAULT_NODE_TYPE = (): string => 'default';

/** Pure default layout used only when a node has no x/y: lays nodes out in a
 * simple grid so nothing overlaps at (0,0). Real auto-layout (dagre/elkjs,
 * grouped by `group`) is a per-mode concern for #265/#267, not this core
 * package's job. */
function layoutMissingPositions(nodes: DiagramNodeVM[]): Map<string, { x: number; y: number }> {
    const positions = new Map<string, { x: number; y: number }>();
    const columns = Math.max(1, Math.ceil(Math.sqrt(nodes.length || 1)));
    nodes.forEach((node, index) => {
        if (typeof node.x === 'number' && typeof node.y === 'number') {
            positions.set(node.id, { x: node.x, y: node.y });
            return;
        }
        positions.set(node.id, { x: (index % columns) * 220, y: Math.floor(index / columns) * 140 });
    });
    return positions;
}

function toFlowNodes(
    diagramNodes: DiagramNodeVM[],
    typeFor: (node: DiagramNodeVM) => string
): Node[] {
    const positions = layoutMissingPositions(diagramNodes);
    return diagramNodes.map(node => ({
        id: node.id,
        type: typeFor(node),
        position: positions.get(node.id) ?? { x: 0, y: 0 },
        data: { label: node.label, kind: node.kind, group: node.group, ...node.data }
    }));
}

function toFlowEdges(
    diagramEdges: DiagramEdgeVM[],
    styleFor?: (edge: DiagramEdgeVM) => React.CSSProperties,
    markerFor?: (edge: DiagramEdgeVM) => { markerStart?: string; markerEnd?: string }
): Edge[] {
    return diagramEdges.map(edge => {
        const markers = markerFor?.(edge);
        return {
            id: edge.id,
            source: edge.source,
            target: edge.target,
            label: edge.label,
            markerStart: markers?.markerStart,
            markerEnd: markers?.markerEnd ?? { type: MarkerType.ArrowClosed },
            style: styleFor?.(edge),
            data: { kind: edge.kind, ...edge.data }
        };
    });
}

/**
 * Renders a DiagramModel as an interactive React Flow canvas and reports
 * user interaction back via onEvent. This component owns no application
 * state: it derives its React Flow nodes/edges from `model` on every
 * change, and every interaction (drag, connect, selection) is reported
 * upward rather than applied to a locally-owned copy of the domain model.
 */
export function DiagramCanvas(props: DiagramCanvasProps): React.ReactElement {
    const typeFor = props.nodeTypeFor ?? DEFAULT_NODE_TYPE;
    const [flowNodes, setFlowNodes] = useState<Node[]>(() => toFlowNodes(props.model.nodes, typeFor));
    const [selectedIds, setSelectedIds] = useState<string[]>([]);
    const flowEdges = useMemo(
        () => toFlowEdges(props.model.edges, props.edgeStyleFor, props.edgeMarkerFor),
        [props.model.edges, props.edgeStyleFor, props.edgeMarkerFor]
    );

    useEffect(() => {
        setFlowNodes(toFlowNodes(props.model.nodes, typeFor));
        setSelectedIds([]);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.model.nodes]);

    const handleNodesChange: OnNodesChange = useCallback(changes => {
        setFlowNodes(current => {
            const next = applyNodeChanges(changes, current);
            if ((changes as NodeChange[]).some(change => change.type === 'select')) {
                setSelectedIds(next.filter(node => node.selected).map(node => node.id));
            }
            return next;
        });
        for (const change of changes as NodeChange[]) {
            if (change.type === 'position' && change.position && change.dragging === false) {
                props.onEvent({ type: 'nodeMoved', id: change.id, x: change.position.x, y: change.position.y });
            }
            if (change.type === 'select' && change.selected) {
                props.onEvent({ type: 'nodeSelected', id: change.id });
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent]);

    const handleConnect: OnConnect = useCallback((connection: Connection) => {
        if (!connection.source || !connection.target) return;
        setFlowNodes(current => current);
        props.onEvent({ type: 'edgeCreated', source: connection.source, target: connection.target });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent]);

    const handlePaneClick = useCallback(() => {
        setSelectedIds([]);
        props.onEvent({ type: 'nodeSelected', id: null });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent]);

    const handlePruneSelection = useCallback(() => {
        if (!selectedIds.length) return;
        props.onEvent({ type: 'nodesPruned', ids: selectedIds });
    }, [props.onEvent, selectedIds]);

    return (
        <div className={props.className ?? 'renovatio-diagram-canvas'} style={{ width: '100%', height: '100%', position: 'relative' }}>
            {props.defs && <svg width={0} height={0} style={{ position: 'absolute' }} aria-hidden='true'><defs>{props.defs}</defs></svg>}
            {props.enablePrune && selectedIds.length > 0 && <button
                type='button'
                className='renovatio-diagram-prune-selection'
                onClick={handlePruneSelection}
            >{props.pruneLabel ?? `Prune selection (${selectedIds.length})`}</button>}
            <ReactFlow
                nodes={flowNodes}
                edges={flowEdges}
                nodeTypes={props.nodeTypes}
                onNodesChange={handleNodesChange}
                onConnect={handleConnect}
                onPaneClick={handlePaneClick}
                selectionOnDrag
                multiSelectionKeyCode={['Shift', 'Meta', 'Control']}
                fitView
            >
                <Background />
                <Controls />
                <MiniMap pannable zoomable />
            </ReactFlow>
        </div>
    );
}

// addEdge is re-exported for hosts that want to preview a connection
// optimistically before the backend confirms the new relation exists.
export { addEdge };
