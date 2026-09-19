import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    ReactFlow,
    Background,
    Controls,
    MarkerType,
    MiniMap,
    addEdge,
    applyEdgeChanges,
    applyNodeChanges,
    reconnectEdge,
    type Connection,
    type Edge,
    type EdgeChange,
    type EdgeTypes,
    type Node,
    type NodeChange,
    type OnConnect,
    type OnEdgesChange,
    type OnNodesChange,
    type OnReconnect,
    type ReactFlowInstance
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { DiagramEdgeVM, DiagramEvent, DiagramModel, DiagramNodeVM } from '../common/diagram-protocol';
import { FloatingEdge } from './floating-edge';
import { DiagramEditContext } from './diagram-edit-context';

const EDGE_TYPES: EdgeTypes = { floating: FloatingEdge };

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
    /** React Flow edge routing style, applied to every edge: 'smoothstep'
     * (default — right-angle bends, reads as routing around nodes rather
     * than through them), 'step' (the same but sharp corners), 'straight'
     * or 'default' (a bezier curve). A host that lets the user pick a line
     * style (#280 gap fix) passes the current selection through here. */
    edgeType?: 'straight' | 'default' | 'step' | 'smoothstep';
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

// Fallback box size for nodes that carry no explicit width/height (e.g.
// Domain's UML class boxes, whose real rendered height grows with however
// many properties a node has — CSS-driven, unknown to this package). Picked
// generous enough for a class box with a handful of properties; the gap
// between cells must be at least one box wide/tall in each direction (a
// reported bug: rows touching with zero gap, boxes overlapping once a node
// had more properties than the row height assumed).
const FALLBACK_NODE_WIDTH = 260;
const FALLBACK_NODE_HEIGHT = 220;

/** Pure default layout used only when a node has no x/y: lays nodes out in a
 * simple grid so nothing overlaps at (0,0). Real auto-layout (dagre/elkjs,
 * grouped by `group`) is a per-mode concern for #265/#267, not this core
 * package's job. */
function layoutMissingPositions(nodes: DiagramNodeVM[]): Map<string, { x: number; y: number }> {
    const positions = new Map<string, { x: number; y: number }>();
    const columns = Math.max(1, Math.ceil(Math.sqrt(nodes.length || 1)));
    const cellWidth = FALLBACK_NODE_WIDTH * 2;
    const cellHeight = FALLBACK_NODE_HEIGHT * 2;
    nodes.forEach((node, index) => {
        if (typeof node.x === 'number' && typeof node.y === 'number') {
            positions.set(node.id, { x: node.x, y: node.y });
            return;
        }
        positions.set(node.id, { x: (index % columns) * cellWidth, y: Math.floor(index / columns) * cellHeight });
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
        // UML-style containment (e.g. a package containing its classes) is
        // real parent/child nesting, not an edge — a host sets parentId on
        // the contained node and width/height on the container (see #267
        // follow-up: a "membership edge" was tried first and correctly
        // rejected as not how containment reads visually). React Flow
        // requires the parent to appear earlier in the array than any node
        // naming it; every mapper here already emits containers first.
        ...(node.parentId ? { parentId: node.parentId, extent: 'parent' as const } : {}),
        ...(node.width || node.height ? { style: { width: node.width, height: node.height } } : {}),
        data: { label: node.label, kind: node.kind, group: node.group, ...node.data }
    }));
}

function toFlowEdges(
    diagramEdges: DiagramEdgeVM[],
    styleFor?: (edge: DiagramEdgeVM) => React.CSSProperties,
    markerFor?: (edge: DiagramEdgeVM) => { markerStart?: string; markerEnd?: string },
    // Defaults to 'smoothstep': a straight/bezier line between two
    // grid-arranged nodes often cuts diagonally across an unrelated node
    // sitting between them — the node's own z-index (see
    // webview.css/renovatio-workbench.css) then visibly severs that line
    // mid-path. Right-angle bends along the gutters between grid cells
    // read as routing around nodes rather than through them.
    edgeType: DiagramCanvasProps['edgeType'] = 'smoothstep'
): Edge[] {
    return diagramEdges.map(edge => {
        const markers = markerFor?.(edge);
        return {
            id: edge.id,
            source: edge.source,
            target: edge.target,
            label: edge.label,
            // Always the floating edge (see floating-edge.tsx) — it
            // recomputes its own endpoints from the two nodes' rectangles
            // every render, so it always attaches at whichever side is
            // actually closest to the other node. `lineStyle` (not React
            // Flow's own `type`) is what picks straight/curved/step now.
            type: 'floating',
            reconnectable: true,
            markerStart: markers?.markerStart,
            markerEnd: markers?.markerEnd ?? { type: MarkerType.ArrowClosed },
            style: styleFor?.(edge),
            data: { kind: edge.kind, lineStyle: edgeType, ...edge.data }
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
    const reactFlowInstance = useRef<ReactFlowInstance | null>(null);
    // `fitView` on <ReactFlow> only runs once, at mount — a mode whose model
    // arrives async (e.g. Domain) mounts with zero nodes, so that initial
    // fit has nothing to frame. Once real nodes show up for the first time,
    // fit the view to them explicitly instead of leaving the camera parked
    // on an empty canvas.
    const hasFramedNodes = useRef(false);
    // Local state (not a plain useMemo) so a reconnect drag can update the
    // dropped endpoint immediately, before the host round-trips the event
    // back down through props.model — otherwise the edge would visibly
    // snap back to its old endpoint until that round-trip completes.
    const [flowEdges, setFlowEdges] = useState<Edge[]>(
        () => toFlowEdges(props.model.edges, props.edgeStyleFor, props.edgeMarkerFor, props.edgeType)
    );

    useEffect(() => {
        setFlowEdges(toFlowEdges(props.model.edges, props.edgeStyleFor, props.edgeMarkerFor, props.edgeType));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.model.edges, props.edgeStyleFor, props.edgeMarkerFor, props.edgeType]);

    useEffect(() => {
        // Reconcile incoming nodes with the current selection instead of
        // treating every new `model.nodes` array identity as a full reset.
        // Selecting a node itself triggers a host re-render that remaps a
        // fresh nodes array (new object identity, same ids) — clearing
        // selectedIds here made the prune button disappear and lost the
        // selection before a user could act on it (see #276 review).
        const validIds = new Set(props.model.nodes.map(node => node.id));
        setSelectedIds(current => current.filter(id => validIds.has(id)));
        setFlowNodes(current => {
            const selected = new Set(current.filter(node => node.selected).map(node => node.id));
            return toFlowNodes(props.model.nodes, typeFor).map(node => ({ ...node, selected: selected.has(node.id) }));
        });
        if (!hasFramedNodes.current && props.model.nodes.length > 0) {
            hasFramedNodes.current = true;
            // Wait a tick so React Flow has measured the just-added nodes
            // before framing them (fitView on the same frame they appear
            // can under-measure and frame the wrong extent).
            requestAnimationFrame(() => reactFlowInstance.current?.fitView());
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.model.nodes]);

    const handleNodesChange: OnNodesChange = useCallback(changes => {
        const nonRemovalChanges = (changes as NodeChange[]).filter(change => change.type !== 'remove');
        if (nonRemovalChanges.length === 0) {
            return;
        }
        setFlowNodes(current => {
            const next = applyNodeChanges(nonRemovalChanges, current);
            const stableNext = next.length === 0 && props.model.nodes.length > 0
                ? toFlowNodes(props.model.nodes, typeFor)
                : next;
            if (nonRemovalChanges.some(change => change.type === 'select')) {
                setSelectedIds(stableNext.filter(node => node.selected).map(node => node.id));
            }
            return stableNext;
        });
        for (const change of nonRemovalChanges) {
            if (change.type === 'position' && change.position && change.dragging === false) {
                props.onEvent({ type: 'nodeMoved', id: change.id, x: change.position.x, y: change.position.y });
            }
            if (change.type === 'select' && change.selected) {
                props.onEvent({ type: 'nodeSelected', id: change.id });
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent, props.model.nodes, typeFor]);

    // Without this, edges never gain a `selected: true` flag when clicked
    // (React Flow reports the click as a 'select' EdgeChange but does
    // nothing with it unless the host applies it) — which meant the
    // relation editor popover in FloatingEdge, gated on `selected`, could
    // never appear, and Delete/Backspace on a selected edge had nothing to
    // remove it from either.
    const handleEdgesChange: OnEdgesChange = useCallback(changes => {
        const removedEdgeIds: string[] = [];
        setFlowEdges(current => applyEdgeChanges(changes, current));
        for (const change of changes as EdgeChange[]) {
            if (change.type === 'remove') {
                removedEdgeIds.push(change.id);
            }
        }
        if (removedEdgeIds.length > 0) {
            props.onEvent({ type: 'edgesDeleted', ids: removedEdgeIds });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent]);

    const handleEdgeDelete = useCallback((edgeId: string) => {
        setFlowEdges(current => current.filter(edge => edge.id !== edgeId));
        props.onEvent({ type: 'edgesDeleted', ids: [edgeId] });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent]);

    const handleConnect: OnConnect = useCallback((connection: Connection) => {
        if (!connection.source || !connection.target) return;
        setFlowNodes(current => current);
        props.onEvent({ type: 'edgeCreated', source: connection.source, target: connection.target });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent]);

    const handleReconnect: OnReconnect = useCallback((oldEdge, newConnection) => {
        if (!newConnection.source || !newConnection.target) return;
        setFlowEdges(current => reconnectEdge(oldEdge, newConnection, current));
        props.onEvent({ type: 'edgeReconnected', id: oldEdge.id, source: newConnection.source, target: newConnection.target });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent]);

    const handleEdgeLabelChange = useCallback((edgeId: string, label: string) => {
        setFlowEdges(current => current.map(edge => (edge.id === edgeId ? { ...edge, label } : edge)));
        props.onEvent({ type: 'edgeLabelChanged', id: edgeId, label });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [props.onEvent]);
    const editContextValue = useMemo(
        () => ({ onEdgeLabelChange: handleEdgeLabelChange, onEdgeDelete: handleEdgeDelete }),
        [handleEdgeLabelChange, handleEdgeDelete]
    );

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
            <DiagramEditContext.Provider value={editContextValue}>
                <ReactFlow
                    nodes={flowNodes}
                    edges={flowEdges}
                    nodeTypes={props.nodeTypes}
                    edgeTypes={EDGE_TYPES}
                    onNodesChange={handleNodesChange}
                    onEdgesChange={handleEdgesChange}
                    onConnect={handleConnect}
                    onReconnect={handleReconnect}
                    edgesReconnectable
                    deleteKeyCode={null}
                    onPaneClick={handlePaneClick}
                    onInit={instance => { reactFlowInstance.current = instance; }}
                    selectionOnDrag
                    multiSelectionKeyCode={['Shift', 'Meta', 'Control']}
                    elevateNodesOnSelect={false}
                    elevateEdgesOnSelect={false}
                    fitView
                >
                    <Background />
                    <Controls />
                    <MiniMap pannable zoomable />
                </ReactFlow>
            </DiagramEditContext.Provider>
        </div>
    );
}

// addEdge is re-exported for hosts that want to preview a connection
// optimistically before the backend confirms the new relation exists.
export { addEdge };
