import React from '@theia/core/shared/react';
import { Handle, Position, type NodeProps } from '@xyflow/react';
import type { ArchitectureDiagnostic } from './renovatio-shell-widget';

export interface ArchitectureNodeData {
    label: string;
    kind: string;
    layer?: string;
    packageName?: string;
    className?: string;
    componentId?: string;
    componentCount?: number;
    pendingSuggestionCount?: number;
    excluded?: boolean;
    exclusionReason?: string;
    diagnostics?: ArchitectureDiagnostic[];
    [key: string]: unknown;
}

function kindLabel(kind: string): string {
    return kind === 'ARCHITECTURE_LAYER' ? 'LAYER' : kind.replace(/_/g, ' ');
}

export function ArchitectureNode(props: NodeProps): React.ReactElement {
    const data = props.data as unknown as ArchitectureNodeData;
    const hasDiagnostics = Boolean(data.diagnostics?.length);
    const isLayer = data.kind === 'ARCHITECTURE_LAYER';
    return (
        <div
            className={`renovatio-architecture-flow-node ${isLayer ? 'is-layer' : 'is-component'} ${data.excluded ? 'is-excluded' : ''} ${hasDiagnostics ? 'has-diagnostics' : ''} ${props.selected ? 'is-selected' : ''}`}
            title={data.excluded ? data.exclusionReason || 'Excluded from generation' : hasDiagnostics ? data.diagnostics?.map(diagnostic => diagnostic.message).join('\n') : undefined}
        >
            <Handle type='target' position={Position.Left} />
            <Handle type='source' position={Position.Right} />
            <header>
                <span>{kindLabel(data.kind)}</span>
                {isLayer && typeof data.componentCount === 'number' && <em>{data.componentCount}</em>}
                {Boolean(data.pendingSuggestionCount) && <em className='renovatio-architecture-suggestion-badge'>{data.pendingSuggestionCount}</em>}
            </header>
            <strong>{data.label}{data.excluded ? ' (excluded)' : ''}</strong>
            <p>{data.packageName || data.layer || 'unmapped package'}</p>
            <small>{data.className || data.componentId || 'generated component'}</small>
        </div>
    );
}
