import * as vscode from 'vscode';

export const WORKSPACE_MANIFEST_RELATIVE_PATH = '.renovatio/workspace.renovatio.json';

const DEFAULT_INCLUDE = ['**/*.cbl', '**/*.cob', '**/*.cpy', '**/*.jcl'];
const DEFAULT_EXCLUDE = ['**/target/**', '**/.git/**'];

export interface RenovatioWorkspaceManifest {
    version: '1';
    projectId: string;
    source: {
        language: string;
        roots: string[];
        include: string[];
        exclude: string[];
    };
    targets: RenovatioTarget[];
    artifacts: {
        domainModel: string;
        persistenceModel: string;
        architecture: string;
        migrationMap: string;
        evidenceDir: string;
    };
    backend: {
        url: string;
        environment: string;
        healthEndpoint?: string;
        capabilitiesEndpoint?: string;
        allowLocalProcessControl: boolean;
        commands?: {
            start?: string;
            stop?: string;
            restart?: string;
            reloadConfig?: string;
        };
    };
    llm: {
        provider: string;
        model: string;
        purpose: string;
        temperature: number;
        maxTokens: number;
        cacheEnabled: boolean;
        promptProfile: string;
        fallbackModel: string | null;
    };
    sync?: {
        enabled: boolean;
        mode: 'manual' | 'pull-on-open' | 'push-on-save';
        lastSyncedAt: string | null;
        lastSyncedRevision: string | null;
        artifacts: Array<'domainModel' | 'persistenceModel' | 'architecture' | 'migrationMap'>;
        conflictPolicy: 'prompt' | 'prefer-local' | 'prefer-remote';
    };
}

interface RenovatioTarget {
    language: string;
    root: string;
    package?: string;
    framework?: string;
}

export class RenovatioWorkspaceManifestService implements vscode.Disposable {
    private readonly diagnostics = vscode.languages.createDiagnosticCollection('renovatio-workspace');
    private readonly disposables: vscode.Disposable[] = [];

    constructor(private readonly output: vscode.OutputChannel) {
        this.disposables.push(
            this.diagnostics,
            vscode.workspace.onDidOpenTextDocument(document => this.validateDocumentIfManifest(document)),
            vscode.workspace.onDidSaveTextDocument(document => this.validateDocumentIfManifest(document)),
            vscode.workspace.onDidChangeWorkspaceFolders(() => void this.validateWorkspace())
        );
    }

    dispose(): void {
        this.disposables.forEach(disposable => disposable.dispose());
    }

    async initializeWorkspace(): Promise<void> {
        const folder = await this.pickWorkspaceFolder();
        if (!folder) return;

        const manifestUri = this.manifestUri(folder);
        if (await exists(manifestUri)) {
            const overwrite = await vscode.window.showWarningMessage(
                'Renovatio workspace manifest already exists. Overwrite it?',
                { modal: true },
                'Overwrite'
            );
            if (overwrite !== 'Overwrite') return;
        }

        const defaults = this.defaultsFromSettings(folder);
        const projectId = await vscode.window.showInputBox({
            title: 'Renovatio: Initialize Workspace',
            prompt: 'Project id used for local Renovatio artifacts',
            value: defaults.projectId,
            validateInput: value => /^[a-zA-Z0-9._-]+$/.test(value.trim())
                ? undefined
                : 'Use letters, numbers, dot, underscore or dash.'
        });
        if (!projectId) return;

        const manifest = this.createDefaultManifest(folder, projectId.trim(), defaults);
        await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(folder.uri, '.renovatio'));
        await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(folder.uri, '.renovatio', 'diagrams'));
        await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(folder.uri, '.renovatio', 'evidence'));
        await vscode.workspace.fs.writeFile(manifestUri, encodeJson(manifest));
        this.output.appendLine(`Created Renovatio workspace manifest: ${manifestUri.fsPath}`);
        await this.openWorkspaceManifest(folder);
        await this.validateWorkspace(folder);
    }

    async openWorkspaceManifest(folder?: vscode.WorkspaceFolder): Promise<void> {
        const targetFolder = folder ?? await this.pickWorkspaceFolder();
        if (!targetFolder) return;
        const uri = this.manifestUri(targetFolder);
        if (!await exists(uri)) {
            const action = await vscode.window.showInformationMessage(
                'No Renovatio workspace manifest exists for this workspace.',
                'Initialize Workspace'
            );
            if (action === 'Initialize Workspace') {
                await this.initializeWorkspace();
            }
            return;
        }
        const document = await vscode.workspace.openTextDocument(uri);
        await vscode.window.showTextDocument(document, { preview: false });
    }

    async validateWorkspace(folder?: vscode.WorkspaceFolder): Promise<boolean> {
        const folders = folder ? [folder] : vscode.workspace.workspaceFolders ?? [];
        if (!folders.length) {
            vscode.window.showWarningMessage('Open a VS Code workspace before validating Renovatio artifacts.');
            return false;
        }

        let valid = true;
        for (const workspaceFolder of folders) {
            const manifestUri = this.manifestUri(workspaceFolder);
            if (!await exists(manifestUri)) {
                this.diagnostics.delete(manifestUri);
                continue;
            }
            const text = decodeBytes(await vscode.workspace.fs.readFile(manifestUri));
            const diagnostics = this.validateManifestText(text);
            this.diagnostics.set(manifestUri, diagnostics);
            valid = valid && diagnostics.length === 0;
        }

        if (valid) {
            vscode.window.showInformationMessage('Renovatio workspace validation passed.');
        } else {
            vscode.window.showWarningMessage('Renovatio workspace validation found issues. See Problems.');
        }
        return valid;
    }

    async formatArtifacts(): Promise<void> {
        const active = vscode.window.activeTextEditor?.document;
        if (active && isRenovatioJson(active.uri)) {
            await this.formatDocument(active);
            return;
        }

        const files = await vscode.workspace.findFiles('**/.renovatio/**/*.json', '**/{node_modules,target,.git}/**');
        if (!files.length) {
            vscode.window.showInformationMessage('No Renovatio JSON artifacts found to format.');
            return;
        }

        let formatted = 0;
        for (const uri of files) {
            const document = await vscode.workspace.openTextDocument(uri);
            if (await this.formatDocument(document, { silent: true })) formatted += 1;
        }
        vscode.window.showInformationMessage(`Formatted ${formatted} Renovatio artifact(s).`);
    }

    async load(folder?: vscode.WorkspaceFolder): Promise<RenovatioWorkspaceManifest | undefined> {
        const targetFolder = folder ?? vscode.workspace.workspaceFolders?.[0];
        if (!targetFolder) return undefined;
        const uri = this.manifestUri(targetFolder);
        if (!await exists(uri)) return undefined;
        const text = decodeBytes(await vscode.workspace.fs.readFile(uri));
        return parseJsonc(text) as RenovatioWorkspaceManifest;
    }

    async update(
        mutator: (manifest: RenovatioWorkspaceManifest) => void,
        folder?: vscode.WorkspaceFolder
    ): Promise<RenovatioWorkspaceManifest | undefined> {
        const targetFolder = folder ?? vscode.workspace.workspaceFolders?.[0];
        if (!targetFolder) return undefined;
        const uri = this.manifestUri(targetFolder);
        if (!await exists(uri)) {
            await this.initializeWorkspace();
            if (!await exists(uri)) return undefined;
        }
        const manifest = await this.load(targetFolder);
        if (!manifest) return undefined;
        mutator(manifest);
        await vscode.workspace.fs.writeFile(uri, encodeJson(manifest));
        await this.validateWorkspace(targetFolder);
        return manifest;
    }

    private async formatDocument(document: vscode.TextDocument, options: { silent?: boolean } = {}): Promise<boolean> {
        try {
            const parsed = parseJsonc(document.getText());
            const formatted = `${JSON.stringify(parsed, null, 2)}\n`;
            if (formatted === document.getText()) return true;
            const edit = new vscode.WorkspaceEdit();
            const end = document.lineCount === 0
                ? new vscode.Position(0, 0)
                : document.lineAt(document.lineCount - 1).rangeIncludingLineBreak.end;
            edit.replace(document.uri, new vscode.Range(new vscode.Position(0, 0), end), formatted);
            const applied = await vscode.workspace.applyEdit(edit);
            if (!applied) throw new Error('VS Code rejected the format edit.');
            await document.save();
            return true;
        } catch (error) {
            if (!options.silent) {
                vscode.window.showErrorMessage(`Could not format Renovatio artifact: ${message(error)}`);
            }
            return false;
        }
    }

    private validateDocumentIfManifest(document: vscode.TextDocument): void {
        if (!document.uri.fsPath.endsWith(WORKSPACE_MANIFEST_RELATIVE_PATH)) return;
        this.diagnostics.set(document.uri, this.validateManifestText(document.getText()));
    }

    private validateManifestText(text: string): vscode.Diagnostic[] {
        let parsed: unknown;
        try {
            parsed = parseJsonc(text);
        } catch (error) {
            return [diagnostic(`Invalid JSON: ${message(error)}`)];
        }
        return validateManifest(parsed).map(diagnostic);
    }

    private createDefaultManifest(
        folder: vscode.WorkspaceFolder,
        projectId: string,
        defaults: ReturnType<RenovatioWorkspaceManifestService['defaultsFromSettings']>
    ): RenovatioWorkspaceManifest {
        const targetLanguage = defaults.targetLanguage;
        const targetRoot = defaults.generatedRoots[0] ?? `generated/${targetLanguage}`;
        return {
            version: '1',
            projectId,
            source: {
                language: 'cobol',
                roots: defaults.cobolRoots,
                include: DEFAULT_INCLUDE,
                exclude: DEFAULT_EXCLUDE
            },
            targets: [{
                language: targetLanguage,
                root: targetRoot,
                package: defaults.targetPackage,
                framework: targetLanguage === 'java' ? 'spring' : undefined
            }],
            artifacts: {
                domainModel: `.renovatio/diagrams/${projectId}.renovatio-domain.json`,
                persistenceModel: `.renovatio/diagrams/${projectId}-persistence.renovatio-domain.json`,
                architecture: `.renovatio/diagrams/${projectId}.renovatio-arch.json`,
                migrationMap: '.renovatio/migration-map.renovatio.json',
                evidenceDir: '.renovatio/evidence'
            },
            backend: {
                url: defaults.backendUrl,
                environment: 'local',
                healthEndpoint: '/actuator/health',
                capabilitiesEndpoint: '/api/capabilities',
                allowLocalProcessControl: true
            },
            llm: {
                provider: 'ollama',
                model: 'codellama:13b',
                purpose: 'cobol-reverse-engineering',
                temperature: 0.1,
                maxTokens: 8192,
                cacheEnabled: true,
                promptProfile: 'cobol.domain.entities.v1',
                fallbackModel: null
            },
            sync: {
                enabled: false,
                mode: 'manual',
                lastSyncedAt: null,
                lastSyncedRevision: null,
                artifacts: ['domainModel', 'persistenceModel', 'architecture', 'migrationMap'],
                conflictPolicy: 'prompt'
            }
        };
    }

    private defaultsFromSettings(folder: vscode.WorkspaceFolder): {
        projectId: string;
        cobolRoots: string[];
        generatedRoots: string[];
        targetLanguage: string;
        targetPackage: string;
        backendUrl: string;
    } {
        const config = vscode.workspace.getConfiguration('renovatio', folder.uri);
        const targetLanguage = String(config.get('targetLanguage') || 'java');
        const generatedRoot = String(config.get('generatedRoot') || `generated/${targetLanguage}`);
        const generatedRoots = uniqueStrings([
            ...stringArray(config.get('generatedRoots')),
            generatedRoot
        ]).map(value => relativePath(folder, value));
        const cobolRoots = stringArray(config.get('cobolRoots')).map(value => relativePath(folder, value));
        return {
            projectId: sanitizeProjectId(folder.name),
            cobolRoots: cobolRoots.length ? cobolRoots : ['src/mainframe', 'copybooks'],
            generatedRoots: generatedRoots.length ? generatedRoots : [`generated/${targetLanguage}`],
            targetLanguage,
            targetPackage: String(config.get('targetPackage') || 'com.example.modernized'),
            backendUrl: String(config.get('backendUrl') || 'http://127.0.0.1:8081').replace(/\/$/, '')
        };
    }

    private async pickWorkspaceFolder(): Promise<vscode.WorkspaceFolder | undefined> {
        const folders = vscode.workspace.workspaceFolders ?? [];
        if (folders.length === 0) {
            vscode.window.showWarningMessage('Open a VS Code workspace before using Renovatio workspace commands.');
            return undefined;
        }
        if (folders.length === 1) return folders[0];
        const selected = await vscode.window.showQuickPick(
            folders.map(folder => ({ label: folder.name, description: folder.uri.fsPath, folder })),
            { placeHolder: 'Select the workspace folder for Renovatio artifacts' }
        );
        return selected?.folder;
    }

    private manifestUri(folder: vscode.WorkspaceFolder): vscode.Uri {
        return vscode.Uri.joinPath(folder.uri, ...WORKSPACE_MANIFEST_RELATIVE_PATH.split('/'));
    }
}

function validateManifest(value: unknown): string[] {
    const issues: string[] = [];
    if (!isRecord(value)) return ['Manifest must be a JSON object.'];
    requireString(value, 'version', issues, ['1']);
    requireString(value, 'projectId', issues);
    const source = requireObject(value, 'source', issues);
    if (source) {
        requireString(source, 'language', issues, ['cobol']);
        requireStringArray(source, 'roots', issues);
        requireStringArray(source, 'include', issues);
        requireStringArray(source, 'exclude', issues);
    }
    const targets = value.targets;
    if (!Array.isArray(targets) || targets.length === 0) {
        issues.push('targets must contain at least one target.');
    } else {
        targets.forEach((target, index) => {
            if (!isRecord(target)) {
                issues.push(`targets[${index}] must be an object.`);
                return;
            }
            requireString(target, 'language', issues);
            requireString(target, 'root', issues);
        });
    }
    const artifacts = requireObject(value, 'artifacts', issues);
    if (artifacts) {
        for (const key of ['domainModel', 'persistenceModel', 'architecture', 'migrationMap', 'evidenceDir']) {
            requireWorkspaceRelativePath(artifacts, key, issues);
        }
    }
    const backend = requireObject(value, 'backend', issues);
    if (backend) {
        requireString(backend, 'url', issues);
        requireString(backend, 'environment', issues);
        if (backend.healthEndpoint !== undefined) requireString(backend, 'healthEndpoint', issues);
        if (backend.capabilitiesEndpoint !== undefined) requireString(backend, 'capabilitiesEndpoint', issues);
        if (backend.commands !== undefined && !isRecord(backend.commands)) {
            issues.push('backend.commands must be an object.');
        }
        if (typeof backend.allowLocalProcessControl !== 'boolean') {
            issues.push('backend.allowLocalProcessControl must be a boolean.');
        }
    }
    const llm = requireObject(value, 'llm', issues);
    if (llm) {
        requireString(llm, 'provider', issues);
        requireString(llm, 'model', issues);
        requireString(llm, 'purpose', issues);
        requireNumber(llm, 'temperature', issues);
        requireNumber(llm, 'maxTokens', issues);
        requireString(llm, 'promptProfile', issues);
        if (typeof llm.cacheEnabled !== 'boolean') {
            issues.push('llm.cacheEnabled must be a boolean.');
        }
        if (llm.fallbackModel !== null && llm.fallbackModel !== undefined && typeof llm.fallbackModel !== 'string') {
            issues.push('llm.fallbackModel must be a string or null.');
        }
    }
    if (value.sync !== undefined) {
        const sync = requireObject(value, 'sync', issues);
        if (sync) {
            if (typeof sync.enabled !== 'boolean') {
                issues.push('sync.enabled must be a boolean.');
            }
            requireString(sync, 'mode', issues, ['manual', 'pull-on-open', 'push-on-save']);
            requireNullableString(sync, 'lastSyncedAt', issues);
            requireNullableString(sync, 'lastSyncedRevision', issues);
            requireStringArray(sync, 'artifacts', issues, ['domainModel', 'persistenceModel', 'architecture', 'migrationMap']);
            requireString(sync, 'conflictPolicy', issues, ['prompt', 'prefer-local', 'prefer-remote']);
        }
    }
    return issues;
}

function requireObject(target: Record<string, unknown>, key: string, issues: string[]): Record<string, unknown> | undefined {
    const value = target[key];
    if (!isRecord(value)) {
        issues.push(`${key} must be an object.`);
        return undefined;
    }
    return value;
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

function requireNumber(target: Record<string, unknown>, key: string, issues: string[]): void {
    if (typeof target[key] !== 'number' || !Number.isFinite(target[key])) {
        issues.push(`${key} must be a finite number.`);
    }
}

function requireNullableString(target: Record<string, unknown>, key: string, issues: string[]): void {
    const value = target[key];
    if (value !== null && value !== undefined && typeof value !== 'string') {
        issues.push(`${key} must be a string or null.`);
    }
}

function requireStringArray(target: Record<string, unknown>, key: string, issues: string[], allowed?: string[]): void {
    const value = target[key];
    if (!Array.isArray(value) || value.some(entry => typeof entry !== 'string' || entry.trim() === '')) {
        issues.push(`${key} must be an array of non-empty strings.`);
        return;
    }
    if (allowed && value.some(entry => !allowed.includes(entry))) {
        issues.push(`${key} must contain only: ${allowed.join(', ')}.`);
    }
}

function requireWorkspaceRelativePath(target: Record<string, unknown>, key: string, issues: string[]): void {
    const value = target[key];
    if (typeof value !== 'string' || value.trim() === '') {
        issues.push(`artifacts.${key} must be a workspace-relative path.`);
        return;
    }
    if (value.startsWith('/') || /^[a-zA-Z]:[\\/]/.test(value)) {
        issues.push(`artifacts.${key} must be workspace-relative, not absolute.`);
    }
}

function diagnostic(messageText: string): vscode.Diagnostic {
    return new vscode.Diagnostic(
        new vscode.Range(new vscode.Position(0, 0), new vscode.Position(0, 1)),
        messageText,
        vscode.DiagnosticSeverity.Error
    );
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

function relativePath(folder: vscode.WorkspaceFolder, value: string): string {
    const normalized = value.replace(/\\/g, '/');
    const root = folder.uri.fsPath.replace(/\\/g, '/');
    if (normalized.startsWith(`${root}/`)) return normalized.slice(root.length + 1);
    return normalized;
}

function stringArray(value: unknown): string[] {
    return Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === 'string') : [];
}

function uniqueStrings(values: string[]): string[] {
    return [...new Set(values.filter(value => value.trim().length > 0))];
}

function sanitizeProjectId(value: string): string {
    const sanitized = value.trim().toLowerCase().replace(/[^a-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '');
    return sanitized || 'renovatio-workspace';
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return Boolean(value && typeof value === 'object' && !Array.isArray(value));
}

function isRenovatioJson(uri: vscode.Uri): boolean {
    return uri.fsPath.endsWith('.renovatio.json')
        || uri.fsPath.endsWith('.renovatio-domain.json')
        || uri.fsPath.endsWith('.renovatio-arch.json')
        || uri.fsPath.endsWith(WORKSPACE_MANIFEST_RELATIVE_PATH);
}

async function exists(uri: vscode.Uri): Promise<boolean> {
    try {
        await vscode.workspace.fs.stat(uri);
        return true;
    } catch {
        return false;
    }
}

function encodeJson(value: unknown): Uint8Array {
    return new TextEncoder().encode(`${JSON.stringify(value, null, 2)}\n`);
}

function decodeBytes(value: Uint8Array): string {
    return new TextDecoder('utf-8').decode(value);
}

function message(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
