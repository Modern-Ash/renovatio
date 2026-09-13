import React from '@theia/core/shared/react';
import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { DomainProperty } from './renovatio-shell-widget';

/** DOMAIN_KINDS -> a short badge color class. Kept in sync manually with the
 * DOMAIN_KINDS constant in renovatio-shell-widget.tsx; an unknown kind falls
 * back to the neutral badge rather than failing to render. */
const KIND_BADGE_CLASS: Record<string, string> = {
    ENTITY: 'is-entity',
    VALUE_OBJECT: 'is-value-object',
    AGGREGATE: 'is-aggregate',
    USE_CASE: 'is-use-case',
    DOMAIN_SERVICE: 'is-domain-service',
    REPOSITORY: 'is-repository',
    EXTERNAL_SYSTEM: 'is-external-system',
    EVENT: 'is-event',
    BOUNDED_CONTEXT: 'is-bounded-context'
};

export interface DomainClassNodeData {
    label: string;
    kind: string;
    properties: DomainProperty[];
    confidence?: number;
    [key: string]: unknown;
}

/**
 * Renders one DomainNode as a UML-style class box: name + stereotype badge
 * in the header, properties (name: type, marked * if required) in the body.
 * Selection/positioning/dragging is handled by React Flow itself
 * (@renovatio/diagram-canvas); this component only renders the box.
 */
export function DomainClassNode(props: NodeProps): React.ReactElement {
    const data = props.data as unknown as DomainClassNodeData;
    const badgeClass = KIND_BADGE_CLASS[data.kind] ?? 'is-other';
    return (
        <div className={`renovatio-domain-class-node ${props.selected ? 'is-selected' : ''}`}>
            <Handle type='target' position={Position.Left} />
            <Handle type='source' position={Position.Right} />
            <header>
                <span className={`renovatio-domain-class-badge ${badgeClass}`}>{data.kind}</span>
                <strong>{data.label}</strong>
            </header>
            <ul className='renovatio-domain-class-properties'>
                {data.properties.length
                    ? data.properties.map((property, index) => (
                        <li key={`${property.name}:${index}`}>
                            <span className='renovatio-domain-class-property-name'>{property.name}</span>
                            <span className='renovatio-domain-class-property-type'>: {property.type}{property.required ? '*' : ''}</span>
                        </li>
                    ))
                    : <li className='renovatio-domain-class-empty'>No properties</li>}
            </ul>
        </div>
    );
}
