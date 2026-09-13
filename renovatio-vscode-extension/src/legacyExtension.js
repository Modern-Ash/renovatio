const vscode = require('vscode');
const http = require('http');
const https = require('https');
const os = require('os');
const path = require('path');
const zlib = require('zlib');

const state = {
  projects: [],
  activeProjectId: undefined,
  analysis: undefined,
  latestJob: undefined,
  domainModel: undefined
};

let statusItem;
let projectsProvider;
let operationsProvider;
let analysisProvider;
let domainModelProvider;
let output;
let extensionContext;
let controlDeckPanel;
let analysisPanel;
let projectPanel;
let domainModelPanel;
let persistencePanel;

function activate(context) {
  extensionContext = context;
  output = vscode.window.createOutputChannel('Renovatio');
  statusItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 80);
  statusItem.command = 'renovatio.selectProject';
  statusItem.show();

  projectsProvider = new ProjectsProvider();
  operationsProvider = new OperationsProvider();
  analysisProvider = new AnalysisProvider();
  domainModelProvider = new DomainModelProvider();
  context.subscriptions.push(
    output,
    statusItem,
    vscode.window.registerTreeDataProvider('renovatio.projects', projectsProvider),
    vscode.window.registerTreeDataProvider('renovatio.operations', operationsProvider),
    vscode.window.registerTreeDataProvider('renovatio.analysis', analysisProvider),
    vscode.window.registerTreeDataProvider('renovatio.domainModel', domainModelProvider),
    vscode.commands.registerCommand('renovatio.refresh', refresh),
    vscode.commands.registerCommand('renovatio.selectProject', selectProject),
    vscode.commands.registerCommand('renovatio.analyzeSelectedPath', analyzeSelectedPath),
    vscode.commands.registerCommand('renovatio.analyzeCobolSources', analyzeCobolSources),
    vscode.commands.registerCommand('renovatio.analyzeWorkspace', analyzeWorkspace),
    vscode.commands.registerCommand('renovatio.addCobolSourceRoot', addCobolSourceRoot),
    vscode.commands.registerCommand('renovatio.removeCobolSourceRoot', removeCobolSourceRoot),
    vscode.commands.registerCommand('renovatio.selectGeneratedOutputFolder', selectGeneratedOutputFolder),
    vscode.commands.registerCommand('renovatio.openControlDeck', openControlDeck),
    vscode.commands.registerCommand('renovatio.openDomainModel', openDomainModel),
    vscode.commands.registerCommand('renovatio.openNativeDomainDiagram', openNativeDomainDiagram),
    vscode.commands.registerCommand('renovatio.openNativePersistenceDiagram', openNativePersistenceDiagram),
    vscode.commands.registerCommand('renovatio.openNativeArchitectureDiagram', openNativeArchitectureDiagram),
    vscode.commands.registerCommand('renovatio.openPersistenceModel', openPersistenceModel),
    vscode.commands.registerCommand('renovatio.openGeneratedCode', openGeneratedCode),
    vscode.commands.registerCommand('renovatio.openFile', openFile),
    vscode.commands.registerCommand('renovatio.openEvidence', openEvidence),
    vscode.workspace.onDidChangeConfiguration(event => {
      if (event.affectsConfiguration('renovatio')) {
        updateStatus();
        projectsProvider.refresh();
        operationsProvider.refresh();
        void refreshVisiblePanels();
      }
    }),
    vscode.workspace.onDidChangeWorkspaceFolders(async () => {
      await syncActiveProjectWithWorkspace();
      await refresh();
    })
  );

  state.activeProjectId = context.workspaceState.get('renovatio.activeProjectId');
  void refresh();
}

function deactivate() {}

function config() {
  const cfg = vscode.workspace.getConfiguration('renovatio');
  return {
    backendUrl: String(cfg.get('backendUrl') || 'http://127.0.0.1:8081').replace(/\/$/, ''),
    role: String(cfg.get('role') || 'ADMIN')
  };
}

function request(method, path, body) {
  const { backendUrl, role } = config();
  const target = new URL(`${backendUrl}${path}`);
  const transport = target.protocol === 'https:' ? https : http;
  const payload = body === undefined ? undefined : JSON.stringify(body);
  const headers = { 'X-Role': role };
  if (payload) {
    headers['Content-Type'] = 'application/json';
    headers['Content-Length'] = Buffer.byteLength(payload);
  }
  return new Promise((resolve, reject) => {
    const req = transport.request(target, { method, headers }, res => {
      let raw = '';
      res.setEncoding('utf8');
      res.on('data', chunk => { raw += chunk; });
      res.on('end', () => {
        if (!res.statusCode || res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(raw || `Renovatio API returned ${res.statusCode}`));
          return;
        }
        if (!raw) {
          resolve(undefined);
          return;
        }
        try {
          resolve(JSON.parse(raw));
        } catch {
          resolve(raw);
        }
      });
    });
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}

async function refresh() {
  try {
    state.projects = await request('GET', '/api/workbench/projects');
    await syncActiveProjectWithWorkspace();
    if (!state.activeProjectId || !state.projects.some(project => project.id === state.activeProjectId)) {
      state.activeProjectId = state.projects[0]?.id;
    }
    await refreshAnalysis();
    await refreshDomainModel();
    updateStatus();
    projectsProvider.refresh();
    operationsProvider.refresh();
    analysisProvider.refresh();
    domainModelProvider.refresh();
    if (controlDeckPanel) await openControlDeck();
    if (analysisPanel) await openAnalysisPanel();
    if (domainModelPanel) await openDomainModel();
    if (persistencePanel) await openPersistenceModel();
    if (projectPanel) {
      const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
      if (project) await openProjectPanel(project);
    }
  } catch (error) {
    showError('Could not refresh Renovatio projects', error);
  }
}

async function refreshAnalysis() {
  if (!state.activeProjectId) {
    state.analysis = undefined;
    return;
  }
  try {
    state.analysis = await request('GET', `/api/projects/${encodeURIComponent(state.activeProjectId)}/workbench/analysis`);
  } catch (error) {
    output.appendLine(`Analysis refresh failed: ${message(error)}`);
    state.analysis = undefined;
  }
}

async function refreshDomainModel() {
  if (!state.activeProjectId) {
    state.domainModel = undefined;
    return;
  }
  try {
    state.domainModel = await request('GET', `/api/projects/${encodeURIComponent(state.activeProjectId)}/workbench/domain-model`);
  } catch (error) {
    output.appendLine(`Domain model refresh failed: ${message(error)}`);
    state.domainModel = undefined;
  }
}

async function selectProject(projectId) {
  await refresh();
  if (projectId) {
    const project = state.projects.find(candidate => candidate.id === projectId);
    if (project) {
      const hydrated = await hydrateProject(project);
      await activateProject(hydrated, { openWorkspace: true });
      await refreshAnalysis();
      await refreshDomainModel();
      analysisProvider.refresh();
      domainModelProvider.refresh();
      await openProjectPanel(hydrated);
      return;
    }
  }
  const picks = state.projects.map(project => ({
    label: project.name,
    description: project.workspacePath || project.cobolScanRoot || project.id,
    project
  }));
  const selected = await vscode.window.showQuickPick(picks, { placeHolder: 'Select Renovatio project' });
  if (!selected) return;
  const hydrated = await hydrateProject(selected.project);
  await activateProject(hydrated, { openWorkspace: true });
  await refreshAnalysis();
  await refreshDomainModel();
  analysisProvider.refresh();
  domainModelProvider.refresh();
  await openProjectPanel(hydrated);
}

async function activateProject(project, options = {}) {
  state.activeProjectId = project.id;
  await extensionContext.workspaceState.update('renovatio.activeProjectId', project.id);
  updateStatus();
  projectsProvider.refresh();
  operationsProvider.refresh();
  if (options.openWorkspace) {
    await ensureProjectWorkspaceFolder(project);
  }
}

async function ensureProjectWorkspaceFolder(project) {
  if (!project.workspacePath) return;
  const uri = vscode.Uri.file(project.workspacePath);
  const current = vscode.workspace.workspaceFolders || [];
  if (current.some(folder => samePath(folder.uri.fsPath, uri.fsPath))) return;
  vscode.workspace.updateWorkspaceFolders(current.length, 0, { uri, name: project.name });
}

async function analyzeSelectedPath(resource) {
  const resourceUri = resource instanceof vscode.Uri ? resource : undefined;
  const project = await ensureActiveProject(resourceUri);
  if (!project) return;
  const scanRoot = resourceUri?.fsPath || vscode.window.activeTextEditor?.document.uri.fsPath || await selectWorkspaceFolderPath();
  if (!scanRoot) {
    vscode.window.showWarningMessage('Select a folder or file in Explorer before running Renovatio analysis.');
    return;
  }
  const workspaceRoot = workspaceFolderPathFor(vscode.Uri.file(scanRoot)) || project.workspacePath || scanRoot;
  const ok = await confirmExternalScanRoot(project, scanRoot, workspaceRoot);
  if (!ok) return;
  await runAnalysis(project, scanRoot, workspaceRoot);
}

async function analyzeCobolSources() {
  const project = await ensureActiveProject();
  if (!project) return;
  const settings = workspaceSettings(project);
  if (!settings.cobolRoots.length) {
    const action = await vscode.window.showWarningMessage('No COBOL source roots configured for this VS Code workspace.', 'Add COBOL Source Root');
    if (action) await addCobolSourceRoot();
    return;
  }
  const scanRoot = settings.cobolRoots.length === 1
    ? settings.cobolRoots[0]
    : await pickCobolSourceRoot(settings.cobolRoots);
  if (!scanRoot) return;
  await runAnalysis(project, scanRoot, settings.workspaceFolderPath || project.workspacePath || scanRoot);
}

async function analyzeWorkspace() {
  const project = await ensureActiveProject();
  if (!project) return;
  const settings = workspaceSettings(project);
  if (settings.configuredCobolRoots.length) {
    await analyzeCobolSources();
    return;
  }
  const workspacePath = await selectWorkspaceFolderPath();
  if (!workspacePath) {
    vscode.window.showWarningMessage('Open or select a Renovatio workspace before analyzing.');
    return;
  }
  await runAnalysis(project, workspacePath, workspacePath);
}

async function runAnalysis(project, scanRoot, workspaceRoot) {
  const settings = workspaceSettings(project, workspaceRoot);
  const generatedRoot = settings.generatedRoot || project.javaOutputPath || null;
  const apiWorkspace = apiWorkspacePath(project, workspaceRoot);
  await vscode.window.withProgress({ location: vscode.ProgressLocation.Notification, title: 'Renovatio analysis', cancellable: false }, async progress => {
    progress.report({ message: `Using ${scanRoot}` });
    let jobProject = project;
    try {
      const updated = await request('PUT', `/api/projects/${encodeURIComponent(project.id)}`, {
        workspacePath: apiWorkspace,
        cobolScanRoot: scanRoot,
        javaOutputPath: project.javaOutputPath || null
      });
      rememberProject(updated);
      jobProject = updated;
    } catch (error) {
      output.appendLine(`Project path sync skipped; using VS Code workspace settings for analysis: ${message(error)}`);
    }
    await activateProject(jobProject, { openWorkspace: false });
    progress.report({ message: 'Starting analyzer job' });
    const job = await request('POST', `/api/projects/${encodeURIComponent(jobProject.id)}/jobs`, {
      operation: 'analyze',
      params: {
        workspacePath: apiWorkspace,
        scanRoot,
        cobolScanRoot: scanRoot,
        workspaceLabel: scanRoot,
        outputDir: generatedRoot,
        targetLanguage: settings.targetLanguage,
        targetPackage: settings.targetPackage
      }
    });
    state.latestJob = job;
    output.appendLine(`Analyze job ${job.id} queued for ${scanRoot}`);
    await openAnalysisPanel(scanRoot);
    const completed = await waitForJob(job.id, progress, scanRoot);
    state.latestJob = completed;
    vscode.window.showInformationMessage(`Renovatio analyze ${completed.status || 'completed'}: ${completed.id}`);
    await refreshAnalysis();
    await refreshDomainModel();
    await openAnalysisPanel(scanRoot);
    updateStatus();
    projectsProvider.refresh();
    operationsProvider.refresh();
    analysisProvider.refresh();
    domainModelProvider.refresh();
    if (domainModelPanel) await openDomainModel();
    if (persistencePanel) await openPersistenceModel();
  });
}

async function addCobolSourceRoot(resource) {
  const resourceUri = resource instanceof vscode.Uri ? resource : undefined;
  const project = await ensureActiveProject(resourceUri);
  const workspaceRoot = workspaceFolderPathFor(resourceUri) || workspaceSettings(project).workspaceFolderPath || await selectWorkspaceFolderPath();
  if (!workspaceRoot) {
    vscode.window.showWarningMessage('Open a VS Code workspace before adding COBOL source roots.');
    return;
  }
  let selectedUris = [];
  if (resourceUri) {
    selectedUris = [await directoryUriFor(resourceUri)];
  } else {
    selectedUris = await vscode.window.showOpenDialog({
      canSelectFiles: false,
      canSelectFolders: true,
      canSelectMany: true,
      defaultUri: vscode.Uri.file(workspaceRoot),
      openLabel: 'Add COBOL source root',
      title: 'Select COBOL source root'
    }) || [];
  }
  if (!selectedUris.length) return;
  const settings = workspaceSettings(project, workspaceRoot);
  const selectedPaths = selectedUris.map(uri => uri.fsPath);
  const mode = await chooseCobolImportMode(project, settings, selectedPaths);
  if (!mode || mode === 'cancel') return;
  if (mode === 'newProject') {
    await createProjectFromCobolRoots(selectedPaths);
    return;
  }
  const additions = selectedPaths.map(fsPath => serializePathForWorkspace(fsPath, workspaceRoot));
  const next = mode === 'replace'
    ? uniqueStrings(additions)
    : uniqueStrings([...settings.rawCobolRoots, ...additions]);
  await updateWorkspaceSetting('cobolRoots', next, workspaceRoot);
  vscode.window.showInformationMessage(`${mode === 'replace' ? 'Replaced' : 'Configured'} ${next.length} COBOL source root(s).`);
  projectsProvider.refresh();
  operationsProvider.refresh();
  await refreshVisiblePanels();
}

async function removeCobolSourceRoot() {
  const project = await ensureActiveProject();
  if (!project) return;
  const settings = workspaceSettings(project);
  if (!settings.rawCobolRoots.length) {
    vscode.window.showWarningMessage('No configured COBOL source roots to remove.');
    return;
  }
  const selected = await vscode.window.showQuickPick(settings.rawCobolRoots.map((root, index) => ({
    label: root,
    description: settings.cobolRoots[index],
    root
  })), { placeHolder: 'Remove COBOL source root' });
  if (!selected) return;
  await updateWorkspaceSetting('cobolRoots', settings.rawCobolRoots.filter(root => root !== selected.root), settings.workspaceFolderPath);
  projectsProvider.refresh();
  operationsProvider.refresh();
  await refreshVisiblePanels();
}

async function chooseCobolImportMode(project, settings, selectedPaths) {
  const selected = selectedPaths.map(value => path.normalize(value));
  const existing = settings.configuredCobolRoots.map(value => path.normalize(value));
  const hasExistingWork = Boolean(
    existing.length
    || asArray(state.domainModel?.model?.nodes).length
    || state.analysis
    || project?.cobolScanRoot
  );
  if (!hasExistingWork) return 'add';
  const alreadyKnown = selected.every(candidate => existing.some(root => samePath(candidate, root)));
  if (alreadyKnown) return 'add';
  const action = await vscode.window.showWarningMessage(
    `You selected a different COBOL source path for "${project?.name || 'the current Renovatio project'}". Keep the current work separate or update this project?`,
    { modal: true, detail: `Selected:\n${selected.join('\n')}\n\nCurrent roots:\n${existing.length ? existing.join('\n') : project?.cobolScanRoot || project?.workspacePath || 'workspace fallback'}` },
    'Create New Project',
    'Add to Current Project',
    'Replace Current Roots',
    'Cancel'
  );
  if (action === 'Create New Project') return 'newProject';
  if (action === 'Add to Current Project') return 'add';
  if (action === 'Replace Current Roots') {
    const confirm = await vscode.window.showWarningMessage(
      'Replace the current COBOL roots for this VS Code workspace? Existing Renovatio backend/domain history is kept, but future analysis will update the active project from the new path.',
      { modal: true },
      'Replace Roots',
      'Cancel'
    );
    return confirm === 'Replace Roots' ? 'replace' : 'cancel';
  }
  return 'cancel';
}

async function createProjectFromCobolRoots(selectedPaths) {
  const primaryRoot = selectedPaths[0];
  if (!primaryRoot) return;
  const defaultName = path.basename(primaryRoot) || 'renovatio-project';
  const name = await vscode.window.showInputBox({
    title: 'Create Renovatio project',
    prompt: 'Name for the new Renovatio project',
    value: defaultName,
    ignoreFocusOut: true
  });
  if (!name) return;
  const workspacePath = primaryRoot;
  try {
    const created = await request('POST', '/api/projects', {
      name,
      workspacePath,
      javaOutputPath: path.join(workspacePath, 'generated-java-stubs'),
      javaPackage: workspaceSettings().targetPackage,
      javaArchitecture: 'layered'
    });
    rememberProject(created);
    await activateProject(created, { openWorkspace: true });
    await updateWorkspaceSetting('cobolRoots', selectedPaths, workspacePath, { optional: true });
    await refresh();
    vscode.window.showInformationMessage(`Created Renovatio project "${created.name || name}" for ${primaryRoot}.`);
    projectsProvider.refresh();
    operationsProvider.refresh();
    await refreshVisiblePanels();
  } catch (error) {
    showError('Could not create Renovatio project for the selected COBOL path', error);
  }
}

async function confirmExternalScanRoot(project, scanRoot, workspaceRoot) {
  const settings = workspaceSettings(project, workspaceRoot);
  const normalized = path.normalize(scanRoot);
  const knownRoots = uniqueStrings([
    ...settings.configuredCobolRoots,
    project?.cobolScanRoot,
    project?.workspacePath
  ].filter(Boolean)).map(value => path.normalize(value));
  if (!knownRoots.length) return true;
  const known = knownRoots.some(root => samePath(normalized, root) || isInsidePath(normalized, root) || isInsidePath(root, normalized));
  if (known) return true;
  const action = await vscode.window.showWarningMessage(
    `This path is not configured as a COBOL root for "${project?.name || 'the active Renovatio project'}".`,
    { modal: true, detail: `Selected:\n${normalized}\n\nConfigured/current:\n${knownRoots.join('\n')}` },
    'Analyze Current Project',
    'Add Root First',
    'Cancel'
  );
  if (action === 'Analyze Current Project') return true;
  if (action === 'Add Root First') {
    const next = uniqueStrings([...settings.rawCobolRoots, serializePathForWorkspace(normalized, workspaceRoot)]);
    await updateWorkspaceSetting('cobolRoots', next, workspaceRoot);
    projectsProvider.refresh();
    operationsProvider.refresh();
    return true;
  }
  return false;
}

async function selectGeneratedOutputFolder() {
  const project = await ensureActiveProject();
  if (!project) return;
  const settings = workspaceSettings(project);
  const workspaceRoot = settings.workspaceFolderPath || await selectWorkspaceFolderPath();
  if (!workspaceRoot) {
    vscode.window.showWarningMessage('Open a VS Code workspace before configuring future output.');
    return;
  }
  const selected = await vscode.window.showOpenDialog({
    canSelectFiles: false,
    canSelectFolders: true,
    canSelectMany: false,
    defaultUri: vscode.Uri.file(settings.generatedRoot || workspaceRoot),
    openLabel: 'Use future output folder',
    title: 'Configure future output folder'
  });
  const uri = selected?.[0];
  if (!uri) return;
  const storedPath = serializePathForWorkspace(uri.fsPath, workspaceRoot);
  await updateWorkspaceSetting('generatedRoot', storedPath, workspaceRoot);
  try {
    const current = state.projects.find(candidate => candidate.id === state.activeProjectId) || project;
    const currentSettings = workspaceSettings(current, workspaceRoot);
    const updated = await request('PUT', `/api/projects/${encodeURIComponent(current.id)}`, {
      workspacePath: apiWorkspacePath(current, workspaceRoot),
      cobolScanRoot: current.cobolScanRoot || currentSettings.cobolRoots[0] || workspaceRoot,
      javaOutputPath: current.javaOutputPath || null
    });
    rememberProject(updated);
  } catch (error) {
    output.appendLine(`Future output synced to VS Code settings but not backend: ${message(error)}`);
  }
  projectsProvider.refresh();
  operationsProvider.refresh();
  await refreshVisiblePanels();
}

async function openGeneratedCode() {
  const project = await ensureActiveProject();
  if (!project) return;
  const generated = await loadGeneratedArtifacts(project);
  if (!generated.exists) {
    const action = await vscode.window.showWarningMessage(`No future output folder found at ${generated.root}`, 'Create Folder');
    if (action) {
      const uri = vscode.Uri.file(generated.root);
      await vscode.workspace.fs.createDirectory(uri);
      await vscode.commands.executeCommand('revealInExplorer', uri);
    }
    await openProjectPanel(project);
    return;
  }
  if (!generated.files.length) {
    await vscode.commands.executeCommand('revealInExplorer', vscode.Uri.file(generated.root));
    vscode.window.showInformationMessage(`Future output exists but has no generated source files yet: ${generated.root}`);
    await openProjectPanel(project);
    return;
  }
  const first = generated.files[0];
  const document = await vscode.workspace.openTextDocument(vscode.Uri.file(path.join(generated.root, first.relativePath)));
  await vscode.window.showTextDocument(document, { preview: false });
}

async function openFile(fsPath) {
  if (!fsPath) return;
  const document = await vscode.workspace.openTextDocument(vscode.Uri.file(String(fsPath)));
  await vscode.window.showTextDocument(document, { preview: false });
}

async function openEvidence(sourceRef) {
  const parsed = parseSourceRef(sourceRef);
  if (!parsed.fsPath) return;
  const resolved = await resolveEvidencePath(parsed.fsPath);
  if (!resolved) {
    vscode.window.showWarningMessage(`Could not resolve evidence source: ${parsed.fsPath}`);
    return;
  }
  const document = await vscode.workspace.openTextDocument(vscode.Uri.file(resolved));
  const editor = await vscode.window.showTextDocument(document, { preview: false });
  if (parsed.line && parsed.line > 0) {
    const position = new vscode.Position(Math.min(parsed.line - 1, document.lineCount - 1), 0);
    editor.selection = new vscode.Selection(position, position);
    editor.revealRange(new vscode.Range(position, position), vscode.TextEditorRevealType.InCenter);
  }
}

function parseSourceRef(sourceRef) {
  const raw = String(sourceRef || '').trim();
  if (!raw) return { fsPath: '', line: undefined };
  const match = raw.match(/^(.*?)(?::(\d+)(?::\d+)?)?$/);
  if (!match) return { fsPath: raw, line: undefined };
  return {
    fsPath: match[1] || raw,
    line: match[2] ? Number(match[2]) : undefined
  };
}

async function resolveEvidencePath(fsPath) {
  const expanded = expandHome(fsPath);
  if (path.isAbsolute(expanded) && await pathExists(expanded)) return path.normalize(expanded);
  const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
  const settings = workspaceSettings(project);
  const roots = uniqueStrings([
    ...settings.cobolRoots,
    settings.workspaceFolderPath,
    project?.cobolScanRoot,
    project?.workspacePath,
    ...asArray(vscode.workspace.workspaceFolders).map(folder => folder.uri.fsPath)
  ].filter(Boolean));
  for (const root of roots) {
    const candidate = path.normalize(path.join(root, expanded));
    if (await pathExists(candidate)) return candidate;
  }
  return path.isAbsolute(expanded) ? path.normalize(expanded) : '';
}

async function pathExists(fsPath) {
  try {
    await vscode.workspace.fs.stat(vscode.Uri.file(fsPath));
    return true;
  } catch {
    return false;
  }
}

async function ensureActiveProject(resourceUri) {
  if (!state.projects.length) await refresh();
  const resourceProject = resourceUri ? projectForResource(resourceUri) : undefined;
  if (resourceProject) {
    const hydrated = await hydrateProject(resourceProject);
    await activateProject(hydrated, { openWorkspace: false });
    return hydrated;
  }
  const workspaceProject = projectForOpenWorkspace();
  if (workspaceProject) {
    const hydrated = await hydrateProject(workspaceProject);
    await activateProject(hydrated, { openWorkspace: false });
    return hydrated;
  }
  let project = state.projects.find(candidate => candidate.id === state.activeProjectId);
  if (!project) {
    await selectProject();
    project = state.projects.find(candidate => candidate.id === state.activeProjectId);
  }
  if (!project) vscode.window.showWarningMessage('Create or select a Renovatio project first.');
  return hydrateProject(project);
}

async function hydrateProject(project) {
  if (!project || project.workspacePath) return project;
  try {
    const full = await request('GET', `/api/projects/${encodeURIComponent(project.id)}`);
    const merged = { ...project, ...full };
    rememberProject(merged);
    return merged;
  } catch (error) {
    output.appendLine(`Could not load Renovatio project details for ${project.id}: ${message(error)}`);
    return project;
  }
}

async function syncActiveProjectWithWorkspace() {
  const project = projectForOpenWorkspace();
  if (!project || project.id === state.activeProjectId) return;
  state.activeProjectId = project.id;
  await extensionContext.workspaceState.update('renovatio.activeProjectId', project.id);
}

function projectForOpenWorkspace() {
  const folders = vscode.workspace.workspaceFolders || [];
  return state.projects.find(project => folders.some(folder => projectMatchesFolder(project, folder.uri.fsPath)));
}

function projectForResource(resourceUri) {
  const folderPath = workspaceFolderPathFor(resourceUri);
  if (!folderPath) return undefined;
  return state.projects.find(project => projectMatchesFolder(project, folderPath));
}

function projectMatchesFolder(project, folderPath) {
  const candidates = [project.workspacePath, project.cobolScanRoot].filter(Boolean);
  return candidates.some(candidate => samePath(candidate, folderPath) || isInsidePath(candidate, folderPath) || isInsidePath(folderPath, candidate));
}

function workspaceFolderPathFor(uri) {
  if (!uri) return undefined;
  const folder = vscode.workspace.getWorkspaceFolder(uri);
  return folder?.uri.fsPath;
}

async function selectWorkspaceFolderPath() {
  const folders = vscode.workspace.workspaceFolders || [];
  if (!folders.length) return undefined;
  if (folders.length === 1) return folders[0].uri.fsPath;
  const selected = await vscode.window.showWorkspaceFolderPick({ placeHolder: 'Select VS Code workspace folder for Renovatio analysis' });
  return selected?.uri.fsPath;
}

function workspaceSettings(project, workspaceRoot) {
  const folderPath = workspaceRoot || workspaceFolderForProject(project)?.uri.fsPath || vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || project?.workspacePath;
  const cfg = workspaceConfiguration(folderPath);
  const targetLanguage = String(cfg.get('targetLanguage') || 'java');
  const targetPackage = String(cfg.get('targetPackage') || 'com.example.modernized');
  const rawCobolRoots = arraySetting(cfg.get('cobolRoots'));
  const configuredCobolRoots = rawCobolRoots.map(root => resolveConfiguredPath(root, folderPath)).filter(Boolean);
  const fallbackCobolRoot = project?.cobolScanRoot || project?.workspacePath || folderPath;
  const rawGeneratedRoot = String(cfg.get('generatedRoot') || `generated/${targetLanguage}`);
  const configuredGeneratedRoot = resolveConfiguredPath(rawGeneratedRoot || project?.javaOutputPath || `generated/${targetLanguage}`, folderPath || project?.workspacePath || project?.cobolScanRoot);
  const suggestedGeneratedRoot = resolveConfiguredPath(`generated/${targetLanguage}`, folderPath || project?.workspacePath || project?.cobolScanRoot);
  const generatedRootWarning = generatedRootConflict(configuredGeneratedRoot, configuredCobolRoots);
  const generatedRoot = generatedRootWarning ? suggestedGeneratedRoot : configuredGeneratedRoot;
  return {
    workspaceFolderPath: folderPath,
    rawCobolRoots,
    configuredCobolRoots,
    cobolRoots: configuredCobolRoots.length ? configuredCobolRoots : [fallbackCobolRoot].filter(Boolean),
    rawGeneratedRoot,
    configuredGeneratedRoot,
    suggestedGeneratedRoot,
    generatedRootWarning,
    generatedRoot,
    targetLanguage,
    targetPackage
  };
}

function apiWorkspacePath(project, fallbackWorkspaceRoot) {
  return project?.workspacePath || fallbackWorkspaceRoot;
}

function generatedRootConflict(generatedRoot, cobolRoots) {
  if (!generatedRoot || !cobolRoots.length) return '';
  const conflict = cobolRoots.find(root => samePath(generatedRoot, root) || isInsidePath(root, generatedRoot));
  if (!conflict) return '';
  return `Configured future output overlaps a COBOL source root: ${conflict}`;
}

function workspaceFolderForProject(project) {
  const folders = vscode.workspace.workspaceFolders || [];
  if (!project) return folders[0];
  return folders.find(folder => projectMatchesFolder(project, folder.uri.fsPath)) || folders[0];
}

function arraySetting(value) {
  return Array.isArray(value) ? value.map(item => String(item).trim()).filter(Boolean) : [];
}

function resolveConfiguredPath(value, basePath) {
  if (!value) return '';
  const expanded = expandHome(String(value));
  if (path.isAbsolute(expanded)) return path.normalize(expanded);
  if (!basePath) return path.normalize(expanded);
  return path.normalize(path.join(basePath, expanded));
}

function expandHome(value) {
  if (value === '~') return os.homedir();
  if (value.startsWith(`~${path.sep}`) || value.startsWith('~/')) return path.join(os.homedir(), value.slice(2));
  return value;
}

function serializePathForWorkspace(fsPath, workspaceRoot) {
  if (!workspaceRoot) return fsPath;
  const relative = path.relative(workspaceRoot, fsPath);
  if (!relative) return '.';
  if (!relative.startsWith('..') && !path.isAbsolute(relative)) return relative;
  return fsPath;
}

async function updateWorkspaceSetting(key, value, workspaceRoot, options = {}) {
  const cfg = workspaceConfiguration(workspaceRoot);
  const folder = workspaceRoot ? workspaceFolderForPath(workspaceRoot) : undefined;
  if (folder) {
    await cfg.update(key, value, vscode.ConfigurationTarget.WorkspaceFolder);
    return true;
  }
  if (vscode.workspace.workspaceFolders?.length) {
    await cfg.update(key, value, vscode.ConfigurationTarget.Workspace);
    return true;
  }
  if (options.optional) {
    output.appendLine(`Skipped writing renovatio.${key}; no VS Code workspace is open.`);
    return false;
  }
  const action = await vscode.window.showWarningMessage(
    `Unable to save renovatio.${key} because no VS Code workspace is open.`,
    'Open Folder',
    'Save Globally',
    'Cancel'
  );
  if (action === 'Open Folder' && workspaceRoot) {
    vscode.workspace.updateWorkspaceFolders(0, 0, { uri: vscode.Uri.file(workspaceRoot), name: path.basename(workspaceRoot) || 'Renovatio' });
    return false;
  }
  if (action === 'Save Globally') {
    await cfg.update(key, value, vscode.ConfigurationTarget.Global);
    return true;
  }
  return false;
}

function workspaceConfiguration(folderPath) {
  if (!folderPath) return vscode.workspace.getConfiguration('renovatio');
  return vscode.workspace.getConfiguration('renovatio', vscode.Uri.file(folderPath));
}

function workspaceFolderForPath(folderPath) {
  if (!folderPath) return undefined;
  const folders = vscode.workspace.workspaceFolders || [];
  return folders.find(folder => samePath(folder.uri.fsPath, folderPath) || isInsidePath(folderPath, folder.uri.fsPath));
}

async function pickCobolSourceRoot(roots) {
  const selected = await vscode.window.showQuickPick(roots.map(root => ({
    label: path.basename(root) || root,
    description: root,
    root
  })), { placeHolder: 'Select COBOL source root to analyze' });
  return selected?.root;
}

async function directoryUriFor(uri) {
  try {
    const stat = await vscode.workspace.fs.stat(uri);
    if (stat.type === vscode.FileType.Directory) return uri;
  } catch {
    // Fall back to the parent path below.
  }
  return vscode.Uri.file(path.dirname(uri.fsPath));
}

function uniqueStrings(values) {
  const seen = new Set();
  return values.filter(value => {
    const key = String(value);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

async function refreshVisiblePanels() {
  if (controlDeckPanel) await openControlDeck();
  if (analysisPanel) await openAnalysisPanel();
  if (domainModelPanel) await openDomainModel();
  if (persistencePanel) await openPersistenceModel();
  if (projectPanel) {
    const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
    if (project) await openProjectPanel(project);
  }
}

function rememberProject(project) {
  const index = state.projects.findIndex(candidate => candidate.id === project.id);
  if (index >= 0) {
    state.projects.splice(index, 1, project);
  } else {
    state.projects.push(project);
  }
}

async function waitForJob(jobId, progress, scanRoot) {
  let latest = state.latestJob;
  for (let attempt = 0; attempt < 120; attempt += 1) {
    await sleep(1000);
    latest = await request('GET', `/api/jobs/${encodeURIComponent(jobId)}`);
    state.latestJob = latest;
    const percent = normalizeProgress(latest.progress);
    progress.report({ message: `${latest.status || 'RUNNING'} ${percent}% · ${scanRoot}` });
    await refreshAnalysis();
    await refreshDomainModel();
    analysisProvider.refresh();
    domainModelProvider.refresh();
    if (analysisPanel) await openAnalysisPanel(scanRoot);
    if (domainModelPanel) await openDomainModel();
    if (persistencePanel) await openPersistenceModel();
    if (latest.status && !['PENDING', 'RUNNING'].includes(String(latest.status).toUpperCase())) {
      return latest;
    }
  }
  output.appendLine(`Analyze job ${jobId} did not finish within the VS Code polling window.`);
  return latest || { id: jobId, status: 'PENDING', progress: 0 };
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function openControlDeck() {
  if (!controlDeckPanel) {
    controlDeckPanel = vscode.window.createWebviewPanel('renovatioControlDeck', 'Renovatio Control Deck', vscode.ViewColumn.One, { enableCommandUris: true });
    controlDeckPanel.onDidDispose(() => { controlDeckPanel = undefined; });
  }
  controlDeckPanel.reveal(vscode.ViewColumn.One);
  const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
  const inventory = state.analysis?.inventory || {};
  const domainModel = state.domainModel?.model || {};
  const generated = await loadGeneratedArtifacts(project);
  controlDeckPanel.webview.html = renderPanel('Renovatio', project, `
    ${renderHero(project, 'Control Deck', 'Governed modernization cockpit for characterization, IR, decisions and later emission.')}
    ${renderModernizationPremises(inventory)}
    ${renderPaths(project, generated)}
    ${renderMetricGrid([
      ['COBOL files', inventory.sourceFiles ?? inventory.programs ?? 0],
      ['Programs', inventory.programs ?? 0],
      ['Copybooks', inventory.copybooks ?? 0],
      ['JCL', inventory.jcl ?? inventory.jclFiles ?? 0],
      ['Domain nodes', asArray(domainModel.nodes).length],
      ['Future output files', generated.files.length],
      ['Backend', config().backendUrl]
    ])}
    ${renderFutureOutput(generated)}
  `);
}

async function openProjectPanel(project) {
  if (!projectPanel) {
    projectPanel = vscode.window.createWebviewPanel('renovatioProject', 'Renovatio Project', vscode.ViewColumn.One, { enableCommandUris: true });
    projectPanel.onDidDispose(() => { projectPanel = undefined; });
  }
  projectPanel.reveal(vscode.ViewColumn.One);
  const inventory = state.analysis?.inventory || {};
  const generated = await loadGeneratedArtifacts(project);
  projectPanel.webview.html = renderPanel(project.name, project, `
    ${renderHero(project, 'Project', 'Workspace, COBOL source roots, analysis readiness and future output settings.')}
    ${renderPaths(project, generated)}
    ${renderModernizationPremises(inventory)}
    ${renderMetricGrid([
      ['Inventory', sumInventory(inventory)],
      ['Programs', inventory.programs ?? 0],
      ['Copybooks', inventory.copybooks ?? 0],
      ['Future output files', generated.files.length]
    ])}
    ${renderFutureOutput(generated)}
  `);
}

async function openAnalysisPanel(scanRoot) {
  if (!analysisPanel) {
    analysisPanel = vscode.window.createWebviewPanel('renovatioAnalysis', 'Renovatio Discovery', vscode.ViewColumn.One, { enableCommandUris: true });
    analysisPanel.onDidDispose(() => { analysisPanel = undefined; });
  }
  analysisPanel.reveal(vscode.ViewColumn.One);
  const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
  const inventory = state.analysis?.inventory || {};
  const job = state.latestJob;
  const settings = workspaceSettings(project);
  const activeScanRoot = scanRoot || settings.cobolRoots[0] || project?.cobolScanRoot || project?.workspacePath;
  const source = await loadCobolArtifacts(activeScanRoot);
  const progress = normalizeProgress(job?.progress ?? (job?.status === 'COMPLETED' ? 1 : 0));
  analysisPanel.webview.html = renderPanel('Analysis', project, `
    ${renderHero(project, 'Discovery report', 'COBOL inventory, inferred persistence, data records and source evidence from the latest analysis.')}
    ${renderDiscoveryRunState(job, state.analysis, state.domainModel, inventory, progress)}
    ${renderInferredPersistence(state.domainModel, project)}
    ${renderDataShapeFindings(state.domainModel, project)}
    ${renderRelationshipFindings(state.domainModel)}
    ${renderAnalysisIssues(state.domainModel, inventory)}
    ${renderCobolArtifacts(source)}
  `);
}

async function openDomainModel() {
  const project = await ensureActiveProject();
  if (!project) return;
  await refreshDomainModel();
  if (!domainModelPanel) {
    domainModelPanel = vscode.window.createWebviewPanel('renovatioDomainModel', 'Renovatio Domain Model', vscode.ViewColumn.One, { enableCommandUris: true, enableScripts: true });
    domainModelPanel.onDidDispose(() => { domainModelPanel = undefined; });
  }
  domainModelPanel.reveal(vscode.ViewColumn.One);
  const domain = state.domainModel;
  domainModelPanel.webview.html = renderPanel('Domain Model', project, `
    ${renderHero(project, 'Domain Model', 'Versioned business model projected from COBOL discovery evidence.')}
    ${renderNativeDiagramActions()}
    ${renderDomainModelOverview(domain)}
    ${renderDomainModelDiagram(domain)}
    ${renderInferredClassModel(domain, project)}
    ${renderDomainModelGraph(domain)}
    ${renderDomainModelGovernance(domain)}
  `);
}

async function openClassDiagram() {
  const project = await ensureActiveProject();
  if (!project) return;
  await refreshDomainModel();
  const artifact = await writeClassDiagramArtifact(project, state.domainModel);
  const document = await vscode.workspace.openTextDocument(artifact.uri);
  await vscode.window.showTextDocument(document, { preview: false, viewColumn: vscode.ViewColumn.One });
  await vscode.commands.executeCommand('markdown.showPreviewToSide', artifact.uri);
  output.appendLine(`Opened Renovatio UML class diagram: ${artifact.uri.fsPath}`);
  if (!hasMermaidMarkdownExtension() && !extensionContext.globalState.get('renovatio.mermaidPrompted')) {
    await extensionContext.globalState.update('renovatio.mermaidPrompted', true);
    const action = await vscode.window.showInformationMessage(
      'Renovatio generated Mermaid UML. Install a Mermaid Markdown preview extension if the preview shows only source.',
      'Search Mermaid Extensions'
    );
    if (action) await vscode.commands.executeCommand('workbench.extensions.search', 'mermaid markdown preview');
  }
}

async function openDrawioDomainModel() {
  const project = await ensureActiveProject();
  if (!project) return;
  await refreshDomainModel();
  const artifact = await writeDrawioDomainModelArtifact(project, state.domainModel);
  await openDrawioArtifact(artifact, 'Renovatio UML domain model');
}

async function openDrawioEntityModel() {
  const project = await ensureActiveProject();
  if (!project) return;
  await refreshDomainModel();
  const artifact = await writeDrawioEntityModelArtifact(project, state.domainModel);
  await openDrawioArtifact(artifact, 'Renovatio UML entity model');
}

async function openDrawioPersistenceErd() {
  const project = await ensureActiveProject();
  if (!project) return;
  await refreshDomainModel();
  const artifact = await writeDrawioPersistenceErdArtifact(project, state.domainModel);
  await openDrawioArtifact(artifact, 'Renovatio persistence DER');
}

async function openDrawioArtifact(artifact, label) {
  output.appendLine(`Opened ${label}: ${artifact.uri.fsPath}`);
  if (hasHedietDrawioExtension()) {
    try {
      await vscode.commands.executeCommand('vscode.openWith', artifact.uri, 'hediet.vscode-drawio-text', { preview: false, viewColumn: vscode.ViewColumn.One });
      return;
    } catch (error) {
      output.appendLine(`Draw.io editor open failed, falling back to default editor: ${message(error)}`);
    }
  }
  await vscode.commands.executeCommand('vscode.open', artifact.uri, { preview: false });
  if (!hasHedietDrawioExtension() && !extensionContext.globalState.get('renovatio.drawioPrompted')) {
    await extensionContext.globalState.update('renovatio.drawioPrompted', true);
    const action = await vscode.window.showInformationMessage(
      'Renovatio generated an editable .drawio model. Install Draw.io Integration to edit UML and DER diagrams graphically inside VS Code.',
      'Search Draw.io Extensions'
    );
    if (action) await vscode.commands.executeCommand('workbench.extensions.search', 'hediet.vscode-drawio');
  }
}

async function openNativeDomainDiagram() {
  await runNativeDiagramCommand('domain', async () => {
    const project = await ensureActiveProject();
    if (!project) return;
    await refreshDomainModel();
    const artifact = await writeNativeDomainDiagramArtifact(project, state.domainModel);
    await openNativeDiagramArtifact(artifact, 'renovatio.diagram.domain', 'Renovatio native domain diagram');
  });
}

async function openNativePersistenceDiagram() {
  await runNativeDiagramCommand('persistence', async () => {
    const project = await ensureActiveProject();
    if (!project) return;
    await refreshDomainModel();
    const artifact = await writeNativePersistenceDiagramArtifact(project, state.domainModel);
    await openNativeDiagramArtifact(artifact, 'renovatio.diagram.domain', 'Renovatio native persistence diagram');
  });
}

async function openNativeArchitectureDiagram() {
  await runNativeDiagramCommand('architecture', async () => {
    const project = await ensureActiveProject();
    if (!project) return;
    await refreshDomainModel();
    const artifact = await writeNativeArchitectureDiagramArtifact(project, state.domainModel);
    await openNativeDiagramArtifact(artifact, 'renovatio.diagram.architecture', 'Renovatio native architecture diagram');
  });
}

async function runNativeDiagramCommand(kind, action) {
  try {
    await action();
  } catch (error) {
    const detail = message(error);
    output.appendLine(`Native ${kind} diagram failed: ${detail}`);
    vscode.window.showErrorMessage(`Renovatio native ${kind} diagram failed: ${detail}`);
  }
}

async function openNativeDiagramArtifact(artifact, viewType, label) {
  output.appendLine(`Opened ${label}: ${artifact.uri.fsPath}`);
  await vscode.commands.executeCommand('vscode.openWith', artifact.uri, viewType, {
    preview: false,
    viewColumn: vscode.ViewColumn.One
  });
}

async function openPersistenceModel() {
  const project = await ensureActiveProject();
  if (!project) return;
  await refreshDomainModel();
  if (!persistencePanel) {
    persistencePanel = vscode.window.createWebviewPanel('renovatioPersistenceModel', 'Renovatio Persistence Model', vscode.ViewColumn.One, { enableCommandUris: true, enableScripts: true });
    persistencePanel.onDidDispose(() => { persistencePanel = undefined; });
  }
  persistencePanel.reveal(vscode.ViewColumn.One);
  const domain = state.domainModel;
  persistencePanel.webview.html = renderPanel('Persistence Model', project, `
    ${renderHero(project, 'Inferred Persistence', 'ER-style table and file model inferred from COBOL I/O, DB2 access and record evidence.')}
    ${renderNativeDiagramActions()}
    ${renderPersistenceSummary(domain)}
    ${renderPersistenceErd(domain, project)}
    <section class="panel">
      <div class="sectionTitle">Source mappings</div>
      <p class="sectionLead">Validate which programs touched each persistence resource and which COBOL record shape supplied the candidate fields.</p>
      ${renderPersistenceDiagram(domain)}
      ${renderPersistenceShapeGaps(domainDiscovery(domain))}
    </section>
    ${renderPersistenceRepositoryList(domain)}
  `);
}

function renderPanel(title, project, body) {
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
  h1, h2, h3, p { margin-top: 0; }
  .shell { max-width: 1180px; margin: 0 auto; }
  .hero {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 18px;
    align-items: end;
    padding: 0 0 18px;
    border-bottom: 1px solid var(--vscode-panel-border);
  }
  .eyebrow {
    color: var(--vscode-descriptionForeground);
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0;
    text-transform: uppercase;
  }
  .hero h1 { margin: 8px 0 6px; font-size: 30px; line-height: 1.15; }
  .hero p, .muted { color: var(--vscode-descriptionForeground); }
  .panel {
    margin-top: 18px;
    padding: 16px;
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-sideBar-background);
  }
  .panel.compact { padding: 14px 16px; }
  .sectionTitle {
    margin-bottom: 12px;
    color: var(--vscode-descriptionForeground);
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0;
    text-transform: uppercase;
  }
  .sectionLead {
    margin: -4px 0 14px;
    color: var(--vscode-descriptionForeground);
    line-height: 1.45;
  }
  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
    gap: 10px;
  }
  .flow {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
    gap: 8px;
  }
  .stage {
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    padding: 10px;
    min-height: 74px;
  }
  .stage.current {
    border-color: var(--vscode-focusBorder);
    background: var(--vscode-list-activeSelectionBackground);
  }
  .stage span {
    display: block;
    color: var(--vscode-descriptionForeground);
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
  }
  .stage strong {
    display: block;
    margin-top: 5px;
  }
  .stage small {
    display: block;
    margin-top: 5px;
    color: var(--vscode-descriptionForeground);
  }
  .pathGrid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 10px;
  }
  .pathGrid div {
    display: grid;
    gap: 6px;
    min-width: 0;
  }
  .pathGrid span {
    color: var(--vscode-descriptionForeground);
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
  }
  .metric {
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    padding: 12px;
    min-height: 58px;
  }
  .metric span {
    display: block;
    color: var(--vscode-descriptionForeground);
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
  }
  .metric strong {
    display: block;
    margin-top: 6px;
    font-size: 24px;
    line-height: 1.1;
    overflow-wrap: anywhere;
  }
  .paths {
    display: grid;
    gap: 9px;
  }
  .stack {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
  }
  .stack.inline {
    flex-direction: row;
    flex-wrap: wrap;
  }
  .kv {
    display: grid;
    grid-template-columns: minmax(120px, 170px) 1fr;
    gap: 12px;
    align-items: baseline;
    margin: 8px 0;
  }
  .kv span {
    color: var(--vscode-descriptionForeground);
    font-weight: 700;
  }
  code {
    color: var(--vscode-textPreformat-foreground);
    background: var(--vscode-textCodeBlock-background);
    padding: 2px 5px;
    overflow-wrap: anywhere;
  }
  .artifact {
    margin-top: 12px;
    border-top: 1px solid var(--vscode-panel-border);
    padding-top: 12px;
  }
  .modelMetaBar {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
    color: var(--vscode-descriptionForeground);
    font-size: 12px;
  }
  .modelMetaBar span {
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-sideBar-background);
    padding: 4px 7px;
  }
  .modelMetaBar strong {
    color: var(--vscode-foreground);
    margin-left: 4px;
  }
  .artifactHeader {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    color: var(--vscode-descriptionForeground);
    font-size: 12px;
  }
  .progress {
    height: 8px;
    margin: 0 0 14px;
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    overflow: hidden;
  }
  .progress span {
    display: block;
    height: 100%;
    min-width: 3px;
    background: var(--vscode-progressBar-background);
  }
  .empty {
    padding: 18px;
    border: 1px dashed var(--vscode-panel-border);
    color: var(--vscode-descriptionForeground);
  }
  .warning {
    padding: 12px;
    border-left: 3px solid var(--vscode-editorWarning-foreground);
    background: var(--vscode-inputValidation-warningBackground);
    color: var(--vscode-foreground);
  }
  .fileStrip {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin: 8px 0 12px;
  }
  .fileChip {
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    padding: 5px 8px;
    font-size: 12px;
    color: var(--vscode-descriptionForeground);
  }
  .fileStrip a {
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    padding: 5px 8px;
    font-size: 12px;
    overflow-wrap: anywhere;
  }
  .fileList {
    display: grid;
    border-top: 1px solid var(--vscode-panel-border);
  }
  .fileRow {
    display: grid;
    grid-template-columns: 1fr auto auto;
    gap: 12px;
    align-items: center;
    padding: 9px 0;
    border-bottom: 1px solid var(--vscode-panel-border);
  }
  .fileRow strong {
    overflow-wrap: anywhere;
  }
  .fileRow span {
    color: var(--vscode-descriptionForeground);
    font-size: 12px;
  }
  .domainGrid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 10px;
  }
  .nodeCard {
    display: grid;
    gap: 8px;
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    padding: 12px;
    min-width: 0;
  }
  .tableCard {
    border-left: 3px solid var(--vscode-focusBorder);
  }
  .nodeCard strong,
  .relationRow strong {
    overflow-wrap: anywhere;
  }
  .nodeMeta,
  .evidenceList,
  .pillRow {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .pill {
    border: 1px solid var(--vscode-panel-border);
    color: var(--vscode-descriptionForeground);
    padding: 3px 6px;
    font-size: 12px;
  }
  .fieldGrid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
    gap: 6px;
  }
  .field {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 8px;
    align-items: center;
    border: 1px solid var(--vscode-panel-border);
    padding: 6px 8px;
    background: var(--vscode-sideBar-background);
    min-width: 0;
  }
  .field strong {
    font-size: 12px;
  }
  .field span {
    color: var(--vscode-descriptionForeground);
    font-size: 11px;
    white-space: nowrap;
  }
  .inlineAction {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 12px;
    color: var(--vscode-descriptionForeground);
  }
  .statusLine {
    margin-top: 10px;
    color: var(--vscode-descriptionForeground);
    font-size: 12px;
  }
  .evidenceList code {
    font-size: 11px;
  }
  .relationList {
    display: grid;
    gap: 8px;
  }
  .relationRow {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    gap: 10px;
    align-items: center;
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    padding: 10px;
  }
  .relationKind {
    color: var(--vscode-descriptionForeground);
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
  }
  .diagramToolbar {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: center;
    margin-bottom: 10px;
  }
  .debugControls {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 6px;
  }
  .debugControls button {
    border: 1px solid var(--vscode-button-border, var(--vscode-panel-border));
    background: var(--vscode-button-secondaryBackground);
    color: var(--vscode-button-secondaryForeground);
    padding: 4px 8px;
    font: inherit;
    font-size: 12px;
    cursor: pointer;
  }
  .debugControls button:hover {
    background: var(--vscode-button-secondaryHoverBackground);
  }
  .diagramLegend {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: var(--vscode-descriptionForeground);
    font-size: 12px;
  }
  .legendItem {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
  .legendSwatch {
    width: 16px;
    height: 3px;
    background: var(--vscode-focusBorder);
  }
  .legendSwatch.maps {
    background: var(--vscode-charts-yellow, #cca700);
  }
  .legendSwatch.unresolved {
    background: var(--vscode-editorWarning-foreground);
  }
  .diagramViewport {
    overflow: auto;
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    height: min(64vh, 680px);
    min-height: 320px;
    resize: vertical;
  }
  .diagramSvg {
    display: block;
    min-width: 1080px;
    font-family: var(--vscode-font-family);
  }
  .diagramColumnLabel {
    fill: var(--vscode-descriptionForeground);
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
  }
  .diagramBox {
    fill: var(--vscode-sideBar-background);
    stroke: var(--vscode-panel-border);
    stroke-width: 1;
  }
  .diagramBox.repo {
    stroke: var(--vscode-focusBorder);
    stroke-width: 1.4;
  }
  .diagramBox.unresolved {
    stroke: var(--vscode-editorWarning-foreground);
    stroke-dasharray: 5 4;
  }
  .diagramBox.record {
    stroke: var(--vscode-charts-yellow, #cca700);
    stroke-width: 1.2;
  }
  .diagramTitle {
    fill: var(--vscode-foreground);
    font-size: 13px;
    font-weight: 700;
  }
  .diagramMeta,
  .diagramField {
    fill: var(--vscode-descriptionForeground);
    font-size: 11px;
  }
  .diagramField {
    fill: var(--vscode-textPreformat-foreground);
  }
  .diagramEdge {
    fill: none;
    stroke: var(--vscode-focusBorder);
    stroke-width: 1.2;
    opacity: .78;
  }
  .diagramEdge.maps {
    stroke: var(--vscode-charts-yellow, #cca700);
  }
  .erdViewport {
    overflow: auto;
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-editor-background);
    height: min(64vh, 680px);
    min-height: 320px;
    resize: vertical;
  }
  .erdSvg {
    display: block;
    min-width: 1080px;
    font-family: var(--vscode-font-family);
  }
  .erdTable {
    fill: var(--vscode-sideBar-background);
    stroke: var(--vscode-panel-border);
    stroke-width: 1;
  }
  .erdTable.unresolved {
    stroke: var(--vscode-editorWarning-foreground);
    stroke-dasharray: 5 4;
  }
  .erdHeader {
    fill: var(--vscode-list-activeSelectionBackground);
    stroke: var(--vscode-focusBorder);
    stroke-width: 1;
  }
  .erdTitle {
    fill: var(--vscode-foreground);
    font-size: 13px;
    font-weight: 800;
  }
  .erdKind,
  .erdField,
  .erdMeta {
    fill: var(--vscode-descriptionForeground);
    font-size: 11px;
  }
  .erdField {
    fill: var(--vscode-textPreformat-foreground);
  }
  .erdLink {
    fill: none;
    stroke: var(--vscode-charts-yellow, #cca700);
    stroke-width: 1.1;
    opacity: .68;
    stroke-dasharray: 4 4;
  }
  .debugNode {
    cursor: move;
  }
  .debugNode text[data-editable-label="true"] {
    cursor: text;
    text-decoration: underline dotted var(--vscode-descriptionForeground);
    text-underline-offset: 3px;
  }
  .shapeGapList {
    display: grid;
    gap: 8px;
    margin-top: 12px;
  }
  .shapeGap {
    display: grid;
    grid-template-columns: minmax(180px, .7fr) 1fr;
    gap: 12px;
    border: 1px solid var(--vscode-panel-border);
    border-left: 3px solid var(--vscode-editorWarning-foreground);
    background: var(--vscode-editor-background);
    padding: 10px;
  }
  .shapeGap strong {
    overflow-wrap: anywhere;
  }
  a {
    color: var(--vscode-textLink-foreground);
    text-decoration: none;
    font-weight: 700;
  }
  a:hover { text-decoration: underline; }
</style>
</head>
<body><main class="shell" aria-label="${escapeHtml(title)}">${body}</main>${renderDebugDiagramScript()}</body>
</html>`;
}

function renderDebugDiagramScript() {
  return `<script>
(() => {
  const diagrams = [...document.querySelectorAll('svg[data-debug-diagram="true"]')];
  const byNodeId = (svg, id) => [...svg.querySelectorAll('[data-node-id]')].find(node => node.dataset.nodeId === id);
  const currentBox = node => {
    const x = Number(node.dataset.x || 0) + Number(node.dataset.dx || 0);
    const y = Number(node.dataset.y || 0) + Number(node.dataset.dy || 0);
    return { x, y, width: Number(node.dataset.width || 0), height: Number(node.dataset.height || 0) };
  };
  const endpoint = (box, other) => {
    const fromLeft = box.x + box.width / 2 <= other.x + other.width / 2;
    return {
      x: fromLeft ? box.x + box.width : box.x,
      y: box.y + box.height / 2,
      direction: fromLeft ? 1 : -1
    };
  };
  const edgePath = (from, to) => {
    const start = endpoint(from, to);
    const end = endpoint(to, from);
    return 'M ' + start.x + ' ' + start.y + ' C ' + (start.x + 46 * start.direction) + ' ' + start.y + ', ' + (end.x + 46 * end.direction) + ' ' + end.y + ', ' + end.x + ' ' + end.y;
  };
  const updateEdges = svg => {
    for (const edge of svg.querySelectorAll('[data-edge="true"]')) {
      const from = byNodeId(svg, edge.dataset.from);
      const to = byNodeId(svg, edge.dataset.to);
      if (from && to) edge.setAttribute('d', edgePath(currentBox(from), currentBox(to)));
    }
  };
  const pointFor = (svg, event) => {
    const point = svg.createSVGPoint();
    point.x = event.clientX;
    point.y = event.clientY;
    return point.matrixTransform(svg.getScreenCTM().inverse());
  };
  const applyTransform = node => {
    const dx = Number(node.dataset.dx || 0);
    const dy = Number(node.dataset.dy || 0);
    node.setAttribute('transform', 'translate(' + dx + ' ' + dy + ')');
  };
  for (const svg of diagrams) {
    const initialViewBox = svg.getAttribute('viewBox') || '0 0 1080 600';
    const base = initialViewBox.split(/\\s+/).map(Number);
    const width = Number(base[2] || 1080);
    let scale = 1;
    let active = null;
    svg.dataset.initialViewBox = initialViewBox;
    svg.dataset.initialWidth = String(width);

    for (const node of svg.querySelectorAll('.debugNode')) {
      node.dataset.dx = '0';
      node.dataset.dy = '0';
      node.addEventListener('pointerdown', event => {
        if (event.target?.dataset?.editableLabel === 'true') return;
        active = {
          node,
          start: pointFor(svg, event),
          dx: Number(node.dataset.dx || 0),
          dy: Number(node.dataset.dy || 0)
        };
        node.setPointerCapture(event.pointerId);
        event.preventDefault();
      });
      node.addEventListener('pointermove', event => {
        if (!active || active.node !== node) return;
        const point = pointFor(svg, event);
        node.dataset.dx = String(active.dx + point.x - active.start.x);
        node.dataset.dy = String(active.dy + point.y - active.start.y);
        applyTransform(node);
        updateEdges(svg);
      });
      node.addEventListener('pointerup', event => {
        if (active?.node === node) {
          try { node.releasePointerCapture(event.pointerId); } catch {}
          active = null;
        }
      });
    }

    svg.addEventListener('dblclick', event => {
      if (event.target?.dataset?.editableLabel !== 'true') return;
      const current = event.target.textContent.trim();
      const next = prompt('Rename for this debug view', current);
      if (next && next.trim()) event.target.textContent = next.trim();
    });

    updateEdges(svg);
    document.querySelectorAll('[data-diagram-action][data-target="' + svg.id + '"]').forEach(button => {
      button.addEventListener('click', () => {
        const action = button.dataset.diagramAction;
        if (action === 'zoomIn') scale = Math.min(3, scale * 1.2);
        if (action === 'zoomOut') scale = Math.max(0.45, scale / 1.2);
        if (action === 'actual') {
          scale = 1;
          svg.style.minWidth = width + 'px';
          svg.style.width = width + 'px';
        }
        if (action === 'fit') {
          scale = 1;
          svg.style.minWidth = '0';
          svg.style.width = '100%';
        }
        if (action === 'reset') {
          scale = 1;
          svg.style.minWidth = '';
          svg.style.width = '';
          svg.setAttribute('viewBox', initialViewBox);
          svg.querySelectorAll('.debugNode').forEach(node => {
            node.dataset.dx = '0';
            node.dataset.dy = '0';
            node.removeAttribute('transform');
          });
          updateEdges(svg);
          return;
        }
        const centerX = Number(base[0] || 0) + width / 2;
        const height = Number(base[3] || 600);
        const centerY = Number(base[1] || 0) + height / 2;
        const nextWidth = width / scale;
        const nextHeight = height / scale;
        svg.setAttribute('viewBox', [centerX - nextWidth / 2, centerY - nextHeight / 2, nextWidth, nextHeight].join(' '));
      });
    });
  }
})();
</script>`;
}

function renderHero(project, title, subtitle) {
  return `<section class="hero">
    <div>
      <div class="eyebrow">Renovatio Modernization</div>
      <h1>${escapeHtml(title)}</h1>
      <p>${escapeHtml(subtitle)}</p>
    </div>
  </section>`;
}

function renderNativeDiagramActions() {
  return `<section class="panel">
    <div class="sectionTitle">Native diagrams</div>
    <p class="sectionLead">Open Renovatio diagrams with the built-in VS Code editor.</p>
    <div class="inlineAction">
      ${commandLink('renovatio.openNativeDomainDiagram', [], 'Open domain diagram')}
      ${commandLink('renovatio.openNativePersistenceDiagram', [], 'Open persistence diagram')}
      ${commandLink('renovatio.openNativeArchitectureDiagram', [], 'Open architecture diagram')}
    </div>
  </section>`;
}

function renderPaths(project, generated) {
  const settings = workspaceSettings(project);
  const roots = settings.cobolRoots.length
    ? `${settings.cobolRoots.map(root => `<code>${escapeHtml(root)}</code>`).join('')}${settings.configuredCobolRoots.length ? '' : '<span class="muted">Using workspace fallback until a COBOL source root is configured.</span>'}`
    : '<code>Not configured</code>';
  return `<section class="panel">
    <div class="sectionTitle">Paths</div>
    <div class="paths">
      <div class="kv"><span>VS Code workspace</span><code>${escapeHtml(settings.workspaceFolderPath || project?.workspacePath || 'Not configured')}</code></div>
      <div class="kv"><span>COBOL source roots</span><div class="stack">${roots}</div></div>
      <div class="kv"><span>Future output root</span><code>${escapeHtml(generated.root || 'Not configured')}</code></div>
      <div class="kv"><span>Target</span><div class="stack inline"><code>${escapeHtml(settings.targetLanguage)}</code><code>${escapeHtml(settings.targetPackage)}</code></div></div>
    </div>
  </section>`;
}

function renderModernizationPremises(inventory) {
  const hasInventory = sumInventory(inventory) > 0;
  return `<section class="panel compact">
    <div class="sectionTitle">Renovatio flow</div>
    <div class="flow">
      ${renderStage('01', 'Characterize', 'golden-master safety net', false)}
      ${renderStage('02', 'Parse to neutral IR', 'COBOL, copybooks, JCL', true)}
      ${renderStage('03', 'Map relations', 'calls, data, jobs, dependencies', hasInventory)}
      ${renderStage('04', 'Decide', 'target, architecture, persistence', false)}
      ${renderStage('05', 'Emit later', 'target code after approval', false)}
    </div>
  </section>`;
}

function renderStage(index, title, description, current) {
  return `<div class="stage ${current ? 'current' : ''}">
    <span>${escapeHtml(index)}</span>
    <strong>${escapeHtml(title)}</strong>
    <small>${escapeHtml(description)}</small>
  </div>`;
}

function renderMetricGrid(metrics) {
  return `<div class="grid">${metrics.map(([label, value]) => `<div class="metric"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`).join('')}</div>`;
}

function renderDiagramDebugControls(targetId) {
  return `<div class="debugControls" data-controls-for="${escapeHtml(targetId)}" aria-label="Diagram controls">
    <button type="button" data-diagram-action="fit" data-target="${escapeHtml(targetId)}">Fit</button>
    <button type="button" data-diagram-action="zoomIn" data-target="${escapeHtml(targetId)}">Zoom +</button>
    <button type="button" data-diagram-action="zoomOut" data-target="${escapeHtml(targetId)}">Zoom -</button>
    <button type="button" data-diagram-action="actual" data-target="${escapeHtml(targetId)}">100%</button>
    <button type="button" data-diagram-action="reset" data-target="${escapeHtml(targetId)}">Reset</button>
  </div>`;
}

function renderDiscoveryRunState(job, analysis, domain, inventory, progress) {
  const discovery = domainDiscovery(domain);
  const diagnostics = asArray(domain?.diagnostics);
  const status = job?.status || analysis?.status || (sumInventory(inventory) ? 'COMPLETED' : 'IDLE');
  const issueCount = Number(inventory?.errors || 0) + diagnostics.length;
  const parsedFiles = parsedFileCount(inventory);
  const lastJob = job?.id
    ? `<div class="statusLine">Latest job <code>${escapeHtml(job.id)}</code> · ${escapeHtml(status)} · ${Number(progress) || 0}%</div>`
    : '<div class="statusLine">No active job in this VS Code session. Refresh or run analysis to load the latest persisted evidence.</div>';
  return `<section class="panel">
    <div class="sectionTitle">Run result</div>
    <div class="progress"><span style="width:${Number(progress) || 0}%"></span></div>
    ${renderMetricGrid([
      ['Status', status],
      ['Parsed files', parsedFiles],
      ['Tables/files', discovery.repositories.length],
      ['Data records', discovery.recordNodes.length],
      ['Relations', discovery.relations.length],
      ['Issues', issueCount]
    ])}
    ${lastJob}
  </section>`;
}

function renderInferredPersistence(domain, project) {
  const discovery = domainDiscovery(domain);
  if (!discovery.nodes.length) {
    return `<section class="panel">
      <div class="sectionTitle">Inferred persistence</div>
      <div class="empty">No inferred tables or file-backed repositories are stored yet. Re-run Analyze COBOL Sources with the current backend so Renovatio can persist the DomainModel discovered from COBOL, DB2 and file I/O.</div>
    </section>`;
  }
  if (!discovery.repositories.length) {
    return `<section class="panel">
      <div class="sectionTitle">Inferred persistence</div>
      <div class="empty">No DB2 table or file repository candidates were inferred from this run. Check whether the source uses EXEC SQL, SELECT/FD file definitions, or unsupported persistence patterns.</div>
    </section>`;
  }
  return `<section class="panel">
    <div class="sectionTitle">Inferred persistence</div>
    <p class="sectionLead">Tables and file-backed datasets Renovatio detected from COBOL I/O, with the COBOL record shapes it could bind as candidate columns.</p>
    ${renderPersistenceShapeGaps(discovery)}
    <div class="domainGrid">
      ${discovery.repositories.slice(0, 12).map(repository => renderRepositoryFinding(repository)).join('')}
    </div>
    ${discovery.repositories.length > 12 ? `<div class="statusLine">${discovery.repositories.length - 12} more persistence candidate(s) are available in the ER model view.</div>` : ''}
    <div class="inlineAction">
      ${commandLink('renovatio.openPersistenceModel', [], 'Open ER model')}
    </div>
  </section>`;
}

function renderPersistenceSummary(domain) {
  const discovery = domainDiscovery(domain);
  if (!domain || !discovery.nodes.length) {
    return `<div class="modelMetaBar">
      <span>Persistence not loaded</span>
      <span>Run Analyze COBOL Sources</span>
    </div>`;
  }
  const repositories = discovery.repositories;
  const withFields = repositories.filter(repository => asArray(repository.properties).length).length;
  const unresolved = repositories.length - withFields;
  const mappedRecords = uniqueNodes(repositories.flatMap(repository => asArray(repository.mappedNodes))).length;
  return `<div class="modelMetaBar">
    <span>Tables/files <strong>${repositories.length}</strong></span>
    <span>With fields <strong>${withFields}</strong></span>
    <span>Shape unresolved <strong>${unresolved}</strong></span>
    <span>Mapped records <strong>${mappedRecords}</strong></span>
    <span>Revision ${escapeHtml(domain.revision ?? 0)}</span>
    <span>Saved ${escapeHtml(formatSavedAt(domain.savedAt))}</span>
  </div>`;
}

function renderPersistenceErd(domain, project) {
  const erd = buildPersistenceErd(domain);
  if (!erd.nodes.length) {
    return `<section class="panel">
      <div class="sectionTitle">ER model</div>
      <div class="empty">No ER-style persistence model is available yet.</div>
    </section>`;
  }
  return `<section class="panel">
    <div class="diagramToolbar">
      <div>
        <div class="sectionTitle">ER model</div>
        <p class="sectionLead">A table/file-first view. <code>0 fields</code> means the access point was discovered, but Renovatio has not yet bound a COBOL record/copybook shape as columns.</p>
      </div>
      <div class="diagramLegend" aria-label="ER model legend">
        <span class="legendItem"><span class="legendSwatch maps"></span>shared record/field evidence</span>
        <span class="legendItem"><span class="legendSwatch unresolved"></span>shape unresolved</span>
      </div>
      ${renderDiagramDebugControls('persistenceErd')}
    </div>
    <div class="erdViewport" data-debug-viewport="true">
      <svg id="persistenceErd" class="erdSvg" data-debug-diagram="true" viewBox="0 0 ${erd.width} ${erd.height}" role="img" aria-label="Inferred persistence ER diagram">
        ${erd.edges.map(renderErdEdge).join('')}
        ${erd.nodes.map(renderErdTable).join('')}
      </svg>
    </div>
    ${erd.notice ? `<div class="statusLine">${escapeHtml(erd.notice)}</div>` : ''}
  </section>`;
}

function buildPersistenceErd(domain) {
  const discovery = domainDiscovery(domain);
  const repositories = [...discovery.repositories]
    .sort(compareRepositoriesForDiagram)
    .slice(0, 12);
  if (!repositories.length) {
    return { width: 1080, height: 220, nodes: [], edges: [], notice: '' };
  }
  const width = 1080;
  const cardWidth = 326;
  const cardHeight = 178;
  const gapX = 24;
  const gapY = 24;
  const left = 18;
  const top = 18;
  const columns = 3;
  const nodes = repositories.map((repository, index) => ({
    source: repository,
    x: left + (index % columns) * (cardWidth + gapX),
    y: top + Math.floor(index / columns) * (cardHeight + gapY),
    width: cardWidth,
    height: cardHeight
  }));
  const byId = new Map(nodes.map(node => [node.source.logicalId || node.source.id, node]));
  const edges = [];
  const edgeIds = new Set();
  for (const node of nodes) {
    for (const related of relatedPersistenceNodes(node.source, repositories)) {
      const to = byId.get(related.logicalId || related.id);
      if (!to || to === node) continue;
      const id = [node.source.logicalId || node.source.id, to.source.logicalId || to.source.id].sort().join('|');
      if (edgeIds.has(id)) continue;
      edgeIds.add(id);
      edges.push({ from: node, to });
    }
  }
  const rows = Math.ceil(nodes.length / columns);
  const height = top * 2 + rows * cardHeight + Math.max(0, rows - 1) * gapY;
  const notice = discovery.repositories.length > repositories.length
    ? `Showing ${repositories.length} of ${discovery.repositories.length} inferred persistence resources. Open the list below for the rest.`
    : '';
  return { width, height, nodes, edges, notice };
}

function relatedPersistenceNodes(repository, repositories) {
  const mappedIds = new Set(asArray(repository.mappedNodes).map(node => node.id).filter(Boolean));
  const fieldNames = new Set(asArray(repository.properties).map(property => normalizeName(property.name)).filter(Boolean));
  return repositories.filter(candidate => {
    if (candidate === repository) return false;
    if (asArray(candidate.mappedNodes).some(node => mappedIds.has(node.id))) return true;
    return asArray(candidate.properties).some(property => fieldNames.has(normalizeName(property.name)));
  }).slice(0, 3);
}

function renderErdEdge(edge) {
  const x1 = edge.from.x + edge.from.width;
  const y1 = edge.from.y + edge.from.height / 2;
  const x2 = edge.to.x;
  const y2 = edge.to.y + edge.to.height / 2;
  const leftToRight = x1 < x2;
  const startX = leftToRight ? x1 : edge.from.x;
  const endX = leftToRight ? x2 : edge.to.x + edge.to.width;
  return `<path class="erdLink" data-edge="true" data-from="${escapeHtml(diagramNodeId(edge.from))}" data-to="${escapeHtml(diagramNodeId(edge.to))}" d="M ${startX} ${y1} C ${startX + (leftToRight ? 40 : -40)} ${y1}, ${endX + (leftToRight ? -40 : 40)} ${y2}, ${endX} ${y2}" />`;
}

function renderErdTable(node) {
  const repository = node.source;
  const properties = asArray(repository.properties);
  const unresolved = !properties.length;
  const fields = properties.slice(0, 7);
  const title = repository.name || repository.id;
  return `<g class="debugNode" data-node-id="${escapeHtml(diagramNodeId(node))}" data-x="${node.x}" data-y="${node.y}" data-width="${node.width}" data-height="${node.height}">
    <title>${escapeHtml(title)}</title>
    <rect class="erdTable${unresolved ? ' unresolved' : ''}" x="${node.x}" y="${node.y}" width="${node.width}" height="${node.height}" rx="3" />
    <rect class="erdHeader" x="${node.x}" y="${node.y}" width="${node.width}" height="38" rx="3" />
    ${renderSvgLines(title, node.x + 12, node.y + 23, 'erdTitle', 34, 1, 14, { editable: true })}
    <text class="erdKind" x="${node.x + 12}" y="${node.y + 57}">${escapeHtml(persistenceKind(repository))} · ${properties.length} field${properties.length === 1 ? '' : 's'} · ${asArray(repository.users).length} use${asArray(repository.users).length === 1 ? '' : 's'}</text>
    ${fields.length ? fields.map((property, index) =>
      `<text class="erdField" x="${node.x + 14}" y="${node.y + 80 + index * 13}">${escapeHtml(compactField(property))}</text>`).join('') : renderSvgLines('Shape unresolved. No bound COBOL record fields yet.', node.x + 14, node.y + 82, 'erdMeta', 44, 3, 14)}
    ${properties.length > fields.length ? `<text class="erdMeta" x="${node.x + 14}" y="${node.y + 80 + fields.length * 13}">+${properties.length - fields.length} more fields</text>` : ''}
  </g>`;
}

function renderPersistenceRepositoryList(domain) {
  const discovery = domainDiscovery(domain);
  if (!discovery.repositories.length) return '';
  return `<section class="panel">
    <div class="sectionTitle">Tables / files</div>
    <div class="domainGrid">
      ${discovery.repositories.map(repository => renderRepositoryFinding(repository)).join('')}
    </div>
  </section>`;
}

function renderRepositoryFinding(repository) {
  const properties = repository.properties;
  const mappedNames = repository.mappedNodes.map(node => node.name || node.id);
  const users = repository.users.map(node => node.name || node.id);
  const evidence = mergedEvidence([repository, ...repository.mappedNodes]);
  return `<div class="nodeCard tableCard">
    <div class="artifactHeader">
      <strong>${escapeHtml(repository.name || repository.id)}</strong>
      <span>${properties.length} field${properties.length === 1 ? '' : 's'}</span>
    </div>
    <div class="nodeMeta">
      <span class="pill">${escapeHtml(persistenceKind(repository))}</span>
      <span class="pill">${Math.round(Number(repository.confidence || 0) * 100)}% confidence</span>
      ${repository.instances > 1 ? `<span class="pill">${repository.instances} references</span>` : ''}
      ${mappedNames.length ? `<span class="pill">maps to ${escapeHtml(mappedNames.slice(0, 2).join(', '))}</span>` : '<span class="pill">unmapped record</span>'}
    </div>
    ${properties.length ? renderPropertyGrid(properties, 18) : `<div class="empty">${escapeHtml(unresolvedShapeReason(repository))}</div>`}
    ${users.length ? `<div class="statusLine">Used by ${escapeHtml(users.slice(0, 4).join(', '))}${users.length > 4 ? ` and ${users.length - 4} more` : ''}</div>` : ''}
    ${renderEvidence(evidence)}
  </div>`;
}

function renderPersistenceDiagram(domain) {
  const diagram = buildPersistenceDiagram(domain);
  if (!diagram.nodes.length) return '';
  return `<div class="artifact">
    <div class="diagramToolbar">
      <div class="artifactHeader"><strong>Table map</strong><span>${diagram.repositories.length} shown</span></div>
      <div class="diagramLegend" aria-label="Persistence diagram legend">
        <span class="legendItem"><span class="legendSwatch"></span>USES</span>
        <span class="legendItem"><span class="legendSwatch maps"></span>MAPS_TO</span>
        <span class="legendItem"><span class="legendSwatch unresolved"></span>shape unresolved</span>
      </div>
      ${renderDiagramDebugControls('persistenceMap')}
    </div>
    <div class="diagramViewport" data-debug-viewport="true">
      <svg id="persistenceMap" class="diagramSvg" data-debug-diagram="true" viewBox="0 0 ${diagram.width} ${diagram.height}" role="img" aria-label="Inferred persistence table diagram">
        <defs>
          <marker id="persistenceArrowUses" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--vscode-focusBorder)" />
          </marker>
          <marker id="persistenceArrowMaps" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--vscode-charts-yellow, #cca700)" />
          </marker>
        </defs>
        <text class="diagramColumnLabel" x="24" y="30">Programs / use cases</text>
        <text class="diagramColumnLabel" x="386" y="30">Inferred tables / files</text>
        <text class="diagramColumnLabel" x="746" y="30">COBOL source records</text>
        ${diagram.edges.map(edge => renderDiagramEdge(edge, 'persistence')).join('')}
        ${diagram.useCases.map(node => renderDiagramNode(node, 'use')).join('')}
        ${diagram.repositories.map(node => renderDiagramNode(node, 'repo')).join('')}
        ${diagram.records.map(node => renderDiagramNode(node, 'record')).join('')}
      </svg>
    </div>
    ${diagram.notice ? `<div class="statusLine">${escapeHtml(diagram.notice)}</div>` : ''}
  </div>`;
}

function renderPersistenceShapeGaps(discovery) {
  const unresolved = discovery.repositories
    .filter(repository => !asArray(repository.properties).length)
    .slice(0, 8);
  if (!unresolved.length) return '';
  return `<div class="artifact">
    <div class="artifactHeader"><strong>Tables without inferred fields</strong><span>${unresolved.length}</span></div>
    <div class="shapeGapList">
      ${unresolved.map(repository => `<div class="shapeGap">
        <strong>${escapeHtml(repository.name || repository.id)}</strong>
        <span>${escapeHtml(unresolvedShapeReason(repository))}</span>
      </div>`).join('')}
    </div>
    ${discovery.repositories.filter(repository => !asArray(repository.properties).length).length > unresolved.length
      ? `<div class="statusLine">${discovery.repositories.filter(repository => !asArray(repository.properties).length).length - unresolved.length} more unresolved table shape(s) are available in the Domain Model view.</div>`
      : ''}
  </div>`;
}

function renderDataShapeFindings(domain, project) {
  const discovery = domainDiscovery(domain);
  const candidates = discovery.recordNodes
    .filter(node => asArray(node.properties).length)
    .slice(0, 10);
  if (!discovery.nodes.length) {
    return `<section class="panel">
      <div class="sectionTitle">Records and entities</div>
      <div class="empty">No COBOL record shapes are persisted yet.</div>
    </section>`;
  }
  if (!candidates.length) {
    return `<section class="panel">
      <div class="sectionTitle">Records and entities</div>
      <div class="empty">Domain nodes exist, but no structured fields were inferred. The next useful gap is improving copybook/data-division extraction for this source set.</div>
    </section>`;
  }
  return `<section class="panel">
    <div class="sectionTitle">Records and entities</div>
    <p class="sectionLead">COBOL groups/value objects projected into neutral domain shapes. Use this to validate names, fields and evidence before choosing architecture or target language.</p>
    <div class="domainGrid">
      ${candidates.map(node => `<div class="nodeCard">
        <div class="artifactHeader">
          <strong>${escapeHtml(node.name || node.id)}</strong>
          <span>${asArray(node.properties).length} field${asArray(node.properties).length === 1 ? '' : 's'}</span>
        </div>
        <div class="nodeMeta">
          <span class="pill">${escapeHtml(humanize(node.kind || 'record'))}</span>
          <span class="pill">${Math.round(Number(node.confidence || 0) * 100)}% confidence</span>
        </div>
        ${renderPropertyGrid(asArray(node.properties), 12)}
        ${renderEvidence(asArray(node.evidence))}
      </div>`).join('')}
    </div>
    ${discovery.recordNodes.length > candidates.length ? `<div class="statusLine">${discovery.recordNodes.length - candidates.length} more data shape(s) are available in the Domain Model view.</div>` : ''}
  </section>`;
}

function renderRelationshipFindings(domain) {
  const discovery = domainDiscovery(domain);
  if (!discovery.relations.length) {
    return `<section class="panel">
      <div class="sectionTitle">Relationship map</div>
      <div class="empty">No relationships persisted yet.</div>
    </section>`;
  }
  const important = discovery.relations.filter(relation =>
    sameKind(relation.kind, 'USES') || sameKind(relation.kind, 'MAPS_TO') || sameKind(relation.kind, 'CONTAINS'));
  const rows = (important.length ? important : discovery.relations).slice(0, 18);
  return `<section class="panel">
    <div class="sectionTitle">Relationship map</div>
    <div class="relationList">${rows.map(relation => {
      const from = discovery.nodeById.get(relation.fromId);
      const to = discovery.nodeById.get(relation.toId);
      return `<div class="relationRow">
        <strong>${escapeHtml(from?.name || relation.fromId)}</strong>
        <span class="relationKind">${escapeHtml(humanize(relation.kind || 'relates'))}</span>
        <strong>${escapeHtml(to?.name || relation.toId)}</strong>
      </div>`;
    }).join('')}</div>
    ${discovery.relations.length > rows.length ? `<div class="statusLine">${discovery.relations.length - rows.length} more relation(s) are available in the Domain Model view.</div>` : ''}
  </section>`;
}

function renderAnalysisIssues(domain, inventory) {
  const diagnostics = asArray(domain?.diagnostics);
  const errors = Number(inventory?.errors || 0);
  const suggestions = asArray(domain?.suggestions);
  if (!diagnostics.length && !errors && !suggestions.length) {
    return `<section class="panel">
      <div class="sectionTitle">Review signals</div>
      <div class="empty">No parser errors, DomainModel diagnostics or pending domain suggestions are loaded.</div>
    </section>`;
  }
  return `<section class="panel">
    <div class="sectionTitle">Review signals</div>
    <div class="relationList">
      ${errors ? `<div class="warning"><strong>Parser errors</strong><br>${escapeHtml(errors)} error(s) were reported by the inventory.</div>` : ''}
      ${diagnostics.map(diagnostic => `<div class="warning"><strong>${escapeHtml(diagnostic.code)}</strong> · ${escapeHtml(diagnostic.targetId)}<br>${escapeHtml(diagnostic.message)}</div>`).join('')}
      ${suggestions.map(suggestion => `<div class="warning"><strong>${escapeHtml(suggestion.name || suggestion.targetId)}</strong> · ${escapeHtml(suggestion.status || 'pending')}<br>${escapeHtml(suggestion.targetType || 'domain suggestion')}</div>`).join('')}
    </div>
  </section>`;
}

function renderPropertyGrid(properties, limit) {
  const visible = asArray(properties).slice(0, limit);
  if (!visible.length) return '';
  const hidden = properties.length - visible.length;
  return `<div class="fieldGrid">
    ${visible.map(property => `<div class="field">
      <strong>${escapeHtml(property.name || 'field')}</strong>
      <span>${escapeHtml(property.type || 'unknown')}${property.required === false ? ' optional' : ''}</span>
    </div>`).join('')}
    ${hidden > 0 ? `<div class="field"><strong>${hidden} more</strong><span>fields</span></div>` : ''}
  </div>`;
}

function domainDiscovery(domain) {
  const model = domain?.model || {};
  const nodes = asArray(model.nodes);
  const relations = asArray(model.relations);
  const nodeById = new Map(nodes.map(node => [node.id, node]));
  const repositoryNodes = nodes
    .filter(node => sameKind(node.kind, 'REPOSITORY'))
    .map(repository => {
      const mappedNodes = relations
        .filter(relation => sameKind(relation.kind, 'MAPS_TO') && relation.fromId === repository.id)
        .map(relation => nodeById.get(relation.toId))
        .filter(Boolean);
      const users = relations
        .filter(relation => sameKind(relation.kind, 'USES') && relation.toId === repository.id)
        .map(relation => nodeById.get(relation.fromId))
        .filter(Boolean);
      return {
        ...repository,
        mappedNodes,
        users,
        properties: mergeProperties([asArray(repository.properties), ...mappedNodes.map(node => asArray(node.properties))].flat())
      };
    });
  const repositories = mergeRepositories(repositoryNodes);
  const recordNodes = nodes
    .filter(node => sameKind(node.kind, 'ENTITY') || sameKind(node.kind, 'VALUE_OBJECT') || sameKind(node.kind, 'AGGREGATE'))
    .sort((left, right) => asArray(right.properties).length - asArray(left.properties).length
      || String(left.name || left.id).localeCompare(String(right.name || right.id)));
  return { nodes, relations, nodeById, repositories, repositoryNodes, recordNodes };
}

function mergeRepositories(repositories) {
  const byResource = new Map();
  for (const repository of repositories) {
    const key = `${persistenceKind(repository)}:${String(repository.name || repository.id).toUpperCase()}`;
    const current = byResource.get(key);
    if (!current) {
      byResource.set(key, { ...repository, instances: 1 });
      continue;
    }
    byResource.set(key, {
      ...current,
      confidence: Math.max(Number(current.confidence || 0), Number(repository.confidence || 0)),
      instances: Number(current.instances || 1) + 1,
      properties: mergeProperties([...asArray(current.properties), ...asArray(repository.properties)]),
      mappedNodes: uniqueNodes([...asArray(current.mappedNodes), ...asArray(repository.mappedNodes)]),
      users: uniqueNodes([...asArray(current.users), ...asArray(repository.users)]),
      evidence: mergedEvidence([current, repository])
    });
  }
  return [...byResource.values()].sort((left, right) =>
    String(left.name || left.id).localeCompare(String(right.name || right.id)));
}

function uniqueNodes(nodes) {
  const byId = new Map();
  for (const node of asArray(nodes)) {
    const key = node?.id || node?.name;
    if (key && !byId.has(key)) byId.set(key, node);
  }
  return [...byId.values()];
}

function mergeProperties(properties) {
  const byName = new Map();
  for (const property of asArray(properties)) {
    const key = String(property?.name || '').toUpperCase();
    if (!key || byName.has(key)) continue;
    byName.set(key, property);
  }
  return [...byName.values()].sort((left, right) => String(left.name).localeCompare(String(right.name)));
}

function mergedEvidence(nodes) {
  const evidenceByRef = new Map();
  for (const node of nodes) {
    for (const item of asArray(node?.evidence)) {
      const key = item.sourceRef || item.provenance || JSON.stringify(item);
      if (!evidenceByRef.has(key)) evidenceByRef.set(key, item);
    }
    for (const property of asArray(node?.properties)) {
      for (const item of asArray(property?.evidence)) {
        const key = item.sourceRef || item.provenance || JSON.stringify(item);
        if (!evidenceByRef.has(key)) evidenceByRef.set(key, item);
      }
    }
  }
  return [...evidenceByRef.values()];
}

function persistenceKind(repository) {
  const name = String(repository?.name || repository?.id || '');
  if (/DB2 table/i.test(name)) return 'DB2 table';
  if (/file record/i.test(name)) return 'File record';
  return 'Repository';
}

function parsedFileCount(inventory) {
  const source = Number(inventory?.sourceFiles ?? inventory?.programs ?? 0) || 0;
  const copybooks = Number(inventory?.copybooks ?? 0) || 0;
  const jcl = Number(inventory?.jcl ?? inventory?.jclFiles ?? 0) || 0;
  const total = source + copybooks + jcl;
  return total || sumInventory(inventory);
}

function renderInventory(inventory) {
  const entries = Object.entries(inventory || {});
  if (!entries.length) {
    return '<div class="empty">No inventory is loaded yet. Run analysis or refresh after the parser job completes.</div>';
  }
  return renderMetricGrid(entries.map(([key, value]) => [humanize(key), value]));
}

function renderCobolArtifacts(source) {
  if (!source.root) {
    return `<section class="panel">
      <div class="sectionTitle">Source evidence</div>
      <div class="empty">No COBOL scan root is configured yet.</div>
    </section>`;
  }
  if (!source.exists) {
    return `<section class="panel">
      <div class="sectionTitle">Source evidence</div>
      <div class="empty">Scan root does not exist: <code>${escapeHtml(source.root)}</code>.</div>
    </section>`;
  }
  if (!source.files.length) {
    return `<section class="panel">
      <div class="sectionTitle">Source evidence</div>
      <div class="empty">No COBOL, copybook or JCL files were found in the configured scan root.</div>
    </section>`;
  }
  return `<section class="panel">
    <div class="sectionTitle">Source evidence</div>
    <p class="sectionLead">Representative parsed inputs. Open any file in the native VS Code editor for source-level review.</p>
    <div class="fileStrip">${source.files.slice(0, 12).map(file => `<span class="fileChip">${escapeHtml(file.relativePath)}</span>`).join('')}</div>
    ${renderFileList(source.files, 'Open in editor')}
  </section>`;
}

function renderFutureOutput(generated) {
  const root = generated.root || 'Not configured';
  const notice = generated.notice ? `<div class="warning">${escapeHtml(generated.notice)}</div>` : '';
  if (generated.warning) {
    return `<section class="panel">
      <div class="sectionTitle">Emission planning</div>
      <div class="warning">${escapeHtml(generated.warning)}</div>
      <div class="kv"><span>Future output root</span><code>${escapeHtml(root)}</code></div>
      <p class="muted">Generation is intentionally downstream of analysis, decisions and approval.</p>
    </section>`;
  }
  return `<section class="panel">
    <div class="sectionTitle">Emission planning</div>
    ${notice}
    <div class="kv"><span>Future output root</span><code>${escapeHtml(root)}</code></div>
    <div class="empty">This folder is not used by discovery. Renovatio first builds inventory, relationships, neutral IR and decision evidence; target code is emitted later after architecture approval.</div>
  </section>`;
}

function renderDomainModelOverview(domain) {
  if (!domain) {
    return `<div class="modelMetaBar"><span>DomainModel not loaded</span><span>Run discovery or refresh Renovatio</span></div>`;
  }
  const model = domain.model || {};
  const nodes = asArray(model.nodes);
  const relations = asArray(model.relations);
  return `<div class="modelMetaBar" title="Canonical hash: ${escapeHtml(domain.canonicalHash || 'not saved')}">
    <span>Revision ${escapeHtml(domain.revision ?? 0)}</span>
    <span>${nodes.length} nodes</span>
    <span>${relations.length} relations</span>
    <span>Saved ${escapeHtml(formatSavedAt(domain.savedAt))}</span>
  </div>`;
}

function formatSavedAt(value) {
  if (!value) return 'not saved yet';
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    if (year && month && day) {
      return `${padDate(year, 4)}-${padDate(month)}-${padDate(day)} ${padDate(hour)}:${padDate(minute)}:${padDate(second)}`;
    }
  }
  return String(value).replace(/[TZ]/g, ' ').replace(/\.\d+/, '').trim();
}

function padDate(value, width = 2) {
  return String(value).padStart(width, '0');
}

function renderDomainModelDiagram(domain) {
  const diagram = buildDomainDiagram(domain);
  if (!diagram.nodes.length) {
    return `<section class="panel">
      <div class="sectionTitle">Visual model</div>
      <div class="empty">No graphical model is available yet. Run Analyze COBOL Sources to seed domain nodes, persistence resources and relations.</div>
    </section>`;
  }
  return `<section class="panel">
    <div class="diagramToolbar">
      <div>
        <div class="sectionTitle">Visual model</div>
        <p class="sectionLead">Programs/use cases, inferred persistence and COBOL data shapes connected by discovered relations.</p>
      </div>
      <div class="diagramLegend" aria-label="Diagram legend">
        <span class="legendItem"><span class="legendSwatch"></span>USES</span>
        <span class="legendItem"><span class="legendSwatch maps"></span>MAPS_TO</span>
      </div>
      ${renderDiagramDebugControls('domainDiagram')}
    </div>
    <div class="diagramViewport" data-debug-viewport="true">
      <svg id="domainDiagram" class="diagramSvg" data-debug-diagram="true" viewBox="0 0 ${diagram.width} ${diagram.height}" role="img" aria-label="Domain model entity relation diagram">
        <defs>
          <marker id="arrowUses" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--vscode-focusBorder)" />
          </marker>
          <marker id="arrowMaps" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--vscode-charts-yellow, #cca700)" />
          </marker>
        </defs>
        <text class="diagramColumnLabel" x="24" y="30">Programs / use cases</text>
        <text class="diagramColumnLabel" x="386" y="30">Tables / files</text>
        <text class="diagramColumnLabel" x="746" y="30">COBOL records / entities</text>
        ${diagram.edges.map(renderDiagramEdge).join('')}
        ${diagram.useCases.map(node => renderDiagramNode(node, 'use')).join('')}
        ${diagram.repositories.map(node => renderDiagramNode(node, 'repo')).join('')}
        ${diagram.records.map(node => renderDiagramNode(node, 'record')).join('')}
      </svg>
    </div>
    ${diagram.evidence.length ? `<div class="artifact">
      <div class="artifactHeader"><strong>Source evidence</strong><span>${diagram.evidence.length}</span></div>
      <div class="fileStrip">${diagram.evidence.slice(0, 10).map(item =>
        openEvidenceLink(item.sourceRef || item.provenance, item.sourceRef || 'Open evidence')).join('')}</div>
    </div>` : ''}
    ${diagram.notice ? `<div class="statusLine">${escapeHtml(diagram.notice)}</div>` : ''}
  </section>`;
}

function renderInferredClassModel(domain, project) {
  const projection = classModelProjection(domain);
  if (!projection.items.length) {
    return `<section class="panel">
      <div class="sectionTitle">UML class model</div>
      <div class="empty">No class or component candidates are available yet.</div>
    </section>`;
  }
  return `<section class="panel">
    <div class="sectionTitle">UML class model</div>
    <p class="sectionLead">Class and component projection generated from Renovatio's persisted DomainModel.</p>
    <div class="modelMetaBar">
      <span>Packages <strong>${projection.packages.length}</strong></span>
      <span>Classes <strong>${projection.items.length}</strong></span>
      <span>Relations <strong>${projection.relations.length}</strong></span>
    </div>
  </section>`;
}

async function writeClassDiagramArtifact(project, domain) {
  const rootPath = workspaceSettings(project).workspaceFolderPath || project?.workspacePath || extensionContext.globalStorageUri.fsPath;
  const diagramsDir = vscode.Uri.joinPath(vscode.Uri.file(rootPath), '.renovatio', 'diagrams');
  await vscode.workspace.fs.createDirectory(diagramsDir);
  const uri = vscode.Uri.joinPath(diagramsDir, `${sanitizeFileName(project?.name || 'renovatio')}-uml-class-model.md`);
  await vscode.workspace.fs.writeFile(uri, Buffer.from(renderClassDiagramMarkdown(project, domain), 'utf8'));
  return { uri };
}

function renderClassDiagramMarkdown(project, domain) {
  const projection = classModelProjection(domain);
  const generatedAt = new Date().toISOString();
  return `# Renovatio UML Class Model

Project: ${project?.name || 'Renovatio'}
Generated: ${generatedAt}

This file is generated by the Renovatio VS Code extension from the persisted DomainModel. Render it with VS Code Markdown Preview plus a Mermaid-capable extension.

\`\`\`mermaid
${renderMermaidClassDiagram(projection)}
\`\`\`
`;
}

async function writeDrawioDomainModelArtifact(project, domain) {
  const rootPath = workspaceSettings(project).workspaceFolderPath || project?.workspacePath || extensionContext.globalStorageUri.fsPath;
  const diagramsDir = vscode.Uri.joinPath(vscode.Uri.file(rootPath), '.renovatio', 'diagrams');
  await vscode.workspace.fs.createDirectory(diagramsDir);
  const uri = vscode.Uri.joinPath(diagramsDir, `${sanitizeFileName(project?.name || 'renovatio')}-domain-model.drawio`);
  await vscode.workspace.fs.writeFile(uri, Buffer.from(renderDrawioDomainModel(project, domain), 'utf8'));
  return { uri };
}

async function writeDrawioEntityModelArtifact(project, domain) {
  const rootPath = workspaceSettings(project).workspaceFolderPath || project?.workspacePath || extensionContext.globalStorageUri.fsPath;
  const diagramsDir = vscode.Uri.joinPath(vscode.Uri.file(rootPath), '.renovatio', 'diagrams');
  await vscode.workspace.fs.createDirectory(diagramsDir);
  const uri = vscode.Uri.joinPath(diagramsDir, `${sanitizeFileName(project?.name || 'renovatio')}-entity-model.drawio`);
  await vscode.workspace.fs.writeFile(uri, Buffer.from(renderDrawioEntityModel(project, domain), 'utf8'));
  return { uri };
}

async function writeDrawioPersistenceErdArtifact(project, domain) {
  const rootPath = workspaceSettings(project).workspaceFolderPath || project?.workspacePath || extensionContext.globalStorageUri.fsPath;
  const diagramsDir = vscode.Uri.joinPath(vscode.Uri.file(rootPath), '.renovatio', 'diagrams');
  await vscode.workspace.fs.createDirectory(diagramsDir);
  const uri = vscode.Uri.joinPath(diagramsDir, `${sanitizeFileName(project?.name || 'renovatio')}-persistence-der.drawio`);
  await vscode.workspace.fs.writeFile(uri, Buffer.from(renderDrawioPersistenceErd(project, domain), 'utf8'));
  return { uri };
}

async function writeNativeDomainDiagramArtifact(project, domain) {
  const uri = await nativeDiagramUri(project, 'domain-model.renovatio-domain.json');
  await vscode.workspace.fs.writeFile(uri, Buffer.from(JSON.stringify(nativeDomainDocument(domain), null, 2) + '\n', 'utf8'));
  return { uri };
}

async function writeNativePersistenceDiagramArtifact(project, domain) {
  const uri = await nativeDiagramUri(project, 'persistence-model.renovatio-domain.json');
  await vscode.workspace.fs.writeFile(uri, Buffer.from(JSON.stringify(nativePersistenceDocument(domain), null, 2) + '\n', 'utf8'));
  return { uri };
}

async function writeNativeArchitectureDiagramArtifact(project, domain) {
  const uri = await nativeDiagramUri(project, 'architecture.renovatio-arch.json');
  await vscode.workspace.fs.writeFile(uri, Buffer.from(JSON.stringify(nativeArchitectureDocument(project, domain), null, 2) + '\n', 'utf8'));
  return { uri };
}

async function nativeDiagramUri(project, suffix) {
  const rootPath = workspaceSettings(project).workspaceFolderPath || project?.workspacePath || extensionContext.globalStorageUri.fsPath;
  const diagramsDir = vscode.Uri.joinPath(vscode.Uri.file(rootPath), '.renovatio', 'diagrams');
  await vscode.workspace.fs.createDirectory(diagramsDir);
  return vscode.Uri.joinPath(diagramsDir, `${sanitizeFileName(project?.name || 'renovatio')}-${suffix}`);
}

function nativeDomainDocument(domain) {
  const model = domain?.model || {};
  const allNodes = asArray(model.nodes);
  const selectedIds = new Set();
  const selectedNodes = [];
  const addNode = node => {
    if (!node?.id || selectedIds.has(node.id)) return;
    selectedIds.add(node.id);
    selectedNodes.push(node);
  };
  allNodes
    .filter(node => sameKind(node.kind, 'USE_CASE') || sameKind(node.kind, 'SERVICE'))
    .slice(0, 80)
    .forEach(addNode);
  allNodes
    .filter(node => sameKind(node.kind, 'AGGREGATE') || sameKind(node.kind, 'ENTITY') || sameKind(node.kind, 'VALUE_OBJECT'))
    .sort((left, right) => asArray(right.properties).length - asArray(left.properties).length
      || String(left.name || left.id).localeCompare(String(right.name || right.id)))
    .slice(0, 140)
    .forEach(addNode);
  allNodes
    .filter(node => sameKind(node.kind, 'REPOSITORY'))
    .slice(0, 80)
    .forEach(addNode);
  if (!selectedNodes.length) {
    allNodes.slice(0, 260).forEach(addNode);
  }
  const relations = asArray(model.relations)
    .filter(relation => selectedIds.has(relation.fromId) && selectedIds.has(relation.toId))
    .slice(0, 420);
  return {
    revision: domain?.revision || 0,
    savedAt: domain?.savedAt,
    nodes: selectedNodes,
    relations,
    invariants: asArray(model.invariants),
    layout: nativeLayoutForNodes(selectedNodes, 280, 160),
    excludedNodeIds: []
  };
}

function nativePersistenceDocument(domain) {
  const discovery = domainDiscovery(domain);
  const repositories = discovery.repositories.slice(0, 80).map(repository => ({
    id: String(repository.logicalId || repository.id || repository.name),
    kind: 'REPOSITORY',
    name: repository.name || repository.id,
    properties: asArray(repository.properties),
    confidence: repository.confidence,
    origin: repository.origin,
    evidence: repository.evidence
  }));
  const records = uniqueNodes(discovery.repositories.flatMap(repository => asArray(repository.mappedNodes))).slice(0, 80);
  const nodes = [...repositories, ...records];
  const relations = [];
  const relationIds = new Set();
  for (const repository of discovery.repositories) {
    const repositoryId = String(repository.logicalId || repository.id || repository.name);
    for (const record of asArray(repository.mappedNodes)) {
      const id = `maps:${repositoryId}->${record.id}`;
      if (relationIds.has(id)) continue;
      relationIds.add(id);
      relations.push({
        id,
        fromId: repositoryId,
        toId: record.id,
        kind: 'MAPS_TO',
        sourceCardinality: 'ONE',
        targetCardinality: 'ONE'
      });
    }
  }
  return {
    revision: domain?.revision || 0,
    savedAt: domain?.savedAt,
    nodes,
    relations,
    invariants: [],
    layout: nativeLayoutForNodes(nodes, 320, 150),
    excludedNodeIds: []
  };
}

function nativeArchitectureDocument(project, domain) {
  const projection = classModelProjection(domain);
  const canvas = projection.items.map((item, index) => ({
    id: item.id,
    layer: nativeArchitectureLayer(item.packageName),
    kind: 'COMPONENT',
    label: item.name || item.id,
    packageName: nativeArchitecturePackage(project, item.packageName),
    className: item.name || item.id,
    componentId: item.sourceId || item.logicalId || item.id,
    x: 320 + (index % 3) * 260,
    y: Math.floor(index / 3) * 150 + nativeArchitectureLayerOffset(item.packageName)
  }));
  return {
    profile: {
      packageRoots: {
        controller: `${nativeTargetPackage(project)}.adapter.in`,
        service: `${nativeTargetPackage(project)}.application`,
        model: `${nativeTargetPackage(project)}.domain`,
        persistence: `${nativeTargetPackage(project)}.adapter.out.persistence`
      },
      suffixes: {
        controller: 'Controller',
        service: 'Service',
        model: '',
        persistence: 'Repository'
      },
      dependencyRules: [
        { fromLayer: 'controller', toLayer: 'service', allowed: true, reason: 'Inbound adapters call application services.' },
        { fromLayer: 'service', toLayer: 'model', allowed: true, reason: 'Application services orchestrate domain behavior.' },
        { fromLayer: 'service', toLayer: 'persistence', allowed: true, reason: 'Application services use outbound persistence ports/adapters.' },
        { fromLayer: 'model', toLayer: 'controller', allowed: false, reason: 'Domain model must not depend on inbound adapters.' },
        { fromLayer: 'persistence', toLayer: 'controller', allowed: false, reason: 'Persistence adapters must not depend on controllers.' }
      ],
      layout: Object.fromEntries(canvas.map(node => [node.id, { x: node.x, y: node.y }])),
      excludedNodeIds: []
    },
    canvas: canvas.map(({ x, y, ...node }) => node),
    dependencyDiagnostics: []
  };
}

function nativeLayoutForNodes(nodes, columnWidth, rowHeight) {
  return Object.fromEntries(asArray(nodes).map((node, index) => [
    String(node.id || node.name || `node:${index}`),
    { x: 80 + (index % 3) * columnWidth, y: 90 + Math.floor(index / 3) * rowHeight }
  ]));
}

function nativeArchitectureLayer(packageName) {
  const normalized = String(packageName || '').toLowerCase();
  if (normalized.includes('application')) return 'service';
  if (normalized.includes('infrastructure') || normalized.includes('persistence')) return 'persistence';
  return 'model';
}

function nativeArchitectureLayerOffset(packageName) {
  const layer = nativeArchitectureLayer(packageName);
  if (layer === 'service') return 220;
  if (layer === 'persistence') return 440;
  return 40;
}

function nativeArchitecturePackage(project, packageName) {
  return `${nativeTargetPackage(project)}.${String(packageName || 'domain').replace(/[_-]+/g, '.')}`;
}

function nativeTargetPackage(project) {
  return workspaceSettings(project).targetPackage || 'com.example.modernized';
}

function renderDrawioDomainModel(project, domain) {
  return renderDrawioUmlPackageModel(project, classModelProjection(domain), `${project?.name || 'Renovatio'} UML domain model`);
}

function renderDrawioEntityModel(project, domain) {
  return renderDrawioUmlPackageModel(project, entityModelProjection(domain), `${project?.name || 'Renovatio'} UML entity model`);
}

function renderDrawioUmlPackageModel(project, projection, diagramName) {
  const packageWidth = 360;
  const gap = 36;
  const margin = 40;
  const packageHeader = 44;
  const classInset = 20;
  const columns = projection.packages.map(packageName => ({
    packageName,
    items: projection.items.filter(item => item.packageName === packageName)
  })).filter(group => group.items.length);
  const cells = [
    '<mxCell id="0"/>',
    '<mxCell id="1" parent="0"/>'
  ];
  const classCellById = new Map();
  let maxHeight = 260;
  if (!columns.length) {
    cells.push(renderDrawioVertex({
      id: 'renovatio_empty_model',
      parent: '1',
      value: '<b>No domain model yet</b><br/>Run Renovatio analysis to persist COBOL programs, records, repositories and relations.',
      style: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#1e1e1e;strokeColor=#5f6a6a;fontColor=#d4d4d4;align=left;spacing=14;',
      x: margin,
      y: margin,
      width: 420,
      height: 110
    }));
  }
  columns.forEach((group, columnIndex) => {
    const packageId = drawioCellId(`pkg_${group.packageName}`);
    const x = margin + columnIndex * (packageWidth + gap);
    let y = packageHeader + 18;
    const planned = group.items.map(item => {
      const height = drawioClassHeight(item);
      const plannedItem = { item, y, height };
      y += height + 18;
      return plannedItem;
    });
    const packageHeight = Math.max(220, y + 22);
    maxHeight = Math.max(maxHeight, margin + packageHeight + margin);
    cells.push(renderDrawioVertex({
      id: packageId,
      parent: '1',
      value: `${formatPackageName(group.packageName)} package`,
      style: 'swimlane;html=1;whiteSpace=wrap;rounded=1;collapsible=1;recursiveResize=0;startSize=32;fillColor=#1e1e1e;strokeColor=#3794ff;fontColor=#d4d4d4;fontStyle=1;spacingLeft=10;',
      x,
      y: margin,
      width: packageWidth,
      height: packageHeight
    }));
    planned.forEach(({ item, y: itemY, height }) => {
      const id = drawioCellId(`class_${item.id}`);
      classCellById.set(item.id, id);
      cells.push(renderDrawioVertex({
        id,
        parent: packageId,
        value: drawioClassLabel(item),
        style: drawioClassStyle(item),
        x: classInset,
        y: itemY,
        width: packageWidth - classInset * 2,
        height
      }));
    });
  });
  projection.relations.forEach((relation, index) => {
    const source = classCellById.get(relation.from);
    const target = classCellById.get(relation.to);
    if (!source || !target) return;
    const dashed = relation.operator === '..>' ? 'dashed=1;' : '';
    cells.push(`<mxCell id="${xmlAttr(drawioCellId(`edge_${index}_${relation.from}_${relation.to}`))}" value="${xmlAttr(relation.label)}" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;endFill=1;${dashed}strokeColor=#c586c0;fontColor=#cccccc;" edge="1" parent="1" source="${xmlAttr(source)}" target="${xmlAttr(target)}">
      <mxGeometry relative="1" as="geometry"/>
    </mxCell>`);
  });
  const width = Math.max(980, margin * 2 + Math.max(1, columns.length) * packageWidth + Math.max(0, columns.length - 1) * gap);
  const height = Math.max(640, maxHeight);
  const generatedAt = new Date().toISOString();
  const graphXml = `<mxGraphModel dx="${width}" dy="${height}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${width}" pageHeight="${height}" math="0" shadow="0">
  <root>
    ${cells.join('\n    ')}
  </root>
</mxGraphModel>`;
  const compressedGraph = compressDrawioGraphXml(graphXml);
  return `<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net" modified="${xmlAttr(generatedAt)}" agent="Renovatio VS Code" version="24.7.8" type="device" pages="1">
  <diagram id="renovatio-uml-model" name="${xmlAttr(diagramName)}">${compressedGraph}</diagram>
</mxfile>
`;
}

function renderDrawioPersistenceErd(project, domain) {
  const discovery = domainDiscovery(domain);
  const repositories = [...discovery.repositories].sort(compareRepositoriesForDiagram);
  const groups = layoutPersistenceErdGroups(persistenceErdGroups(repositories));
  const margin = 40;
  const schemaWidth = Math.max(900, groups.schemaWidth);
  const schemaHeight = Math.max(520, groups.schemaHeight);
  const graphWidth = Math.max(980, schemaWidth + margin * 2);
  const graphHeight = Math.max(640, schemaHeight + margin * 2);
  const schemaId = 'pkg_inferred_persistence';
  const cells = [
    '<mxCell id="0"/>',
    '<mxCell id="1" parent="0"/>',
    renderDrawioVertex({
      id: schemaId,
      parent: '1',
      value: 'inferred persistence schema',
      style: 'swimlane;html=1;whiteSpace=wrap;rounded=1;collapsible=1;recursiveResize=0;startSize=32;fillColor=#1e1e1e;strokeColor=#d7ba7d;fontColor=#d4d4d4;fontStyle=1;spacingLeft=10;',
      x: margin,
      y: margin,
      width: schemaWidth,
      height: schemaHeight
    })
  ];
  const tableCellById = new Map();
  const groupCellById = new Map();
  if (!repositories.length) {
    cells.push(renderDrawioVertex({
      id: 'persistence_empty_model',
      parent: schemaId,
      value: '<b>No inferred persistence yet</b><br/>Run Renovatio analysis to discover DB2 tables and file-backed records.',
      style: 'rounded=1;whiteSpace=wrap;html=1;fillColor=#1e1e1e;strokeColor=#5f6a6a;fontColor=#d4d4d4;align=left;spacing=14;',
      x: 20,
      y: 62,
      width: 420,
      height: 110
    }));
  }
  groups.groups.forEach(group => {
    const groupCellId = drawioCellId(`record_group_${group.id}`);
    groupCellById.set(group.id, groupCellId);
    cells.push(renderDrawioVertex({
      id: groupCellId,
      parent: schemaId,
      value: drawioPersistenceGroupLabel(group),
      style: 'swimlane;html=1;whiteSpace=wrap;rounded=1;collapsible=1;recursiveResize=0;startSize=34;fillColor=#1e1e1e;strokeColor=#d7ba7d;fontColor=#d4d4d4;fontStyle=1;spacingLeft=10;',
      x: group.x,
      y: group.y,
      width: group.width,
      height: group.height
    }));
    group.repositories.forEach((repository, index) => {
      const key = persistenceRepositoryKey(repository, index);
      const id = drawioCellId(`table_${key}`);
      tableCellById.set(key, id);
      cells.push(renderDrawioVertex({
        id,
        parent: groupCellId,
        value: drawioPersistenceTableLabel(repository),
        style: drawioPersistenceTableStyle(repository),
        x: repository.__drawio.x,
        y: repository.__drawio.y,
        width: repository.__drawio.width,
        height: repository.__drawio.height
      }));
    });
  });
  drawioPersistenceGroupRelations(groups.groups).forEach((relation, index) => {
    const source = groupCellById.get(relation.from.id);
    const target = groupCellById.get(relation.to.id);
    if (!source || !target) return;
    cells.push(`<mxCell id="${xmlAttr(drawioCellId(`erd_${index}_${source}_${target}`))}" value="" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;dashed=1;strokeColor=#d7ba7d;strokeWidth=1;opacity=45;" edge="1" parent="${xmlAttr(schemaId)}" source="${xmlAttr(source)}" target="${xmlAttr(target)}">
      <mxGeometry relative="1" as="geometry"/>
    </mxCell>`);
  });
  const graphXml = `<mxGraphModel dx="${graphWidth}" dy="${graphHeight}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${graphWidth}" pageHeight="${graphHeight}" math="0" shadow="0">
  <root>
    ${cells.join('\n    ')}
  </root>
</mxGraphModel>`;
  const compressedGraph = compressDrawioGraphXml(graphXml);
  return `<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net" modified="${xmlAttr(new Date().toISOString())}" agent="Renovatio VS Code" version="24.7.8" type="device" pages="1">
  <diagram id="renovatio-persistence-der" name="${xmlAttr(`${project?.name || 'Renovatio'} persistence DER`)}">${compressedGraph}</diagram>
</mxfile>
`;
}

function persistenceErdGroups(repositories) {
  const byRecord = new Map();
  repositories.forEach((repository, index) => {
    const mapped = asArray(repository.mappedNodes);
    const record = mapped[0];
    const groupName = record?.name || record?.id || `Unmapped ${persistenceKind(repository)}`;
    const key = record ? `record:${normalizeName(groupName)}` : `unmapped:${persistenceKind(repository)}`;
    const current = byRecord.get(key) || {
      id: key,
      name: groupName,
      kind: record ? 'record' : 'unmapped',
      repositories: []
    };
    current.repositories.push({ ...repository, __drawioIndex: index });
    byRecord.set(key, current);
  });
  return [...byRecord.values()].sort((left, right) =>
    right.repositories.length - left.repositories.length
    || String(left.name).localeCompare(String(right.name)));
}

function layoutPersistenceErdGroups(groups) {
  const laneWidth = 740;
  const groupGapX = 44;
  const groupGapY = 36;
  const tableWidth = 320;
  const tableHeight = 214;
  const tableGapX = 24;
  const tableGapY = 22;
  const top = 58;
  const left = 20;
  const columns = groups.length > 1 ? 2 : 1;
  const columnHeights = Array(columns).fill(54);
  const laidOut = groups.map(group => {
    const tableColumns = group.repositories.length > 1 ? 2 : 1;
    const tableRows = Math.max(1, Math.ceil(group.repositories.length / tableColumns));
    const height = top + tableRows * tableHeight + Math.max(0, tableRows - 1) * tableGapY + 24;
    const column = columnHeights.indexOf(Math.min(...columnHeights));
    const x = 20 + column * (laneWidth + groupGapX);
    const y = columnHeights[column];
    columnHeights[column] += height + groupGapY;
    const repositories = group.repositories.map((repository, index) => ({
      ...repository,
      __drawio: {
        x: left + (index % tableColumns) * (tableWidth + tableGapX),
        y: top + Math.floor(index / tableColumns) * (tableHeight + tableGapY),
        width: tableWidth,
        height: tableHeight
      }
    }));
    return { ...group, repositories, x, y, width: laneWidth, height };
  });
  return {
    groups: laidOut,
    schemaWidth: 40 + columns * laneWidth + Math.max(0, columns - 1) * groupGapX,
    schemaHeight: Math.max(...columnHeights) + 20
  };
}

function drawioPersistenceGroupLabel(group) {
  const noun = group.kind === 'record' ? 'record' : 'unmapped';
  const fields = mergeProperties(group.repositories.flatMap(repository => asArray(repository.properties))).length;
  return `${noun}: ${group.name} · ${group.repositories.length} resource${group.repositories.length === 1 ? '' : 's'} · ${fields} field${fields === 1 ? '' : 's'}`;
}

function persistenceRepositoryKey(repository, fallback = 'repository') {
  return repository?.logicalId || repository?.id || repository?.name || fallback;
}

function drawioPersistenceTableLabel(repository) {
  const properties = asArray(repository.properties);
  const fields = properties.slice(0, 12).map(property => `<div>${drawioText(derFieldLabel(property))}</div>`).join('');
  const hidden = properties.length > 12 ? `<div>+${properties.length - 12} more fields</div>` : '';
  const mapped = asArray(repository.mappedNodes).map(node => node.name || node.id).slice(0, 2).join(', ');
  const meta = `${persistenceKind(repository)} · ${properties.length} fields · ${asArray(repository.users).length} uses`;
  return `<div style="text-align:left;"><b>${drawioText(repository.name || repository.id)}</b><br/><font style="font-size:10px;color:#8aa6a3">${drawioText(meta)}</font>${mapped ? `<br/><font style="font-size:10px;color:#8aa6a3">maps ${drawioText(mapped)}</font>` : ''}<hr/>${fields || '<div>shape unresolved</div>'}${hidden}</div>`;
}

function drawioPersistenceTableStyle(repository) {
  const unresolved = !asArray(repository.properties).length;
  const fill = unresolved ? '#3b2f14' : '#1f3326';
  const stroke = persistenceKind(repository) === 'DB2 table' ? '#4ec9b0' : '#d7ba7d';
  return `rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;spacing=10;fillColor=${fill};strokeColor=${stroke};fontColor=#d4d4d4;`;
}

function derFieldLabel(property) {
  const name = property?.name || 'field';
  const type = property?.type || 'unknown';
  const marker = /\b(ID|KEY|NUM|NUMBER)\b|[_-](ID|KEY|NUM)$/i.test(name) ? 'PK? ' : '';
  return `${marker}${name}: ${type}`;
}

function drawioPersistenceGroupRelations(groups) {
  const relations = [];
  for (let leftIndex = 0; leftIndex < groups.length; leftIndex += 1) {
    for (let rightIndex = leftIndex + 1; rightIndex < groups.length; rightIndex += 1) {
      const label = persistenceGroupRelationLabel(groups[leftIndex], groups[rightIndex]);
      if (label) relations.push({ from: groups[leftIndex], to: groups[rightIndex], label });
      if (relations.length >= 40) return relations;
    }
  }
  return relations;
}

function persistenceGroupRelationLabel(left, right) {
  const leftFields = new Set(left.repositories
    .flatMap(repository => asArray(repository.properties))
    .map(property => normalizeErdFieldName(property.name))
    .filter(Boolean));
  const sharedFields = right.repositories
    .flatMap(repository => asArray(repository.properties))
    .map(property => property.name)
    .filter(name => leftFields.has(normalizeErdFieldName(name)));
  if (uniqueStrings(sharedFields).length >= 4) return 'shared field evidence';
  return '';
}

function normalizeErdFieldName(value) {
  const normalized = normalizeName(value);
  if (!normalized || normalized === 'FILLER' || normalized === 'STATUS') return '';
  return normalized;
}

function compressDrawioGraphXml(graphXml) {
  return zlib.deflateRawSync(Buffer.from(encodeURIComponent(graphXml), 'utf8')).toString('base64');
}

function renderDrawioVertex({ id, parent, value, style, x, y, width, height }) {
  return `<mxCell id="${xmlAttr(id)}" value="${xmlAttr(value)}" style="${xmlAttr(style)}" vertex="1" parent="${xmlAttr(parent)}">
      <mxGeometry x="${Number(x)}" y="${Number(y)}" width="${Number(width)}" height="${Number(height)}" as="geometry"/>
    </mxCell>`;
}

function drawioClassLabel(item) {
  const members = item.members.slice(0, 10).map(member => `<div>${drawioText(member)}</div>`).join('');
  const operations = item.operations.slice(0, 4).map(operation => `<div>${drawioText(operation)}</div>`).join('');
  return `<div style="text-align:left;"><b>${drawioText(item.name || item.id)}</b><br/><font style="font-size:10px;color:#8aa6a3">&lt;&lt;${drawioText(item.stereotype)}&gt;&gt;</font>${members ? `<hr/>${members}` : ''}${operations ? `<hr/>${operations}` : ''}</div>`;
}

function drawioClassStyle(item) {
  const palette = {
    component: 'fillColor=#0e639c;strokeColor=#3794ff;fontColor=#ffffff;',
    entity: 'fillColor=#123524;strokeColor=#4ec9b0;fontColor=#d4d4d4;',
    aggregate: 'fillColor=#123524;strokeColor=#4ec9b0;fontColor=#d4d4d4;',
    valueObject: 'fillColor=#123524;strokeColor=#4ec9b0;fontColor=#d4d4d4;',
    repository: 'fillColor=#3b2f14;strokeColor=#d7ba7d;fontColor=#d4d4d4;',
    fileAdapter: 'fillColor=#3b2f14;strokeColor=#d7ba7d;fontColor=#d4d4d4;'
  };
  return `swimlane;html=1;whiteSpace=wrap;rounded=0;startSize=30;align=left;spacingLeft=10;spacingRight=10;resizeWidth=1;${palette[item.stereotype] || palette.entity}`;
}

function drawioClassHeight(item) {
  return Math.max(118, 60 + Math.min(item.members.length, 10) * 18 + Math.min(item.operations.length, 4) * 18);
}

function renderMermaidClassDiagram(projection) {
  if (!projection.items.length) return 'classDiagram\n  class EmptyModel';
  const lines = ['classDiagram', 'direction LR'];
  for (const packageName of projection.packages) {
    const items = projection.items.filter(item => item.packageName === packageName);
    if (!items.length) continue;
    lines.push(`namespace ${packageName} {`);
    for (const item of items) {
      lines.push(`  class ${item.id} {`);
      lines.push(`    <<${item.stereotype}>>`);
      for (const member of item.members.slice(0, 12)) lines.push(`    ${member}`);
      for (const operation of item.operations.slice(0, 5)) lines.push(`    ${operation}`);
      lines.push('  }');
    }
    lines.push('}');
  }
  for (const relation of projection.relations) {
    lines.push(`${relation.from} ${relation.operator} ${relation.to} : ${relation.label}`);
  }
  return lines.join('\n');
}

function classModelProjection(domain) {
  const discovery = domainDiscovery(domain);
  const taken = new Set();
  const itemBySourceId = new Map();
  const componentNodes = uniqueNodes([
    ...discovery.repositories.flatMap(repository => asArray(repository.users)),
    ...discovery.nodes.filter(node => sameKind(node.kind, 'SERVICE') || sameKind(node.kind, 'USE_CASE'))
  ]).slice(0, 18);
  const classNodes = uniqueNodes([
    ...discovery.repositories.flatMap(repository => asArray(repository.mappedNodes)),
    ...discovery.recordNodes.filter(node => asArray(node.properties).length)
  ])
    .sort((left, right) => asArray(right.properties).length - asArray(left.properties).length
      || String(left.name || left.id).localeCompare(String(right.name || right.id)))
    .slice(0, 28);
  const repositoryNodes = discovery.repositories
    .filter(repository => asArray(repository.properties).length || asArray(repository.mappedNodes).length || asArray(repository.users).length)
    .sort(compareRepositoriesForDiagram)
    .slice(0, 28);
  const items = [
    ...componentNodes.map((node, index) => projectUmlItem(node, 'application', 'component', taken, index)),
    ...classNodes.map((node, index) => projectUmlItem(node, 'domain_model', classStereotype(node), taken, index)),
    ...repositoryNodes.map((node, index) => projectUmlItem(node, 'infrastructure_persistence', repositoryStereotype(node), taken, index))
  ];
  for (const item of items) {
    if (item.sourceId) itemBySourceId.set(item.sourceId, item);
    if (item.logicalId) itemBySourceId.set(item.logicalId, item);
  }
  const relations = [];
  const relationIds = new Set();
  for (const repository of repositoryNodes) {
    const repositoryItem = itemBySourceId.get(repository.logicalId || repository.id);
    if (!repositoryItem) continue;
    for (const user of asArray(repository.users)) {
      const componentItem = itemBySourceId.get(user.id);
      if (componentItem) addMermaidRelation(relations, relationIds, componentItem.id, repositoryItem.id, '..>', 'uses adapter');
      for (const mapped of asArray(repository.mappedNodes)) {
        const classItem = itemBySourceId.get(mapped.id);
        if (componentItem && classItem) addMermaidRelation(relations, relationIds, componentItem.id, classItem.id, '..>', 'uses');
      }
    }
    for (const mapped of asArray(repository.mappedNodes)) {
      const classItem = itemBySourceId.get(mapped.id);
      if (classItem) {
        addMermaidRelation(relations, relationIds, classItem.id, repositoryItem.id, '-->', 'persists via');
        addMermaidRelation(relations, relationIds, repositoryItem.id, classItem.id, '..>', 'maps record');
      }
    }
  }
  return {
    packages: ['application', 'domain_model', 'infrastructure_persistence'],
    items,
    relations
  };
}

function entityModelProjection(domain) {
  const discovery = domainDiscovery(domain);
  const taken = new Set();
  const itemBySourceId = new Map();
  const nodes = discovery.recordNodes
    .filter(node => asArray(node.properties).length || sameKind(node.kind, 'AGGREGATE') || sameKind(node.kind, 'ENTITY') || sameKind(node.kind, 'VALUE_OBJECT'))
    .slice(0, 80);
  const items = nodes.map((node, index) =>
    projectUmlItem(node, entityPackageForKind(node.kind), classStereotype(node), taken, index));
  for (const item of items) {
    if (item.sourceId) itemBySourceId.set(item.sourceId, item);
    if (item.logicalId) itemBySourceId.set(item.logicalId, item);
  }
  const relations = [];
  const relationIds = new Set();
  for (const relation of discovery.relations) {
    const from = itemBySourceId.get(relation.fromId);
    const to = itemBySourceId.get(relation.toId);
    if (!from || !to) continue;
    const { operator, label } = umlRelationForKind(relation.kind);
    addMermaidRelation(relations, relationIds, from.id, to.id, operator, label);
  }
  return {
    packages: ['aggregates', 'entities', 'value_objects'],
    items,
    relations
  };
}

function entityPackageForKind(kind) {
  if (sameKind(kind, 'AGGREGATE')) return 'aggregates';
  if (sameKind(kind, 'VALUE_OBJECT')) return 'value_objects';
  return 'entities';
}

function umlRelationForKind(kind) {
  if (sameKind(kind, 'CONTAINS')) return { operator: '*--', label: 'contains' };
  if (sameKind(kind, 'MAPS_TO')) return { operator: '..>', label: 'maps to' };
  if (sameKind(kind, 'USES')) return { operator: '..>', label: 'uses' };
  return { operator: '-->', label: humanize(kind || 'relates') };
}

function projectUmlItem(node, packageName, stereotype, taken, index) {
  const id = uniqueMermaidIdentifier(node?.name || node?.id, `${stereotype}${index + 1}`, taken);
  const properties = asArray(node?.properties);
  return {
    id,
    name: node?.name || node?.id || id,
    packageName,
    stereotype,
    sourceId: node?.id,
    logicalId: node?.logicalId,
    members: umlMembersFor(node, stereotype, properties),
    operations: umlOperationsFor(stereotype, properties)
  };
}

function umlMembersFor(node, stereotype, properties) {
  if (stereotype === 'component') {
    return [`+source ${mermaidMemberName(node?.name || node?.id || 'program')}`];
  }
  if (stereotype === 'repository' || stereotype === 'fileAdapter') {
    const mapped = asArray(node?.mappedNodes).map(candidate => mermaidMemberName(candidate.name || candidate.id)).slice(0, 4);
    return mapped.length ? mapped.map(name => `+maps ${name}`) : [`+resource ${mermaidMemberName(node?.name || node?.id || 'repository')}`];
  }
  return properties.length
    ? properties.slice(0, 12).map(property => `+${mermaidType(property.type)} ${mermaidMemberName(property.name || 'field')}`)
    : [`+source ${mermaidMemberName(node?.name || node?.id || 'record')}`];
}

function umlOperationsFor(stereotype, properties) {
  if (stereotype === 'component') return ['+execute()', '+coordinate()'];
  if (stereotype === 'repository') return ['+load()', '+save()', '+query()'];
  if (stereotype === 'fileAdapter') return ['+read()', '+write()'];
  return properties.length ? ['+validate()', '+toDTO()'] : ['+discoverShape()'];
}

function classStereotype(node) {
  const kind = String(node?.kind || '').toUpperCase();
  if (kind.includes('VALUE')) return 'valueObject';
  if (kind.includes('AGGREGATE')) return 'aggregate';
  return 'entity';
}

function repositoryStereotype(node) {
  return persistenceKind(node) === 'DB2 table' ? 'repository' : 'fileAdapter';
}

function addMermaidRelation(relations, relationIds, from, to, operator, label) {
  const id = `${from}:${operator}:${to}:${label}`;
  if (relationIds.has(id)) return;
  relationIds.add(id);
  relations.push({ from, to, operator, label });
}

function uniqueMermaidIdentifier(value, fallback, taken) {
  const words = String(value || fallback).replace(/[^A-Za-z0-9]+/g, ' ').trim().split(/\s+/).filter(Boolean);
  let base = words.map(word => word.charAt(0).toUpperCase() + word.slice(1)).join('') || fallback;
  if (!/^[A-Za-z_]/.test(base)) base = `${fallback}${base}`;
  let candidate = base;
  let suffix = 2;
  while (taken.has(candidate)) {
    candidate = `${base}${suffix}`;
    suffix += 1;
  }
  taken.add(candidate);
  return candidate;
}

function mermaidMemberName(value) {
  const normalized = String(value || 'value').replace(/[^A-Za-z0-9_]+/g, '_').replace(/^_+|_+$/g, '');
  return normalized || 'value';
}

function mermaidType(value) {
  return mermaidMemberName(value || 'unknown');
}

function classDiagramRelativePath(project) {
  return `.renovatio/diagrams/${sanitizeFileName(project?.name || 'renovatio')}-uml-class-model.md`;
}

function drawioDiagramRelativePath(project) {
  return `.renovatio/diagrams/${sanitizeFileName(project?.name || 'renovatio')}-domain-model.drawio`;
}

function drawioEntityModelRelativePath(project) {
  return `.renovatio/diagrams/${sanitizeFileName(project?.name || 'renovatio')}-entity-model.drawio`;
}

function drawioPersistenceErdRelativePath(project) {
  return `.renovatio/diagrams/${sanitizeFileName(project?.name || 'renovatio')}-persistence-der.drawio`;
}

function drawioCellId(value) {
  const normalized = String(value || 'cell').replace(/[^A-Za-z0-9_-]+/g, '_').replace(/^_+|_+$/g, '');
  return normalized || 'cell';
}

function drawioText(value) {
  return escapeHtml(value);
}

function xmlAttr(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&apos;'
  }[char]));
}

function formatPackageName(value) {
  return String(value || 'model').replace(/[_-]+/g, ' ');
}

function sanitizeFileName(value) {
  return String(value || 'renovatio').toLowerCase().replace(/[^a-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '') || 'renovatio';
}

function hasMermaidMarkdownExtension() {
  return Boolean(vscode.extensions.getExtension('bierner.markdown-mermaid')
    || vscode.extensions.getExtension('vstirbu.vscode-mermaid-preview')
    || vscode.extensions.getExtension('mermaidchart.vscode-mermaid-chart')
    || vscode.extensions.getExtension('bpruitt-goddard.mermaid-markdown-syntax-highlighting'));
}

function hasHedietDrawioExtension() {
  return Boolean(vscode.extensions.getExtension('hediet.vscode-drawio'));
}

function buildDomainDiagram(domain) {
  const discovery = domainDiscovery(domain);
  if (!discovery.nodes.length) {
    return { width: 1080, height: 220, nodes: [], useCases: [], repositories: [], records: [], edges: [], evidence: [] };
  }
  const repositories = discovery.repositories
    .filter(repository => asArray(repository.properties).length || asArray(repository.mappedNodes).length || asArray(repository.users).length)
    .sort((left, right) => asArray(right.properties).length - asArray(left.properties).length
      || asArray(right.users).length - asArray(left.users).length
      || String(left.name || left.id).localeCompare(String(right.name || right.id)))
    .slice(0, 8);
  const selectedRepositories = repositories.length ? repositories : discovery.repositories.slice(0, 8);
  const selectedRepositoryIds = new Set(selectedRepositories.map(repository => repository.logicalId || repository.id));
  const useCases = uniqueNodes(selectedRepositories.flatMap(repository => asArray(repository.users)))
    .filter(node => sameKind(node.kind, 'USE_CASE') || String(node.id || '').startsWith('use-case:'))
    .slice(0, 8);
  const records = uniqueNodes([
    ...selectedRepositories.flatMap(repository => asArray(repository.mappedNodes)),
    ...discovery.recordNodes.filter(node => asArray(node.properties).length)
  ]).slice(0, 8);

  const width = 1080;
  const top = 54;
  const slot = 134;
  const boxHeight = 106;
  const rowCount = Math.max(1, useCases.length, selectedRepositories.length, records.length);
  const height = top + rowCount * slot + 30;
  const diagramUseCases = layoutDiagramColumn(useCases, 'use', 24, top, 282, boxHeight, slot);
  const diagramRepositories = layoutDiagramColumn(selectedRepositories, 'repo', 386, top, 300, boxHeight, slot);
  const diagramRecords = layoutDiagramColumn(records, 'record', 746, top, 310, boxHeight, slot);
  const useById = new Map(diagramUseCases.map(node => [node.source.id, node]));
  const repoById = new Map(diagramRepositories.map(node => [node.source.logicalId || node.source.id, node]));
  const recordById = new Map(diagramRecords.map(node => [node.source.id, node]));
  const edges = [];
  const edgeIds = new Set();
  for (const repository of selectedRepositories) {
    const repoNode = repoById.get(repository.logicalId || repository.id);
    if (!repoNode) continue;
    for (const user of asArray(repository.users)) {
      const useNode = useById.get(user.id);
      if (useNode) addDiagramEdge(edges, edgeIds, useNode, repoNode, 'uses');
    }
    for (const record of asArray(repository.mappedNodes)) {
      const recordNode = recordById.get(record.id);
      if (recordNode) addDiagramEdge(edges, edgeIds, repoNode, recordNode, 'maps');
    }
  }
  const evidence = mergedEvidence([...useCases, ...selectedRepositories, ...records]);
  const totalInteresting = discovery.repositories.length + discovery.recordNodes.filter(node => asArray(node.properties).length).length;
  const shownInteresting = selectedRepositoryIds.size + records.length;
  const notice = totalInteresting > shownInteresting
    ? `Showing the most connected ${shownInteresting} of ${totalInteresting} persistence/data-shape candidates. The full model remains below.`
    : '';
  return {
    width,
    height,
    nodes: [...diagramUseCases, ...diagramRepositories, ...diagramRecords],
    useCases: diagramUseCases,
    repositories: diagramRepositories,
    records: diagramRecords,
    edges,
    evidence,
    notice
  };
}

function buildPersistenceDiagram(domain) {
  const discovery = domainDiscovery(domain);
  if (!discovery.repositories.length) {
    return { width: 1080, height: 220, nodes: [], useCases: [], repositories: [], records: [], edges: [], notice: '' };
  }
  const repositoriesWithFields = discovery.repositories
    .filter(repository => asArray(repository.properties).length)
    .sort(compareRepositoriesForDiagram)
    .slice(0, 6);
  const repositoriesWithoutFields = discovery.repositories
    .filter(repository => !asArray(repository.properties).length)
    .sort(compareRepositoriesForDiagram)
    .slice(0, 4);
  const selectedRepositories = uniqueNodes([...repositoriesWithFields, ...repositoriesWithoutFields]).slice(0, 10);
  const useCases = uniqueNodes(selectedRepositories.flatMap(repository => asArray(repository.users)))
    .filter(node => sameKind(node.kind, 'USE_CASE') || String(node.id || '').startsWith('use-case:'))
    .slice(0, 10);
  const records = uniqueNodes(selectedRepositories.flatMap(repository => asArray(repository.mappedNodes)))
    .sort((left, right) => asArray(right.properties).length - asArray(left.properties).length
      || String(left.name || left.id).localeCompare(String(right.name || right.id)))
    .slice(0, 10);
  const width = 1080;
  const top = 54;
  const slot = 134;
  const boxHeight = 106;
  const rowCount = Math.max(1, useCases.length, selectedRepositories.length, records.length);
  const height = top + rowCount * slot + 30;
  const diagramUseCases = layoutDiagramColumn(useCases, 'use', 24, top, 282, boxHeight, slot);
  const diagramRepositories = layoutDiagramColumn(selectedRepositories, 'repo', 386, top, 300, boxHeight, slot);
  const diagramRecords = layoutDiagramColumn(records, 'record', 746, top, 310, boxHeight, slot);
  const useById = new Map(diagramUseCases.map(node => [node.source.id, node]));
  const repoById = new Map(diagramRepositories.map(node => [node.source.logicalId || node.source.id, node]));
  const recordById = new Map(diagramRecords.map(node => [node.source.id, node]));
  const edges = [];
  const edgeIds = new Set();
  for (const repository of selectedRepositories) {
    const repoNode = repoById.get(repository.logicalId || repository.id);
    if (!repoNode) continue;
    for (const user of asArray(repository.users)) {
      const useNode = useById.get(user.id);
      if (useNode) addDiagramEdge(edges, edgeIds, useNode, repoNode, 'uses');
    }
    for (const record of asArray(repository.mappedNodes)) {
      const recordNode = recordById.get(record.id);
      if (recordNode) addDiagramEdge(edges, edgeIds, repoNode, recordNode, 'maps');
    }
  }
  const unresolvedCount = discovery.repositories.filter(repository => !asArray(repository.properties).length).length;
  const notice = unresolvedCount
    ? `${unresolvedCount} table/resource shape(s) still have no inferred fields. They were detected from I/O, but no COBOL record shape was confidently bound yet.`
    : '';
  return {
    width,
    height,
    nodes: [...diagramUseCases, ...diagramRepositories, ...diagramRecords],
    useCases: diagramUseCases,
    repositories: diagramRepositories,
    records: diagramRecords,
    edges,
    notice
  };
}

function compareRepositoriesForDiagram(left, right) {
  const leftDb = persistenceKind(left) === 'DB2 table' ? 1 : 0;
  const rightDb = persistenceKind(right) === 'DB2 table' ? 1 : 0;
  return rightDb - leftDb
    || asArray(right.properties).length - asArray(left.properties).length
    || asArray(right.users).length - asArray(left.users).length
    || String(left.name || left.id).localeCompare(String(right.name || right.id));
}

function layoutDiagramColumn(nodes, type, x, top, width, height, slot) {
  return nodes.map((node, index) => ({
    type,
    source: node,
    x,
    y: top + index * slot,
    width,
    height
  }));
}

function addDiagramEdge(edges, edgeIds, from, to, kind) {
  const id = `${kind}:${from.type}:${from.source.id}->${to.type}:${to.source.id || to.source.logicalId}`;
  if (edgeIds.has(id)) return;
  edgeIds.add(id);
  edges.push({ from, to, kind });
}

function diagramNodeId(node) {
  return String(node?.source?.logicalId || node?.source?.id || node?.source?.name || '');
}

function renderDiagramEdge(edge, markerScope = 'domain') {
  const x1 = edge.from.x + edge.from.width;
  const y1 = edge.from.y + edge.from.height / 2;
  const x2 = edge.to.x;
  const y2 = edge.to.y + edge.to.height / 2;
  const className = edge.kind === 'maps' ? 'diagramEdge maps' : 'diagramEdge';
  const marker = markerScope === 'persistence'
    ? edge.kind === 'maps' ? 'persistenceArrowMaps' : 'persistenceArrowUses'
    : edge.kind === 'maps' ? 'arrowMaps' : 'arrowUses';
  return `<path class="${className}" data-edge="true" data-from="${escapeHtml(diagramNodeId(edge.from))}" data-to="${escapeHtml(diagramNodeId(edge.to))}" d="M ${x1} ${y1} C ${x1 + 46} ${y1}, ${x2 - 46} ${y2}, ${x2} ${y2}" marker-end="url(#${marker})" />`;
}

function renderDiagramNode(node, variant) {
  const source = node.source;
  const title = source.name || source.id;
  const properties = asArray(source.properties);
  const meta = diagramNodeMeta(source, variant);
  const unresolved = variant === 'repo' && !asArray(source.properties).length;
  const boxClass = variant === 'repo'
    ? `diagramBox repo${unresolved ? ' unresolved' : ''}`
    : variant === 'record' ? 'diagramBox record' : 'diagramBox';
  return `<g class="debugNode" data-node-id="${escapeHtml(diagramNodeId(node))}" data-x="${node.x}" data-y="${node.y}" data-width="${node.width}" data-height="${node.height}">
    <title>${escapeHtml(title)}</title>
    <rect class="${boxClass}" x="${node.x}" y="${node.y}" width="${node.width}" height="${node.height}" rx="3" />
    ${renderSvgLines(title, node.x + 12, node.y + 22, 'diagramTitle', variant === 'record' ? 30 : 28, 2, 15, { editable: true })}
    <text class="diagramMeta" x="${node.x + 12}" y="${node.y + 55}">${escapeHtml(meta)}</text>
    ${properties.slice(0, 3).map((property, index) =>
      `<text class="diagramField" x="${node.x + 12}" y="${node.y + 74 + index * 14}">${escapeHtml(compactField(property))}</text>`).join('')}
    ${properties.length > 3 ? `<text class="diagramMeta" x="${node.x + 12}" y="${node.y + 74 + 3 * 14}">+${properties.length - 3} more fields</text>` : ''}
  </g>`;
}

function diagramNodeMeta(source, variant) {
  if (variant === 'repo') {
    const refs = Number(source.instances || 1);
    return `${persistenceKind(source)} · ${asArray(source.properties).length} fields · ${refs} ref${refs === 1 ? '' : 's'}`;
  }
  if (variant === 'record') {
    return `${humanize(source.kind || 'record')} · ${asArray(source.properties).length} fields`;
  }
  return humanize(source.kind || 'use case');
}

function renderSvgLines(value, x, y, className, maxChars, maxLines, lineHeight, options = {}) {
  return wrapText(value, maxChars, maxLines).map((line, index) =>
    `<text class="${className}"${options.editable && index === 0 ? ' data-editable-label="true"' : ''} x="${x}" y="${y + index * lineHeight}">${escapeHtml(line)}</text>`).join('');
}

function wrapText(value, maxChars, maxLines) {
  const normalized = String(value || '').replace(/\s+/g, ' ').trim();
  if (!normalized) return [''];
  const chunks = [];
  let remaining = normalized;
  while (remaining.length && chunks.length < maxLines) {
    if (remaining.length <= maxChars) {
      chunks.push(remaining);
      remaining = '';
      break;
    }
    let cut = remaining.lastIndexOf(' ', maxChars);
    if (cut < Math.floor(maxChars * 0.55)) cut = remaining.lastIndexOf('-', maxChars);
    if (cut < Math.floor(maxChars * 0.55)) cut = maxChars;
    chunks.push(remaining.slice(0, cut).trim());
    remaining = remaining.slice(cut).replace(/^[-\s]+/, '');
  }
  if (remaining && chunks.length) {
    chunks[chunks.length - 1] = `${chunks[chunks.length - 1].replace(/[.\s]+$/, '')}...`;
  }
  return chunks;
}

function compactField(property) {
  return `${property.name || 'field'}: ${property.type || 'unknown'}`;
}

function unresolvedShapeReason(repository) {
  const mapped = asArray(repository?.mappedNodes);
  if (mapped.length) {
    return 'Shape unresolved: Renovatio linked this table to a COBOL record candidate, but that record has no parsed child fields yet. Check copybook resolution and DATA DIVISION/group parsing for the mapped record.';
  }
  const users = asArray(repository?.users);
  if (users.length) {
    return 'Shape unresolved: table access was detected from COBOL I/O, but no matching COBOL group/host-variable record was confidently bound as its column shape.';
  }
  return 'Shape unresolved: repository/table candidate exists in the DomainModel, but the current evidence has no source program and no mapped COBOL record shape.';
}

function renderDomainModelGraph(domain) {
  const model = domain?.model || {};
  const nodes = asArray(model.nodes);
  const relations = asArray(model.relations);
  const invariants = asArray(model.invariants);
  if (!nodes.length) {
    return `<section class="panel">
      <div class="sectionTitle">Model graph</div>
      <div class="empty">No domain nodes are persisted yet. Run Analyze COBOL Sources; Renovatio will seed the first model from semantic IR when no model exists.</div>
    </section>`;
  }
  const nodeById = new Map(nodes.map(node => [node.id, node]));
  return `<section class="panel">
    <div class="sectionTitle">Domain nodes</div>
    ${renderNodeGroups(nodes)}
  </section>
  <section class="panel">
    <div class="sectionTitle">Relations</div>
    ${renderRelations(relations, nodeById)}
  </section>
  <section class="panel">
    <div class="sectionTitle">Business invariants</div>
    ${renderInvariants(invariants, nodeById)}
  </section>`;
}

function renderNodeGroups(nodes) {
  const grouped = new Map();
  for (const node of nodes) {
    const kind = node.kind || 'UNKNOWN';
    if (!grouped.has(kind)) grouped.set(kind, []);
    grouped.get(kind).push(node);
  }
  return [...grouped.entries()].sort(([left], [right]) => left.localeCompare(right)).map(([kind, values]) => `
    <div class="artifact">
      <div class="artifactHeader"><strong>${escapeHtml(humanize(kind))}</strong><span>${values.length}</span></div>
      <div class="domainGrid">
        ${values.map(renderDomainNode).join('')}
      </div>
    </div>`).join('');
}

function renderDomainNode(node) {
  const properties = asArray(node.properties);
  const evidence = asArray(node.evidence);
  return `<div class="nodeCard">
    <strong>${escapeHtml(node.name || node.id)}</strong>
    <div class="nodeMeta">
      <span class="pill">${escapeHtml(node.kind || 'node')}</span>
      <span class="pill">${escapeHtml(node.origin || 'UNKNOWN')}</span>
      <span class="pill">${Math.round(Number(node.confidence || 0) * 100)}%</span>
    </div>
    ${properties.length ? `<div class="pillRow">${properties.slice(0, 8).map(property => `<span class="pill">${escapeHtml(property.name)}: ${escapeHtml(property.type)}</span>`).join('')}</div>` : ''}
    ${renderEvidence(evidence)}
    <code>${escapeHtml(node.id)}</code>
  </div>`;
}

function renderRelations(relations, nodeById) {
  if (!relations.length) return '<div class="empty">No relations persisted yet.</div>';
  return `<div class="relationList">${relations.map(relation => {
    const from = nodeById.get(relation.fromId);
    const to = nodeById.get(relation.toId);
    return `<div class="relationRow">
      <strong>${escapeHtml(from?.name || relation.fromId)}</strong>
      <span class="relationKind">${escapeHtml(humanize(relation.kind || 'relates'))}</span>
      <strong>${escapeHtml(to?.name || relation.toId)}</strong>
    </div>`;
  }).join('')}</div>`;
}

function renderInvariants(invariants, nodeById) {
  if (!invariants.length) return '<div class="empty">No business invariants persisted yet.</div>';
  return `<div class="domainGrid">${invariants.map(invariant => {
    const subject = nodeById.get(invariant.subjectId);
    return `<div class="nodeCard">
      <strong>${escapeHtml(invariant.expression || invariant.id)}</strong>
      <div class="nodeMeta">
        <span class="pill">${escapeHtml(subject?.name || invariant.subjectId)}</span>
        <span class="pill">${escapeHtml(invariant.origin || 'UNKNOWN')}</span>
      </div>
      ${renderEvidence(asArray(invariant.evidence))}
    </div>`;
  }).join('')}</div>`;
}

function renderDomainModelGovernance(domain) {
  const diagnostics = asArray(domain?.diagnostics);
  const suggestions = asArray(domain?.suggestions);
  return `<section class="panel">
    <div class="sectionTitle">Governance</div>
    ${diagnostics.length ? `<div class="relationList">${diagnostics.map(diagnostic => `<div class="warning"><strong>${escapeHtml(diagnostic.code)}</strong> · ${escapeHtml(diagnostic.targetId)}<br>${escapeHtml(diagnostic.message)}</div>`).join('')}</div>` : '<div class="empty">No DomainModel diagnostics.</div>'}
    ${suggestions.length ? `<div class="artifact">
      <div class="artifactHeader"><strong>Suggestions</strong><span>${suggestions.length}</span></div>
      <div class="relationList">${suggestions.map(suggestion => `<div class="relationRow">
        <strong>${escapeHtml(suggestion.name || suggestion.targetId)}</strong>
        <span class="relationKind">${escapeHtml(suggestion.targetType || 'suggestion')}</span>
        <span class="pill">${escapeHtml(suggestion.status || 'pending')}</span>
      </div>`).join('')}</div>
    </div>` : ''}
  </section>`;
}

function renderEvidence(evidence) {
  if (!evidence.length) return '';
  return `<div class="evidenceList">${evidence.slice(0, 3).map(item => `<code title="${escapeHtml(item.rationale || '')}">${escapeHtml(item.sourceRef || item.provenance || 'evidence')}</code>`).join('')}</div>`;
}

function renderFileList(files, actionLabel) {
  if (!files.length) return '';
  return `<div class="fileList">${files.map(file => `<div class="fileRow">
    <strong>${escapeHtml(file.relativePath)}</strong>
    <span>${escapeHtml(file.language)}</span>
    ${openFileLink(file.fsPath, actionLabel)}
  </div>`).join('')}</div>`;
}

function openFileLink(fsPath, label) {
  return commandLink('renovatio.openFile', [fsPath], label);
}

function openEvidenceLink(sourceRef, label) {
  return commandLink('renovatio.openEvidence', [sourceRef], label);
}

function commandLink(command, args, label) {
  const encoded = encodeURIComponent(JSON.stringify(args));
  return `<a href="command:${command}?${encoded}">${escapeHtml(label)}</a>`;
}

async function loadGeneratedArtifacts(project) {
  const settings = workspaceSettings(project);
  const root = settings.generatedRoot;
  if (!root) return { root: '', exists: false, files: [] };
  const configurationWarning = settings.generatedRootWarning
    ? `${settings.generatedRootWarning}. Using ${settings.generatedRoot} instead.`
    : '';
  const rootUri = vscode.Uri.file(root);
  try {
    const stat = await vscode.workspace.fs.stat(rootUri);
    if (stat.type !== vscode.FileType.Directory) return { root, exists: false, files: [], notice: configurationWarning };
  } catch {
    return { root, exists: false, files: [], notice: configurationWarning };
  }
  if (await containsCobolArtifacts(rootUri, 0)) {
    return {
      root,
      exists: true,
      files: [],
      warning: 'The future output folder appears to contain COBOL/JCL input files. Future output must be separate from COBOL source roots.'
    };
  }
  const uris = [];
  await collectGeneratedFiles(rootUri, root, uris, 0);
  const files = [];
  for (const uri of uris.slice(0, 6)) {
    files.push({
      fsPath: uri.fsPath,
      relativePath: path.relative(root, uri.fsPath),
      language: languageForFile(uri.fsPath)
    });
  }
  return { root, exists: true, files, notice: configurationWarning };
}

async function loadCobolArtifacts(root) {
  if (!root) return { root: '', exists: false, files: [] };
  const rootUri = vscode.Uri.file(root);
  try {
    const stat = await vscode.workspace.fs.stat(rootUri);
    if (stat.type !== vscode.FileType.Directory) return { root, exists: false, files: [] };
  } catch {
    return { root, exists: false, files: [] };
  }
  const uris = [];
  await collectCobolFiles(rootUri, root, uris, 0);
  const files = [];
  for (const uri of uris.slice(0, 10)) {
    files.push({
      fsPath: uri.fsPath,
      relativePath: path.relative(root, uri.fsPath),
      language: languageForFile(uri.fsPath)
    });
  }
  return { root, exists: true, files };
}

async function collectGeneratedFiles(uri, root, files, depth) {
  if (depth > 6 || files.length >= 16) return;
  let entries = [];
  try {
    entries = await vscode.workspace.fs.readDirectory(uri);
  } catch {
    return;
  }
  entries.sort(([leftName, leftType], [rightName, rightType]) => {
    if (leftType !== rightType) return leftType === vscode.FileType.Directory ? -1 : 1;
    return leftName.localeCompare(rightName);
  });
  for (const [name, type] of entries) {
    if (files.length >= 16) break;
    const child = vscode.Uri.joinPath(uri, name);
    if (type === vscode.FileType.Directory) {
      if (!shouldSkipPreviewDirectory(name)) {
        await collectGeneratedFiles(child, root, files, depth + 1);
      }
    } else if (isPreviewableSource(name)) {
      files.push(child);
    }
  }
}

async function collectCobolFiles(uri, root, files, depth) {
  if (depth > 6 || files.length >= 20) return;
  let entries = [];
  try {
    entries = await vscode.workspace.fs.readDirectory(uri);
  } catch {
    return;
  }
  entries.sort(([leftName, leftType], [rightName, rightType]) => {
    if (leftType !== rightType) return leftType === vscode.FileType.Directory ? -1 : 1;
    return leftName.localeCompare(rightName);
  });
  for (const [name, type] of entries) {
    if (files.length >= 20) break;
    const child = vscode.Uri.joinPath(uri, name);
    if (type === vscode.FileType.Directory) {
      if (!shouldSkipPreviewDirectory(name)) {
        await collectCobolFiles(child, root, files, depth + 1);
      }
    } else if (isCobolArtifact(name)) {
      files.push(child);
    }
  }
}

async function containsCobolArtifacts(uri, depth) {
  if (depth > 4) return false;
  let entries = [];
  try {
    entries = await vscode.workspace.fs.readDirectory(uri);
  } catch {
    return false;
  }
  for (const [name, type] of entries) {
    if (type === vscode.FileType.Directory) {
      if (!shouldSkipPreviewDirectory(name) && await containsCobolArtifacts(vscode.Uri.joinPath(uri, name), depth + 1)) {
        return true;
      }
    } else if (isCobolArtifact(name)) {
      return true;
    }
  }
  return false;
}

function shouldSkipPreviewDirectory(name) {
  return name.startsWith('.') || ['node_modules', 'target', 'build', 'dist', 'out'].includes(name);
}

function generatedOutputPath(project) {
  if (!project) return '';
  return workspaceSettings(project).generatedRoot;
}

function isPreviewableSource(name) {
  return /\.(java|kt|py|js|ts|xml|json|ya?ml)$/i.test(name);
}

function isCobolArtifact(name) {
  return /\.(cbl|cob|cobol|cpy|copybook|jcl|job|proc)$/i.test(name);
}

function languageForFile(filePath) {
  const extension = path.extname(filePath).replace('.', '').toLowerCase();
  return extension || 'text';
}

function sumInventory(inventory) {
  return Object.values(inventory || {}).reduce((total, value) => total + (Number(value) || 0), 0);
}

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function normalizeProgress(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return 0;
  if (numeric <= 1) return Math.round(numeric * 100);
  return Math.round(Math.min(numeric, 100));
}

function humanize(key) {
  return String(key).replace(/([a-z])([A-Z])/g, '$1 $2').replace(/[_-]+/g, ' ');
}

function normalizeName(value) {
  return String(value || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
}

function sameKind(value, expected) {
  return String(value || '').toUpperCase() === String(expected || '').toUpperCase();
}

function samePath(left, right) {
  if (!left || !right) return false;
  return path.resolve(left) === path.resolve(right);
}

function isInsidePath(candidate, parent) {
  if (!candidate || !parent) return false;
  const relative = path.relative(path.resolve(parent), path.resolve(candidate));
  return Boolean(relative) && !relative.startsWith('..') && !path.isAbsolute(relative);
}

function updateStatus() {
  const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
  statusItem.text = project ? `$(tools) Renovatio: ${project.name}` : '$(tools) Renovatio';
  statusItem.tooltip = project ? `Workspace: ${project.workspacePath || 'not configured'}\nScan root: ${project.cobolScanRoot || project.workspacePath || 'not configured'}` : 'Select Renovatio project';
}

class ProjectsProvider {
  constructor() {
    this._onDidChangeTreeData = new vscode.EventEmitter();
    this.onDidChangeTreeData = this._onDidChangeTreeData.event;
  }
  refresh() { this._onDidChangeTreeData.fire(); }
  getTreeItem(item) { return item; }
  getChildren(item) {
    if (item?.children) return item.children;
    const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
    if (!project) {
      const select = new vscode.TreeItem('Select Renovatio project', vscode.TreeItemCollapsibleState.None);
      select.iconPath = new vscode.ThemeIcon('folder-active');
      select.command = { command: 'renovatio.selectProject', title: 'Select Renovatio Project' };
      return [select];
    }
    const settings = workspaceSettings(project);
    const workspace = new vscode.TreeItem(path.basename(settings.workspaceFolderPath || project.workspacePath || 'Workspace'), vscode.TreeItemCollapsibleState.None);
    workspace.description = settings.workspaceFolderPath || project.workspacePath || '';
    workspace.iconPath = new vscode.ThemeIcon('root-folder');
    workspace.tooltip = settings.workspaceFolderPath || project.workspacePath || 'VS Code workspace';

    const projectItem = new vscode.TreeItem(project.name, vscode.TreeItemCollapsibleState.None);
    projectItem.description = 'Renovatio project';
    projectItem.contextValue = 'renovatioProject';
    projectItem.iconPath = new vscode.ThemeIcon('repo');
    projectItem.command = { command: 'renovatio.selectProject', title: 'Select Renovatio Project', arguments: [project.id] };

    const sourceRootItems = settings.cobolRoots.map(root => {
      const rootItem = new vscode.TreeItem(path.basename(root) || root, vscode.TreeItemCollapsibleState.None);
      rootItem.description = root;
      rootItem.iconPath = new vscode.ThemeIcon('folder');
      rootItem.tooltip = `Analyze COBOL source root\n${root}`;
      rootItem.command = { command: 'renovatio.analyzeSelectedPath', title: 'Analyze COBOL Source Root', arguments: [vscode.Uri.file(root)] };
      return rootItem;
    });
    const addRoot = new vscode.TreeItem('Add COBOL source root', vscode.TreeItemCollapsibleState.None);
    addRoot.iconPath = new vscode.ThemeIcon('add');
    addRoot.command = { command: 'renovatio.addCobolSourceRoot', title: 'Add COBOL Source Root' };
    sourceRootItems.push(addRoot);
    if (settings.rawCobolRoots.length) {
      const removeRoot = new vscode.TreeItem('Remove COBOL source root', vscode.TreeItemCollapsibleState.None);
      removeRoot.iconPath = new vscode.ThemeIcon('remove');
      removeRoot.command = { command: 'renovatio.removeCobolSourceRoot', title: 'Remove COBOL Source Root' };
      sourceRootItems.push(removeRoot);
    }
    const sourcesLabel = settings.configuredCobolRoots.length
      ? `COBOL source roots (${settings.configuredCobolRoots.length})`
      : 'COBOL source roots (workspace fallback)';
    const sources = new vscode.TreeItem(sourcesLabel, vscode.TreeItemCollapsibleState.Expanded);
    sources.iconPath = new vscode.ThemeIcon('references');
    sources.children = sourceRootItems;

    const generated = new vscode.TreeItem(path.basename(settings.generatedRoot || 'Future output'), vscode.TreeItemCollapsibleState.None);
    generated.description = settings.generatedRoot;
    generated.iconPath = new vscode.ThemeIcon('file-code');
    generated.tooltip = settings.generatedRoot;
    generated.command = { command: 'renovatio.selectGeneratedOutputFolder', title: 'Configure Future Output Folder' };

    const target = new vscode.TreeItem(`${settings.targetLanguage}`, vscode.TreeItemCollapsibleState.None);
    target.description = settings.targetPackage;
    target.iconPath = new vscode.ThemeIcon('symbol-namespace');

    return [workspace, projectItem, sources, generated, target];
  }
}

class OperationsProvider {
  constructor() {
    this._onDidChangeTreeData = new vscode.EventEmitter();
    this.onDidChangeTreeData = this._onDidChangeTreeData.event;
  }
  refresh() { this._onDidChangeTreeData.fire(); }
  getTreeItem(item) { return item; }
  getChildren() {
    const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
    const settings = workspaceSettings(project);
    const operations = [
      {
        label: 'Select Renovatio project',
        description: project?.name || '',
        icon: 'folder-active',
        command: 'renovatio.selectProject'
      },
      {
        label: 'Add COBOL source root',
        description: settings.workspaceFolderPath || '',
        icon: 'add',
        command: 'renovatio.addCobolSourceRoot'
      },
      {
        label: 'Configure future output',
        description: settings.rawGeneratedRoot || '',
        icon: 'file-code',
        command: 'renovatio.selectGeneratedOutputFolder'
      },
      {
        label: 'Analyze COBOL sources',
        description: settings.configuredCobolRoots.length ? `${settings.configuredCobolRoots.length} configured` : 'workspace fallback',
        icon: 'run-all',
        command: 'renovatio.analyzeCobolSources'
      },
      {
        label: 'Analyze Explorer selection',
        description: 'Explorer context',
        icon: 'play',
        command: 'renovatio.analyzeSelectedPath'
      },
      {
        label: 'Open control deck',
        description: 'summary dashboard',
        icon: 'dashboard',
        command: 'renovatio.openControlDeck'
      },
      {
        label: 'Open domain model',
        description: state.domainModel?.revision ? `revision ${state.domainModel.revision}` : 'seeded after discovery',
        icon: 'symbol-structure',
        command: 'renovatio.openDomainModel'
      },
      {
        label: 'Open native domain diagram',
        description: state.domainModel?.revision ? `revision ${state.domainModel.revision}` : 'run discovery first',
        icon: 'type-hierarchy',
        command: 'renovatio.openNativeDomainDiagram'
      },
      {
        label: 'Open native persistence diagram',
        description: domainDiscovery(state.domainModel).repositories.length ? 'repositories and records' : 'run discovery first',
        icon: 'database',
        command: 'renovatio.openNativePersistenceDiagram'
      },
      {
        label: 'Open native architecture diagram',
        description: 'target layers',
        icon: 'circuit-board',
        command: 'renovatio.openNativeArchitectureDiagram'
      },
      {
        label: 'Open inferred persistence',
        description: domainDiscovery(state.domainModel).repositories.length ? 'ER model' : 'seeded after discovery',
        icon: 'database',
        command: 'renovatio.openPersistenceModel'
      },
      {
        label: 'Open future output',
        description: generatedOutputPath(project),
        icon: 'file-code',
        command: 'renovatio.openGeneratedCode'
      },
      {
        label: 'Refresh Renovatio',
        description: config().backendUrl,
        icon: 'refresh',
        command: 'renovatio.refresh'
      }
    ];
    return operations.map(operation => {
      const item = new vscode.TreeItem(operation.label, vscode.TreeItemCollapsibleState.None);
      item.description = operation.description;
      item.iconPath = new vscode.ThemeIcon(operation.icon);
      item.command = { command: operation.command, title: operation.label };
      return item;
    });
  }
}

class AnalysisProvider {
  constructor() {
    this._onDidChangeTreeData = new vscode.EventEmitter();
    this.onDidChangeTreeData = this._onDidChangeTreeData.event;
  }
  refresh() { this._onDidChangeTreeData.fire(); }
  getTreeItem(item) { return item; }
  getChildren(item) {
    if (item?.children) return item.children;
    const inventory = state.analysis?.inventory || {};
    const entries = Object.entries(inventory);
    const discovery = domainDiscovery(state.domainModel);
    const result = [];

    if (discovery.repositories.length) {
      const group = new vscode.TreeItem(`Inferred persistence (${discovery.repositories.length})`, vscode.TreeItemCollapsibleState.Expanded);
      group.iconPath = new vscode.ThemeIcon('database');
      group.command = { command: 'renovatio.openPersistenceModel', title: 'Open Persistence Model' };
      group.children = discovery.repositories.slice(0, 12).map(repository => {
        const item = new vscode.TreeItem(repository.name || repository.id, vscode.TreeItemCollapsibleState.None);
        item.description = `${repository.properties.length} fields`;
        item.tooltip = `${repository.name || repository.id}\n${repository.properties.map(property => property.name).slice(0, 12).join(', ')}`;
        item.iconPath = new vscode.ThemeIcon(persistenceKind(repository) === 'DB2 table' ? 'database' : 'file-binary');
        item.command = { command: 'renovatio.openPersistenceModel', title: 'Open Persistence Model' };
        return item;
      });
      result.push(group);
    }

    const shapedRecords = discovery.recordNodes.filter(node => asArray(node.properties).length);
    if (shapedRecords.length) {
      const group = new vscode.TreeItem(`Data records (${shapedRecords.length})`, vscode.TreeItemCollapsibleState.Collapsed);
      group.iconPath = new vscode.ThemeIcon('symbol-structure');
      group.command = { command: 'renovatio.openDomainModel', title: 'Open Domain Model' };
      group.children = shapedRecords.slice(0, 12).map(record => {
        const item = new vscode.TreeItem(record.name || record.id, vscode.TreeItemCollapsibleState.None);
        item.description = `${asArray(record.properties).length} fields`;
        item.tooltip = `${record.name || record.id}\n${asArray(record.properties).map(property => property.name).slice(0, 12).join(', ')}`;
        item.iconPath = new vscode.ThemeIcon('symbol-field');
        item.command = { command: 'renovatio.openDomainModel', title: 'Open Domain Model' };
        return item;
      });
      result.push(group);
    }

    if (entries.length) {
      const group = new vscode.TreeItem(`Parsed inventory (${parsedFileCount(inventory)})`, vscode.TreeItemCollapsibleState.Collapsed);
      group.iconPath = new vscode.ThemeIcon('list-tree');
      group.children = entries.map(([key, value]) => {
        const item = new vscode.TreeItem(`${humanize(key)}: ${value}`, vscode.TreeItemCollapsibleState.None);
        item.iconPath = new vscode.ThemeIcon(key.toLowerCase().includes('jcl') ? 'terminal' : 'symbol-file');
        return item;
      });
      result.push(group);
    }

    if (!result.length) {
      const item = new vscode.TreeItem('No analysis loaded', vscode.TreeItemCollapsibleState.None);
      item.iconPath = new vscode.ThemeIcon('warning');
      item.command = { command: 'renovatio.analyzeCobolSources', title: 'Analyze COBOL Sources' };
      return [item];
    }
    return result;
  }
}

class DomainModelProvider {
  constructor() {
    this._onDidChangeTreeData = new vscode.EventEmitter();
    this.onDidChangeTreeData = this._onDidChangeTreeData.event;
  }
  refresh() { this._onDidChangeTreeData.fire(); }
  getTreeItem(item) { return item; }
  getChildren(item) {
    if (item?.children) return item.children;
    const project = state.projects.find(candidate => candidate.id === state.activeProjectId);
    if (!project) {
      const select = new vscode.TreeItem('Select Renovatio project', vscode.TreeItemCollapsibleState.None);
      select.iconPath = new vscode.ThemeIcon('folder-active');
      select.command = { command: 'renovatio.selectProject', title: 'Select Renovatio Project' };
      return [select];
    }
    const domain = state.domainModel;
    if (!domain) {
      const refreshItem = new vscode.TreeItem('Domain model not loaded', vscode.TreeItemCollapsibleState.None);
      refreshItem.iconPath = new vscode.ThemeIcon('refresh');
      refreshItem.command = { command: 'renovatio.refresh', title: 'Refresh Renovatio' };
      return [refreshItem];
    }
    const model = domain.model || {};
    const nodes = asArray(model.nodes);
    const relations = asArray(model.relations);
    const invariants = asArray(model.invariants);
    const suggestions = asArray(domain.suggestions);
    if (!nodes.length && !relations.length && !invariants.length) {
      const analyze = new vscode.TreeItem('No DomainModel persisted yet', vscode.TreeItemCollapsibleState.None);
      analyze.description = 'run discovery';
      analyze.iconPath = new vscode.ThemeIcon('symbol-structure');
      analyze.command = { command: 'renovatio.analyzeCobolSources', title: 'Analyze COBOL Sources' };
      return [analyze];
    }
    const root = new vscode.TreeItem(`Revision ${domain.revision || 0}`, vscode.TreeItemCollapsibleState.None);
    root.description = `${nodes.length} nodes · ${relations.length} relations`;
    root.iconPath = new vscode.ThemeIcon('repo');
    root.command = { command: 'renovatio.openDomainModel', title: 'Open Domain Model' };

    const nodeGroup = new vscode.TreeItem(`Nodes (${nodes.length})`, vscode.TreeItemCollapsibleState.Expanded);
    nodeGroup.iconPath = new vscode.ThemeIcon('symbol-class');
    nodeGroup.children = nodes.map(node => {
      const child = new vscode.TreeItem(node.name || node.id, vscode.TreeItemCollapsibleState.None);
      child.description = node.kind || '';
      child.tooltip = `${node.id}\n${Math.round(Number(node.confidence || 0) * 100)}% confidence`;
      child.iconPath = new vscode.ThemeIcon(domainIconForKind(node.kind));
      child.command = { command: 'renovatio.openDomainModel', title: 'Open Domain Model' };
      return child;
    });

    const relationGroup = new vscode.TreeItem(`Relations (${relations.length})`, vscode.TreeItemCollapsibleState.Collapsed);
    relationGroup.iconPath = new vscode.ThemeIcon('references');
    relationGroup.children = relations.map(relation => {
      const child = new vscode.TreeItem(relation.kind || 'relation', vscode.TreeItemCollapsibleState.None);
      child.description = `${relation.fromId} -> ${relation.toId}`;
      child.iconPath = new vscode.ThemeIcon('arrow-right');
      child.command = { command: 'renovatio.openDomainModel', title: 'Open Domain Model' };
      return child;
    });

    const invariantGroup = new vscode.TreeItem(`Invariants (${invariants.length})`, vscode.TreeItemCollapsibleState.Collapsed);
    invariantGroup.iconPath = new vscode.ThemeIcon('shield');
    invariantGroup.children = invariants.map(invariant => {
      const child = new vscode.TreeItem(invariant.expression || invariant.id, vscode.TreeItemCollapsibleState.None);
      child.description = invariant.subjectId || '';
      child.iconPath = new vscode.ThemeIcon('symbol-boolean');
      child.command = { command: 'renovatio.openDomainModel', title: 'Open Domain Model' };
      return child;
    });

    const result = [root, nodeGroup, relationGroup, invariantGroup];
    if (suggestions.length) {
      const suggestionGroup = new vscode.TreeItem(`Suggestions (${suggestions.length})`, vscode.TreeItemCollapsibleState.Collapsed);
      suggestionGroup.iconPath = new vscode.ThemeIcon('sparkle');
      suggestionGroup.children = suggestions.map(suggestion => {
        const child = new vscode.TreeItem(suggestion.name || suggestion.targetId, vscode.TreeItemCollapsibleState.None);
        child.description = suggestion.status || 'pending';
        child.iconPath = new vscode.ThemeIcon('lightbulb');
        child.command = { command: 'renovatio.openDomainModel', title: 'Open Domain Model' };
        return child;
      });
      result.push(suggestionGroup);
    }
    return result;
  }
}

function domainIconForKind(kind) {
  const normalized = String(kind || '').toUpperCase();
  if (normalized.includes('USE_CASE')) return 'run';
  if (normalized.includes('REPOSITORY')) return 'database';
  if (normalized.includes('EXTERNAL')) return 'plug';
  if (normalized.includes('VALUE')) return 'symbol-value';
  if (normalized.includes('SERVICE')) return 'gear';
  return 'symbol-class';
}

function showError(prefix, error) {
  const text = `${prefix}: ${message(error)}`;
  output.appendLine(text);
  vscode.window.showErrorMessage(text);
}

function message(error) {
  return error instanceof Error ? error.message : String(error);
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]));
}

module.exports = { activate, deactivate };
