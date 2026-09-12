import * as vscode from 'vscode';
import {
    applyDiagramEvent,
    formatDiagramDocument,
    parseDiagramDocument,
    type ParsedDiagramDocument
} from './model';
import * as legacyExtension from './legacyExtension';

const DOMAIN_VIEW_TYPE = 'renovatio.diagram.domain';
const ARCHITECTURE_VIEW_TYPE = 'renovatio.diagram.architecture';

export function activate(context: vscode.ExtensionContext): void {
    const output = vscode.window.createOutputChannel('Renovatio Diagrams');
    legacyExtension.activate(context);

    const provider = new RenovatioDiagramEditorProvider(context, output);
    const tree = new RenovatioWelcomeTree(context);
    context.subscriptions.push(
        output,
        vscode.window.registerCustomEditorProvider(DOMAIN_VIEW_TYPE, provider, { webviewOptions: { retainContextWhenHidden: true } }),
        vscode.window.registerCustomEditorProvider(ARCHITECTURE_VIEW_TYPE, provider, { webviewOptions: { retainContextWhenHidden: true } }),
        vscode.window.registerTreeDataProvider('renovatio.views.welcome', tree),
        vscode.commands.registerCommand('renovatio.openDomainSample', () => openSample(context, 'sample.renovatio-domain.json')),
        vscode.commands.registerCommand('renovatio.openArchitectureSample', () => openSample(context, 'sample.renovatio-arch.json'))
    );
}

export function deactivate(): void {
    // No background resources are kept alive.
}

class RenovatioDiagramEditorProvider implements vscode.CustomTextEditorProvider {
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
                model: parsed.model
            });
        };

        const changeSubscription = vscode.workspace.onDidChangeTextDocument(event => {
            if (event.document.uri.toString() === document.uri.toString()) {
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
            try {
                const parsed = parseDiagramDocument(document.getText(), document.fileName);
                const next = applyDiagramEvent(parsed, message.event);
                await this.replaceDocument(document, formatDiagramDocument(next.raw));
            } catch (error) {
                const detail = error instanceof Error ? error.message : String(error);
                vscode.window.showErrorMessage(`Unable to update Renovatio diagram: ${detail}`);
            }
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

    private async replaceDocument(document: vscode.TextDocument, text: string): Promise<void> {
        const edit = new vscode.WorkspaceEdit();
        const start = new vscode.Position(0, 0);
        const end = document.lineCount === 0
            ? start
            : document.lineAt(document.lineCount - 1).rangeIncludingLineBreak.end;
        edit.replace(document.uri, new vscode.Range(start, end), text);
        await vscode.workspace.applyEdit(edit);
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

class RenovatioWelcomeTree implements vscode.TreeDataProvider<RenovatioTreeItem> {
    constructor(private readonly context: vscode.ExtensionContext) {}

    getTreeItem(element: RenovatioTreeItem): vscode.TreeItem {
        return element;
    }

    getChildren(): RenovatioTreeItem[] {
        return [
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
