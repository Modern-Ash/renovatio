import * as vscode from 'vscode';
import {
    WORKSPACE_MANIFEST_RELATIVE_PATH,
    type RenovatioWorkspaceManifest,
    type RenovatioWorkspaceManifestService
} from './workspaceManifest';
import { encodeText, exists, workspaceUri } from './changeSet';

const SAMPLE_FILES = [
    {
        from: ['examples', 'sample.renovatio-domain.json'],
        to: '.renovatio/diagrams/carddemo.renovatio-domain.json'
    },
    {
        from: ['examples', 'sample.renovatio-arch.json'],
        to: '.renovatio/diagrams/carddemo.renovatio-arch.json'
    },
    {
        from: ['examples', 'sample.migration-map.renovatio.json'],
        to: '.renovatio/migration-map.renovatio.json'
    },
    {
        from: ['examples', 'src', 'mainframe', 'CARDDEMO.cbl'],
        to: 'src/mainframe/CARDDEMO.cbl'
    },
    {
        from: ['examples', 'copybooks', 'CARDREC.cpy'],
        to: 'copybooks/CARDREC.cpy'
    },
    {
        from: ['examples', 'generated', 'java', 'src', 'main', 'java', 'com', 'example', 'modernized', 'Carddemo.java'],
        to: 'generated/java/src/main/java/com/example/modernized/Carddemo.java'
    },
    {
        from: ['examples', 'evidence', 'evaluator-summary.md'],
        to: '.renovatio/evidence/evaluator-summary.md'
    }
];

export class RenovatioOnboardingService implements vscode.Disposable {
    private readonly disposables: vscode.Disposable[] = [];
    private guidePanel?: vscode.WebviewPanel;

    constructor(
        private readonly context: vscode.ExtensionContext,
        private readonly manifestService: RenovatioWorkspaceManifestService
    ) {}

    register(context: vscode.ExtensionContext): void {
        this.disposables.push(
            vscode.commands.registerCommand('renovatio.openEvaluatorGuide', () => this.openEvaluatorGuide()),
            vscode.commands.registerCommand('renovatio.installDemoWorkspace', () => this.installDemoWorkspace()),
            vscode.commands.registerCommand('renovatio.openCobolSample', () => this.openBundledSample(['examples', 'src', 'mainframe', 'CARDDEMO.cbl'])),
            vscode.commands.registerCommand('renovatio.openGeneratedJavaSample', () => this.openBundledSample(['examples', 'generated', 'java', 'src', 'main', 'java', 'com', 'example', 'modernized', 'Carddemo.java'])),
            vscode.commands.registerCommand('renovatio.openEvidenceSummarySample', () => this.openEvidenceSummarySample()),
            vscode.commands.registerCommand('renovatio.runEvaluatorChecks', () => this.runEvaluatorChecks())
        );
        context.subscriptions.push(...this.disposables);
    }

    dispose(): void {
        this.disposables.forEach(disposable => disposable.dispose());
    }

    private async openEvaluatorGuide(): Promise<void> {
        if (!this.guidePanel) {
            this.guidePanel = vscode.window.createWebviewPanel(
                'renovatioEvaluatorGuide',
                'Renovatio Evaluator Guide',
                vscode.ViewColumn.One,
                { enableCommandUris: true }
            );
            this.guidePanel.onDidDispose(() => { this.guidePanel = undefined; });
        }
        this.guidePanel.reveal(vscode.ViewColumn.One);
        const manifest = await this.manifestService.load();
        this.guidePanel.webview.html = this.renderGuide(manifest);
    }

    private async installDemoWorkspace(): Promise<void> {
        const folder = await pickWorkspaceFolder();
        if (!folder) return;

        const manifestUri = workspaceUri(folder, WORKSPACE_MANIFEST_RELATIVE_PATH);
        if (!await exists(manifestUri)) {
            await vscode.workspace.fs.createDirectory(parentUri(manifestUri));
            await vscode.workspace.fs.writeFile(manifestUri, encodeText(`${JSON.stringify(defaultDemoManifest(folder), null, 2)}\n`));
        }

        for (const sample of SAMPLE_FILES) {
            const source = vscode.Uri.joinPath(this.context.extensionUri, ...sample.from);
            const target = workspaceUri(folder, sample.to);
            await vscode.workspace.fs.createDirectory(parentUri(target));
            await vscode.workspace.fs.writeFile(target, await vscode.workspace.fs.readFile(source));
        }

        await this.manifestService.validateWorkspace(folder);
        vscode.window.showInformationMessage('Renovatio demo workspace assets are ready.', 'Open Guide', 'Analyze Workspace')
            .then(action => {
                if (action === 'Open Guide') void this.openEvaluatorGuide();
                if (action === 'Analyze Workspace') void vscode.commands.executeCommand('renovatio.analyzeWorkspace');
            });
    }

    private async openEvidenceSummarySample(): Promise<void> {
        const folder = vscode.workspace.workspaceFolders?.[0];
        if (folder) {
            const local = workspaceUri(folder, '.renovatio/evidence/evaluator-summary.md');
            if (await exists(local)) {
                await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(local), { preview: false });
                return;
            }
        }
        await this.openBundledSample(['examples', 'evidence', 'evaluator-summary.md']);
    }

    private async runEvaluatorChecks(): Promise<void> {
        const manifest = await this.manifestService.load();
        if (!manifest) {
            const action = await vscode.window.showInformationMessage(
                'Create or install a Renovatio workspace manifest before running backend and LLM checks.',
                'Install Demo Workspace',
                'Initialize Workspace'
            );
            if (action === 'Install Demo Workspace') await this.installDemoWorkspace();
            if (action === 'Initialize Workspace') await this.manifestService.initializeWorkspace();
            return;
        }
        await vscode.commands.executeCommand('renovatio.testBackendConnection');
        await vscode.commands.executeCommand('renovatio.testLlmReverseEngineering');
    }

    private async openBundledSample(relativeParts: string[]): Promise<void> {
        const uri = vscode.Uri.joinPath(this.context.extensionUri, ...relativeParts);
        await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(uri), { preview: false });
    }

    private renderGuide(manifest: RenovatioWorkspaceManifest | undefined): string {
        const status = manifest ? 'Ready' : 'Needs setup';
        const backend = manifest ? `${manifest.backend.url} (${manifest.backend.environment})` : 'Install demo workspace or initialize manifest';
        const llm = manifest ? `${manifest.llm.provider} / ${manifest.llm.model}` : 'Choose provider and model after setup';
        return `<!doctype html>
<html>
<head>
<meta charset="utf-8">
<style>
  :root { color-scheme: dark; }
  body {
    margin: 0;
    padding: 24px;
    color: var(--vscode-foreground);
    background: var(--vscode-editor-background);
    font-family: var(--vscode-font-family);
  }
  main { max-width: 860px; margin: 0 auto; }
  h1 { margin: 0 0 6px; font-size: 28px; line-height: 1.15; }
  p { color: var(--vscode-descriptionForeground); line-height: 1.45; }
  .status {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
    gap: 8px;
    margin: 20px 0;
  }
  .cell {
    border: 1px solid var(--vscode-panel-border);
    padding: 10px;
    background: var(--vscode-sideBar-background);
  }
  .cell span {
    display: block;
    color: var(--vscode-descriptionForeground);
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
  }
  .cell strong { display: block; margin-top: 5px; overflow-wrap: anywhere; }
  .steps { display: grid; gap: 10px; margin-top: 18px; }
  .step {
    display: grid;
    grid-template-columns: 26px 1fr auto;
    gap: 12px;
    align-items: center;
    border-top: 1px solid var(--vscode-panel-border);
    padding-top: 10px;
  }
  .step b { color: var(--vscode-descriptionForeground); }
  a {
    color: var(--vscode-textLink-foreground);
    font-weight: 700;
    text-decoration: none;
    white-space: nowrap;
  }
  a:hover { text-decoration: underline; }
  code {
    color: var(--vscode-textPreformat-foreground);
    background: var(--vscode-textCodeBlock-background);
    padding: 2px 5px;
  }
</style>
</head>
<body>
<main>
  <h1>Renovatio in 5 minutes</h1>
  <p>Use this path to see setup, backend/LLM identity, discovery, review, migration traceability and evidence without touching a terminal.</p>
  <section class="status">
    <div class="cell"><span>Workspace</span><strong>${escapeHtml(status)}</strong></div>
    <div class="cell"><span>Backend</span><strong>${escapeHtml(backend)}</strong></div>
    <div class="cell"><span>LLM</span><strong>${escapeHtml(llm)}</strong></div>
  </section>
  <section class="steps">
    ${guideStep('1', 'Create demo assets', 'Manifest, COBOL, generated Java, model, migration map and evidence.', 'renovatio.installDemoWorkspace', 'Install Demo Workspace')}
    ${guideStep('2', 'Confirm backend and LLM', 'Shows Offline, Ready or unsupported endpoints before analysis.', 'renovatio.runEvaluatorChecks', 'Run Checks')}
    ${guideStep('3', 'Analyze COBOL', 'Parse the configured roots and populate discovery output.', 'renovatio.analyzeWorkspace', 'Analyze Workspace')}
    ${guideStep('4', 'Review models', 'Open domain and architecture diagrams with native VS Code editors.', 'renovatio.openNativeDomainDiagram', 'Open Domain Diagram')}
    ${guideStep('5', 'Review traceability', 'Inspect legacy-to-target mapping and stale states.', 'renovatio.openMigrationMap', 'Open Migration Map')}
    ${guideStep('6', 'Export evidence', 'Package reports, checksums and review context.', 'renovatio.exportEvidenceBundle', 'Export Evidence')}
  </section>
  <p>Samples: <a href="command:renovatio.openCobolSample">COBOL</a> · <a href="command:renovatio.openGeneratedJavaSample">Java target</a> · <a href="command:renovatio.openEvidenceSummarySample">Evidence summary</a></p>
</main>
</body>
</html>`;
    }
}

function guideStep(index: string, title: string, description: string, command: string, label: string): string {
    return `<div class="step"><b>${escapeHtml(index)}</b><div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(description)}</p></div><a href="command:${command}">${escapeHtml(label)}</a></div>`;
}

async function pickWorkspaceFolder(): Promise<vscode.WorkspaceFolder | undefined> {
    const folders = vscode.workspace.workspaceFolders ?? [];
    if (!folders.length) {
        vscode.window.showWarningMessage('Open a VS Code folder before installing Renovatio demo assets.');
        return undefined;
    }
    if (folders.length === 1) return folders[0];
    const selected = await vscode.window.showQuickPick(
        folders.map(folder => ({ label: folder.name, description: folder.uri.fsPath, folder })),
        { placeHolder: 'Select the workspace folder for Renovatio demo assets' }
    );
    return selected?.folder;
}

function defaultDemoManifest(folder: vscode.WorkspaceFolder): RenovatioWorkspaceManifest {
    return {
        version: '1',
        projectId: sanitizeProjectId(folder.name || 'carddemo'),
        source: {
            language: 'cobol',
            roots: ['src/mainframe', 'copybooks'],
            include: ['**/*.cbl', '**/*.cob', '**/*.cpy', '**/*.jcl'],
            exclude: ['**/target/**', '**/.git/**']
        },
        targets: [{
            language: 'java',
            root: 'generated/java',
            package: 'com.example.modernized',
            framework: 'spring'
        }],
        artifacts: {
            domainModel: '.renovatio/diagrams/carddemo.renovatio-domain.json',
            persistenceModel: '.renovatio/diagrams/carddemo-persistence.renovatio-domain.json',
            architecture: '.renovatio/diagrams/carddemo.renovatio-arch.json',
            migrationMap: '.renovatio/migration-map.renovatio.json',
            evidenceDir: '.renovatio/evidence'
        },
        backend: {
            url: 'http://127.0.0.1:8081',
            environment: 'local',
            healthEndpoint: '/actuator/health',
            capabilitiesEndpoint: '/api/capabilities',
            allowLocalProcessControl: true,
            commands: {
                start: './mvnw -pl renovatio-api spring-boot:run',
                reloadConfig: 'curl -X POST http://127.0.0.1:8081/api/admin/reload'
            }
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

function parentUri(uri: vscode.Uri): vscode.Uri {
    const parts = uri.path.split('/');
    parts.pop();
    return uri.with({ path: parts.join('/') || '/' });
}

function sanitizeProjectId(value: string): string {
    return value.trim().replace(/[^a-zA-Z0-9._-]+/g, '-').replace(/^-+|-+$/g, '') || 'carddemo';
}

function escapeHtml(value: string): string {
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
