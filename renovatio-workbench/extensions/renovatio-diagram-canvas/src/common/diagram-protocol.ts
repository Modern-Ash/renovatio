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
    | { type: 'nodesPruned'; ids: string[] };
