/**
 * View-model contract for the diagram canvas.
 *
 * This module intentionally knows nothing about Renovatio's real domain
 * (DomainModel, ArchitectureProfileDraft, etc.) or about the backend API.
 * The host widget (in @renovatio/core-ui) is responsible for mapping the
 * real model to/from this shape. Keeping the boundary here means the canvas
 * package can be reused for Domain, DER and Architecture views without any
 * of them leaking into each other, and can later be reused unmodified by a
 * VS Code Custom Editor host (see issue #270).
 */

/** One node on the canvas. Positions are optional: when absent, the host
 * is expected to run an auto-layout pass before rendering. */
export interface DiagramNodeVM {
    /** Stable id — must match the id of the real model element it represents. */
    id: string;
    /** Discriminates rendering (e.g. a DOMAIN_KIND, an architecture layer role, ...). */
    kind: string;
    /** Primary label shown on the node. */
    label: string;
    /** Optional grouping key (e.g. architecture layer, bounded context) used for
     * layout banding/coloring; the canvas does not interpret its value. */
    group?: string;
    /** Id of another node in the same model that visually CONTAINS this one
     * (e.g. a UML package containing a class) — not a relationship/edge, a
     * nesting. When set, this node renders and drags inside the parent's
     * bounds; x/y become relative to the parent's origin instead of the
     * canvas origin. The parent node must appear earlier in `nodes` than
     * any node naming it as parentId. */
    parentId?: string;
    /** Explicit box size in canvas pixels. Required on any node used as a
     * parentId target (a container needs a size to contain its children);
     * optional otherwise, where the node type's natural size applies. */
    width?: number;
    height?: number;
    /** Last known position. Omit to let the canvas auto-layout the node. */
    x?: number;
    y?: number;
    /** Free-form data the host wants to render inside a custom node type
     * (e.g. class members, table columns) without widening this interface
     * for every new diagram mode. The canvas core never reads this itself;
     * only host-supplied custom node renderers do. */
    data?: Record<string, unknown>;
}

/** One directed edge on the canvas. */
export interface DiagramEdgeVM {
    id: string;
    source: string;
    target: string;
    /** Discriminates rendering (e.g. a RELATION_KIND, an ArchitectureRule outcome). */
    kind: string;
    label?: string;
    data?: Record<string, unknown>;
}

export interface DiagramModel {
    nodes: DiagramNodeVM[];
    edges: DiagramEdgeVM[];
}

/** Events the canvas emits back to its host. The canvas never mutates the
 * real model itself — it only reports what the user did; the host decides
 * what that means for DomainModel / ArchitectureProfileDraft and how (or
 * whether) to persist it. */
export type DiagramEvent =
    | { type: 'nodeMoved'; id: string; x: number; y: number }
    | { type: 'nodeSelected'; id: string | null }
    | { type: 'edgeCreated'; source: string; target: string }
    | { type: 'nodesPruned'; ids: string[] }
    /** An existing edge got dragged off one of its endpoints and dropped
     * onto a different node — same relationship record, new source/target. */
    | { type: 'edgeReconnected'; id: string; source: string; target: string }
    /** The user edited an edge's label through the canvas's built-in
     * relation editor popover. */
    | { type: 'edgeLabelChanged'; id: string; label: string }
    /** The user deleted a relation — either via the relation editor
     * popover's delete button or by selecting the edge and pressing
     * Delete/Backspace. */
    | { type: 'edgesDeleted'; ids: string[] };
