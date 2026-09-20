export type MigrationMapStatus =
    | 'proposed'
    | 'accepted'
    | 'generated'
    | 'manually-edited'
    | 'stale-source'
    | 'stale-target'
    | 'needs-review'
    | 'rejected';

export interface MigrationMapArtifact {
    version: '1';
    projectId: string;
    generatedAt: string;
    sourceHash?: string;
    targetHash?: string;
    entries: MigrationMapEntry[];
}

export interface MigrationMapEntry {
    id: string;
    kind: string;
    source?: MigrationLocation;
    renovatio?: {
        domainNodeIds: string[];
        architectureNodeIds: string[];
        semanticIds: string[];
    };
    target?: MigrationLocation;
    status: MigrationMapStatus;
    confidence?: number;
    evidence: string[];
    lastDecision?: {
        actor: string;
        action: string;
        at: string;
        reason?: string;
    };
}

export interface MigrationLocation {
    language: string;
    path: string;
    symbol?: string;
    range?: {
        startLine: number;
        startColumn: number;
        endLine: number;
        endColumn: number;
    };
    hash?: string;
}

export type NavigationSide = 'source' | 'target';

const STATUSES = ['proposed', 'accepted', 'generated', 'manually-edited', 'stale-source', 'stale-target', 'needs-review', 'rejected'];
const ENTRY_KINDS = ['program', 'paragraph', 'section', 'copybook', 'record', 'field', 'jcl-job', 'jcl-step', 'business-rule', 'dataset', 'table', 'test-fixture'];

export function parseJsonObject(text: string, label: string): Record<string, unknown> {
    const parsed = JSON.parse(text) as unknown;
    if (!isRecord(parsed)) {
        throw new Error(`${label} must be a JSON object.`);
    }
    return parsed;
}

export function formatMigrationMap(value: MigrationMapArtifact): string {
    const normalized: MigrationMapArtifact = {
        ...value,
        entries: [...(value.entries ?? [])].sort((left, right) => left.id.localeCompare(right.id))
    };
    return `${JSON.stringify(normalized, null, 2)}\n`;
}

export function validateMigrationMapArtifact(value: unknown): string[] {
    const issues: string[] = [];
    if (!isRecord(value)) return ['Migration map must be a JSON object.'];
    requireString(value, 'version', issues, ['1']);
    requireString(value, 'projectId', issues);
    requireString(value, 'generatedAt', issues);
    if (!Array.isArray(value.entries)) {
        issues.push('entries must be an array.');
        return issues;
    }
    const ids = new Set<string>();
    value.entries.forEach((entry, index) => {
        if (!isRecord(entry)) {
            issues.push(`entries[${index}] must be an object.`);
            return;
        }
        requireString(entry, 'id', issues);
        if (typeof entry.id === 'string') {
            if (ids.has(entry.id)) issues.push(`entries[${index}].id is duplicated: ${entry.id}`);
            ids.add(entry.id);
        }
        requireString(entry, 'kind', issues, ENTRY_KINDS);
        requireString(entry, 'status', issues, STATUSES);
        if (entry.confidence !== undefined && (typeof entry.confidence !== 'number' || entry.confidence < 0 || entry.confidence > 1)) {
            issues.push(`entries[${index}].confidence must be between 0 and 1.`);
        }
        if (!Array.isArray(entry.evidence) || entry.evidence.some(value => typeof value !== 'string')) {
            issues.push(`entries[${index}].evidence must be an array of strings.`);
        }
        validateLocation(entry.source, `entries[${index}].source`, issues);
        validateLocation(entry.target, `entries[${index}].target`, issues);
    });
    return issues;
}

export function entriesForMigrationPath(
    artifact: MigrationMapArtifact,
    documentPath: string,
    side: NavigationSide
): MigrationMapEntry[] {
    const path = normalizePath(documentPath);
    return artifact.entries.filter(entry => normalizePath(entry[side]?.path) === path);
}

export function migrationHoverMarkdown(entry: MigrationMapEntry, side: NavigationSide): string {
    const source = entry.source;
    const target = entry.target;
    const confidence = typeof entry.confidence === 'number' ? `${Math.round(entry.confidence * 100)}%` : 'unknown';
    const decision = entry.lastDecision
        ? `${entry.lastDecision.action} by ${entry.lastDecision.actor} at ${entry.lastDecision.at}`
        : 'none';
    const warnings = staleWarnings(entry);
    const openOtherCommand = side === 'source' ? 'renovatio.openMigrationTarget' : 'renovatio.openMigrationSource';
    const openOtherLabel = side === 'source' ? 'Open target' : 'Open legacy source';
    const evidenceCount = entry.evidence?.length ?? 0;
    const lines = [
        `**Renovatio migration** \`${entry.id}\``,
        '',
        `- Status: \`${entry.status}\``,
        `- Confidence: ${confidence}`,
        `- Source: ${formatLocation(source)}`,
        `- Target: ${formatLocation(target)}`,
        `- Evidence: ${evidenceCount}`,
        `- Last decision: ${decision}`
    ];
    if (side === 'target') {
        lines.push(`- Source hash: ${source?.hash ?? 'not recorded'}`);
        lines.push(`- Target hash: ${target?.hash ?? 'not recorded'}`);
    }
    if (warnings.length) lines.push(`- Warnings: ${warnings.join(', ')}`);
    lines.push('');
    lines.push(`[${openOtherLabel}](command:${openOtherCommand}?${encodeURIComponent(JSON.stringify([entry.id]))})`);
    lines.push(`[Show evidence](command:renovatio.showMigrationEvidence?${encodeURIComponent(JSON.stringify([entry.id]))})`);
    return lines.join('\n');
}

export function staleWarnings(entry: MigrationMapEntry): string[] {
    const warnings: string[] = [];
    if (entry.status === 'stale-source') warnings.push('source changed');
    if (entry.status === 'stale-target') warnings.push('target changed');
    if (entry.status === 'needs-review') warnings.push('needs review');
    return warnings;
}

export function classifyHashState(recordedHash: string | undefined, actualHash: string | undefined, side: NavigationSide): 'missing' | 'clean' | 'stale-source' | 'stale-target' {
    if (!recordedHash || !actualHash) return 'missing';
    return sameHash(recordedHash, actualHash) ? 'clean' : side === 'source' ? 'stale-source' : 'stale-target';
}

export function sameHash(left: string, right: string): boolean {
    return normalizeHash(left) === normalizeHash(right);
}

export async function sha256Text(value: string): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
    return `sha256:${[...new Uint8Array(digest)].map(byte => byte.toString(16).padStart(2, '0')).join('')}`;
}

export function workspaceRelativePath(workspaceRoot: string, filePath: string): string {
    const root = normalizeFilePath(workspaceRoot).replace(/\/+$/, '');
    const file = normalizeFilePath(filePath);
    return file.startsWith(`${root}/`) ? file.slice(root.length + 1) : file;
}

export function migrationRangeContains(
    range: MigrationLocation['range'] | undefined,
    line: number,
    column: number
): boolean {
    if (!range) return line === 0;
    const startLine = Math.max(0, range.startLine - 1);
    const endLine = Math.max(startLine, range.endLine - 1);
    const startColumn = Math.max(0, range.startColumn - 1);
    const endColumn = Math.max(startColumn, range.endColumn - 1);
    if (line < startLine || line > endLine) return false;
    if (line === startLine && column < startColumn) return false;
    if (line === endLine && column > endColumn) return false;
    return true;
}

function validateLocation(value: unknown, label: string, issues: string[]): void {
    if (value === undefined) return;
    if (!isRecord(value)) {
        issues.push(`${label} must be an object.`);
        return;
    }
    requireString(value, 'language', issues);
    requireString(value, 'path', issues);
    const range = value.range;
    if (range !== undefined) {
        if (!isRecord(range)) {
            issues.push(`${label}.range must be an object.`);
            return;
        }
        for (const key of ['startLine', 'startColumn', 'endLine', 'endColumn']) {
            if (!Number.isInteger(range[key]) || Number(range[key]) < 1) {
                issues.push(`${label}.range.${key} must be a positive integer.`);
            }
        }
    }
}

function requireString(target: Record<string, unknown>, key: string, issues: string[], allowed?: string[]): void {
    const value = target[key];
    if (typeof value !== 'string' || value.trim() === '') {
        issues.push(`${key} must be a non-empty string.`);
        return;
    }
    if (allowed && !allowed.includes(value)) {
        issues.push(`${key} must be one of: ${allowed.join(', ')}.`);
    }
}

function formatLocation(location: MigrationLocation | undefined): string {
    if (!location) return 'not mapped';
    const symbol = location.symbol ? `#${location.symbol}` : '';
    return `\`${location.path}${symbol}\``;
}

function normalizePath(value: string | undefined): string {
    return String(value ?? '').replace(/\\/g, '/');
}

function normalizeFilePath(value: string): string {
    return value.replace(/\\/g, '/');
}

function normalizeHash(value: string): string {
    return value.trim().toLowerCase().replace(/^sha256:/, '');
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return Boolean(value && typeof value === 'object' && !Array.isArray(value));
}
