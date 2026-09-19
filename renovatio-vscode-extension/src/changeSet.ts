import * as vscode from 'vscode';

export type ChangeKind = 'create' | 'modify' | 'delete';
export type ChangeStatus = 'pending' | 'approved' | 'rejected' | 'applied' | 'conflict' | 'skipped';

export interface RenovatioChangeSet {
    version: '1';
    id: string;
    createdAt: string;
    sourceHash?: string;
    targetLanguage: string;
    backend: {
        url: string;
        llmModel: string;
        promptProfile: string;
        mode: 'backend' | 'local-preview';
    };
    changes: RenovatioChange[];
}

export interface RenovatioChange {
    id: string;
    path: string;
    kind: ChangeKind;
    status: ChangeStatus;
    migrationEntryIds: string[];
    beforeHash: string | null;
    afterHash: string | null;
    beforePath?: string;
    afterPath?: string;
    diffPath: string;
    message?: string;
}

export const CHANGESET_ROOT = '.renovatio/changesets';

export function formatChangeSet(value: RenovatioChangeSet): string {
    return `${JSON.stringify(value, null, 2)}\n`;
}

export function changeSetDirectory(id: string): string {
    return `${CHANGESET_ROOT}/${id}`;
}

export function changeSetArtifactPath(id: string): string {
    return `${changeSetDirectory(id)}/changeset.renovatio-changeset.json`;
}

export async function readLatestChangeSet(folder: vscode.WorkspaceFolder): Promise<{ uri: vscode.Uri; artifact: RenovatioChangeSet } | undefined> {
    const files = await vscode.workspace.findFiles(
        new vscode.RelativePattern(folder, `${CHANGESET_ROOT}/**/changeset.renovatio-changeset.json`),
        '**/{node_modules,target,.git}/**'
    );
    if (!files.length) return undefined;
    const parsed = await Promise.all(files.map(async uri => {
        const artifact = JSON.parse(decodeBytes(await vscode.workspace.fs.readFile(uri))) as RenovatioChangeSet;
        return { uri, artifact };
    }));
    return parsed.sort((left, right) => right.artifact.createdAt.localeCompare(left.artifact.createdAt))[0];
}

export async function writeChangeSet(
    folder: vscode.WorkspaceFolder,
    artifact: RenovatioChangeSet
): Promise<vscode.Uri> {
    const uri = workspaceUri(folder, changeSetArtifactPath(artifact.id));
    await vscode.workspace.fs.createDirectory(parentUri(uri));
    await vscode.workspace.fs.writeFile(uri, encodeText(formatChangeSet(artifact)));
    return uri;
}

export function workspaceUri(folder: vscode.WorkspaceFolder, relativePath: string): vscode.Uri {
    return vscode.Uri.joinPath(folder.uri, ...relativePath.split('/').filter(Boolean));
}

export function relativePath(folder: vscode.WorkspaceFolder, uri: vscode.Uri): string {
    const root = normalizePath(folder.uri.fsPath);
    const file = normalizePath(uri.fsPath);
    return file.startsWith(`${root}/`) ? file.slice(root.length + 1) : file;
}

export async function exists(uri: vscode.Uri): Promise<boolean> {
    try {
        await vscode.workspace.fs.stat(uri);
        return true;
    } catch {
        return false;
    }
}

export async function sha256Text(value: string): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', encodeText(value));
    return `sha256:${[...new Uint8Array(digest)].map(byte => byte.toString(16).padStart(2, '0')).join('')}`;
}

export async function sha256File(uri: vscode.Uri): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', await vscode.workspace.fs.readFile(uri));
    return `sha256:${[...new Uint8Array(digest)].map(byte => byte.toString(16).padStart(2, '0')).join('')}`;
}

export function encodeText(value: string): Uint8Array {
    return new TextEncoder().encode(value);
}

export function decodeBytes(value: Uint8Array): string {
    return new TextDecoder('utf-8').decode(value);
}

function parentUri(uri: vscode.Uri): vscode.Uri {
    const parts = uri.path.split('/');
    parts.pop();
    return uri.with({ path: parts.join('/') || '/' });
}

function normalizePath(value: string): string {
    return value.replace(/\\/g, '/').replace(/\/+$/, '');
}
