import * as vscode from 'vscode';
import {
    WORKSPACE_MANIFEST_RELATIVE_PATH,
    RenovatioWorkspaceManifestService,
    type RenovatioWorkspaceManifest
} from './workspaceManifest';
import {
    CHANGESET_ROOT,
    decodeBytes,
    encodeText,
    exists,
    relativePath,
    sha256File,
    workspaceUri
} from './changeSet';
import type { MigrationMapArtifact, MigrationMapEntry } from './migrationMap';

interface BundleArtifact {
    path: string;
    source?: string;
    sha256: string;
    required: boolean;
}

interface BundleWarning {
    path: string;
    message: string;
}

interface EvidenceBundleManifest {
    version: '1';
    projectId: string;
    exportedAt: string;
    partial: boolean;
    backend: {
        url: string;
        environment: string;
        version: string;
    };
    llm: {
        provider: string;
        model: string;
        promptProfile: string;
        cacheEnabled: boolean;
    };
    artifacts: BundleArtifact[];
    risks: string[];
    warnings: BundleWarning[];
    summary: {
        sourceFiles: number;
        migrationEntries: number;
        generatedTargets: number;
        staleEntries: number;
        evidenceFiles: number;
    };
}

export class RenovatioEvidenceBundleService implements vscode.Disposable {
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
            vscode.commands.registerCommand('renovatio.exportEvidenceBundle', () => this.exportEvidenceBundle()),
            vscode.commands.registerCommand('renovatio.openLatestEvidenceBundle', () => this.openLatestEvidenceBundle()),
            vscode.commands.registerCommand('renovatio.copyEvidenceSummary', () => this.copyEvidenceSummary())
        );
    }

    async exportEvidenceBundle(): Promise<void> {
        const context = await this.context();
        if (!context) return;

        const { folder, manifest } = context;
        const migrationMapUri = workspaceUri(folder, manifest.artifacts.migrationMap);
        const hasMigrationMap = await exists(migrationMapUri);
        if (!hasMigrationMap) {
            const selected = await vscode.window.showWarningMessage(
                'Migration map is missing. Export a partial evidence bundle?',
                { modal: true },
                'Export Partial'
            );
            if (selected !== 'Export Partial') return;
        }

        const bundleName = `renovatio-evidence-${sanitizeName(manifest.projectId)}-${timestampId(new Date())}`;
        const bundleUri = workspaceUri(folder, `.renovatio/evidence-bundles/${bundleName}`);
        await vscode.workspace.fs.createDirectory(bundleUri);

        const artifacts: BundleArtifact[] = [];
        const warnings: BundleWarning[] = [];
        const risks: string[] = [];
        const checksums: string[] = [];

        await this.copyArtifact(folder, bundleUri, WORKSPACE_MANIFEST_RELATIVE_PATH, 'workspace.renovatio.json', true, artifacts, warnings, checksums);
        await this.copyArtifact(folder, bundleUri, manifest.artifacts.migrationMap, 'migration-map.renovatio.json', true, artifacts, warnings, checksums);
        await this.copyArtifact(folder, bundleUri, manifest.artifacts.domainModel, 'domain-model.renovatio-domain.json', false, artifacts, warnings, checksums);
        await this.copyArtifact(folder, bundleUri, manifest.artifacts.persistenceModel, 'persistence-model.renovatio-domain.json', false, artifacts, warnings, checksums);
        await this.copyArtifact(folder, bundleUri, manifest.artifacts.architecture, 'architecture.renovatio-arch.json', false, artifacts, warnings, checksums);

        const migrationMap = hasMigrationMap
            ? JSON.parse(decodeBytes(await vscode.workspace.fs.readFile(migrationMapUri))) as MigrationMapArtifact
            : undefined;
        await this.copyChangeSets(folder, bundleUri, artifacts, warnings, checksums);
        await this.copyEvidenceReferences(folder, bundleUri, manifest, migrationMap, artifacts, warnings, checksums);

        const manifestArtifact = await this.writeBundleManifest(bundleUri, manifest, migrationMap, artifacts, warnings, risks, checksums);
        const summaryArtifact = await this.writeSummary(bundleUri, manifest, migrationMap, manifestArtifact, warnings, checksums);
        artifacts.push(manifestArtifact, summaryArtifact);

        await vscode.workspace.fs.writeFile(workspaceUriFromBase(bundleUri, 'checksums.txt'), encodeText(`${checksums.sort().join('\n')}\n`));
        this.output.appendLine(`[evidence] exported ${relativePath(folder, bundleUri)} with ${artifacts.length} artifact(s)`);

        const selected = await vscode.window.showInformationMessage(
            `Renovatio evidence bundle exported: ${relativePath(folder, bundleUri)}`,
            'Open Folder',
            'Copy Summary'
        );
        if (selected === 'Open Folder') {
            await vscode.commands.executeCommand('revealFileInOS', bundleUri);
        } else if (selected === 'Copy Summary') {
            await vscode.env.clipboard.writeText(decodeBytes(await vscode.workspace.fs.readFile(workspaceUriFromBase(bundleUri, 'summary.md'))));
        }
    }

    async openLatestEvidenceBundle(): Promise<void> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) {
            vscode.window.showWarningMessage('Open a VS Code workspace before opening an evidence bundle.');
            return;
        }
        const latest = await latestBundle(folder);
        if (!latest) {
            vscode.window.showInformationMessage('No Renovatio evidence bundle exists yet.');
            return;
        }
        const summary = workspaceUriFromBase(latest, 'summary.md');
        if (await exists(summary)) {
            await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(summary), { preview: false });
            return;
        }
        await vscode.commands.executeCommand('revealFileInOS', latest);
    }

    async copyEvidenceSummary(): Promise<void> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) {
            vscode.window.showWarningMessage('Open a VS Code workspace before copying an evidence summary.');
            return;
        }
        const latest = await latestBundle(folder);
        if (!latest) {
            vscode.window.showInformationMessage('No Renovatio evidence bundle exists yet.');
            return;
        }
        const summary = workspaceUriFromBase(latest, 'summary.md');
        if (!await exists(summary)) {
            vscode.window.showWarningMessage('Latest Renovatio evidence bundle does not contain summary.md.');
            return;
        }
        await vscode.env.clipboard.writeText(decodeBytes(await vscode.workspace.fs.readFile(summary)));
        vscode.window.showInformationMessage('Renovatio evidence summary copied.');
    }

    private async copyArtifact(
        folder: vscode.WorkspaceFolder,
        bundleUri: vscode.Uri,
        sourcePath: string,
        bundlePath: string,
        required: boolean,
        artifacts: BundleArtifact[],
        warnings: BundleWarning[],
        checksums: string[]
    ): Promise<void> {
        const source = workspaceUri(folder, sourcePath);
        if (!await exists(source)) {
            const message = `${required ? 'Required' : 'Optional'} artifact missing: ${sourcePath}`;
            warnings.push({ path: sourcePath, message });
            if (required) this.output.appendLine(`[evidence] required artifact missing: ${sourcePath}`);
            return;
        }
        const target = workspaceUriFromBase(bundleUri, bundlePath);
        await vscode.workspace.fs.createDirectory(parentUri(target));
        await vscode.workspace.fs.writeFile(target, await vscode.workspace.fs.readFile(source));
        const sha256 = await sha256File(target);
        artifacts.push({ path: bundlePath, source: sourcePath, sha256, required });
        checksums.push(`${sha256}  ${bundlePath}`);
    }

    private async copyChangeSets(
        folder: vscode.WorkspaceFolder,
        bundleUri: vscode.Uri,
        artifacts: BundleArtifact[],
        warnings: BundleWarning[],
        checksums: string[]
    ): Promise<void> {
        const files = await vscode.workspace.findFiles(
            new vscode.RelativePattern(folder, `${CHANGESET_ROOT}/**/*`),
            '**/{node_modules,target,.git}/**'
        );
        if (!files.length) {
            warnings.push({ path: CHANGESET_ROOT, message: 'No change sets were found.' });
            return;
        }
        for (const file of files.sort((left, right) => left.fsPath.localeCompare(right.fsPath))) {
            const sourceRelative = relativePath(folder, file);
            const bundlePath = sourceRelative.replace(`${CHANGESET_ROOT}/`, 'changesets/');
            await this.copyArtifact(folder, bundleUri, sourceRelative, bundlePath, false, artifacts, warnings, checksums);
        }
    }

    private async copyEvidenceReferences(
        folder: vscode.WorkspaceFolder,
        bundleUri: vscode.Uri,
        manifest: RenovatioWorkspaceManifest,
        migrationMap: MigrationMapArtifact | undefined,
        artifacts: BundleArtifact[],
        warnings: BundleWarning[],
        checksums: string[]
    ): Promise<void> {
        const evidence = new Set<string>();
        for (const entry of migrationMap?.entries ?? []) {
            for (const value of entry.evidence ?? []) {
                if (looksLikeWorkspaceFile(value)) evidence.add(value);
            }
        }
        const evidenceFiles = await vscode.workspace.findFiles(
            new vscode.RelativePattern(folder, `${manifest.artifacts.evidenceDir}/**/*`),
            '**/{node_modules,target,.git}/**'
        );
        for (const file of evidenceFiles) evidence.add(relativePath(folder, file));
        for (const value of [...evidence].sort()) {
            const source = await exists(workspaceUri(folder, value))
                ? value
                : `${manifest.artifacts.evidenceDir}/${value}`;
            if (!await exists(workspaceUri(folder, source))) {
                warnings.push({ path: value, message: 'Evidence reference does not exist.' });
                continue;
            }
            await this.copyArtifact(folder, bundleUri, source, `reports/${source.split('/').pop()}`, false, artifacts, warnings, checksums);
        }
    }

    private async writeBundleManifest(
        bundleUri: vscode.Uri,
        manifest: RenovatioWorkspaceManifest,
        migrationMap: MigrationMapArtifact | undefined,
        artifacts: BundleArtifact[],
        warnings: BundleWarning[],
        risks: string[],
        checksums: string[]
    ): Promise<BundleArtifact> {
        const bundleManifest: EvidenceBundleManifest = {
            version: '1',
            projectId: manifest.projectId,
            exportedAt: new Date().toISOString(),
            partial: warnings.some(warning => warning.message.startsWith('Required artifact missing')),
            backend: {
                url: manifest.backend.url,
                environment: manifest.backend.environment,
                version: 'unknown'
            },
            llm: {
                provider: manifest.llm.provider,
                model: manifest.llm.model,
                promptProfile: manifest.llm.promptProfile,
                cacheEnabled: manifest.llm.cacheEnabled
            },
            artifacts: [...artifacts].sort((left, right) => left.path.localeCompare(right.path)),
            risks,
            warnings,
            summary: summaryCounts(manifest, migrationMap, warnings)
        };
        const uri = workspaceUriFromBase(bundleUri, 'manifest.json');
        await vscode.workspace.fs.writeFile(uri, encodeText(`${JSON.stringify(bundleManifest, null, 2)}\n`));
        const sha256 = await sha256File(uri);
        checksums.push(`${sha256}  manifest.json`);
        return { path: 'manifest.json', sha256, required: true };
    }

    private async writeSummary(
        bundleUri: vscode.Uri,
        manifest: RenovatioWorkspaceManifest,
        migrationMap: MigrationMapArtifact | undefined,
        manifestArtifact: BundleArtifact,
        warnings: BundleWarning[],
        checksums: string[]
    ): Promise<BundleArtifact> {
        const counts = countsByStatus(migrationMap?.entries ?? []);
        const generated = (migrationMap?.entries ?? [])
            .filter(entry => entry.target?.path && (entry.status === 'generated' || entry.status === 'manually-edited'))
            .map(entry => `- ${entry.id}: ${entry.target?.path}`);
        const staleOrMissing = [
            ...(migrationMap?.entries ?? [])
                .filter(entry => entry.status === 'stale-source' || entry.status === 'stale-target')
                .map(entry => `- ${entry.id}: ${entry.status}`),
            ...warnings.map(warning => `- ${warning.path}: ${warning.message}`)
        ];
        const lines = [
            `# Renovatio Evidence Bundle`,
            '',
            `Project: ${manifest.projectId}`,
            `Exported: ${new Date().toISOString()}`,
            `Manifest checksum: ${manifestArtifact.sha256}`,
            '',
            `## Backend`,
            '',
            `- URL: ${manifest.backend.url}`,
            `- Environment: ${manifest.backend.environment}`,
            `- Version: unknown`,
            '',
            `## LLM`,
            '',
            `- Provider: ${manifest.llm.provider}`,
            `- Model: ${manifest.llm.model}`,
            `- Prompt profile: ${manifest.llm.promptProfile}`,
            `- Cache: ${manifest.llm.cacheEnabled ? 'enabled' : 'disabled'}`,
            '',
            `## Workspace`,
            '',
            ...manifest.source.roots.map(root => `- Source root: ${root}`),
            ...manifest.targets.map(target => `- Target root (${target.language}): ${target.root}`),
            '',
            `## Migration Status`,
            '',
            ...Object.entries(counts).map(([status, count]) => `- ${status}: ${count}`),
            '',
            `## Generated Targets`,
            '',
            ...(generated.length ? generated : ['- none']),
            '',
            `## Stale Or Missing Evidence`,
            '',
            ...(staleOrMissing.length ? staleOrMissing : ['- none']),
            '',
            `## Verification Notes`,
            '',
            `- Checksums are recorded in checksums.txt.`,
            `- Apply decisions and diffs are included when change sets exist.`,
            `- Equivalence execution is out of scope for this bundle export.`,
            ''
        ];
        const uri = workspaceUriFromBase(bundleUri, 'summary.md');
        await vscode.workspace.fs.writeFile(uri, encodeText(lines.join('\n')));
        const sha256 = await sha256File(uri);
        checksums.push(`${sha256}  summary.md`);
        return { path: 'summary.md', sha256, required: true };
    }

    private async context(): Promise<{ folder: vscode.WorkspaceFolder; manifest: RenovatioWorkspaceManifest } | undefined> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (!folder) {
            vscode.window.showWarningMessage('Open a VS Code workspace before exporting Renovatio evidence.');
            return undefined;
        }
        const manifest = await this.manifestService.load(folder);
        if (!manifest) {
            const selected = await vscode.window.showWarningMessage(
                'Renovatio workspace manifest is required before exporting evidence.',
                'Initialize Workspace'
            );
            if (selected === 'Initialize Workspace') await this.manifestService.initializeWorkspace();
            return undefined;
        }
        return { folder, manifest };
    }
}

async function latestBundle(folder: vscode.WorkspaceFolder): Promise<vscode.Uri | undefined> {
    const files = await vscode.workspace.findFiles(
        new vscode.RelativePattern(folder, '.renovatio/evidence-bundles/*/manifest.json'),
        '**/{node_modules,target,.git}/**'
    );
    if (!files.length) return undefined;
    const sorted = files.sort((left, right) => right.fsPath.localeCompare(left.fsPath));
    return parentUri(sorted[0]);
}

function summaryCounts(
    manifest: RenovatioWorkspaceManifest,
    migrationMap: MigrationMapArtifact | undefined,
    warnings: BundleWarning[]
): EvidenceBundleManifest['summary'] {
    return {
        sourceFiles: manifest.source.roots.length,
        migrationEntries: migrationMap?.entries.length ?? 0,
        generatedTargets: (migrationMap?.entries ?? []).filter(entry => entry.target?.path && entry.status === 'generated').length,
        staleEntries: (migrationMap?.entries ?? []).filter(entry => entry.status === 'stale-source' || entry.status === 'stale-target').length,
        evidenceFiles: warnings.filter(warning => warning.message.includes('Evidence')).length
    };
}

function countsByStatus(entries: MigrationMapEntry[]): Record<string, number> {
    return entries.reduce<Record<string, number>>((counts, entry) => {
        counts[entry.status] = (counts[entry.status] ?? 0) + 1;
        return counts;
    }, {});
}

function workspaceUriFromBase(base: vscode.Uri, relativePath: string): vscode.Uri {
    return vscode.Uri.joinPath(base, ...relativePath.split('/').filter(Boolean));
}

function parentUri(uri: vscode.Uri): vscode.Uri {
    const parts = uri.path.split('/');
    parts.pop();
    return uri.with({ path: parts.join('/') || '/' });
}

function sanitizeName(value: string): string {
    return value.toLowerCase().replace(/[^a-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '') || 'workspace';
}

function timestampId(value: Date): string {
    return value.toISOString().slice(0, 10).replace(/-/g, '');
}

function looksLikeWorkspaceFile(value: string): boolean {
    if (/^[a-z][a-z0-9+.-]*:/i.test(value)) return false;
    return value.includes('/') || value.includes('\\') || /\.(json|ya?ml|md|txt|log|sarif|xml|html?|diff)$/i.test(value);
}
