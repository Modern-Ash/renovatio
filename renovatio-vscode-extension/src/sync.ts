import * as vscode from 'vscode';
import {
    BackendClientError,
    RenovatioBackendArtifactClient,
    type RemoteArtifact,
    type RenovatioSyncArtifactKey
} from './backendClient';
import {
    decodeBytes,
    encodeText,
    exists,
    relativePath,
    sha256File,
    workspaceUri
} from './changeSet';
import type { RenovatioWorkspaceManifest, RenovatioWorkspaceManifestService } from './workspaceManifest';

type SyncConflictState = 'clean' | 'local-changed' | 'remote-changed' | 'both-changed' | 'remote-unavailable' | 'schema-mismatch';

interface SyncStateFile {
    version: '1';
    artifacts: Record<string, SyncArtifactState>;
}

interface SyncArtifactState {
    localHash: string;
    remoteHash?: string;
    revision?: string | null;
    syncedAt: string;
}

interface ArtifactStatus {
    key: RenovatioSyncArtifactKey;
    path: string;
    uri: vscode.Uri;
    state: SyncConflictState;
    localHash: string | null;
    remote?: RemoteArtifact;
    detail: string;
}

const SYNC_STATE_PATH = '.renovatio/sync-state.json';
const SYNC_PREVIEW_ROOT = '.renovatio/sync-preview';
const BACKUP_ROOT = '.renovatio/backups';
const SYNC_ARTIFACT_KEYS: RenovatioSyncArtifactKey[] = ['domainModel', 'persistenceModel', 'architecture', 'migrationMap'];

export class RenovatioSyncService implements vscode.Disposable {
    private readonly disposables: vscode.Disposable[] = [];
    private readonly statusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 84);

    constructor(
        private readonly manifestService: RenovatioWorkspaceManifestService,
        private readonly output: vscode.OutputChannel
    ) {
        this.statusBar.command = 'renovatio.syncStatus';
        this.statusBar.text = '$(sync) Renovatio sync';
        this.statusBar.tooltip = 'Show Renovatio backend sync status';
        this.statusBar.show();
        this.disposables.push(this.statusBar);
    }

    register(context: vscode.ExtensionContext): void {
        this.disposables.push(
            vscode.commands.registerCommand('renovatio.syncStatus', () => this.syncStatus()),
            vscode.commands.registerCommand('renovatio.pullFromBackend', () => this.pullFromBackend()),
            vscode.commands.registerCommand('renovatio.pushToBackend', () => this.pushToBackend()),
            vscode.commands.registerCommand('renovatio.compareLocalAndBackend', () => this.compareLocalAndBackend()),
            vscode.commands.registerCommand('renovatio.resolveSyncConflict', () => this.resolveSyncConflict()),
            vscode.workspace.onDidOpenTextDocument(document => void this.onDocumentOpened(document)),
            vscode.workspace.onDidSaveTextDocument(document => void this.onDocumentSaved(document))
        );
        context.subscriptions.push(...this.disposables);
        void this.refreshStatusBar();
    }

    dispose(): void {
        this.disposables.forEach(disposable => disposable.dispose());
    }

    async refreshStatusBar(): Promise<void> {
        const context = await this.loadContext();
        if (!context) {
            this.statusBar.text = '$(sync) Renovatio sync';
            this.statusBar.tooltip = 'Initialize a Renovatio workspace to enable sync status';
            return;
        }
        if (!context.manifest.sync?.enabled) {
            this.statusBar.text = '$(circle-slash) Renovatio sync off';
            this.statusBar.tooltip = 'Backend sync is disabled in the workspace manifest';
            return;
        }
        const statuses = await this.collectStatuses(context);
        const worst = highestPriorityStatus(statuses);
        this.statusBar.text = `$(sync) Renovatio ${statusLabel(worst)}`;
        this.statusBar.tooltip = statuses.map(status => `${status.key}: ${status.state}`).join('\n');
    }

    private async syncStatus(): Promise<void> {
        const context = await this.requireSyncContext();
        if (!context) return;
        const statuses = await this.collectStatuses(context);
        this.output.show();
        this.output.appendLine('');
        this.output.appendLine(`Renovatio sync status for ${context.manifest.projectId}`);
        for (const status of statuses) {
            this.output.appendLine(`- ${status.key}: ${status.state} (${status.detail})`);
        }
        const selected = await this.pickStatus(statuses, 'Select an artifact to inspect');
        if (selected) {
            await this.showArtifactActions(selected);
        }
        await this.refreshStatusBar();
    }

    private async compareLocalAndBackend(): Promise<void> {
        const context = await this.requireSyncContext();
        if (!context) return;
        const selected = await this.pickStatus(await this.collectStatuses(context), 'Compare local artifact with backend');
        if (!selected) return;
        if (!selected.remote) {
            vscode.window.showWarningMessage(`No backend artifact is available for ${selected.key}: ${selected.detail}`);
            return;
        }
        const previewUri = workspaceUri(context.folder, `${SYNC_PREVIEW_ROOT}/${selected.key}.remote.json`);
        await vscode.workspace.fs.createDirectory(parentUri(previewUri));
        await vscode.workspace.fs.writeFile(previewUri, encodeText(selected.remote.content));
        await vscode.commands.executeCommand('vscode.diff', selected.uri, previewUri, `Renovatio ${selected.key}: Local <> Backend`);
    }

    private async pullFromBackend(): Promise<void> {
        const context = await this.requireSyncContext();
        if (!context) return;
        const selected = await this.pickStatus(await this.collectStatuses(context), 'Pull artifact from backend');
        if (!selected) return;
        if (!selected.remote) {
            vscode.window.showWarningMessage(`Cannot pull ${selected.key}: ${selected.detail}`);
            return;
        }
        const confirm = await vscode.window.showWarningMessage(
            `Pull backend ${selected.key} and replace ${selected.path}? A backup will be created first.`,
            { modal: true },
            'Pull'
        );
        if (confirm !== 'Pull') return;

        if (await exists(selected.uri)) {
            const backupUri = workspaceUri(context.folder, `${BACKUP_ROOT}/${timestampForPath()}/${selected.key}-${basename(selected.path)}`);
            await vscode.workspace.fs.createDirectory(parentUri(backupUri));
            await vscode.workspace.fs.copy(selected.uri, backupUri, { overwrite: true });
            this.output.appendLine(`Backup created: ${relativePath(context.folder, backupUri)}`);
        }
        await vscode.workspace.fs.createDirectory(parentUri(selected.uri));
        await vscode.workspace.fs.writeFile(selected.uri, encodeText(selected.remote.content));
        await this.writeBaseline(context, selected.key, await sha256File(selected.uri), selected.remote);
        await this.markManifestSynced(context, selected.remote.revision);
        vscode.window.showInformationMessage(`Pulled ${selected.key} from backend.`);
        await this.refreshStatusBar();
    }

    private async pushToBackend(): Promise<void> {
        const context = await this.requireSyncContext();
        if (!context) return;
        const selected = await this.pickStatus(await this.collectStatuses(context), 'Push artifact to backend');
        if (!selected) return;
        if (!await exists(selected.uri)) {
            vscode.window.showWarningMessage(`Cannot push ${selected.key}: local artifact does not exist.`);
            return;
        }
        const confirm = await vscode.window.showWarningMessage(
            `Push local ${selected.key} to backend using revision guard?`,
            { modal: true },
            'Push'
        );
        if (confirm !== 'Push') return;

        const content = decodeBytes(await vscode.workspace.fs.readFile(selected.uri));
        const state = await this.readState(context.folder);
        const expectedRevision = selected.remote?.revision
            ?? state.artifacts[selected.key]?.revision
            ?? context.manifest.sync?.lastSyncedRevision
            ?? null;
        try {
            const remote = await context.client.putArtifact(selected.key, content, selected.path, expectedRevision);
            await this.writeBaseline(context, selected.key, await sha256File(selected.uri), remote);
            await this.markManifestSynced(context, remote.revision);
            vscode.window.showInformationMessage(`Pushed ${selected.key} to backend.`);
        } catch (error) {
            this.reportBackendError(error, selected.key);
        }
        await this.refreshStatusBar();
    }

    private async resolveSyncConflict(): Promise<void> {
        const context = await this.requireSyncContext();
        if (!context) return;
        const conflicted = (await this.collectStatuses(context)).filter(status => status.state !== 'clean');
        const selected = await this.pickStatus(conflicted, 'Resolve backend sync conflict');
        if (!selected) return;
        const action = await vscode.window.showQuickPick([
            { label: 'Compare Local And Backend', command: 'compare' },
            { label: 'Pull From Backend', command: 'pull' },
            { label: 'Push To Backend', command: 'push' },
            { label: 'Mark Current State As Baseline', command: 'baseline' }
        ], { placeHolder: `Resolve ${selected.key}: ${selected.state}` });
        if (!action) return;
        if (action.command === 'compare') await this.compareSelected(context, selected);
        if (action.command === 'pull') await this.pullSelected(context, selected);
        if (action.command === 'push') await this.pushSelected(context, selected);
        if (action.command === 'baseline') await this.markBaseline(context, selected);
        await this.refreshStatusBar();
    }

    private async onDocumentOpened(document: vscode.TextDocument): Promise<void> {
        const context = await this.loadContext();
        if (!context || context.manifest.sync?.mode !== 'pull-on-open' || !context.manifest.sync.enabled) return;
        const key = artifactKeyForDocument(context.manifest, context.folder, document.uri);
        if (!key) return;
        const status = (await this.collectStatuses(context)).find(entry => entry.key === key);
        if (status?.state === 'remote-changed') {
            const action = await vscode.window.showInformationMessage(`Backend has a newer ${key}.`, 'Compare', 'Pull');
            if (action === 'Compare') await this.compareSelected(context, status);
            if (action === 'Pull') await this.pullSelected(context, status);
        }
    }

    private async onDocumentSaved(document: vscode.TextDocument): Promise<void> {
        const context = await this.loadContext();
        if (!context || context.manifest.sync?.mode !== 'push-on-save' || !context.manifest.sync.enabled) return;
        const key = artifactKeyForDocument(context.manifest, context.folder, document.uri);
        if (!key) return;
        const status = (await this.collectStatuses(context)).find(entry => entry.key === key);
        if (status?.state === 'local-changed') {
            await this.pushSelected(context, status);
        }
    }

    private async collectStatuses(context: SyncContext): Promise<ArtifactStatus[]> {
        const state = await this.readState(context.folder);
        const statuses: ArtifactStatus[] = [];
        for (const key of configuredArtifactKeys(context.manifest)) {
            const path = context.manifest.artifacts[key];
            const uri = workspaceUri(context.folder, path);
            const localHash = await exists(uri) ? await sha256File(uri) : null;
            const previous = state.artifacts[key];
            try {
                const remote = await context.client.getArtifact(key);
                statuses.push({
                    key,
                    path,
                    uri,
                    state: resolveConflictState(localHash, remote, previous),
                    localHash,
                    remote,
                    detail: `local ${localHash ?? 'missing'}, remote ${remote.hash}, revision ${remote.revision ?? 'none'}`
                });
            } catch (error) {
                const backendState = error instanceof BackendClientError && error.kind === 'schema-mismatch'
                    ? 'schema-mismatch'
                    : 'remote-unavailable';
                statuses.push({
                    key,
                    path,
                    uri,
                    state: backendState,
                    localHash,
                    detail: error instanceof Error ? error.message : String(error)
                });
            }
        }
        return statuses;
    }

    private async showArtifactActions(status: ArtifactStatus): Promise<void> {
        const action = await vscode.window.showQuickPick([
            { label: 'Compare Local And Backend', command: 'renovatio.compareLocalAndBackend' },
            { label: 'Pull From Backend', command: 'renovatio.pullFromBackend' },
            { label: 'Push To Backend', command: 'renovatio.pushToBackend' },
            { label: 'Resolve Sync Conflict', command: 'renovatio.resolveSyncConflict' }
        ], { placeHolder: `${status.key}: ${status.state}` });
        if (action) await vscode.commands.executeCommand(action.command);
    }

    private async compareSelected(context: SyncContext, status: ArtifactStatus): Promise<void> {
        if (!status.remote) {
            vscode.window.showWarningMessage(`No backend artifact is available for ${status.key}: ${status.detail}`);
            return;
        }
        const previewUri = workspaceUri(context.folder, `${SYNC_PREVIEW_ROOT}/${status.key}.remote.json`);
        await vscode.workspace.fs.createDirectory(parentUri(previewUri));
        await vscode.workspace.fs.writeFile(previewUri, encodeText(status.remote.content));
        await vscode.commands.executeCommand('vscode.diff', status.uri, previewUri, `Renovatio ${status.key}: Local <> Backend`);
    }

    private async pullSelected(context: SyncContext, status: ArtifactStatus): Promise<void> {
        if (!status.remote) {
            vscode.window.showWarningMessage(`Cannot pull ${status.key}: ${status.detail}`);
            return;
        }
        if (await exists(status.uri)) {
            const backupUri = workspaceUri(context.folder, `${BACKUP_ROOT}/${timestampForPath()}/${status.key}-${basename(status.path)}`);
            await vscode.workspace.fs.createDirectory(parentUri(backupUri));
            await vscode.workspace.fs.copy(status.uri, backupUri, { overwrite: true });
        }
        await vscode.workspace.fs.createDirectory(parentUri(status.uri));
        await vscode.workspace.fs.writeFile(status.uri, encodeText(status.remote.content));
        await this.writeBaseline(context, status.key, await sha256File(status.uri), status.remote);
        await this.markManifestSynced(context, status.remote.revision);
    }

    private async pushSelected(context: SyncContext, status: ArtifactStatus): Promise<void> {
        if (!await exists(status.uri)) return;
        try {
            const state = await this.readState(context.folder);
            const content = decodeBytes(await vscode.workspace.fs.readFile(status.uri));
            const expectedRevision = status.remote?.revision
                ?? state.artifacts[status.key]?.revision
                ?? context.manifest.sync?.lastSyncedRevision
                ?? null;
            const remote = await context.client.putArtifact(status.key, content, status.path, expectedRevision);
            await this.writeBaseline(context, status.key, await sha256File(status.uri), remote);
            await this.markManifestSynced(context, remote.revision);
        } catch (error) {
            this.reportBackendError(error, status.key);
        }
    }

    private async markBaseline(context: SyncContext, status: ArtifactStatus): Promise<void> {
        if (!status.localHash || !status.remote) {
            vscode.window.showWarningMessage(`Cannot mark ${status.key} as baseline until local and backend artifacts are available.`);
            return;
        }
        await this.writeBaseline(context, status.key, status.localHash, status.remote);
        vscode.window.showInformationMessage(`Marked ${status.key} current local/backend state as the sync baseline.`);
    }

    private async writeBaseline(
        context: SyncContext,
        key: RenovatioSyncArtifactKey,
        localHash: string,
        remote: RemoteArtifact
    ): Promise<void> {
        const state = await this.readState(context.folder);
        state.artifacts[key] = {
            localHash,
            remoteHash: remote.hash,
            revision: remote.revision,
            syncedAt: new Date().toISOString()
        };
        await this.writeState(context.folder, state);
    }

    private async markManifestSynced(context: SyncContext, revision: string | null): Promise<void> {
        const syncedAt = new Date().toISOString();
        await this.manifestService.update(manifest => {
            manifest.sync = manifest.sync ?? defaultSyncConfig();
            manifest.sync.lastSyncedAt = syncedAt;
            manifest.sync.lastSyncedRevision = revision;
        }, context.folder);
    }

    private async requireSyncContext(): Promise<SyncContext | undefined> {
        const context = await this.loadContext();
        if (!context) {
            vscode.window.showWarningMessage('Open a Renovatio workspace manifest before using backend sync.');
            return undefined;
        }
        if (!context.manifest.sync?.enabled) {
            const action = await vscode.window.showInformationMessage(
                'Renovatio backend sync is disabled in the workspace manifest.',
                'Open Manifest'
            );
            if (action === 'Open Manifest') await this.manifestService.openWorkspaceManifest(context.folder);
            return undefined;
        }
        return context;
    }

    private async loadContext(): Promise<SyncContext | undefined> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) return undefined;
        const manifest = await this.manifestService.load(folder);
        if (!manifest) return undefined;
        const role = String(vscode.workspace.getConfiguration('renovatio', folder.uri).get('role') || 'ADMIN');
        return { folder, manifest, client: new RenovatioBackendArtifactClient(manifest, role) };
    }

    private async readState(folder: vscode.WorkspaceFolder): Promise<SyncStateFile> {
        const uri = workspaceUri(folder, SYNC_STATE_PATH);
        if (!await exists(uri)) return { version: '1', artifacts: {} };
        try {
            const parsed = JSON.parse(decodeBytes(await vscode.workspace.fs.readFile(uri))) as SyncStateFile;
            return parsed.version === '1' && parsed.artifacts ? parsed : { version: '1', artifacts: {} };
        } catch {
            return { version: '1', artifacts: {} };
        }
    }

    private async writeState(folder: vscode.WorkspaceFolder, state: SyncStateFile): Promise<void> {
        const uri = workspaceUri(folder, SYNC_STATE_PATH);
        await vscode.workspace.fs.createDirectory(parentUri(uri));
        await vscode.workspace.fs.writeFile(uri, encodeText(`${JSON.stringify(state, null, 2)}\n`));
    }

    private async pickStatus(statuses: ArtifactStatus[], placeHolder: string): Promise<ArtifactStatus | undefined> {
        if (!statuses.length) {
            vscode.window.showInformationMessage('No artifacts are configured for backend sync.');
            return undefined;
        }
        const selected = await vscode.window.showQuickPick(
            statuses.map(status => ({
                label: status.key,
                description: status.state,
                detail: status.detail,
                status
            })),
            { placeHolder }
        );
        return selected?.status;
    }

    private reportBackendError(error: unknown, key: RenovatioSyncArtifactKey): void {
        const message = error instanceof Error ? error.message : String(error);
        if (error instanceof BackendClientError && error.kind === 'revision-conflict') {
            vscode.window.showWarningMessage(`Cannot push ${key}: ${message}`);
        } else {
            vscode.window.showWarningMessage(`Renovatio backend sync for ${key} did not complete: ${message}`);
        }
        this.output.appendLine(`Sync ${key} failed: ${message}`);
    }
}

interface SyncContext {
    folder: vscode.WorkspaceFolder;
    manifest: RenovatioWorkspaceManifest;
    client: RenovatioBackendArtifactClient;
}

function configuredArtifactKeys(manifest: RenovatioWorkspaceManifest): RenovatioSyncArtifactKey[] {
    const configured = manifest.sync?.artifacts ?? SYNC_ARTIFACT_KEYS;
    return configured.filter((key): key is RenovatioSyncArtifactKey => SYNC_ARTIFACT_KEYS.includes(key as RenovatioSyncArtifactKey));
}

function resolveConflictState(
    localHash: string | null,
    remote: RemoteArtifact,
    previous?: SyncArtifactState
): SyncConflictState {
    if (!localHash) return 'remote-changed';
    if (localHash === remote.hash) return 'clean';
    if (!previous) return 'remote-changed';
    const localChanged = previous.localHash !== localHash;
    const remoteChanged = previous.remoteHash !== remote.hash || previous.revision !== remote.revision;
    if (localChanged && remoteChanged) return 'both-changed';
    if (localChanged) return 'local-changed';
    if (remoteChanged) return 'remote-changed';
    return 'clean';
}

function highestPriorityStatus(statuses: ArtifactStatus[]): SyncConflictState {
    const priorities: SyncConflictState[] = ['both-changed', 'schema-mismatch', 'remote-unavailable', 'remote-changed', 'local-changed', 'clean'];
    return priorities.find(state => statuses.some(status => status.state === state)) ?? 'clean';
}

function statusLabel(state: SyncConflictState): string {
    switch (state) {
        case 'clean':
            return 'sync clean';
        case 'both-changed':
            return 'sync conflict';
        case 'remote-unavailable':
            return 'sync offline';
        case 'schema-mismatch':
            return 'sync schema';
        case 'local-changed':
            return 'sync local';
        case 'remote-changed':
            return 'sync remote';
    }
}

function artifactKeyForDocument(
    manifest: RenovatioWorkspaceManifest,
    folder: vscode.WorkspaceFolder,
    uri: vscode.Uri
): RenovatioSyncArtifactKey | undefined {
    const documentPath = relativePath(folder, uri);
    return configuredArtifactKeys(manifest).find(key => manifest.artifacts[key] === documentPath);
}

function defaultSyncConfig(): NonNullable<RenovatioWorkspaceManifest['sync']> {
    return {
        enabled: false,
        mode: 'manual',
        lastSyncedAt: null,
        lastSyncedRevision: null,
        artifacts: SYNC_ARTIFACT_KEYS,
        conflictPolicy: 'prompt'
    };
}

function parentUri(uri: vscode.Uri): vscode.Uri {
    const parts = uri.path.split('/');
    parts.pop();
    return uri.with({ path: parts.join('/') || '/' });
}

function basename(path: string): string {
    return path.split('/').filter(Boolean).pop() ?? 'artifact.json';
}

function timestampForPath(): string {
    return new Date().toISOString().replace(/[:.]/g, '-');
}
