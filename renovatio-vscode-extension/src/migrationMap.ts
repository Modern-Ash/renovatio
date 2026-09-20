import * as vscode from 'vscode';
import { RenovatioWorkspaceManifestService, type RenovatioWorkspaceManifest } from './workspaceManifest';

export const MIGRATION_MAP_SCHEMA_VERSION = '1';

export type MigrationMapStatus =
    | 'proposed'
    | 'accepted'
    | 'generated'
    | 'manually-edited'
    | 'stale-source'
    | 'stale-target'
    | 'needs-review'
    | 'rejected';

export type MigrationMapEntryKind =
    | 'program'
    | 'paragraph'
    | 'section'
    | 'copybook'
    | 'record'
    | 'field'
    | 'jcl-job'
    | 'jcl-step'
    | 'business-rule'
    | 'dataset'
    | 'table'
    | 'test-fixture';

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
    kind: MigrationMapEntryKind;
    source?: MigrationLocation;
    renovatio?: RenovatioTrace;
    target?: MigrationLocation;
    status: MigrationMapStatus;
    confidence?: number;
    evidence: string[];
    lastDecision?: MigrationDecision;
}

export interface MigrationLocation {
    language: string;
    path: string;
    symbol?: string;
    range?: MigrationRange;
    hash?: string;
}

export interface MigrationRange {
    startLine: number;
    startColumn: number;
    endLine: number;
    endColumn: number;
}

export interface RenovatioTrace {
    domainNodeIds: string[];
    architectureNodeIds: string[];
    semanticIds: string[];
}

export interface MigrationDecision {
    actor: string;
    action: string;
    at: string;
    reason?: string;
}

export class MigrationMapService implements vscode.Disposable {
    private readonly diagnostics = vscode.languages.createDiagnosticCollection('renovatio-migration-map');
    private readonly disposables: vscode.Disposable[] = [];

    constructor(
        private readonly manifestService: RenovatioWorkspaceManifestService,
        private readonly output: vscode.OutputChannel
    ) {
        this.disposables.push(
            this.diagnostics,
            vscode.workspace.onDidOpenTextDocument(document => this.validateDocumentIfMigrationMap(document)),
            vscode.workspace.onDidSaveTextDocument(document => this.validateDocumentIfMigrationMap(document))
        );
    }

    dispose(): void {
        this.disposables.forEach(disposable => disposable.dispose());
    }

    async createMigrationMap(): Promise<void> {
        const context = await this.context();
        if (!context) return;
        const { folder, manifest } = context;
        const uri = this.mapUri(folder, manifest);
        if (await exists(uri)) {
            const selected = await vscode.window.showWarningMessage(
                'Renovatio migration map already exists. Overwrite it?',
                { modal: true },
                'Overwrite'
            );
            if (selected !== 'Overwrite') return;
        }
        await vscode.workspace.fs.createDirectory(parentUri(uri));
        const artifact = await this.createDefaultMap(folder, manifest);
        await vscode.workspace.fs.writeFile(uri, encodeJson(artifact));
        await this.openMigrationMap();
        await this.validateMigrationMap();
    }

    async openMigrationMap(): Promise<void> {
        const context = await this.context();
        if (!context) return;
        const uri = this.mapUri(context.folder, context.manifest);
        if (!await exists(uri)) {
            const selected = await vscode.window.showInformationMessage(
                'Migration map does not exist yet.',
                'Create Migration Map'
            );
            if (selected === 'Create Migration Map') {
                await this.createMigrationMap();
            }
            return;
        }
        await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(uri), { preview: false });
    }

    async validateMigrationMap(): Promise<boolean> {
        const context = await this.context();
        if (!context) return false;
        const uri = this.mapUri(context.folder, context.manifest);
        if (!await exists(uri)) {
            this.diagnostics.delete(uri);
            vscode.window.showWarningMessage('Migration map does not exist yet.');
            return false;
        }
        const document = await vscode.workspace.openTextDocument(uri);
        const diagnostics = await this.validateText(document.getText(), context.folder);
        this.diagnostics.set(uri, diagnostics);
        if (diagnostics.length) {
            vscode.window.showWarningMessage('Migration map validation found issues. See Problems.');
            return false;
        }
        vscode.window.showInformationMessage('Migration map validation passed.');
        return true;
    }

    async formatMigrationMap(): Promise<void> {
        const context = await this.context();
        if (!context) return;
        const uri = this.mapUri(context.folder, context.manifest);
        if (!await exists(uri)) {
            await this.createMigrationMap();
            return;
        }
        const document = await vscode.workspace.openTextDocument(uri);
        const parsed = parseJson(document.getText()) as MigrationMapArtifact;
        const formatted = formatMigrationMap(parsed);
        if (formatted === document.getText()) return;
        const edit = new vscode.WorkspaceEdit();
        const end = document.lineCount === 0
            ? new vscode.Position(0, 0)
            : document.lineAt(document.lineCount - 1).rangeIncludingLineBreak.end;
        edit.replace(document.uri, new vscode.Range(new vscode.Position(0, 0), end), formatted);
        const applied = await vscode.workspace.applyEdit(edit);
        if (!applied) throw new Error('VS Code rejected the migration map format edit.');
        await document.save();
        await this.validateMigrationMap();
    }

    async load(): Promise<MigrationMapArtifact | undefined> {
        const context = await this.context();
        if (!context) return undefined;
        const uri = this.mapUri(context.folder, context.manifest);
        if (!await exists(uri)) return undefined;
        return parseJson(decodeBytes(await vscode.workspace.fs.readFile(uri))) as MigrationMapArtifact;
    }

    private async createDefaultMap(
        folder: vscode.WorkspaceFolder,
        manifest: RenovatioWorkspaceManifest
    ): Promise<MigrationMapArtifact> {
        const sourceRoots = manifest.source.roots.map(root => root.replace(/\\/g, '/'));
        const sourceFiles = await sourceCandidates(folder, manifest);
        const target = manifest.targets[0];
        const entries: MigrationMapEntry[] = sourceFiles.map((path, index) => {
            const symbol = symbolFromPath(path);
            return {
                id: stableEntryId(path, index),
                kind: kindFromPath(path),
                source: {
                    language: manifest.source.language,
                    path,
                    symbol
                },
                renovatio: {
                    domainNodeIds: [],
                    architectureNodeIds: [],
                    semanticIds: symbol ? [`semantic:${symbol}`] : []
                },
                target: target ? {
                    language: target.language,
                    path: targetPathFor(target, path, sourceRoots),
                    symbol: symbol ? targetSymbol(symbol) : undefined
                } : undefined,
                status: 'proposed',
                confidence: 0,
                evidence: []
            };
        });
        return {
            version: '1',
            projectId: manifest.projectId,
            generatedAt: new Date().toISOString(),
            entries
        };
    }

    private async validateDocumentIfMigrationMap(document: vscode.TextDocument): Promise<void> {
        if (!document.uri.fsPath.endsWith('migration-map.renovatio.json')) return;
        const folder = vscode.workspace.getWorkspaceFolder(document.uri);
        if (!folder) return;
        this.diagnostics.set(document.uri, await this.validateText(document.getText(), folder));
    }

    private async validateText(text: string, folder: vscode.WorkspaceFolder): Promise<vscode.Diagnostic[]> {
        let parsed: unknown;
        try {
            parsed = parseJson(text);
        } catch (error) {
            return [diagnostic(`Invalid JSON: ${message(error)}`)];
        }
        const issues = validateMigrationMapArtifact(parsed);
        if (isRecord(parsed) && Array.isArray(parsed.entries)) {
            for (const [index, entry] of parsed.entries.entries()) {
                if (!isRecord(entry)) continue;
                for (const side of ['source', 'target'] as const) {
                    const location = entry[side];
                    if (!isRecord(location) || typeof location.path !== 'string') continue;
                    if (isAbsolutePath(location.path)) {
                        issues.push(`entries[${index}].${side}.path must be workspace-relative.`);
                        continue;
                    }
                    const uri = vscode.Uri.joinPath(folder.uri, ...location.path.split('/'));
                    if (!await exists(uri)) {
                        issues.push(`entries[${index}].${side}.path does not exist: ${location.path}`);
                    }
                }
            }
        }
        return issues.map(diagnostic);
    }

    private async context(): Promise<{ folder: vscode.WorkspaceFolder; manifest: RenovatioWorkspaceManifest } | undefined> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) {
            vscode.window.showWarningMessage('Open a VS Code workspace before using migration map commands.');
            return undefined;
        }
        const manifest = await this.manifestService.load(folder);
        if (!manifest) {
            const selected = await vscode.window.showInformationMessage(
                'Renovatio workspace manifest is required before creating a migration map.',
                'Initialize Workspace'
            );
            if (selected === 'Initialize Workspace') {
                await this.manifestService.initializeWorkspace();
            }
            return undefined;
        }
        return { folder, manifest };
    }

    private mapUri(folder: vscode.WorkspaceFolder, manifest: RenovatioWorkspaceManifest): vscode.Uri {
        return vscode.Uri.joinPath(folder.uri, ...manifest.artifacts.migrationMap.split('/'));
    }
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

const STATUSES: string[] = ['proposed', 'accepted', 'generated', 'manually-edited', 'stale-source', 'stale-target', 'needs-review', 'rejected'];
const ENTRY_KINDS: string[] = ['program', 'paragraph', 'section', 'copybook', 'record', 'field', 'jcl-job', 'jcl-step', 'business-rule', 'dataset', 'table', 'test-fixture'];

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

async function sourceCandidates(folder: vscode.WorkspaceFolder, manifest: RenovatioWorkspaceManifest): Promise<string[]> {
    const files: vscode.Uri[] = [];
    for (const include of manifest.source.include) {
        files.push(...await vscode.workspace.findFiles(new vscode.RelativePattern(folder, include), '**/{node_modules,target,.git}/**', 200));
    }
    const roots = manifest.source.roots.map(root => root.replace(/\\/g, '/'));
    return [...new Set(files.map(uri => uri.toString()))]
        .map(value => vscode.Uri.parse(value))
        .map(uri => relativePath(folder, uri))
        .filter(file => roots.length === 0 || roots.some(root => file === root || file.startsWith(`${root}/`)))
        .sort();
}

function targetPathFor(target: RenovatioWorkspaceManifest['targets'][number], sourcePath: string, roots: string[]): string {
    const root = roots.find(candidate => sourcePath.startsWith(`${candidate}/`));
    const relative = root ? sourcePath.slice(root.length + 1) : sourcePath;
    const base = relative.replace(/\.[^.]+$/, '');
    if (target.language === 'java') {
        const packagePath = (target.package ?? '').replace(/\./g, '/');
        return [target.root, 'src/main/java', packagePath, `${targetSymbol(symbolFromPath(sourcePath) ?? base)}.java`]
            .filter(Boolean)
            .join('/');
    }
    return `${target.root}/${base}`;
}

function relativePath(folder: vscode.WorkspaceFolder, uri: vscode.Uri): string {
    const root = folder.uri.fsPath.replace(/\\/g, '/');
    const file = uri.fsPath.replace(/\\/g, '/');
    return file.startsWith(`${root}/`) ? file.slice(root.length + 1) : file;
}

function stableEntryId(path: string, index: number): string {
    const symbol = symbolFromPath(path);
    return `${kindFromPath(path)}:${symbol ?? index}`;
}

function kindFromPath(path: string): MigrationMapEntryKind {
    const lower = path.toLowerCase();
    if (lower.endsWith('.jcl') || lower.endsWith('.job') || lower.endsWith('.proc')) return 'jcl-job';
    if (lower.endsWith('.cpy') || lower.endsWith('.copybook')) return 'copybook';
    return 'program';
}

function symbolFromPath(path: string): string | undefined {
    const fileName = path.split('/').pop();
    if (!fileName) return undefined;
    return fileName.replace(/\.[^.]+$/, '').toUpperCase();
}

function targetSymbol(symbol: string): string {
    return symbol.toLowerCase().replace(/(^|[-_])([a-z0-9])/g, (_match, _prefix, value: string) => value.toUpperCase());
}

function diagnostic(messageText: string): vscode.Diagnostic {
    return new vscode.Diagnostic(
        new vscode.Range(new vscode.Position(0, 0), new vscode.Position(0, 1)),
        messageText,
        vscode.DiagnosticSeverity.Error
    );
}

function parseJson(text: string): unknown {
    return JSON.parse(text);
}

function encodeJson(value: unknown): Uint8Array {
    return new TextEncoder().encode(`${JSON.stringify(value, null, 2)}\n`);
}

function decodeBytes(value: Uint8Array): string {
    return new TextDecoder('utf-8').decode(value);
}

function parentUri(uri: vscode.Uri): vscode.Uri {
    const parts = uri.path.split('/');
    parts.pop();
    return uri.with({ path: parts.join('/') || '/' });
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return Boolean(value && typeof value === 'object' && !Array.isArray(value));
}

function isAbsolutePath(path: string): boolean {
    return path.startsWith('/') || /^[A-Za-z]:[\\/]/.test(path);
}

async function exists(uri: vscode.Uri): Promise<boolean> {
    try {
        await vscode.workspace.fs.stat(uri);
        return true;
    } catch {
        return false;
    }
}

function message(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
