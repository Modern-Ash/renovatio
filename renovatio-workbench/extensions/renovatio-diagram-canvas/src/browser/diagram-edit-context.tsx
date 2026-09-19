import { createContext } from 'react';

/** Threads the host's edge-label-change handler down to FloatingEdge without
 * prop-drilling it through every edge's `data` (which is host-owned free
 * form data, not canvas plumbing). Only the label editor in floating-edge.tsx
 * reads this. */
export interface DiagramEditContextValue {
    onEdgeLabelChange?: (edgeId: string, label: string) => void;
    onEdgeDelete?: (edgeId: string) => void;
}

export const DiagramEditContext = createContext<DiagramEditContextValue>({});
