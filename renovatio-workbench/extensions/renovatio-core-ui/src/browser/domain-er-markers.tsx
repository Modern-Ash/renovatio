import React from '@theia/core/shared/react';

/**
 * Crow's-foot cardinality glyphs for the Domain DER view (issue #266).
 * Each CARDINALITIES value maps to one marker id; DiagramCanvas renders
 * these <marker> defs once (via its `defs` prop) and edges reference them
 * by id through `edgeMarkerFor` — see domain-diagram-mapper's edge data
 * (sourceCardinality/targetCardinality) and renderDomainDerSurface.
 *
 * Notation (read at the END the marker sits on, pointing away from the line):
 *  - ONE            -> a single perpendicular bar
 *  - ZERO_OR_ONE    -> a circle followed by a single bar
 *  - ONE_OR_MORE    -> a bar followed by a crow's foot
 *  - ZERO_OR_MORE   -> a circle followed by a crow's foot
 *
 * These are intentionally simple path-based glyphs, not a pixel-perfect
 * ERD toolkit's rendering — good enough to read the notation at canvas zoom
 * levels without pulling in a diagramming library beyond React Flow.
 */
export const CARDINALITY_MARKER_ID: Record<string, string> = {
    ONE: 'renovatio-crowsfoot-one',
    ZERO_OR_ONE: 'renovatio-crowsfoot-zero-or-one',
    ONE_OR_MORE: 'renovatio-crowsfoot-one-or-more',
    ZERO_OR_MORE: 'renovatio-crowsfoot-zero-or-more'
};

export function markerUrl(cardinality: string | undefined): string | undefined {
    const id = cardinality ? CARDINALITY_MARKER_ID[cardinality] : undefined;
    return id ? `url(#${id})` : undefined;
}

const STROKE = '#2de1c2';

export function DomainErMarkerDefs(): React.ReactElement {
    return (
        <>
            <marker id={CARDINALITY_MARKER_ID.ONE} viewBox='0 0 20 20' markerWidth='16' markerHeight='16' refX='16' refY='10' orient='auto-start-reverse'>
                <path d='M12 3 L12 17 M16 3 L16 17' stroke={STROKE} strokeWidth='1.6' fill='none' />
            </marker>
            <marker id={CARDINALITY_MARKER_ID.ZERO_OR_ONE} viewBox='0 0 24 20' markerWidth='20' markerHeight='16' refX='20' refY='10' orient='auto-start-reverse'>
                <circle cx='7' cy='10' r='4.5' stroke={STROKE} strokeWidth='1.4' fill='none' />
                <path d='M16 3 L16 17' stroke={STROKE} strokeWidth='1.6' fill='none' />
            </marker>
            <marker id={CARDINALITY_MARKER_ID.ONE_OR_MORE} viewBox='0 0 24 20' markerWidth='20' markerHeight='16' refX='20' refY='10' orient='auto-start-reverse'>
                <path d='M14 3 L14 17' stroke={STROKE} strokeWidth='1.6' fill='none' />
                <path d='M20 10 L4 3 M20 10 L4 17 M20 10 L4 10' stroke={STROKE} strokeWidth='1.4' fill='none' />
            </marker>
            <marker id={CARDINALITY_MARKER_ID.ZERO_OR_MORE} viewBox='0 0 28 20' markerWidth='24' markerHeight='16' refX='24' refY='10' orient='auto-start-reverse'>
                <circle cx='6' cy='10' r='4.5' stroke={STROKE} strokeWidth='1.4' fill='none' />
                <path d='M24 10 L11 3 M24 10 L11 17 M24 10 L11 10' stroke={STROKE} strokeWidth='1.4' fill='none' />
            </marker>
        </>
    );
}
