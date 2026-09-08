const projectId = 'payroll-modernization';
const sourceHash = `sha256:${'a'.repeat(64)}`;
const domainHash = `sha256:${'b'.repeat(64)}`;
const architectureHash = `sha256:${'c'.repeat(64)}`;
const shadowHash = `sha256:${'d'.repeat(64)}`;
const manifestHash = `sha256:${'e'.repeat(64)}`;

export const cobolSource = `IDENTIFICATION DIVISION.
PROGRAM-ID. PAYROLL.
DATA DIVISION.
WORKING-STORAGE SECTION.
01 EMPLOYEE-RECORD.
   05 EMPLOYEE-ID PIC X(10).
   05 GROSS-PAY PIC 9(7)V99.
   05 TAX-RATE PIC 9V999.
PROCEDURE DIVISION.
CALCULATE-PAYROLL.
    COMPUTE NET-PAY = GROSS-PAY - (GROSS-PAY * TAX-RATE).
    STOP RUN.
`;

const projects = [{ id: projectId, name: 'PAYROLL' }];

const assets = [
    { id: 'src/PAYROLL.CBL', name: 'PAYROLL.CBL', category: 'COBOL sources', writable: false },
    { id: 'models/payroll-domain.json', name: 'payroll-domain.json', category: 'Models', writable: false },
    { id: 'target/PayrollService.java', name: 'PayrollService.java', category: 'Generated targets', writable: true }
];

const sourceExplorer = {
    files: [
        {
            id: 'src/PAYROLL.CBL',
            name: 'PAYROLL.CBL',
            kind: 'COBOL',
            path: 'src/PAYROLL.CBL',
            hash: sourceHash,
            encoding: 'UTF-8',
            analysisStatus: 'parsed',
            symbols: [
                { id: 'EMPLOYEE-RECORD', kind: 'record', name: 'EMPLOYEE-RECORD', line: 5, column: 1, parentId: null, irCoordinate: 'ir://payroll/records/employee' },
                { id: 'CALCULATE-PAYROLL', kind: 'paragraph', name: 'CALCULATE-PAYROLL', line: 10, column: 1, parentId: null, irCoordinate: 'ir://payroll/procedure/calculate-payroll' }
            ],
            diagnostics: []
        }
    ],
    datasets: [{ id: 'PAYROLL-MASTER', name: 'PAYROLL-MASTER', referencedBy: ['src/PAYROLL.CBL'] }]
};

const domainModel = {
    schemaVersion: '1',
    projectId,
    nodes: [
        {
            id: 'employee-payroll',
            kind: 'AGGREGATE',
            name: 'Employee Payroll',
            properties: [
                {
                    name: 'grossPay',
                    type: 'Money',
                    required: true,
                    evidence: [{ sourceRef: 'src/PAYROLL.CBL#GROSS-PAY', provenance: sourceHash, rationale: 'COBOL working-storage amount' }]
                }
            ],
            evidence: [{ sourceRef: 'src/PAYROLL.CBL#EMPLOYEE-RECORD', provenance: sourceHash, rationale: 'Employee payroll record extracted from COBOL source' }],
            origin: 'AI_REVIEWED',
            confidence: 0.92
        }
    ],
    relations: [],
    invariants: [
        {
            id: 'net-pay-calculation',
            subjectId: 'employee-payroll',
            expression: 'Net pay equals gross pay minus tax withholding.',
            evidence: [{ sourceRef: 'src/PAYROLL.CBL#CALCULATE-PAYROLL', provenance: sourceHash, rationale: 'COMPUTE statement preserves calculation rule' }],
            origin: 'AI_REVIEWED',
            confidence: 0.88
        }
    ]
};

const domainView = {
    revision: 2,
    canonicalHash: domainHash,
    savedAt: '2026-09-08T00:00:00Z',
    model: domainModel,
    diagnostics: [],
    suggestions: [
        { id: 'suggest-payroll-aggregate', targetType: 'node', targetId: 'employee-payroll', name: 'Confirm payroll aggregate boundary', status: 'accepted', decidedRevision: 2, decidedAt: '2026-09-08T00:01:00Z' }
    ]
};

function architectureView(profilePatch = {}, revision = 3) {
    const profile = {
        style: 'HEXAGONAL',
        moduleGrouping: 'BY_DOMAIN',
        framework: 'SPRING_BOOT',
        persistence: 'JPA',
        packageRoots: {
            controller: 'com.renovatio.payroll.adapter.in',
            service: 'com.renovatio.payroll.application',
            model: 'com.renovatio.payroll.domain'
        },
        suffixes: { controller: 'Controller', service: 'Service', model: 'Aggregate' },
        classNames: { service: 'PayrollService' },
        dependencyRules: [
            { fromLayer: 'controller', toLayer: 'service', allowed: true, reason: 'Inbound adapter may call application service.' },
            { fromLayer: 'model', toLayer: 'controller', allowed: false, reason: 'Domain cannot depend on adapters.' }
        ],
        ...profilePatch
    };
    return {
        revision,
        canonicalHash: architectureHash,
        savedAt: '2026-09-08T00:02:00Z',
        profile,
        preview: { modules: [], components: [], relations: [], diagnostics: [], hasFallback: false },
        canvas: [
            { id: 'payroll-controller', layer: 'controller', kind: 'adapter', label: 'Payroll API', packageName: profile.packageRoots.controller, className: 'PayrollController', componentId: 'employee-payroll' },
            { id: 'payroll-service', layer: 'service', kind: 'application', label: 'Payroll Service', packageName: profile.packageRoots.service, className: 'PayrollService', componentId: 'employee-payroll' },
            { id: 'employee-payroll', layer: 'model', kind: 'aggregate', label: 'Employee Payroll', packageName: profile.packageRoots.model, className: 'EmployeePayrollAggregate', componentId: 'employee-payroll' }
        ],
        dependencyRules: profile.dependencyRules,
        dependencyDiagnostics: [],
        manifest: [
            { path: 'target/java/com/renovatio/payroll/application/PayrollService.java', role: 'generated target', layer: 'service', className: 'PayrollService', packageName: profile.packageRoots.service, componentId: 'employee-payroll' },
            { path: 'target/java/com/renovatio/payroll/domain/EmployeePayrollAggregate.java', role: 'generated target', layer: 'model', className: 'EmployeePayrollAggregate', packageName: profile.packageRoots.model, componentId: 'employee-payroll' }
        ]
    };
}

const shadowImpact = {
    schemaVersion: '1',
    canonicalHash: shadowHash,
    source: { name: 'COBOL source scan', revision: 1, hash: sourceHash, itemCount: 2, status: 'ready' },
    domain: { name: 'DomainModel', revision: 2, hash: domainHash, itemCount: 2, status: 'reviewed' },
    architecture: { name: 'Architecture profile', revision: 3, hash: architectureHash, itemCount: 3, status: 'reviewed' },
    sourceImpacts: [{ sourcePath: 'src/PAYROLL.CBL', kind: 'COBOL', symbolCount: 2, domainElementIds: ['employee-payroll'], artifactPaths: ['target/java/com/renovatio/payroll/application/PayrollService.java'] }],
    artifactImpacts: [{ path: 'target/java/com/renovatio/payroll/application/PayrollService.java', role: 'generated target', layer: 'service', componentId: 'employee-payroll', status: 'planned', determinism: 'deterministic', sourceRefs: ['src/PAYROLL.CBL#CALCULATE-PAYROLL'], domainElementIds: ['employee-payroll'], evidenceRefs: ['src/PAYROLL.CBL#CALCULATE-PAYROLL'] }],
    diff: {
        plannedArtifacts: ['target/java/com/renovatio/payroll/application/PayrollService.java'],
        existingTargets: [],
        added: ['target/java/com/renovatio/payroll/application/PayrollService.java'],
        removed: [],
        changed: [],
        unresolvedEvidence: []
    },
    report: { projectId, sourceHash, domainHash, architectureHash, plannedArtifacts: ['target/java/com/renovatio/payroll/application/PayrollService.java'] }
};

function changeSet(state = 'review', approvedManifestHash = null) {
    return {
        id: 'cs-payroll-java',
        projectId,
        title: 'Generate reviewed PayrollService target',
        state,
        dangerous: false,
        manifestHash,
        approvedManifestHash,
        files: [{ path: 'target/java/com/renovatio/payroll/application/PayrollService.java', action: 'CREATE', beforeHash: 'sha256:0000', afterHash: manifestHash, proposedContent: 'public final class PayrollService {}' }],
        decisions: ['domain:employee-payroll', 'architecture:HEXAGONAL'],
        evidence: ['shadow-impact:manifest', 'src/PAYROLL.CBL#CALCULATE-PAYROLL'],
        diff: {
            summary: 'Create PayrollService from reviewed DomainModel and Architecture profile.',
            requiredBeforeApproval: ['DomainModel revision 2 saved', 'Architecture profile revision 3 saved', 'Shadow impact reviewed'],
            files: [{ path: 'target/java/com/renovatio/payroll/application/PayrollService.java', action: 'CREATE', beforeHash: 'sha256:0000', afterHash: manifestHash, preview: '+ public final class PayrollService' }]
        },
        history: [{ actor: 'architect', action: state, at: '2026-09-08T00:03:00Z', reason: 'Reviewed in Theia workbench', manifestHash }]
    };
}

const equivalence = {
    evidence: [{ id: 'shadow-impact:manifest', name: 'Reviewed shadow manifest' }],
    generatedTargets: [{ id: 'target/java/com/renovatio/payroll/application/PayrollService.java', name: 'PayrollService.java' }],
    verdicts: [{ fixtureId: 'payroll-basic-case', classification: 'equivalent', reason: 'State output and SQL side effects match baseline.', blocksRelease: false }],
    fixtures: [
        {
            id: 'payroll-basic-case',
            name: 'Basic payroll calculation',
            sourceId: 'src/PAYROLL.CBL',
            baselineId: 'cobol-baseline',
            candidateId: 'java-candidate',
            inputs: { fields: { GROSS_PAY: '1000.00', TAX_RATE: '0.210' }, sequentialFiles: [{ ddName: 'PAYIN', contentHash: sourceHash, preview: 'EMP001' }], db2Responses: [{ statementId: 'select-tax', sqlState: '00000', rows: [{ rate: '0.210' }] }] },
            evidenceRefs: ['src/PAYROLL.CBL#CALCULATE-PAYROLL'],
            reproducibilityHash: `sha256:${'f'.repeat(64)}`
        }
    ],
    runs: [
        {
            id: 'run-payroll-basic',
            fixtureId: 'payroll-basic-case',
            state: 'completed',
            progress: 100,
            startedAt: '2026-09-08T00:04:00Z',
            finishedAt: '2026-09-08T00:05:00Z',
            baselineHash: `sha256:${'1'.repeat(64)}`,
            candidateHash: `sha256:${'1'.repeat(64)}`,
            commit: '5835fdeb',
            profileHash: architectureHash,
            changeSetId: 'cs-payroll-java',
            logs: ['COBOL baseline executed', 'Java candidate executed', 'Outputs matched'],
            comparison: { state: 'matched', outputsMatch: true, filesMatch: true, sqlMatches: true, comparedArtifacts: ['PAYOUT'] },
            divergences: [],
            reportHash: `sha256:${'2'.repeat(64)}`
        }
    ],
    gate: { promotable: true, status: 'ready', blockers: [], readinessReason: 'Completed equivalent run is available.' },
    history: [{ actor: 'qa', action: 'completed', at: '2026-09-08T00:05:00Z', targetId: 'run-payroll-basic', reason: 'E2E fixture verified', hash: `sha256:${'2'.repeat(64)}` }]
};

export async function mockCobolModernizationFlow(page) {
    let currentChangeSetState = 'review';
    let currentApprovedManifestHash = null;
    const corsHeaders = {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET,PUT,POST,OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type'
    };

    await page.route('**/api/**', async route => {
        const request = route.request();
        const url = new URL(request.url());
        const pathname = url.pathname;
        const method = request.method();
        const json = data => route.fulfill({
            status: 200,
            contentType: 'application/json',
            headers: corsHeaders,
            body: JSON.stringify(data)
        });

        if (method === 'OPTIONS') return route.fulfill({ status: 204, headers: corsHeaders });
        if (method === 'GET' && pathname === '/api/workbench/projects') return json(projects);
        if (method === 'GET' && pathname.endsWith('/workbench/assets')) return json(assets);
        if (method === 'GET' && pathname.endsWith('/workbench/context')) return json({ activeArea: 'project', selectedAssetId: 'src/PAYROLL.CBL' });
        if (method === 'PUT' && pathname.endsWith('/workbench/context')) return json({ activeArea: 'project', selectedAssetId: 'src/PAYROLL.CBL' });
        if (method === 'GET' && pathname.endsWith('/workbench/assets/src/PAYROLL.CBL')) return route.fulfill({ status: 200, contentType: 'text/plain', headers: corsHeaders, body: cobolSource });
        if (method === 'GET' && pathname.endsWith('/workbench/source-explorer')) return json(sourceExplorer);
        if (method === 'GET' && pathname.endsWith('/workbench/analysis')) return json({ inventory: { cobol: 1, copybooks: 0, jcl: 0, domainElements: 2 }, runs: [{ runId: 'analysis-2026-09-08', dryRun: true, startedAt: '2026-09-08T00:00:00Z' }] });
        if (method === 'GET' && pathname.endsWith('/workbench/domain-model')) return json(domainView);
        if (method === 'GET' && pathname.endsWith('/workbench/domain-model/versions')) return json([{ revision: 1, canonicalHash: sourceHash, savedAt: '2026-09-07T23:59:00Z' }, { revision: 2, canonicalHash: domainHash, savedAt: '2026-09-08T00:00:00Z' }]);
        if (method === 'PUT' && pathname.endsWith('/workbench/domain-model')) return json({ ...domainView, revision: 3, savedAt: '2026-09-08T00:06:00Z' });
        if (method === 'GET' && pathname.endsWith('/workbench/architecture/canvas')) return json(architectureView());
        if (method === 'GET' && pathname.endsWith('/workbench/architecture/canvas/versions')) return json([{ revision: 2, canonicalHash: domainHash, savedAt: '2026-09-08T00:01:00Z', style: 'LAYERED_MVC' }, { revision: 3, canonicalHash: architectureHash, savedAt: '2026-09-08T00:02:00Z', style: 'HEXAGONAL' }]);
        if (method === 'POST' && pathname.endsWith('/workbench/architecture/canvas:preview')) return json(architectureView(await request.postDataJSON()));
        if (method === 'PUT' && pathname.endsWith('/workbench/architecture/canvas')) return json(architectureView({}, 4));
        if (method === 'GET' && pathname.endsWith('/workbench/shadow-impact')) return json(shadowImpact);
        if (method === 'GET' && pathname.endsWith('/workbench/change-sets')) return json([changeSet(currentChangeSetState, currentApprovedManifestHash)]);
        if (method === 'POST' && pathname.endsWith('/workbench/change-sets/cs-payroll-java:approve')) {
            currentChangeSetState = 'approved';
            currentApprovedManifestHash = manifestHash;
            return json(changeSet(currentChangeSetState, currentApprovedManifestHash));
        }
        if (method === 'POST' && pathname.endsWith('/workbench/change-sets/cs-payroll-java:apply')) {
            currentChangeSetState = 'applied';
            return json(changeSet(currentChangeSetState, currentApprovedManifestHash));
        }
        if (method === 'GET' && pathname.endsWith('/workbench/equivalence')) return json(equivalence);
        if (method === 'POST' && pathname.endsWith('/workbench/equivalence/runs')) return json(equivalence.runs[0]);

        return route.fulfill({ status: 404, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify({ message: `No e2e fixture for ${method} ${pathname}` }) });
    });
}
