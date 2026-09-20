import * as vscode from 'vscode';
import { RenovatioWorkspaceManifestService, type RenovatioWorkspaceManifest } from './workspaceManifest';
import {
    formatMigrationMap as formatMigrationMapCore,
    migrationEntryId,
    targetPathForMigration,
    validateMigrationMapArtifact as validateMigrationMapArtifactCore
} from './workbenchCore';

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
                    if (!isWorkspaceRelativePath(location.path)) {
                        issues.push(`entries[${index}].${side}.path must be workspace-relative and stay inside the workspace.`);
                        continue;
                    }
                    const uri = vscode.Uri.joinPath(folder.uri, ...location.path.split('/'));
                    if (side === 'source' && !await exists(uri)) {
                        issues.push(`entries[${index}].${side}.path does not exist: ${location.path}`);
                    }
                    if (side === 'target' && targetMustExist(entry) && !await exists(uri)) {
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
    return formatMigrationMapCore(value);
}

export function validateMigrationMapArtifact(value: unknown): string[] {
    return validateMigrationMapArtifactCore(value);
}

async function sourceCandidates(folder: vscode.WorkspaceFolder, manifest: RenovatioWorkspaceManifest): Promise<string[]> {
    const files: vscode.Uri[] = [];
    const exclude = globAlternatives(manifest.source.exclude);
    for (const include of manifest.source.include) {
        files.push(...await vscode.workspace.findFiles(new vscode.RelativePattern(folder, include), exclude));
    }
    const roots = manifest.source.roots.map(root => root.replace(/\\/g, '/'));
    return [...new Set(files.map(uri => uri.toString()))]
        .map(value => vscode.Uri.parse(value))
        .map(uri => relativePath(folder, uri))
        .filter(file => roots.length === 0 || roots.some(root => file === root || file.startsWith(`${root}/`)))
        .sort();
}

function targetPathFor(target: RenovatioWorkspaceManifest['targets'][number], sourcePath: string, roots: string[]): string {
    return targetPathForMigration(target, sourcePath, roots);
}

function relativePath(folder: vscode.WorkspaceFolder, uri: vscode.Uri): string {
    const root = folder.uri.fsPath.replace(/\\/g, '/');
    const file = uri.fsPath.replace(/\\/g, '/');
    return file.startsWith(`${root}/`) ? file.slice(root.length + 1) : file;
}

function stableEntryId(path: string, index: number): string {
    return migrationEntryId(kindFromPath(path), path, index);
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

function targetMustExist(entry: Record<string, unknown>): boolean {
    return ['generated', 'manually-edited', 'stale-target'].includes(String(entry.status));
}

function isWorkspaceRelativePath(path: string): boolean {
    if (path.startsWith('/') || /^[A-Za-z]:[\\/]/.test(path) || /^[a-z][a-z0-9+.-]*:/i.test(path)) return false;
    return !path.replace(/\\/g, '/').split('/').some(part => part === '..');
}

function globAlternatives(patterns: string[]): string | undefined {
    const cleaned = [...new Set(patterns.filter(pattern => pattern.trim().length > 0))];
    if (!cleaned.length) return undefined;
    return cleaned.length === 1 ? cleaned[0] : `{${cleaned.join(',')}}`;
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
