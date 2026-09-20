import * as vscode from 'vscode';
import {
    WORKSPACE_MANIFEST_RELATIVE_PATH,
    type RenovatioWorkspaceManifest
} from './workspaceManifest';
import {
    type MigrationLocation,
    type MigrationMapArtifact,
    type MigrationMapEntry
} from './migrationMap';

const SUPPORTED_SOURCE_LANGUAGES = new Set(['cobol']);
const SUPPORTED_TARGET_LANGUAGES = new Set(['java', 'python', 'node']);
const LOCAL_ENVIRONMENTS = new Set(['local', 'dev']);
const MAP_STATUSES = new Set([
    'proposed',
    'accepted',
    'generated',
    'manually-edited',
    'stale-source',
    'stale-target',
    'needs-review',
    'rejected'
]);

type DiagnosticMap = Map<string, { uri: vscode.Uri; diagnostics: vscode.Diagnostic[] }>;

interface ParsedDocument<T> {
    uri: vscode.Uri;
    text: string;
    value: T;
}

export class RenovatioArtifactDiagnosticsService implements vscode.Disposable {
    private readonly manifestDiagnostics = vscode.languages.createDiagnosticCollection('renovatio-manifest-contract');
    private readonly migrationMapDiagnostics = vscode.languages.createDiagnosticCollection('renovatio-migration-contract');
    private readonly fileDiagnostics = vscode.languages.createDiagnosticCollection('renovatio-migration-stale-state');
    private readonly disposables: vscode.Disposable[] = [];
    private refreshQueue: Promise<void> = Promise.resolve();

    constructor(private readonly output: vscode.OutputChannel) {
        this.disposables.push(
            this.manifestDiagnostics,
            this.migrationMapDiagnostics,
            this.fileDiagnostics,
            vscode.workspace.createFileSystemWatcher('**/.renovatio/**/*.json'),
            vscode.workspace.onDidSaveTextDocument(() => this.scheduleRefresh()),
            vscode.workspace.onDidChangeWorkspaceFolders(() => this.scheduleRefresh())
        );
        const watcher = this.disposables.find(isFileSystemWatcher);
        watcher?.onDidCreate(() => this.scheduleRefresh(), undefined, this.disposables);
        watcher?.onDidChange(() => this.scheduleRefresh(), undefined, this.disposables);
        watcher?.onDidDelete(() => this.scheduleRefresh(), undefined, this.disposables);
    }

    dispose(): void {
        this.disposables.forEach(disposable => disposable.dispose());
    }

    refreshAll(): Promise<void> {
        this.refreshQueue = this.refreshQueue
            .catch(() => undefined)
            .then(() => this.refreshNow());
        return this.refreshQueue;
    }

    private scheduleRefresh(): void {
        void this.refreshAll();
    }

    private async refreshNow(): Promise<void> {
        this.manifestDiagnostics.clear();
        this.migrationMapDiagnostics.clear();
        this.fileDiagnostics.clear();

        const fileDiagnostics: DiagnosticMap = new Map();
        for (const folder of vscode.workspace.workspaceFolders ?? []) {
            await this.refreshWorkspace(folder, fileDiagnostics);
        }
        for (const value of fileDiagnostics.values()) {
            this.fileDiagnostics.set(value.uri, value.diagnostics);
        }
    }

    private async refreshWorkspace(folder: vscode.WorkspaceFolder, fileDiagnostics: DiagnosticMap): Promise<void> {
        const manifestUri = vscode.Uri.joinPath(folder.uri, ...WORKSPACE_MANIFEST_RELATIVE_PATH.split('/'));
        const manifest = await this.readJson<RenovatioWorkspaceManifest>(manifestUri);
        if (!manifest) return;

        const manifestDiagnostics = await this.validateManifest(folder, manifest);
        this.manifestDiagnostics.set(manifest.uri, manifestDiagnostics);

        const migrationMapPath = stringAt(manifest.value, ['artifacts', 'migrationMap']);
        if (!migrationMapPath || isAbsolutePath(migrationMapPath)) return;

        const migrationMapUri = workspaceUri(folder, migrationMapPath);
        const migrationMap = await this.readJson<MigrationMapArtifact>(migrationMapUri);
        if (!migrationMap) return;

        const mapDiagnostics = await this.validateMigrationMap(folder, migrationMap, manifest.value, fileDiagnostics);
        this.migrationMapDiagnostics.set(migrationMap.uri, mapDiagnostics);
    }

    private async readJson<T>(uri: vscode.Uri): Promise<ParsedDocument<T> | undefined> {
        if (!await exists(uri)) return undefined;
        const text = decodeBytes(await vscode.workspace.fs.readFile(uri));
        try {
            return { uri, text, value: parseJsonc(text) as T };
        } catch (error) {
            const target = uri.fsPath.endsWith('migration-map.renovatio.json')
                ? this.migrationMapDiagnostics
                : this.manifestDiagnostics;
            target.set(uri, [diagnostic(`Invalid JSON: ${message(error)}`, vscode.DiagnosticSeverity.Error)]);
            return undefined;
        }
    }

    private async validateManifest(
        folder: vscode.WorkspaceFolder,
        document: ParsedDocument<RenovatioWorkspaceManifest>
    ): Promise<vscode.Diagnostic[]> {
        const diagnostics: vscode.Diagnostic[] = [];
        const manifest = document.value as unknown;
        if (!isRecord(manifest)) {
            diagnostics.push(diagnostic('Workspace manifest must be a JSON object.', vscode.DiagnosticSeverity.Error));
            return diagnostics;
        }

        const projectId = stringAt(manifest, ['projectId']);
        if (!projectId) addJsonDiagnostic(diagnostics, document.text, 'projectId', 'projectId is required.', vscode.DiagnosticSeverity.Error);

        const sourceLanguage = stringAt(manifest, ['source', 'language']);
        if (!sourceLanguage) {
            addJsonDiagnostic(diagnostics, document.text, 'source', 'source.language is required.', vscode.DiagnosticSeverity.Error);
        } else if (!SUPPORTED_SOURCE_LANGUAGES.has(sourceLanguage)) {
            addJsonDiagnostic(diagnostics, document.text, sourceLanguage, `Unsupported source language: ${sourceLanguage}.`, vscode.DiagnosticSeverity.Error);
        }

        const sourceRoots = arrayAt(manifest, ['source', 'roots']).filter(isString);
        if (!sourceRoots.length) {
            addJsonDiagnostic(diagnostics, document.text, 'roots', 'source.roots must list at least one legacy source root.', vscode.DiagnosticSeverity.Warning);
        }
        validateWorkspacePaths(diagnostics, document.text, folder, sourceRoots, 'source.roots');

        const targets = arrayAt(manifest, ['targets']).filter(isRecord);
        if (!targets.length) {
            addJsonDiagnostic(diagnostics, document.text, 'targets', 'targets must contain at least one target.', vscode.DiagnosticSeverity.Error);
        }
        const targetRoots = new Set<string>();
        for (const [index, target] of targets.entries()) {
            const language = stringAt(target, ['language']);
            const root = stringAt(target, ['root']);
            if (!language) {
                addJsonDiagnostic(diagnostics, document.text, 'targets', `targets[${index}].language is required.`, vscode.DiagnosticSeverity.Error);
            } else if (!SUPPORTED_TARGET_LANGUAGES.has(language)) {
                addJsonDiagnostic(diagnostics, document.text, language, `Unsupported target language: ${language}.`, vscode.DiagnosticSeverity.Error);
            }
            if (!root) {
                addJsonDiagnostic(diagnostics, document.text, 'targets', `targets[${index}].root is required.`, vscode.DiagnosticSeverity.Error);
            } else {
                if (targetRoots.has(normalizePath(root))) {
                    addJsonDiagnostic(diagnostics, document.text, root, `Duplicate target root: ${root}.`, vscode.DiagnosticSeverity.Error);
                }
                targetRoots.add(normalizePath(root));
                validateWorkspacePaths(diagnostics, document.text, folder, [root], `targets[${index}].root`);
            }
        }

        const artifacts = objectAt(manifest, ['artifacts']);
        if (artifacts) {
            for (const key of ['domainModel', 'persistenceModel', 'architecture', 'migrationMap', 'evidenceDir']) {
                const value = stringAt(artifacts, [key]);
                if (!value) {
                    addJsonDiagnostic(diagnostics, document.text, key, `artifacts.${key} is required.`, vscode.DiagnosticSeverity.Error);
                } else {
                    validateWorkspacePaths(diagnostics, document.text, folder, [value], `artifacts.${key}`);
                }
            }
        }

        const backend = objectAt(manifest, ['backend']);
        const backendUrl = backend ? stringAt(backend, ['url']) : undefined;
        if (!backendUrl) {
            addJsonDiagnostic(diagnostics, document.text, 'backend', 'backend.url is required.', vscode.DiagnosticSeverity.Information);
        } else if (!isValidUrl(backendUrl)) {
            addJsonDiagnostic(diagnostics, document.text, backendUrl, `backend.url is not a valid URL: ${backendUrl}.`, vscode.DiagnosticSeverity.Error);
        }
        const environment = backend ? stringAt(backend, ['environment']) : undefined;
        const localControl = backend ? booleanAt(backend, ['allowLocalProcessControl']) : undefined;
        if (localControl && environment && !LOCAL_ENVIRONMENTS.has(environment)) {
            addJsonDiagnostic(
                diagnostics,
                document.text,
                'allowLocalProcessControl',
                `Local process control must be disabled outside local/dev environments (${environment}).`,
                vscode.DiagnosticSeverity.Warning
            );
        }

        const llm = objectAt(manifest, ['llm']);
        const llmProvider = llm ? stringAt(llm, ['provider']) : undefined;
        const llmModel = llm ? stringAt(llm, ['model']) : undefined;
        const promptProfile = llm ? stringAt(llm, ['promptProfile']) : undefined;
        if (!llmProvider) addJsonDiagnostic(diagnostics, document.text, 'llm', 'llm.provider is required.', vscode.DiagnosticSeverity.Error);
        if (!llmModel) addJsonDiagnostic(diagnostics, document.text, 'llm', 'llm.model is required.', vscode.DiagnosticSeverity.Error);
        if (!promptProfile) {
            addJsonDiagnostic(diagnostics, document.text, 'llm', 'llm.promptProfile is required.', vscode.DiagnosticSeverity.Error);
        } else if (looksLikePath(promptProfile) && !await exists(workspaceUri(folder, promptProfile))) {
            addJsonDiagnostic(diagnostics, document.text, promptProfile, `Prompt profile file does not exist: ${promptProfile}.`, vscode.DiagnosticSeverity.Warning);
        }

        return diagnostics;
    }

    private async validateMigrationMap(
        folder: vscode.WorkspaceFolder,
        document: ParsedDocument<MigrationMapArtifact>,
        manifest: RenovatioWorkspaceManifest,
        fileDiagnostics: DiagnosticMap
    ): Promise<vscode.Diagnostic[]> {
        const diagnostics: vscode.Diagnostic[] = [];
        const migrationMap = document.value as unknown;
        if (!isRecord(migrationMap)) {
            diagnostics.push(diagnostic('Migration map must be a JSON object.', vscode.DiagnosticSeverity.Error));
            return diagnostics;
        }
        const entries = Array.isArray(migrationMap.entries) ? migrationMap.entries : [];
        const ids = new Set<string>();
        for (const [index, rawEntry] of entries.entries()) {
            if (!isRecord(rawEntry)) {
                addJsonDiagnostic(diagnostics, document.text, 'entries', `entries[${index}] must be an object.`, vscode.DiagnosticSeverity.Error);
                continue;
            }
            const entry = rawEntry as unknown as MigrationMapEntry;
            const label = entry.id || `entries[${index}]`;
            if (!entry.id) {
                addJsonDiagnostic(diagnostics, document.text, 'entries', `entries[${index}].id is required.`, vscode.DiagnosticSeverity.Error);
            } else if (ids.has(entry.id)) {
                addJsonDiagnostic(diagnostics, document.text, entry.id, `Duplicate migration map entry id: ${entry.id}.`, vscode.DiagnosticSeverity.Error);
            }
            if (entry.id) ids.add(entry.id);
            if (!MAP_STATUSES.has(String(entry.status))) {
                addJsonDiagnostic(diagnostics, document.text, String(entry.status ?? label), `${label} has unknown status: ${String(entry.status)}.`, vscode.DiagnosticSeverity.Error);
            }

            await this.validateLocation(folder, document, diagnostics, fileDiagnostics, entry, 'source', index);
            await this.validateLocation(folder, document, diagnostics, fileDiagnostics, entry, 'target', index);
            await this.validateEntryWorkflow(folder, document, diagnostics, manifest, entry, index);
        }
        return diagnostics;
    }

    private async validateLocation(
        folder: vscode.WorkspaceFolder,
        document: ParsedDocument<MigrationMapArtifact>,
        diagnostics: vscode.Diagnostic[],
        fileDiagnostics: DiagnosticMap,
        entry: MigrationMapEntry,
        side: 'source' | 'target',
        index: number
    ): Promise<void> {
        const location = entry[side];
        const label = entry.id || `entries[${index}]`;
        if (!location?.path) {
            addJsonDiagnostic(diagnostics, document.text, label, `${label} is missing ${side}.path.`, vscode.DiagnosticSeverity.Warning);
            return;
        }
        if (!isWorkspaceRelativePath(location.path)) {
            addJsonDiagnostic(diagnostics, document.text, location.path, `${label} ${side}.path must be workspace-relative and stay inside the workspace.`, vscode.DiagnosticSeverity.Error);
            return;
        }

        const rangeIssue = rangeProblem(location);
        if (rangeIssue) {
            addJsonDiagnostic(diagnostics, document.text, location.path, `${label} ${side}.range ${rangeIssue}.`, vscode.DiagnosticSeverity.Error);
        }

        const uri = workspaceUri(folder, location.path);
        if (!await exists(uri)) {
            const severity = side === 'target' && entry.status === 'proposed'
                ? vscode.DiagnosticSeverity.Warning
                : vscode.DiagnosticSeverity.Error;
            addJsonDiagnostic(diagnostics, document.text, location.path, `${label} ${side}.path does not exist: ${location.path}.`, severity);
            return;
        }

        if (location.hash) {
            const actual = await fileSha256(uri);
            if (!sameHash(location.hash, actual)) {
                const staleStatus = side === 'source' ? 'stale-source' : 'stale-target';
                const messageText = `${label} ${side}.hash differs from disk; mark or reconcile as ${staleStatus}.`;
                addJsonDiagnostic(diagnostics, document.text, location.path, messageText, vscode.DiagnosticSeverity.Warning);
                addFileDiagnostic(fileDiagnostics, uri, location, messageText, vscode.DiagnosticSeverity.Warning);
            }
        } else {
            addJsonDiagnostic(diagnostics, document.text, location.path, `${label} ${side}.hash is missing; stale detection cannot prove freshness.`, vscode.DiagnosticSeverity.Information);
        }
    }

    private async validateEntryWorkflow(
        folder: vscode.WorkspaceFolder,
        document: ParsedDocument<MigrationMapArtifact>,
        diagnostics: vscode.Diagnostic[],
        manifest: RenovatioWorkspaceManifest,
        entry: MigrationMapEntry,
        index: number
    ): Promise<void> {
        const label = entry.id || `entries[${index}]`;
        if ((entry.status === 'accepted' || entry.status === 'generated') && !entry.target?.path) {
            addJsonDiagnostic(diagnostics, document.text, label, `${label} is ${entry.status} without target output.`, vscode.DiagnosticSeverity.Error);
        }
        if (entry.status === 'generated' && (!Array.isArray(entry.evidence) || entry.evidence.length === 0)) {
            addJsonDiagnostic(diagnostics, document.text, label, `${label} is generated without evidence.`, vscode.DiagnosticSeverity.Warning);
        }
        const evidenceEntries = Array.isArray(entry.evidence) ? entry.evidence : [];
        if (!Array.isArray(entry.evidence)) {
            addJsonDiagnostic(diagnostics, document.text, label, `${label} evidence must be an array.`, vscode.DiagnosticSeverity.Error);
        }
        for (const evidence of evidenceEntries) {
            if (!looksLikePath(evidence)) continue;
            if (!isWorkspaceRelativePath(evidence)) {
                addJsonDiagnostic(diagnostics, document.text, evidence, `${label} evidence path must stay inside the workspace: ${evidence}.`, vscode.DiagnosticSeverity.Error);
                continue;
            }
            const exact = workspaceUri(folder, evidence);
            const underEvidenceDir = workspaceUri(folder, [manifest.artifacts.evidenceDir, evidence].join('/'));
            if (!await exists(exact) && !await exists(underEvidenceDir)) {
                addJsonDiagnostic(diagnostics, document.text, evidence, `${label} evidence file does not exist: ${evidence}.`, vscode.DiagnosticSeverity.Warning);
            }
        }
    }
}

function validateWorkspacePaths(
    diagnostics: vscode.Diagnostic[],
    text: string,
    folder: vscode.WorkspaceFolder,
    paths: string[],
    label: string
): void {
    for (const value of paths) {
        if (!value.trim()) {
            addJsonDiagnostic(diagnostics, text, label, `${label} cannot be empty.`, vscode.DiagnosticSeverity.Error);
            continue;
        }
        if (!isWorkspaceRelativePath(value) && !(isAbsolutePath(value) && isUnderWorkspace(folder, value))) {
            addJsonDiagnostic(diagnostics, text, value, `${label} points outside the workspace: ${value}.`, vscode.DiagnosticSeverity.Error);
        }
    }
}

function addFileDiagnostic(
    diagnostics: DiagnosticMap,
    uri: vscode.Uri,
    location: MigrationLocation,
    messageText: string,
    severity: vscode.DiagnosticSeverity
): void {
    const key = uri.toString();
    const bucket = diagnostics.get(key) ?? { uri, diagnostics: [] };
    bucket.diagnostics.push(new vscode.Diagnostic(rangeForLocation(location), messageText, severity));
    diagnostics.set(key, bucket);
}

function addJsonDiagnostic(
    diagnostics: vscode.Diagnostic[],
    text: string,
    needle: string,
    messageText: string,
    severity: vscode.DiagnosticSeverity
): void {
    diagnostics.push(diagnostic(messageText, severity, rangeForNeedle(text, needle)));
}

function diagnostic(
    messageText: string,
    severity: vscode.DiagnosticSeverity,
    range: vscode.Range = new vscode.Range(new vscode.Position(0, 0), new vscode.Position(0, 1))
): vscode.Diagnostic {
    return new vscode.Diagnostic(range, messageText, severity);
}

function rangeForNeedle(text: string, needle: string): vscode.Range {
    const index = text.indexOf(needle);
    if (index < 0) return new vscode.Range(new vscode.Position(0, 0), new vscode.Position(0, 1));
    const before = text.slice(0, index).split(/\r?\n/);
    const line = before.length - 1;
    const character = before[before.length - 1].length;
    return new vscode.Range(new vscode.Position(line, character), new vscode.Position(line, character + Math.max(1, needle.length)));
}

function rangeForLocation(location: MigrationLocation): vscode.Range {
    const range = location.range;
    if (!range) return new vscode.Range(new vscode.Position(0, 0), new vscode.Position(0, 1));
    const startLine = Math.max(0, range.startLine - 1);
    const startColumn = Math.max(0, range.startColumn - 1);
    const endLine = Math.max(startLine, range.endLine - 1);
    const endColumn = Math.max(startColumn + 1, range.endColumn - 1);
    return new vscode.Range(new vscode.Position(startLine, startColumn), new vscode.Position(endLine, endColumn));
}

function rangeProblem(location: MigrationLocation): string | undefined {
    const range = location.range;
    if (!range) return undefined;
    const values = [range.startLine, range.startColumn, range.endLine, range.endColumn];
    if (values.some(value => !Number.isInteger(value) || value < 1)) return 'must use positive integer positions';
    if (range.endLine < range.startLine) return 'endLine must be greater than or equal to startLine';
    if (range.endLine === range.startLine && range.endColumn < range.startColumn) {
        return 'endColumn must be greater than or equal to startColumn on the same line';
    }
    return undefined;
}

async function fileSha256(uri: vscode.Uri): Promise<string> {
    const bytes = await vscode.workspace.fs.readFile(uri);
    const digest = await crypto.subtle.digest('SHA-256', bytes);
    return [...new Uint8Array(digest)].map(value => value.toString(16).padStart(2, '0')).join('');
}

function sameHash(expected: string, actual: string): boolean {
    const normalized = expected.trim().toLowerCase().replace(/^sha-?256:/, '');
    return normalized === actual;
}

function objectAt(value: unknown, path: string[]): Record<string, unknown> | undefined {
    const found = path.reduce<unknown>((current, key) => isRecord(current) ? current[key] : undefined, value);
    return isRecord(found) ? found : undefined;
}

function arrayAt(value: unknown, path: string[]): unknown[] {
    const found = path.reduce<unknown>((current, key) => isRecord(current) ? current[key] : undefined, value);
    return Array.isArray(found) ? found : [];
}

function stringAt(value: unknown, path: string[]): string | undefined {
    const found = path.reduce<unknown>((current, key) => isRecord(current) ? current[key] : undefined, value);
    return typeof found === 'string' && found.trim() ? found : undefined;
}

function booleanAt(value: unknown, path: string[]): boolean | undefined {
    const found = path.reduce<unknown>((current, key) => isRecord(current) ? current[key] : undefined, value);
    return typeof found === 'boolean' ? found : undefined;
}

function workspaceUri(folder: vscode.WorkspaceFolder, relativePath: string): vscode.Uri {
    return vscode.Uri.joinPath(folder.uri, ...relativePath.split('/').filter(Boolean));
}

function isUnderWorkspace(folder: vscode.WorkspaceFolder, absolutePath: string): boolean {
    const root = normalizePath(folder.uri.fsPath);
    const value = normalizePath(absolutePath);
    return value === root || value.startsWith(`${root}/`);
}

function isAbsolutePath(path: string): boolean {
    return path.startsWith('/') || /^[A-Za-z]:[\\/]/.test(path);
}

function isWorkspaceRelativePath(path: string): boolean {
    if (isAbsolutePath(path) || /^[a-z][a-z0-9+.-]*:/i.test(path)) return false;
    return !path.replace(/\\/g, '/').split('/').some(part => part === '..');
}

function normalizePath(value: string): string {
    return value.replace(/\\/g, '/').replace(/\/+$/, '');
}

function isValidUrl(value: string): boolean {
    try {
        const parsed = new URL(value);
        return parsed.protocol === 'http:' || parsed.protocol === 'https:';
    } catch {
        return false;
    }
}

function looksLikePath(value: string): boolean {
    if (/^[a-z][a-z0-9+.-]*:/i.test(value)) return false;
    return value.includes('/') || value.includes('\\') || /\.(json|ya?ml|md|txt|log|sarif|xml|html?)$/i.test(value);
}

async function exists(uri: vscode.Uri): Promise<boolean> {
    try {
        await vscode.workspace.fs.stat(uri);
        return true;
    } catch {
        return false;
    }
}

function decodeBytes(value: Uint8Array): string {
    return new TextDecoder('utf-8').decode(value);
}

function parseJsonc(text: string): unknown {
    return JSON.parse(stripJsonComments(text));
}

function stripJsonComments(text: string): string {
    let output = '';
    let inString = false;
    let escaped = false;
    for (let index = 0; index < text.length; index += 1) {
        const char = text[index];
        const next = text[index + 1];
        if (inString) {
            output += char;
            if (escaped) {
                escaped = false;
            } else if (char === '\\') {
                escaped = true;
            } else if (char === '"') {
                inString = false;
            }
            continue;
        }
        if (char === '"') {
            inString = true;
            output += char;
            continue;
        }
        if (char === '/' && next === '/') {
            while (index < text.length && text[index] !== '\n') {
                output += ' ';
                index += 1;
            }
            output += '\n';
            continue;
        }
        if (char === '/' && next === '*') {
            output += '  ';
            index += 2;
            while (index < text.length && !(text[index] === '*' && text[index + 1] === '/')) {
                output += text[index] === '\n' ? '\n' : ' ';
                index += 1;
            }
            output += '  ';
            index += 1;
            continue;
        }
        output += char;
    }
    return output;
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return Boolean(value && typeof value === 'object' && !Array.isArray(value));
}

function isString(value: unknown): value is string {
    return typeof value === 'string';
}

function isFileSystemWatcher(value: vscode.Disposable): value is vscode.FileSystemWatcher {
    return 'onDidCreate' in value && 'onDidChange' in value && 'onDidDelete' in value;
}

function message(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
