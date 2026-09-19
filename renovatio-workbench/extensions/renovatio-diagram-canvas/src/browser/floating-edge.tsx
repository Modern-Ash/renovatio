import React, { useContext } from 'react';
import {
    BaseEdge,
    EdgeLabelRenderer,
    Position,
    getBezierPath,
    getSmoothStepPath,
    getStraightPath,
    useInternalNode,
    type EdgeProps,
    type InternalNode
} from '@xyflow/react';
import { DiagramEditContext } from './diagram-edit-context';

/**
 * Issue #280 gap fix: connections used to attach at whichever fixed
 * left/right handle a node happened to expose, so a line to a node above or
 * below still routed sideways off the near edge before bending — "the line
 * doesn't attach at the point closest to the other entity". A floating edge
 * recomputes its endpoints every render from the two nodes' actual
 * rectangles (via useInternalNode) instead of trusting the handle that
 * originated the connection, so it always leaves from whichever side is
 * actually closest to the other node — regardless of which handle a host's
 * node component happens to render. One component, registered once here,
 * covers every diagram mode (#265/#266/#267) without touching their node
 * renderers.
 */

function nodeIntersection(intersectionNode: InternalNode, otherNode: InternalNode): { x: number; y: number } {
    const width = intersectionNode.measured.width ?? 0;
    const height = intersectionNode.measured.height ?? 0;
    const intersectionPos = intersectionNode.internals.positionAbsolute;
    const otherPos = otherNode.internals.positionAbsolute;
    const otherWidth = otherNode.measured.width ?? 0;
    const otherHeight = otherNode.measured.height ?? 0;

    const w = width / 2;
    const h = height / 2;
    const x2 = intersectionPos.x + w;
    const y2 = intersectionPos.y + h;
    const x1 = otherPos.x + otherWidth / 2;
    const y1 = otherPos.y + otherHeight / 2;

    const xx1 = (x1 - x2) / (2 * w || 1) - (y1 - y2) / (2 * h || 1);
    const yy1 = (x1 - x2) / (2 * w || 1) + (y1 - y2) / (2 * h || 1);
    const a = 1 / (Math.abs(xx1) + Math.abs(yy1) || 1);
    const xx3 = a * xx1;
    const yy3 = a * yy1;

    return { x: w * (xx3 + yy3) + x2, y: h * (-xx3 + yy3) + y2 };
}

function edgeSide(node: InternalNode, intersection: { x: number; y: number }): Position {
    const pos = node.internals.positionAbsolute;
    const width = node.measured.width ?? 0;
    const height = node.measured.height ?? 0;
    const nx = Math.round(pos.x);
    const ny = Math.round(pos.y);
    const px = Math.round(intersection.x);
    const py = Math.round(intersection.y);

    if (px <= nx + 1) return Position.Left;
    if (px >= nx + width - 1) return Position.Right;
    if (py <= ny + 1) return Position.Top;
    if (py >= ny + height - 1) return Position.Bottom;
    return Position.Top;
}

function floatingEdgeParams(source: InternalNode, target: InternalNode): {
    sx: number; sy: number; tx: number; ty: number; sourcePos: Position; targetPos: Position;
} {
    const sourceIntersection = nodeIntersection(source, target);
    const targetIntersection = nodeIntersection(target, source);
    return {
        sx: sourceIntersection.x,
        sy: sourceIntersection.y,
        tx: targetIntersection.x,
        ty: targetIntersection.y,
        sourcePos: edgeSide(source, sourceIntersection),
        targetPos: edgeSide(target, targetIntersection)
    };
}

const PATH_BY_LINE_STYLE = {
    straight: getStraightPath,
    default: getBezierPath,
    step: getSmoothStepPath,
    smoothstep: getSmoothStepPath
} as const;

export type EdgeLineStyle = keyof typeof PATH_BY_LINE_STYLE;

export function FloatingEdge(props: EdgeProps): React.ReactElement | null {
    const { id, source, target, markerStart, markerEnd, style, label, selected } = props;
    const sourceNode = useInternalNode(source);
    const targetNode = useInternalNode(target);
    const { onEdgeLabelChange, onEdgeDelete } = useContext(DiagramEditContext);
    const [editing, setEditing] = React.useState(false);
    const [draftLabel, setDraftLabel] = React.useState(String(label ?? ''));

    if (!sourceNode || !targetNode) {
        return null;
    }

    const { sx, sy, tx, ty, sourcePos, targetPos } = floatingEdgeParams(sourceNode, targetNode);
    const lineStyle = ((props.data as Record<string, unknown> | undefined)?.lineStyle as EdgeLineStyle) ?? 'smoothstep';
    const pathFn = PATH_BY_LINE_STYLE[lineStyle] ?? getSmoothStepPath;
    const [path, labelX, labelY] = pathFn({
        sourceX: sx,
        sourceY: sy,
        sourcePosition: sourcePos,
        targetX: tx,
        targetY: ty,
        targetPosition: targetPos
    });

    const commitLabel = (): void => {
        setEditing(false);
        if (draftLabel !== String(label ?? '')) {
            onEdgeLabelChange?.(id, draftLabel);
        }
    };

    return (
        <>
            <BaseEdge id={id} path={path} markerStart={markerStart} markerEnd={markerEnd} style={style} />
            {/* A small relation editor popover, shown only for the selected
                edge — the "combo de edición de relación" (#280): rename or
                delete the relationship without leaving the canvas. */}
            {selected && (onEdgeLabelChange || onEdgeDelete) && (
                <EdgeLabelRenderer>
                    <div
                        className='renovatio-edge-label-editor'
                        style={{ position: 'absolute', transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`, pointerEvents: 'all' }}
                    >
                        {editing ? (
                            <input
                                autoFocus
                                value={draftLabel}
                                onChange={event => setDraftLabel(event.target.value)}
                                onBlur={commitLabel}
                                onKeyDown={event => {
                                    if (event.key === 'Enter') commitLabel();
                                    if (event.key === 'Escape') { setDraftLabel(String(label ?? '')); setEditing(false); }
                                }}
                            />
                        ) : (
                            <div className='renovatio-edge-label-editor-actions'>
                                {onEdgeLabelChange && (
                                    <button type='button' onClick={() => { setDraftLabel(String(label ?? '')); setEditing(true); }}>
                                        {String(label ?? '') || 'Edit relation'}
                                    </button>
                                )}
                                {onEdgeDelete && (
                                    <button type='button' className='is-delete' title='Delete relation' onClick={() => onEdgeDelete(id)}>✕</button>
                                )}
                            </div>
                        )}
                    </div>
                </EdgeLabelRenderer>
            )}
            {!selected && label ? (
                <EdgeLabelRenderer>
                    <div
                        className='renovatio-edge-label'
                        style={{ position: 'absolute', transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`, pointerEvents: 'none' }}
                    >
                        {String(label)}
                    </div>
                </EdgeLabelRenderer>
            ) : null}
        </>
    );
}
