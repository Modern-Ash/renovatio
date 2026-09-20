import * as vscode from 'vscode';
import { exec } from 'node:child_process';
import { RenovatioWorkspaceManifestService, type RenovatioWorkspaceManifest } from './workspaceManifest';

type BackendStatus = 'unknown' | 'healthy' | 'unhealthy' | 'unsupported';

interface BackendSnapshot {
    status: BackendStatus;
    checkedAt?: Date;
    version?: string;
    error?: string;
    llmSmoke?: string;
}

export class RenovatioBackendControlCenter implements vscode.TreeDataProvider<BackendTreeItem>, vscode.Disposable {
    private readonly onDidChangeTreeDataEmitter = new vscode.EventEmitter<BackendTreeItem | undefined>();
    readonly onDidChangeTreeData = this.onDidChangeTreeDataEmitter.event;
    private readonly disposables: vscode.Disposable[] = [];
    private snapshot: BackendSnapshot = { status: 'unknown' };

    constructor(
        private readonly manifestService: RenovatioWorkspaceManifestService,
        private readonly output: vscode.OutputChannel
    ) {
        this.disposables.push(this.onDidChangeTreeDataEmitter);
    }

    dispose(): void {
        this.disposables.forEach(disposable => disposable.dispose());
    }

    refresh(): void {
        this.onDidChangeTreeDataEmitter.fire(undefined);
    }

    getTreeItem(element: BackendTreeItem): vscode.TreeItem {
        return element;
    }

    async getChildren(element?: BackendTreeItem): Promise<BackendTreeItem[]> {
        const manifest = await this.manifest();
        if (!manifest) {
            return [
                action('Initialize Renovatio Workspace', 'renovatio.initializeWorkspace', 'required before backend config'),
                action('Open Workspace Manifest', 'renovatio.openWorkspaceManifest')
            ];
        }
        if (!element) {
            return [
                section('Backend Connection', 'backend'),
                section('LLM Reverse Engineering', 'llm'),
                section('Server Control', 'server')
            ];
        }
        if (element.id === 'backend') {
            return [
                value('URL', manifest.backend.url),
                value('Environment', manifest.backend.environment),
                value('Health', healthLabel(this.snapshot), this.snapshot.error),
                value('Version', this.snapshot.version ?? 'unknown'),
                value('Last check', this.snapshot.checkedAt?.toLocaleString() ?? 'never'),
                action('Test Connection', 'renovatio.testBackendConnection', manifest.backend.healthEndpoint ?? '/actuator/health'),
                action('Open Settings', 'renovatio.openBackendSettings'),
                action('Open Logs', 'renovatio.openBackendLogs'),
                action('Reload Config', 'renovatio.reloadBackendConfiguration')
            ];
        }
        if (element.id === 'llm') {
            return [
                value('Provider', manifest.llm.provider),
                value('Model', manifest.llm.model),
                value('Fallback', manifest.llm.fallbackModel ?? 'none'),
                value('Prompt profile', manifest.llm.promptProfile),
                value('Cache', manifest.llm.cacheEnabled ? 'enabled' : 'disabled'),
                value('Smoke test', this.snapshot.llmSmoke ?? 'not run'),
                action('Configure Model', 'renovatio.configureLlmModel'),
                action('Test Reverse Engineering', 'renovatio.testLlmReverseEngineering'),
                action('Clear LLM Cache', 'renovatio.clearLlmCache'),
                action('Open Prompt Profile', 'renovatio.openPromptProfile'),
                action('Compare Against Golden Fixture', 'renovatio.compareLlmOutputAgainstGoldenFixture')
            ];
        }
        return [
            action('Start Backend', 'renovatio.startBackend', controlState(manifest)),
            action('Stop Backend', 'renovatio.stopBackend', controlState(manifest)),
            action('Restart Backend', 'renovatio.restartBackend', controlState(manifest)),
            action('Reload Config', 'renovatio.reloadBackendConfiguration')
        ];
    }

    register(context: vscode.ExtensionContext): void {
        context.subscriptions.push(
            vscode.window.registerTreeDataProvider('renovatio.backend', this),
            vscode.commands.registerCommand('renovatio.openBackendSettings', () => this.manifestService.openWorkspaceManifest()),
            vscode.commands.registerCommand('renovatio.testBackendConnection', () => this.testBackendConnection()),
            vscode.commands.registerCommand('renovatio.openBackendLogs', () => this.output.show()),
            vscode.commands.registerCommand('renovatio.startBackend', () => this.runBackendCommand('start')),
            vscode.commands.registerCommand('renovatio.stopBackend', () => this.runBackendCommand('stop')),
            vscode.commands.registerCommand('renovatio.restartBackend', () => this.runBackendCommand('restart')),
            vscode.commands.registerCommand('renovatio.reloadBackendConfiguration', () => this.runBackendCommand('reloadConfig')),
            vscode.commands.registerCommand('renovatio.configureLlmModel', () => this.configureLlmModel()),
            vscode.commands.registerCommand('renovatio.testLlmReverseEngineering', () => this.testLlmReverseEngineering()),
            vscode.commands.registerCommand('renovatio.clearLlmCache', () => this.clearLlmCache()),
            vscode.commands.registerCommand('renovatio.openPromptProfile', () => this.openPromptProfile()),
            vscode.commands.registerCommand('renovatio.compareLlmOutputAgainstGoldenFixture', () => this.compareLlmOutputAgainstGoldenFixture())
        );
    }

    async testBackendConnection(): Promise<void> {
        const manifest = await this.manifest();
        if (!manifest) return;
        const endpoint = manifest.backend.healthEndpoint ?? '/actuator/health';
        const url = joinUrl(manifest.backend.url, endpoint);
        this.output.appendLine(`[backend] GET ${url}`);
        try {
            const response = await fetchWithTimeout(url, 6000);
            const body = await response.text();
            if (!response.ok) throw new Error(`HTTP ${response.status}: ${body.slice(0, 240)}`);
            const parsed = parseMaybeJson(body);
            this.snapshot = {
                status: healthStatus(parsed),
                checkedAt: new Date(),
                version: versionFrom(parsed),
                error: undefined,
                llmSmoke: this.snapshot.llmSmoke
            };
            this.output.appendLine(`[backend] health ok: ${body.slice(0, 500)}`);
            vscode.window.showInformationMessage(`Renovatio backend health: ${this.snapshot.status}`);
        } catch (error) {
            this.snapshot = {
                status: 'unhealthy',
                checkedAt: new Date(),
                error: message(error),
                llmSmoke: this.snapshot.llmSmoke
            };
            this.output.appendLine(`[backend] health failed: ${message(error)}`);
            vscode.window.showWarningMessage(`Renovatio backend unreachable: ${message(error)}`);
        }
        this.refresh();
    }

    async configureLlmModel(): Promise<void> {
        const manifest = await this.manifest();
        if (!manifest) return;
        const provider = await vscode.window.showInputBox({ title: 'Renovatio LLM Provider', value: manifest.llm.provider });
        if (!provider) return;
        const model = await vscode.window.showInputBox({ title: 'Renovatio LLM Model', value: manifest.llm.model });
        if (!model) return;
        const fallbackModel = await vscode.window.showInputBox({
            title: 'Renovatio fallback model',
            value: manifest.llm.fallbackModel ?? '',
            prompt: 'Leave empty to disable fallback.'
        });
        if (fallbackModel === undefined) return;
        await this.manifestService.update(next => {
            next.llm.provider = provider.trim();
            next.llm.model = model.trim();
            next.llm.fallbackModel = fallbackModel?.trim() || null;
        });
        this.refresh();
    }

    async testLlmReverseEngineering(): Promise<void> {
        const manifest = await this.manifest();
        if (!manifest) return;
        const endpoint = '/api/llm/reverse-engineering/smoke-test';
        const url = joinUrl(manifest.backend.url, endpoint);
        this.output.appendLine(`[llm] POST ${url}`);
        try {
            const response = await fetchWithTimeout(url, 10000, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ provider: manifest.llm.provider, model: manifest.llm.model, promptProfile: manifest.llm.promptProfile })
            });
            const body = await response.text();
            if (response.status === 404 || response.status === 405) {
                this.snapshot = { ...this.snapshot, llmSmoke: 'unsupported by backend' };
                vscode.window.showInformationMessage('LLM smoke test endpoint is not available in this backend.');
            } else if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${body.slice(0, 240)}`);
            } else {
                this.snapshot = { ...this.snapshot, llmSmoke: 'passed' };
                vscode.window.showInformationMessage('LLM reverse-engineering smoke test passed.');
            }
            this.output.appendLine(`[llm] smoke response: ${body.slice(0, 500)}`);
        } catch (error) {
            this.snapshot = { ...this.snapshot, llmSmoke: `failed: ${message(error)}` };
            this.output.appendLine(`[llm] smoke failed: ${message(error)}`);
            vscode.window.showWarningMessage(`LLM smoke test failed: ${message(error)}`);
        }
        this.refresh();
    }

    async clearLlmCache(): Promise<void> {
        await this.postOptional('/api/llm/cache/clear', 'LLM cache clear');
    }

    async openPromptProfile(): Promise<void> {
        const manifest = await this.manifest();
        if (!manifest) return;
        const files = await vscode.workspace.findFiles(`**/${manifest.llm.promptProfile}*`, '**/{node_modules,target,.git}/**', 5);
        if (!files.length) {
            vscode.window.showInformationMessage(`Prompt profile not found in workspace: ${manifest.llm.promptProfile}`);
            return;
        }
        await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(files[0]));
    }

    async compareLlmOutputAgainstGoldenFixture(): Promise<void> {
        this.snapshot = { ...this.snapshot, llmSmoke: 'golden comparison unsupported by backend' };
        vscode.window.showInformationMessage('Golden fixture comparison is not exposed by the backend yet.');
        this.refresh();
    }

    private async runBackendCommand(kind: 'start' | 'stop' | 'restart' | 'reloadConfig'): Promise<void> {
        const manifest = await this.manifest();
        if (!manifest) return;
        if (kind !== 'reloadConfig' && !canControlServer(manifest)) {
            vscode.window.showWarningMessage('Backend process control is available only for local/dev workspaces with allowLocalProcessControl enabled.');
            return;
        }
        const command = manifest.backend.commands?.[kind] ?? defaultCommand(kind, manifest);
        if (!command) {
            vscode.window.showInformationMessage(`No ${kind} command configured in the Renovatio workspace manifest.`);
            return;
        }
        const confirmed = await vscode.window.showWarningMessage(
            `Run Renovatio backend ${kind} command?\n\n${command}`,
            { modal: true },
            'Run Command'
        );
        if (confirmed !== 'Run Command') return;
        await this.runShellCommand(kind, command);
    }

    private async runShellCommand(kind: string, command: string): Promise<void> {
        const cwd = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
        this.output.show(true);
        this.output.appendLine(`[backend:${kind}] ${command}`);
        if (kind === 'start') {
            const terminal = vscode.window.createTerminal({ name: 'Renovatio Backend', cwd });
            terminal.show();
            terminal.sendText(command);
            this.output.appendLine('[backend:start] command sent to VS Code terminal');
            return;
        }
        await new Promise<void>(resolve => {
            const child = exec(command, { cwd, timeout: 120000 }, (error, stdout, stderr) => {
                if (stdout) this.output.appendLine(stdout.trimEnd());
                if (stderr) this.output.appendLine(stderr.trimEnd());
                if (error) {
                    this.output.appendLine(`[backend:${kind}] failed: ${error.message}`);
                    vscode.window.showWarningMessage(`Backend ${kind} failed: ${error.message}`);
                } else {
                    this.output.appendLine(`[backend:${kind}] completed`);
                    vscode.window.showInformationMessage(`Backend ${kind} command completed.`);
                }
                resolve();
            });
            child.stdout?.on('data', chunk => this.output.append(String(chunk)));
            child.stderr?.on('data', chunk => this.output.append(String(chunk)));
        });
    }

    private async postOptional(endpoint: string, label: string): Promise<void> {
        const manifest = await this.manifest();
        if (!manifest) return;
        const url = joinUrl(manifest.backend.url, endpoint);
        try {
            const response = await fetchWithTimeout(url, 8000, { method: 'POST' });
            if (response.status === 404 || response.status === 405) {
                vscode.window.showInformationMessage(`${label} endpoint is not available in this backend.`);
                return;
            }
            if (!response.ok) throw new Error(`HTTP ${response.status}: ${(await response.text()).slice(0, 240)}`);
            vscode.window.showInformationMessage(`${label} completed.`);
        } catch (error) {
            vscode.window.showWarningMessage(`${label} failed: ${message(error)}`);
        }
    }

    private async manifest(): Promise<RenovatioWorkspaceManifest | undefined> {
        const manifest = await this.manifestService.load();
        if (!manifest) {
            vscode.window.setStatusBarMessage('Renovatio workspace manifest not found.', 5000);
        }
        return manifest;
    }
}

class BackendTreeItem extends vscode.TreeItem {
    constructor(
        readonly id: string,
        label: string,
        collapsibleState: vscode.TreeItemCollapsibleState = vscode.TreeItemCollapsibleState.None
    ) {
        super(label, collapsibleState);
    }
}

function section(label: string, id: string): BackendTreeItem {
    const item = new BackendTreeItem(id, label, vscode.TreeItemCollapsibleState.Expanded);
    item.contextValue = 'renovatioBackendSection';
    return item;
}

function value(label: string, description?: string, tooltip?: string): BackendTreeItem {
    const item = new BackendTreeItem(label, label);
    item.description = description;
    item.tooltip = tooltip ?? `${label}: ${description ?? ''}`;
    return item;
}

function action(label: string, command: string, description?: string): BackendTreeItem {
    const item = new BackendTreeItem(command, label);
    item.description = description;
    item.command = { command, title: label };
    item.contextValue = 'renovatioBackendAction';
    return item;
}

function healthLabel(snapshot: BackendSnapshot): string {
    return snapshot.status === 'healthy' ? 'healthy' : snapshot.status === 'unhealthy' ? 'unhealthy' : 'unknown';
}

function controlState(manifest: RenovatioWorkspaceManifest): string {
    return canControlServer(manifest) ? 'enabled' : 'disabled by environment/safety';
}

function canControlServer(manifest: RenovatioWorkspaceManifest): boolean {
    return ['local', 'dev'].includes(manifest.backend.environment)
        && manifest.backend.allowLocalProcessControl === true;
}

function defaultCommand(kind: 'start' | 'stop' | 'restart' | 'reloadConfig', manifest: RenovatioWorkspaceManifest): string | undefined {
    if (kind === 'start') {
        const port = portFromUrl(manifest.backend.url);
        const portArgument = port && port !== '8080' ? ` -Dspring-boot.run.arguments=--server.port=${port}` : '';
        return `./mvnw -pl renovatio-api spring-boot:run${portArgument}`;
    }
    if (kind === 'reloadConfig') return `curl -fsS -X POST ${joinUrl(manifest.backend.url, '/api/admin/reload')}`;
    return undefined;
}

function joinUrl(base: string, endpoint: string): string {
    const cleanBase = base.replace(/\/$/, '');
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    return `${cleanBase}${cleanEndpoint}`;
}

async function fetchWithTimeout(url: string, timeoutMs: number, init: RequestInit = {}): Promise<Response> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
        return await fetch(url, { ...init, signal: controller.signal });
    } finally {
        clearTimeout(timeout);
    }
}

function parseMaybeJson(text: string): unknown {
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

function healthStatus(value: unknown): BackendStatus {
    if (typeof value === 'string') {
        const normalized = value.trim().toUpperCase();
        if (['UP', 'OK', 'HEALTHY', 'READY'].includes(normalized)) return 'healthy';
        if (['DOWN', 'OUT_OF_SERVICE', 'UNHEALTHY', 'FAILED', 'ERROR'].includes(normalized)) return 'unhealthy';
        return 'unknown';
    }
    if (!value || typeof value !== 'object') return 'unknown';
    const status = String((value as { status?: unknown }).status ?? '').toUpperCase();
    return status === 'UP' || status === 'OK' || status === 'HEALTHY' ? 'healthy' : 'unhealthy';
}

function portFromUrl(value: string): string | undefined {
    try {
        const parsed = new URL(value);
        if (parsed.port) return parsed.port;
        return parsed.protocol === 'https:' ? '443' : parsed.protocol === 'http:' ? '80' : undefined;
    } catch {
        return undefined;
    }
}

function versionFrom(value: unknown): string | undefined {
    if (!value || typeof value !== 'object') return undefined;
    const record = value as Record<string, unknown>;
    const direct = record.version ?? record.buildVersion;
    if (typeof direct === 'string') return direct;
    const components = record.components;
    if (components && typeof components === 'object') {
        const info = (components as Record<string, unknown>).info;
        if (info && typeof info === 'object') {
            const details = (info as Record<string, unknown>).details;
            if (details && typeof details === 'object' && typeof (details as Record<string, unknown>).version === 'string') {
                return (details as Record<string, string>).version;
            }
        }
    }
    return undefined;
}

function message(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}
