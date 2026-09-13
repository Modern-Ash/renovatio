import React from '@theia/core/shared/react';
import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { DomainProperty } from './renovatio-shell-widget';

export interface DomainErNodeData {
    label: string;
    tableName?: string | null;
    sourceDataset?: string | null;
    properties: DomainProperty[];
    excluded?: boolean;
    exclusionReason?: string;
    pendingSuggestionCount?: number;
    [key: string]: unknown;
}

/**
 * Renders one DomainNode as an ER table box (issue #266): table name in the
 * header, columns below with a key icon on isKey properties. This is a
 * distinct visual mode from DomainClassNode (#265) — same underlying
 * DomainNode/DomainProperty data, a different projection, per the epic's
 * "DER extends DomainModel, it is not a separate persisted model" decision.
 */
export function DomainErNode(props: NodeProps): React.ReactElement {
    const data = props.data as unknown as DomainErNodeData;
    return (
        <div
            className={`renovatio-domain-er-node ${data.excluded ? 'is-excluded' : ''} ${props.selected ? 'is-selected' : ''}`}
            title={data.excluded ? data.exclusionReason || 'Excluded from generation' : undefined}
        >
            <Handle type='target' position={Position.Left} />
            <Handle type='source' position={Position.Right} />
            <header>
                <strong>{data.tableName || data.label}</strong>
                {data.sourceDataset && <small>source {data.sourceDataset}</small>}
                {Boolean(data.pendingSuggestionCount) && <span className='renovatio-domain-suggestion-badge'>{data.pendingSuggestionCount}</span>}
            </header>
            <table className='renovatio-domain-er-columns'>
                <tbody>
                    {data.properties.length ? data.properties.map((property, index) => (
                        <tr key={`${property.name}:${index}`} className={property.isKey ? 'is-key' : undefined}>
                            <td className='renovatio-domain-er-key'>{property.isKey ? 'PK' : ''}</td>
                            <td className='renovatio-domain-er-column'>{property.columnName ?? property.name}</td>
                            <td className='renovatio-domain-er-type'>{property.type}{property.required ? ' NOT NULL' : ''}</td>
                        </tr>
                    )) : <tr><td colSpan={3} className='renovatio-domain-class-empty'>No columns</td></tr>}
                </tbody>
            </table>
        </div>
    );
}
