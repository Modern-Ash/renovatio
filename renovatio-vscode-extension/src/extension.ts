import * as vscode from 'vscode';
import {
    applyDiagramEvent,
    formatDiagramDocument,
    parseDiagramDocument,
    type ParsedDiagramDocument
} from './model';
import * as legacyExtension from './legacyExtension';
import { RenovatioWorkspaceManifestService } from './workspaceManifest';

const DOMAIN_VIEW_TYPE = 'renovatio.diagram.domain';
const ARCHITECTURE_VIEW_TYPE = 'renovatio.diagram.architecture';

export function activate(context: vscode.ExtensionContext): void {
    const output = vscode.window.createOutputChannel('Renovatio Diagrams');
    legacyExtension.activate(context);

    const provider = new RenovatioDiagramEditorProvider(context, output);
    const tree = new RenovatioWelcomeTree(context);
    const manifestService = new RenovatioWorkspaceManifestService(output);
    context.subscriptions.push(
        output,
        manifestService,
        vscode.window.registerCustomEditorProvider(DOMAIN_VIEW_TYPE, provider, { webviewOptions: { retainContextWhenHidden: true } }),
        vscode.window.registerCustomEditorProvider(ARCHITECTURE_VIEW_TYPE, provider, { webviewOptions: { retainContextWhenHidden: true } }),
        vscode.window.registerTreeDataProvider('renovatio.views.welcome', tree),
        vscode.commands.registerCommand('renovatio.initializeWorkspace', () => manifestService.initializeWorkspace()),
        vscode.commands.registerCommand('renovatio.openWorkspaceManifest', () => manifestService.openWorkspaceManifest()),
        vscode.commands.registerCommand('renovatio.validateWorkspace', () => manifestService.validateWorkspace()),
        vscode.commands.registerCommand('renovatio.formatArtifacts', () => manifestService.formatArtifacts()),
        vscode.commands.registerCommand('renovatio.openDomainSample', () => openSample(context, 'sample.renovatio-domain.json')),
        vscode.commands.registerCommand('renovatio.openArchitectureSample', () => openSample(context, 'sample.renovatio-arch.json'))
    );
    void manifestService.validateWorkspace();
}

export function deactivate(): void {
    // No background resources are kept alive.
}

class RenovatioDiagramEditorProvider implements vscode.CustomTextEditorProvider {
    private updateQueue: Promise<void> = Promise.resolve();
    private readonly mutatingDocuments = new Map<string, number>();

    constructor(
        private readonly context: vscode.ExtensionContext,
        private readonly output: vscode.OutputChannel
    ) {}

    async resolveCustomTextEditor(
        document: vscode.TextDocument,
        webviewPanel: vscode.WebviewPanel,
        token: vscode.CancellationToken
    ): Promise<void> {
        webviewPanel.webview.options = {
            enableScripts: true,
            localResourceRoots: [vscode.Uri.joinPath(this.context.extensionUri, 'dist')]
        };
        webviewPanel.webview.html = this.htmlFor(webviewPanel.webview);

        const postModel = (): void => {
            if (token.isCancellationRequested) {
                return;
            }
            const parsed = this.parseForWebview(document);
            this.output.appendLine(`Rendering ${parsed.kind} diagram: ${document.uri.fsPath} (${parsed.model.nodes.length} nodes, ${parsed.model.edges.length} edges)`);
            webviewPanel.webview.postMessage({
                type: 'setModel',
                documentKind: parsed.kind,
                hasSavedLayout: hasSavedLayout(parsed),
                model: parsed.model
            });
        };

        const changeSubscription = vscode.workspace.onDidChangeTextDocument(event => {
            if (event.document.uri.toString() === document.uri.toString()) {
                if (this.isMutatingDocument(document.uri)) {
                    return;
                }
                postModel();
            }
        });
        webviewPanel.onDidDispose(() => changeSubscription.dispose());

        webviewPanel.webview.onDidReceiveMessage(async message => {
            if (!message) {
                return;
            }
            if (message.type === 'ready') {
                postModel();
                return;
            }
            if (message.type === 'error') {
                const detail = String(message.message ?? 'Unknown webview error');
                this.output.appendLine(`Webview error in ${document.uri.fsPath}: ${detail}`);
                vscode.window.showErrorMessage(`Renovatio diagram webview error: ${detail}`);
                return;
            }
            if (message.type !== 'diagramEvent') {
                return;
            }
            await this.enqueueDiagramUpdate(document, message.event, postModel);
        });

        postModel();
    }

    private parseForWebview(document: vscode.TextDocument): ParsedDiagramDocument {
        try {
            return parseDiagramDocument(document.getText(), document.fileName);
        } catch (error) {
            const detail = error instanceof Error ? error.message : String(error);
            return {
                kind: 'diagram',
                raw: {},
                model: {
                    nodes: [{
                        id: 'parse-error',
                        kind: 'ERROR',
                        label: 'Invalid Renovatio JSON',
                        x: 80,
                        y: 80,
                        data: { properties: [{ name: 'error', type: detail, required: true }] }
                    }],
                    edges: []
                }
            };
        }
    }

    private async enqueueDiagramUpdate(document: vscode.TextDocument, event: unknown, postModel: () => void): Promise<void> {
        if (!isDocumentMutationEvent(event)) {
            return;
        }
        this.updateQueue = this.updateQueue
            .catch(() => undefined)
            .then(async () => {
                try {
                    const parsed = parseDiagramDocument(document.getText(), document.fileName);
                    const next = applyDiagramEvent(parsed, event as never);
                    const layoutOnly = isLayoutOnlyEvent(event);
                    this.beginDocumentMutation(document.uri);
                    try {
                        await this.replaceDocument(document, formatDiagramDocument(next.raw));
                    } finally {
                        this.endDocumentMutation(document.uri);
                    }
                    if (!layoutOnly) {
                        postModel();
                    }
                } catch (error) {
                    const detail = error instanceof Error ? error.message : String(error);
                    vscode.window.showErrorMessage(`Unable to update Renovatio diagram: ${detail}`);
                }
            });
        await this.updateQueue;
    }

    private beginDocumentMutation(uri: vscode.Uri): void {
        const key = uri.toString();
        this.mutatingDocuments.set(key, (this.mutatingDocuments.get(key) ?? 0) + 1);
    }

    private endDocumentMutation(uri: vscode.Uri): void {
        const key = uri.toString();
        const count = this.mutatingDocuments.get(key) ?? 0;
        if (count <= 0) {
            return;
        }
        if (count === 1) {
            this.mutatingDocuments.delete(key);
        } else {
            this.mutatingDocuments.set(key, count - 1);
        }
    }

    private isMutatingDocument(uri: vscode.Uri): boolean {
        return (this.mutatingDocuments.get(uri.toString()) ?? 0) > 0;
    }

    private async replaceDocument(document: vscode.TextDocument, text: string): Promise<void> {
        if (document.getText() === text) {
            return;
        }
        const edit = new vscode.WorkspaceEdit();
        const start = new vscode.Position(0, 0);
        const end = document.lineCount === 0
            ? start
            : document.lineAt(document.lineCount - 1).rangeIncludingLineBreak.end;
        edit.replace(document.uri, new vscode.Range(start, end), text);
        const applied = await vscode.workspace.applyEdit(edit);
        if (!applied) {
            throw new Error('VS Code rejected the diagram document edit.');
        }
        await document.save();
    }

    private htmlFor(webview: vscode.Webview): string {
        const nonce = nonceValue();
        const scriptUri = webview.asWebviewUri(vscode.Uri.joinPath(this.context.extensionUri, 'dist', 'webview.js'));
        const styleUri = webview.asWebviewUri(vscode.Uri.joinPath(this.context.extensionUri, 'dist', 'webview.css'));
        const csp = [
            `default-src 'none'`,
            `img-src ${webview.cspSource} data:`,
            `style-src ${webview.cspSource} 'unsafe-inline'`,
            `script-src 'nonce-${nonce}'`
        ].join('; ');
        return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta http-equiv="Content-Security-Policy" content="${csp}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link rel="stylesheet" href="${styleUri}">
  <title>Renovatio Diagram</title>
</head>
<body>
  <div id="root"></div>
  <script nonce="${nonce}" src="${scriptUri}"></script>
</body>
</html>`;
    }
}

function hasSavedLayout(parsed: ParsedDiagramDocument): boolean {
    const layout = parsed.kind === 'architecture'
        ? parsed.raw.profile?.layout ?? parsed.raw.layout
        : parsed.raw.layout;
    return Boolean(layout && typeof layout === 'object' && Object.keys(layout).length > 0);
}

function isLayoutOnlyEvent(event: unknown): boolean {
    return Boolean(event && typeof event === 'object' && (event as { type?: unknown }).type === 'layoutChanged');
}

function isDocumentMutationEvent(event: unknown): boolean {
    if (!event || typeof event !== 'object') {
        return false;
    }
    const type = (event as { type?: unknown }).type;
    return type === 'nodeMoved'
        || type === 'layoutChanged'
        || type === 'nodesPruned'
        || type === 'edgeCreated'
        || type === 'edgeReconnected'
        || type === 'edgeLabelChanged'
        || type === 'edgesDeleted'
        || type === 'architectureStyleChanged';
}

class RenovatioWelcomeTree implements vscode.TreeDataProvider<RenovatioTreeItem> {
    constructor(private readonly context: vscode.ExtensionContext) {}

    getTreeItem(element: RenovatioTreeItem): vscode.TreeItem {
        return element;
    }

    getChildren(): RenovatioTreeItem[] {
        return [
            new RenovatioTreeItem('Initialize Renovatio Workspace', 'renovatio.initializeWorkspace', '.renovatio/workspace.renovatio.json'),
            new RenovatioTreeItem('Open Workspace Manifest', 'renovatio.openWorkspaceManifest', 'local contract'),
            new RenovatioTreeItem('Validate Renovatio Workspace', 'renovatio.validateWorkspace', 'schemas and manifest'),
            new RenovatioTreeItem('Format Renovatio Artifacts', 'renovatio.formatArtifacts', 'JSON/JSONC'),
            new RenovatioTreeItem('Open Native Domain Diagram', 'renovatio.openNativeDomainDiagram', 'current DomainModel'),
            new RenovatioTreeItem('Open Native Persistence Diagram', 'renovatio.openNativePersistenceDiagram', 'repositories and records'),
            new RenovatioTreeItem('Open Native Architecture Diagram', 'renovatio.openNativeArchitectureDiagram', 'target layers'),
            new RenovatioTreeItem('Open Domain sample', 'renovatio.openDomainSample', 'renovatio-domain.json'),
            new RenovatioTreeItem('Open Architecture sample', 'renovatio.openArchitectureSample', 'renovatio-arch.json'),
            new RenovatioTreeItem('Custom editors are active for *.renovatio-domain.json and *.renovatio-arch.json')
        ];
    }
}

class RenovatioTreeItem extends vscode.TreeItem {
    constructor(label: string, commandId?: string, description?: string) {
        super(label, vscode.TreeItemCollapsibleState.None);
        this.description = description;
        this.tooltip = label;
        if (commandId) {
            this.command = { command: commandId, title: label };
            this.contextValue = 'renovatioAction';
        }
    }
}

async function openSample(context: vscode.ExtensionContext, fileName: string): Promise<void> {
    const uri = vscode.Uri.joinPath(context.extensionUri, 'examples', fileName);
    const document = await vscode.workspace.openTextDocument(uri);
    await vscode.window.showTextDocument(document, { preview: false });
    await vscode.commands.executeCommand('vscode.openWith', uri,
        fileName.endsWith('arch.json') ? ARCHITECTURE_VIEW_TYPE : DOMAIN_VIEW_TYPE);
}

function nonceValue(): string {
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let value = '';
    for (let i = 0; i < 32; i += 1) {
        value += alphabet.charAt(Math.floor(Math.random() * alphabet.length));
    }
    return value;
}
