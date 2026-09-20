import type { RenovatioWorkspaceManifest } from './workspaceManifest';
import { sha256Text } from './changeSet';

export type RenovatioSyncArtifactKey = 'domainModel' | 'persistenceModel' | 'architecture' | 'migrationMap';

export interface RemoteArtifact {
    key: RenovatioSyncArtifactKey;
    content: string;
    hash: string;
    revision: string | null;
    schemaVersion: string | null;
}

export type BackendClientErrorKind = 'remote-unavailable' | 'schema-mismatch' | 'revision-conflict' | 'unsupported';

export class BackendClientError extends Error {
    constructor(
        readonly kind: BackendClientErrorKind,
        message: string
    ) {
        super(message);
    }
}

export class RenovatioBackendArtifactClient {
    constructor(private readonly manifest: RenovatioWorkspaceManifest) {}

    async getArtifact(key: RenovatioSyncArtifactKey): Promise<RemoteArtifact> {
        const response = await this.request('GET', this.artifactPath(key));
        if (response.status === 404 || response.status === 405 || response.status === 501) {
            throw new BackendClientError('unsupported', `Backend artifact endpoint is unsupported for ${key}.`);
        }
        if (!response.ok) {
            throw new BackendClientError('remote-unavailable', `Backend returned HTTP ${response.status} for ${key}.`);
        }
        return this.parseRemoteArtifact(key, await response.text());
    }

    async putArtifact(
        key: RenovatioSyncArtifactKey,
        content: string,
        path: string,
        expectedRevision: string | null
    ): Promise<RemoteArtifact> {
        const hash = await sha256Text(content);
        const response = await this.request('PUT', this.artifactPath(key), {
            content,
            expectedRevision,
            hash,
            path
        });
        if (response.status === 409) {
            throw new BackendClientError('revision-conflict', `Backend revision changed before ${key} could be pushed.`);
        }
        if (response.status === 404 || response.status === 405 || response.status === 501) {
            throw new BackendClientError('unsupported', `Backend artifact endpoint is unsupported for ${key}.`);
        }
        if (!response.ok) {
            throw new BackendClientError('remote-unavailable', `Backend returned HTTP ${response.status} while pushing ${key}.`);
        }
        return this.parseRemoteArtifact(key, await response.text(), content, hash);
    }

    private artifactPath(key: RenovatioSyncArtifactKey): string {
        return `/api/workspaces/${encodeURIComponent(this.manifest.projectId)}/artifacts/${encodeURIComponent(key)}`;
    }

    private async request(method: 'GET' | 'PUT', path: string, body?: unknown): Promise<Response> {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 8000);
        try {
            return await fetch(joinUrl(this.manifest.backend.url, path), {
                method,
                signal: controller.signal,
                headers: {
                    'Accept': 'application/json',
                    ...(body === undefined ? {} : { 'Content-Type': 'application/json' })
                },
                body: body === undefined ? undefined : JSON.stringify(body)
            });
        } catch (error) {
            const detail = error instanceof Error ? error.message : String(error);
            throw new BackendClientError('remote-unavailable', `Backend is unavailable: ${detail}`);
        } finally {
            clearTimeout(timeout);
        }
    }

    private async parseRemoteArtifact(
        key: RenovatioSyncArtifactKey,
        text: string,
        fallbackContent?: string,
        fallbackHash?: string
    ): Promise<RemoteArtifact> {
        const trimmed = text.trim();
        if (trimmed === '' && fallbackContent !== undefined) {
            return {
                key,
                content: fallbackContent,
                hash: fallbackHash ?? await sha256Text(fallbackContent),
                revision: null,
                schemaVersion: null
            };
        }
        let parsed: unknown;
        try {
            parsed = JSON.parse(trimmed);
        } catch {
            throw new BackendClientError('schema-mismatch', `Backend response for ${key} is not JSON.`);
        }
        if (!isRecord(parsed)) {
            throw new BackendClientError('schema-mismatch', `Backend response for ${key} must be an object.`);
        }
        const rawContent = parsed.content;
        if (rawContent === undefined && fallbackContent === undefined) {
            throw new BackendClientError('schema-mismatch', `Backend response for ${key} is missing content.`);
        }
        const content = typeof rawContent === 'string'
            ? rawContent
            : rawContent === undefined
                ? fallbackContent ?? ''
                : `${JSON.stringify(rawContent, null, 2)}\n`;
        const hash = typeof parsed.hash === 'string' && parsed.hash.trim() !== ''
            ? parsed.hash
            : fallbackHash ?? await sha256Text(content);
        return {
            key,
            content,
            hash,
            revision: typeof parsed.revision === 'string' ? parsed.revision : null,
            schemaVersion: typeof parsed.schemaVersion === 'string' ? parsed.schemaVersion : null
        };
    }
}

function joinUrl(baseUrl: string, path: string): string {
    return `${baseUrl.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`;
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}
