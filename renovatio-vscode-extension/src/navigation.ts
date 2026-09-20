import * as vscode from 'vscode';
import { formatMigrationMap, validateMigrationMapArtifact, type MigrationMapArtifact, type MigrationMapEntry, type MigrationLocation } from './migrationMap';
import { type RenovatioWorkspaceManifest, RenovatioWorkspaceManifestService } from './workspaceManifest';
import { entriesForMigrationPath, migrationHoverMarkdown } from './workbenchCore';

type Side = 'source' | 'target';

interface MigrationMapContext {
    folder: vscode.WorkspaceFolder;
    manifest: RenovatioWorkspaceManifest;
    uri: vscode.Uri;
    artifact: MigrationMapArtifact;
}

const SOURCE_LANGUAGES = ['cobol', 'jcl'];
const TARGET_LANGUAGES = ['java', 'python', 'javascript', 'typescript'];

export class MigrationNavigationService implements vscode.CodeLensProvider, vscode.HoverProvider, vscode.Disposable {
    private readonly onDidChangeCodeLensesEmitter = new vscode.EventEmitter<void>();
    readonly onDidChangeCodeLenses = this.onDidChangeCodeLensesEmitter.event;
    private readonly disposables: vscode.Disposable[] = [];

    constructor(private readonly manifestService: RenovatioWorkspaceManifestService) {
        const watcher = vscode.workspace.createFileSystemWatcher('**/*migration-map*.renovatio.json');
        this.disposables.push(
            this.onDidChangeCodeLensesEmitter,
            watcher,
            watcher.onDidChange(() => this.refresh()),
            watcher.onDidCreate(() => this.refresh()),
            watcher.onDidDelete(() => this.refresh()),
            vscode.workspace.onDidSaveTextDocument(document => {
                if (isMigrationMapDocument(document)) this.refresh();
            })
        );
    }

    register(context: vscode.ExtensionContext): void {
        const selector: vscode.DocumentSelector = [
            ...SOURCE_LANGUAGES.map(language => ({ language, scheme: 'file' })),
            ...TARGET_LANGUAGES.map(language => ({ language, scheme: 'file' }))
        ];
        context.subscriptions.push(
            vscode.languages.registerCodeLensProvider(selector, this),
            vscode.languages.registerHoverProvider(selector, this),
            vscode.commands.registerCommand('renovatio.openMigrationSource', (entryId?: string) => this.openSide(entryId, 'source')),
            vscode.commands.registerCommand('renovatio.openMigrationTarget', (entryId?: string) => this.openSide(entryId, 'target')),
            vscode.commands.registerCommand('renovatio.showMigrationEvidence', (entryId?: string) => this.showEvidence(entryId)),
            vscode.commands.registerCommand('renovatio.openMappedDomainNode', (entryId?: string) => this.openMappedDomainNode(entryId)),
            vscode.commands.registerCommand('renovatio.markTargetManuallyRefined', (entryId?: string) => this.markTargetManuallyRefined(entryId)),
            vscode.commands.registerCommand('renovatio.reconcileTargetChange', (entryId?: string) => this.reconcileTargetChange(entryId))
        );
    }

    dispose(): void {
        this.disposables.forEach(disposable => disposable.dispose());
    }

    refresh(): void {
        this.onDidChangeCodeLensesEmitter.fire();
    }

    async provideCodeLenses(document: vscode.TextDocument): Promise<vscode.CodeLens[]> {
        const side = sideForLanguage(document.languageId);
        if (!side) return [];
        const context = await this.contextForDocument(document);
        if (!context) return [];
        const entries = entriesForDocument(context, document, side);
        const lenses: vscode.CodeLens[] = [];
        for (const entry of entries) {
            const range = lensRange(document, entry[side]);
            if (side === 'source') {
                lenses.push(
                    lens(range, 'Renovatio: Open target', 'renovatio.openMigrationTarget', entry.id),
                    lens(range, 'Renovatio: Show migration evidence', 'renovatio.showMigrationEvidence', entry.id),
                    lens(range, 'Renovatio: Open domain node', 'renovatio.openMappedDomainNode', entry.id),
                    lens(range, 'Renovatio: Preview generated diff', 'renovatio.previewMigrationDiff', entry.id)
                );
            } else {
                lenses.push(
                    lens(range, 'Renovatio: Open legacy source', 'renovatio.openMigrationSource', entry.id),
                    lens(range, 'Renovatio: Show migration evidence', 'renovatio.showMigrationEvidence', entry.id),
                    lens(range, 'Renovatio: Mark as manually refined', 'renovatio.markTargetManuallyRefined', entry.id),
                    lens(range, 'Renovatio: Reconcile target change', 'renovatio.reconcileTargetChange', entry.id)
                );
            }
        }
        return lenses;
    }

    async provideHover(document: vscode.TextDocument, position: vscode.Position): Promise<vscode.Hover | undefined> {
        const side = sideForLanguage(document.languageId);
        if (!side) return undefined;
        const context = await this.contextForDocument(document);
        if (!context) return undefined;
        const entries = entriesForDocument(context, document, side)
            .filter(entry => entryMatchesPosition(document, position, entry[side]));
        if (!entries.length) return undefined;
        const markdown = new vscode.MarkdownString(undefined, true);
        markdown.isTrusted = { enabledCommands: ['renovatio.openMigrationTarget', 'renovatio.openMigrationSource', 'renovatio.showMigrationEvidence'] };
        markdown.supportThemeIcons = true;
        markdown.appendMarkdown(entries.slice(0, 3).map(entry => migrationHoverMarkdown(entry, side)).join('\n\n---\n\n'));
        return new vscode.Hover(markdown);
    }

    private async openSide(entryId: string | undefined, side: Side): Promise<void> {
        const resolved = await this.resolveEntry(entryId);
        if (!resolved) return;
        const location = resolved.entry[side];
        if (!location) {
            vscode.window.showWarningMessage(`Migration entry ${resolved.entry.id} has no ${side} location.`);
            return;
        }
        await this.openLocation(resolved, location);
    }

    private async showEvidence(entryId: string | undefined): Promise<void> {
        const resolved = await this.resolveEntry(entryId);
        if (!resolved) return;
        const evidence = resolved.entry.evidence ?? [];
        if (!evidence.length) {
            vscode.window.showInformationMessage(`Migration entry ${resolved.entry.id} has no evidence links yet.`);
            return;
        }
        const selected = await vscode.window.showQuickPick(evidence.map(value => ({ label: value })), {
            title: 'Renovatio Migration Evidence',
            placeHolder: 'Select an evidence link to open if it is a workspace path.'
        });
        if (!selected) return;
        await this.openEvidenceValue(resolved, selected.label);
    }

    private async openMappedDomainNode(entryId: string | undefined): Promise<void> {
        const resolved = await this.resolveEntry(entryId);
        if (!resolved) return;
        const ids = resolved.entry.renovatio?.domainNodeIds ?? [];
        await vscode.commands.executeCommand('renovatio.openDomainModel');
        if (ids.length) {
            vscode.window.showInformationMessage(`Domain node: ${ids[0]}`);
        } else {
            vscode.window.showInformationMessage(`Migration entry ${resolved.entry.id} has no mapped domain node yet.`);
        }
    }

    private async markTargetManuallyRefined(entryId: string | undefined): Promise<void> {
        const resolved = await this.resolveEntry(entryId);
        if (!resolved) return;
        const selected = await vscode.window.showWarningMessage(
            `Mark migration entry ${resolved.entry.id} as manually refined?`,
            { modal: true, detail: 'This updates only the migration map status and decision metadata. It does not modify source or target code.' },
            'Mark Refined'
        );
        if (selected !== 'Mark Refined') return;
        const updated: MigrationMapArtifact = {
            ...resolved.context.artifact,
            entries: resolved.context.artifact.entries.map(entry => entry.id === resolved.entry.id
                ? {
                    ...entry,
                    status: 'manually-edited',
                    lastDecision: {
                        actor: 'vscode',
                        action: 'mark-target-manually-refined',
                        at: new Date().toISOString(),
                        reason: 'Marked from Renovatio VS Code target editor navigation.'
                    }
                }
                : entry)
        };
        await vscode.workspace.fs.writeFile(resolved.context.uri, new TextEncoder().encode(formatMigrationMap(updated)));
        this.refresh();
        vscode.window.showInformationMessage(`Marked ${resolved.entry.id} as manually edited.`);
    }

    private async reconcileTargetChange(entryId: string | undefined): Promise<void> {
        const resolved = await this.resolveEntry(entryId);
        if (!resolved) return;
        await vscode.commands.executeCommand('renovatio.openMigrationMap');
        vscode.window.showInformationMessage(`Review migration entry ${resolved.entry.id} before reconciling generated target changes.`);
    }

    private async openLocation(resolved: { context: MigrationMapContext; entry: MigrationMapEntry }, location: MigrationLocation): Promise<void> {
        const uri = vscode.Uri.joinPath(resolved.context.folder.uri, ...location.path.split('/'));
        if (!await exists(uri)) {
            const selected = await vscode.window.showWarningMessage(
                `Mapped file does not exist: ${location.path}`,
                'Open Migration Map'
            );
            if (selected === 'Open Migration Map') {
                await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(resolved.context.uri), { preview: false });
            }
            return;
        }
        const document = await vscode.workspace.openTextDocument(uri);
        const editor = await vscode.window.showTextDocument(document, { preview: false });
        const range = vscodeRange(location.range, document);
        editor.selection = new vscode.Selection(range.start, range.start);
        editor.revealRange(range, vscode.TextEditorRevealType.InCenterIfOutsideViewport);
    }

    private async openEvidenceValue(resolved: { context: MigrationMapContext }, value: string): Promise<void> {
        const normalized = value.replace(/\\/g, '/').replace(/^file:\/\//, '');
        if (/^https?:\/\//.test(normalized)) {
            await vscode.env.openExternal(vscode.Uri.parse(normalized));
            return;
        }
        const candidate = vscode.Uri.joinPath(resolved.context.folder.uri, ...normalized.split('/'));
        if (await exists(candidate)) {
            await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(candidate), { preview: false });
            return;
        }
        vscode.window.showInformationMessage(value);
    }

    private async resolveEntry(entryId: string | undefined): Promise<{ context: MigrationMapContext; entry: MigrationMapEntry } | undefined> {
        const context = await this.contextForActiveWorkspace();
        if (!context) {
            vscode.window.showWarningMessage('No migration map is available for this workspace.');
            return undefined;
        }
        let entry = entryId ? context.artifact.entries.find(candidate => candidate.id === entryId) : undefined;
        if (!entry) {
            const picked = await vscode.window.showQuickPick(context.artifact.entries.map(candidate => ({
                label: candidate.id,
                description: `${candidate.source?.path ?? 'no source'} -> ${candidate.target?.path ?? 'no target'}`,
                entry: candidate
            })), { title: 'Select Renovatio migration entry' });
            entry = picked?.entry;
        }
        return entry ? { context, entry } : undefined;
    }

    private async contextForDocument(document: vscode.TextDocument): Promise<MigrationMapContext | undefined> {
        const folder = vscode.workspace.getWorkspaceFolder(document.uri);
        if (!folder) return undefined;
        return this.loadContext(folder, { silent: true });
    }

    private async contextForActiveWorkspace(): Promise<MigrationMapContext | undefined> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) return undefined;
        return this.loadContext(folder, { silent: false });
    }

    private async loadContext(folder: vscode.WorkspaceFolder, options: { silent: boolean }): Promise<MigrationMapContext | undefined> {
        const manifest = await this.manifestService.load(folder);
        if (!manifest) return undefined;
        const uri = vscode.Uri.joinPath(folder.uri, ...manifest.artifacts.migrationMap.split('/'));
        if (!await exists(uri)) return undefined;
        try {
            const artifact = JSON.parse(new TextDecoder('utf-8').decode(await vscode.workspace.fs.readFile(uri))) as MigrationMapArtifact;
            const issues = validateMigrationMapArtifact(artifact);
            if (issues.length) {
                if (!options.silent) vscode.window.showWarningMessage(`Migration map is invalid: ${issues[0]}`);
                return undefined;
            }
            return { folder, manifest, uri, artifact };
        } catch (error) {
            if (!options.silent) vscode.window.showErrorMessage(`Could not read migration map: ${message(error)}`);
            return undefined;
        }
    }
}

function sideForLanguage(languageId: string): Side | undefined {
    if (SOURCE_LANGUAGES.includes(languageId)) return 'source';
    if (TARGET_LANGUAGES.includes(languageId)) return 'target';
    return undefined;
}

function entriesForDocument(context: MigrationMapContext, document: vscode.TextDocument, side: Side): MigrationMapEntry[] {
    const path = relativePath(context.folder, document.uri);
    return entriesForMigrationPath(context.artifact, path, side) as MigrationMapEntry[];
}

function entryMatchesPosition(document: vscode.TextDocument, position: vscode.Position, location: MigrationLocation | undefined): boolean {
    if (!location) return false;
    if (location.range) return vscodeRange(location.range, document).contains(position);
    const symbol = location.symbol?.trim();
    if (symbol) {
        const word = document.getText(document.getWordRangeAtPosition(position));
        if (word && word.toUpperCase() === symbol.toUpperCase()) return true;
    }
    return position.line === 0;
}

function lens(range: vscode.Range, title: string, command: string, entryId: string): vscode.CodeLens {
    return new vscode.CodeLens(range, { title, command, arguments: [entryId] });
}

function lensRange(document: vscode.TextDocument, location: MigrationLocation | undefined): vscode.Range {
    return vscodeRange(location?.range, document);
}

function vscodeRange(range: MigrationLocation['range'] | undefined, document: vscode.TextDocument): vscode.Range {
    if (!range) {
        const line = Math.min(0, Math.max(0, document.lineCount - 1));
        return new vscode.Range(line, 0, line, 0);
    }
    const startLine = clamp(range.startLine - 1, 0, Math.max(0, document.lineCount - 1));
    const endLine = clamp(range.endLine - 1, startLine, Math.max(0, document.lineCount - 1));
    const startColumn = Math.max(0, range.startColumn - 1);
    const endColumn = Math.max(startColumn, range.endColumn - 1);
    return new vscode.Range(startLine, startColumn, endLine, endColumn);
}

function relativePath(folder: vscode.WorkspaceFolder, uri: vscode.Uri): string {
    const root = normalizePath(folder.uri.fsPath);
    const file = normalizePath(uri.fsPath);
    return file.startsWith(`${root}/`) ? file.slice(root.length + 1) : file;
}

function normalizePath(value: string | undefined): string {
    return String(value ?? '').replace(/\\/g, '/');
}

function isMigrationMapDocument(document: vscode.TextDocument): boolean {
    return document.uri.fsPath.replace(/\\/g, '/').endsWith('migration-map.renovatio.json');
}

async function exists(uri: vscode.Uri): Promise<boolean> {
    try {
        await vscode.workspace.fs.stat(uri);
        return true;
    } catch {
        return false;
    }
}

function clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
}

function message(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
