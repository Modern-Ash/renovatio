import * as vscode from 'vscode';
import {
    type RenovatioWorkspaceManifest,
    RenovatioWorkspaceManifestService
} from './workspaceManifest';
import {
    formatMigrationMap,
    type MigrationLocation,
    type MigrationMapArtifact,
    type MigrationMapEntry
} from './migrationMap';
import {
    changeSetArtifactPath,
    changeSetDirectory,
    decodeBytes,
    encodeText,
    exists,
    formatChangeSet,
    readLatestChangeSet,
    relativePath,
    sha256File,
    sha256Text,
    workspaceUri,
    writeChangeSet,
    type RenovatioChange,
    type RenovatioChangeSet
} from './changeSet';

type WorkflowContext = {
    folder: vscode.WorkspaceFolder;
    manifest: RenovatioWorkspaceManifest;
    mapUri: vscode.Uri;
    migrationMap: MigrationMapArtifact;
};

export class RenovatioGenerationWorkflow implements vscode.Disposable {
    private readonly disposables: vscode.Disposable[] = [];

    constructor(
        private readonly manifestService: RenovatioWorkspaceManifestService,
        private readonly output: vscode.OutputChannel
    ) {}

    dispose(): void {
        this.disposables.forEach(disposable => disposable.dispose());
    }

    register(context: vscode.ExtensionContext): void {
        context.subscriptions.push(
            vscode.commands.registerCommand('renovatio.previewMigrationDiff', (entryId?: string) => this.previewMigrationDiff(entryId)),
            vscode.commands.registerCommand('renovatio.openChangeSet', (changeId?: string) => this.openChangeSet(changeId)),
            vscode.commands.registerCommand('renovatio.approveChange', (changeId?: string) => this.setChangeStatus(changeId, 'approved')),
            vscode.commands.registerCommand('renovatio.rejectChange', (changeId?: string) => this.setChangeStatus(changeId, 'rejected')),
            vscode.commands.registerCommand('renovatio.applyApprovedChanges', () => this.applyApprovedChanges()),
            vscode.commands.registerCommand('renovatio.reconcileGeneratedCode', (entryId?: string) => this.reconcileGeneratedCode(entryId))
        );
    }

    async previewMigrationDiff(entryId?: string): Promise<void> {
        const context = await this.context();
        if (!context) return;
        const selectedEntries = context.migrationMap.entries.filter(entry => !entryId || entry.id === entryId);
        if (!selectedEntries.length) {
            vscode.window.showWarningMessage(entryId ? `Migration entry not found: ${entryId}` : 'Migration map has no entries.');
            return;
        }

        const backendChangeSet = await this.tryBackendPreview(context, selectedEntries);
        const changeSet = backendChangeSet ?? await this.createLocalPreview(context, selectedEntries);
        const uri = await writeChangeSet(context.folder, changeSet);
        this.output.appendLine(`[preview] wrote ${relativePath(context.folder, uri)} with ${changeSet.changes.length} change(s)`);
        if (!changeSet.changes.length) {
            vscode.window.showInformationMessage('No migration changes were produced for the selected entries.');
            return;
        }
        await this.openChangeSet(changeSet.changes[0].id, changeSet);
        vscode.window.showInformationMessage(`Renovatio change set ready: ${changeSet.id}`);
    }

    async openChangeSet(changeId?: string, provided?: RenovatioChangeSet): Promise<void> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) {
            vscode.window.showWarningMessage('Open a VS Code workspace before opening a Renovatio change set.');
            return;
        }
        const loaded = provided ? undefined : await readLatestChangeSet(folder);
        const artifact = provided ?? loaded?.artifact;
        if (!artifact) {
            vscode.window.showInformationMessage('No Renovatio change set exists yet. Run Preview Migration Diff first.');
            return;
        }
        const change = changeId
            ? artifact.changes.find(candidate => candidate.id === changeId)
            : await this.pickChange(artifact, 'Open change diff');
        if (!change) return;
        if (!change.beforePath && !change.afterPath) {
            await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(workspaceUri(folder, changeSetArtifactPath(artifact.id))));
            return;
        }
        const before = change.beforePath ? workspaceUri(folder, change.beforePath) : emptyPreviewUri(change.path, 'before');
        const after = change.afterPath ? workspaceUri(folder, change.afterPath) : emptyPreviewUri(change.path, 'after');
        await vscode.commands.executeCommand('vscode.diff', before, after, `Renovatio ${change.kind}: ${change.path}`);
    }

    async setChangeStatus(changeId: string | undefined, status: 'approved' | 'rejected'): Promise<void> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) {
            vscode.window.showWarningMessage('Open a VS Code workspace before updating a Renovatio change set.');
            return;
        }
        const loaded = await readLatestChangeSet(folder);
        if (!loaded) {
            vscode.window.showInformationMessage('No Renovatio change set exists yet. Run Preview Migration Diff first.');
            return;
        }
        const change = changeId
            ? loaded.artifact.changes.find(candidate => candidate.id === changeId)
            : await this.pickChange(loaded.artifact, status === 'approved' ? 'Approve change' : 'Reject change');
        if (!change) return;
        change.status = status;
        await vscode.workspace.fs.writeFile(loaded.uri, encodeText(formatChangeSet(loaded.artifact)));
        this.output.appendLine(`[changeset] ${status}: ${change.id} ${change.path}`);
        vscode.window.showInformationMessage(`Renovatio change ${change.id} ${status}.`);
    }

    async applyApprovedChanges(): Promise<void> {
        const context = await this.context();
        if (!context) return;
        const loaded = await readLatestChangeSet(context.folder);
        if (!loaded) {
            vscode.window.showInformationMessage('No Renovatio change set exists yet. Run Preview Migration Diff first.');
            return;
        }
        const approved = loaded.artifact.changes.filter(change => change.status === 'approved');
        if (!approved.length) {
            vscode.window.showInformationMessage('No approved Renovatio changes to apply.');
            return;
        }
        const confirmed = await vscode.window.showWarningMessage(
            `Apply ${approved.length} approved Renovatio change(s)?`,
            { modal: true },
            'Apply'
        );
        if (confirmed !== 'Apply') return;

        let applied = 0;
        let conflicts = 0;
        for (const change of approved) {
            const result = await this.applyChange(context, loaded.artifact, change);
            if (result === 'applied') applied += 1;
            if (result === 'conflict') conflicts += 1;
        }
        await vscode.workspace.fs.writeFile(loaded.uri, encodeText(formatChangeSet(loaded.artifact)));
        await vscode.workspace.fs.writeFile(context.mapUri, encodeText(formatMigrationMap(context.migrationMap)));
        this.output.appendLine(`[apply] applied=${applied} conflicts=${conflicts} changeset=${loaded.artifact.id}`);
        if (conflicts) {
            vscode.window.showWarningMessage(`Applied ${applied} change(s); ${conflicts} conflict(s) were left unapplied.`);
        } else {
            vscode.window.showInformationMessage(`Applied ${applied} Renovatio change(s).`);
        }
    }

    async reconcileGeneratedCode(entryId?: string): Promise<void> {
        const context = await this.context();
        if (!context) return;
        const entries = context.migrationMap.entries.filter(entry => entry.target?.path && (!entryId || entry.id === entryId));
        let reconciled = 0;
        for (const entry of entries) {
            if (!entry.target?.path) continue;
            const uri = workspaceUri(context.folder, entry.target.path);
            if (!await exists(uri)) continue;
            entry.target.hash = await sha256File(uri);
            if (entry.status === 'stale-target') entry.status = 'manually-edited';
            reconciled += 1;
        }
        await vscode.workspace.fs.writeFile(context.mapUri, encodeText(formatMigrationMap(context.migrationMap)));
        vscode.window.showInformationMessage(`Reconciled ${reconciled} generated target mapping(s).`);
    }

    private async tryBackendPreview(
        context: WorkflowContext,
        entries: MigrationMapEntry[]
    ): Promise<RenovatioChangeSet | undefined> {
        const endpoint = joinUrl(context.manifest.backend.url, '/api/migration/changesets/preview');
        this.output.appendLine(`[preview] POST ${endpoint}`);
        try {
            const response = await fetchWithTimeout(endpoint, 8000, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    manifest: context.manifest,
                    migrationMap: context.migrationMap,
                    entryIds: entries.map(entry => entry.id)
                })
            });
            if (response.status === 404 || response.status === 405) {
                this.output.appendLine('[preview] backend preview endpoint unsupported; using local preview');
                return undefined;
            }
            const body = await response.text();
            if (!response.ok) throw new Error(`HTTP ${response.status}: ${body.slice(0, 240)}`);
            const parsed = JSON.parse(body) as RenovatioChangeSet;
            return parsed.version === '1' && Array.isArray(parsed.changes) ? parsed : undefined;
        } catch (error) {
            this.output.appendLine(`[preview] backend unavailable: ${message(error)}; using local preview`);
            const selected = await vscode.window.showWarningMessage(
                `Backend preview unavailable: ${message(error)}`,
                'Open Backend View',
                'Use Local Preview'
            );
            if (selected === 'Open Backend View') {
                await vscode.commands.executeCommand('workbench.view.extension.renovatio');
            }
            return undefined;
        }
    }

    private async createLocalPreview(
        context: WorkflowContext,
        entries: MigrationMapEntry[]
    ): Promise<RenovatioChangeSet> {
        const id = `changeset-${timestampId(new Date())}`;
        const directory = changeSetDirectory(id);
        const changes: RenovatioChange[] = [];
        const targetLanguage = context.manifest.targets[0]?.language ?? 'java';
        for (const [index, entry] of entries.entries()) {
            if (!entry.target?.path || entry.status === 'rejected') continue;
            const targetUri = workspaceUri(context.folder, entry.target.path);
            const before = await exists(targetUri) ? decodeBytes(await vscode.workspace.fs.readFile(targetUri)) : '';
            const after = this.proposedContent(entry, targetLanguage, before);
            const beforeHash = before ? await sha256Text(before) : null;
            const afterHash = await sha256Text(after);
            const changeId = `change-${String(index + 1).padStart(3, '0')}`;
            const beforePath = `${directory}/${changeId}.before`;
            const afterPath = `${directory}/${changeId}.after`;
            const diffPath = `${directory}/${changeId}.diff`;
            await vscode.workspace.fs.createDirectory(workspaceUri(context.folder, directory));
            await vscode.workspace.fs.writeFile(workspaceUri(context.folder, beforePath), encodeText(before));
            await vscode.workspace.fs.writeFile(workspaceUri(context.folder, afterPath), encodeText(after));
            await vscode.workspace.fs.writeFile(workspaceUri(context.folder, diffPath), encodeText(unifiedDiff(entry.target.path, before, after)));
            changes.push({
                id: changeId,
                path: entry.target.path,
                kind: before ? 'modify' : 'create',
                status: 'pending',
                migrationEntryIds: [entry.id],
                beforeHash,
                afterHash,
                beforePath,
                afterPath,
                diffPath,
                message: 'Local preview generated because backend dry-run is unavailable.'
            });
        }
        return {
            version: '1',
            id,
            createdAt: new Date().toISOString(),
            sourceHash: await aggregateSourceHash(context.folder, entries),
            targetLanguage,
            backend: {
                url: context.manifest.backend.url,
                llmModel: context.manifest.llm.model,
                promptProfile: context.manifest.llm.promptProfile,
                mode: 'local-preview'
            },
            changes
        };
    }

    private proposedContent(entry: MigrationMapEntry, targetLanguage: string, before: string): string {
        const symbol = targetSymbol(entry.target) ?? targetSymbol(entry.source) ?? sanitizeIdentifier(entry.id);
        const marker = `Renovatio generated preview for ${entry.id}`;
        if (before.trim()) {
            const comment = lineComment(targetLanguage);
            return before.includes(marker) ? before : `${comment} ${marker}\n${before}`;
        }
        if (targetLanguage === 'python') {
            return `# ${marker}\n\nclass ${symbol}:\n    def execute(self):\n        raise NotImplementedError(\"Generated preview requires backend emitter output\")\n`;
        }
        if (targetLanguage === 'node') {
            return `// ${marker}\n\nexport class ${symbol} {\n  execute() {\n    throw new Error('Generated preview requires backend emitter output');\n  }\n}\n`;
        }
        return `// ${marker}\n\npublic final class ${symbol} {\n    public void execute() {\n        throw new UnsupportedOperationException(\"Generated preview requires backend emitter output\");\n    }\n}\n`;
    }

    private async applyChange(
        context: WorkflowContext,
        changeSet: RenovatioChangeSet,
        change: RenovatioChange
    ): Promise<'applied' | 'conflict'> {
        const targetUri = workspaceUri(context.folder, change.path);
        if (change.kind === 'delete') {
            change.status = 'skipped';
            return 'applied';
        }
        const existsNow = await exists(targetUri);
        if (change.beforeHash === null && existsNow) {
            change.status = 'conflict';
            return 'conflict';
        }
        if (change.beforeHash && existsNow) {
            const currentHash = await sha256File(targetUri);
            if (currentHash !== change.beforeHash) {
                change.status = 'conflict';
                return 'conflict';
            }
        }
        if (!change.afterPath) {
            change.status = 'conflict';
            return 'conflict';
        }
        const after = await vscode.workspace.fs.readFile(workspaceUri(context.folder, change.afterPath));
        await vscode.workspace.fs.createDirectory(parentUri(targetUri));
        await vscode.workspace.fs.writeFile(targetUri, after);
        change.afterHash = await sha256File(targetUri);
        change.status = 'applied';
        this.updateMigrationEntries(context.migrationMap, changeSet, change);
        return 'applied';
    }

    private updateMigrationEntries(
        migrationMap: MigrationMapArtifact,
        changeSet: RenovatioChangeSet,
        change: RenovatioChange
    ): void {
        const evidence = changeSetArtifactPath(changeSet.id);
        for (const entryId of change.migrationEntryIds) {
            const entry = migrationMap.entries.find(candidate => candidate.id === entryId);
            if (!entry) continue;
            entry.status = 'generated';
            if (entry.target) entry.target.hash = change.afterHash ?? undefined;
            entry.evidence = [...new Set([...(entry.evidence ?? []), evidence, change.diffPath])];
            entry.lastDecision = {
                actor: 'vscode-user',
                action: 'apply-generated-change',
                at: new Date().toISOString(),
                reason: `Applied ${change.id} from ${changeSet.id}.`
            };
        }
    }

    private async pickChange(artifact: RenovatioChangeSet, title: string): Promise<RenovatioChange | undefined> {
        const selected = await vscode.window.showQuickPick(
            artifact.changes.map(change => ({
                label: change.id,
                description: change.status,
                detail: `${change.kind}: ${change.path}`,
                change
            })),
            { title }
        );
        return selected?.change;
    }

    private async context(): Promise<WorkflowContext | undefined> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) {
            vscode.window.showWarningMessage('Open a VS Code workspace before using Renovatio migration workflow commands.');
            return undefined;
        }
        const manifest = await this.manifestService.load(folder);
        if (!manifest) {
            const selected = await vscode.window.showInformationMessage(
                'Renovatio workspace manifest is required before previewing migration changes.',
                'Initialize Workspace'
            );
            if (selected === 'Initialize Workspace') await this.manifestService.initializeWorkspace();
            return undefined;
        }
        const mapUri = workspaceUri(folder, manifest.artifacts.migrationMap);
        if (!await exists(mapUri)) {
            vscode.window.showWarningMessage('Create a migration map before previewing generated changes.');
            return undefined;
        }
        const migrationMap = JSON.parse(decodeBytes(await vscode.workspace.fs.readFile(mapUri))) as MigrationMapArtifact;
        return { folder, manifest, mapUri, migrationMap };
    }
}

async function aggregateSourceHash(folder: vscode.WorkspaceFolder, entries: MigrationMapEntry[]): Promise<string | undefined> {
    const hashes: string[] = [];
    for (const entry of entries) {
        if (!entry.source?.path) continue;
        const uri = workspaceUri(folder, entry.source.path);
        if (await exists(uri)) hashes.push(await sha256File(uri));
    }
    return hashes.length ? sha256Text(hashes.join('\n')) : undefined;
}

function targetSymbol(location: MigrationLocation | undefined): string | undefined {
    if (location?.symbol) return sanitizeIdentifier(location.symbol);
    const file = location?.path.split('/').pop()?.replace(/\.[^.]+$/, '');
    return file ? sanitizeIdentifier(file) : undefined;
}

function sanitizeIdentifier(value: string): string {
    const cleaned = value.replace(/[^A-Za-z0-9_]/g, '_').replace(/^([0-9])/, '_$1');
    return cleaned ? cleaned[0].toUpperCase() + cleaned.slice(1) : 'GeneratedMigration';
}

function lineComment(language: string): string {
    return language === 'python' ? '#' : '//';
}

function unifiedDiff(path: string, before: string, after: string): string {
    return [
        `--- a/${path}`,
        `+++ b/${path}`,
        '@@ preview @@',
        ...before.split(/\r?\n/).filter(Boolean).map(line => `-${line}`),
        ...after.split(/\r?\n/).filter(Boolean).map(line => `+${line}`),
        ''
    ].join('\n');
}

function emptyPreviewUri(path: string, side: string): vscode.Uri {
    return vscode.Uri.parse(`untitled:Renovatio ${side} ${path}`);
}

function timestampId(value: Date): string {
    return value.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z');
}

function joinUrl(base: string, path: string): string {
    return `${base.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
}

async function fetchWithTimeout(url: string, timeoutMs: number, init?: RequestInit): Promise<Response> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
        return await fetch(url, { ...init, signal: controller.signal });
    } finally {
        clearTimeout(timeout);
    }
}

function parentUri(uri: vscode.Uri): vscode.Uri {
    const parts = uri.path.split('/');
    parts.pop();
    return uri.with({ path: parts.join('/') || '/' });
}

function message(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
