"use strict";
var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __commonJS = (cb, mod) => function __require() {
  try {
    return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
  } catch (e) {
    throw mod = 0, e;
  }
};
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

// src/legacyExtension.js
var require_legacyExtension = __commonJS({
  "src/legacyExtension.js"(exports2, module2) {
    "use strict";
    var vscode4 = require("vscode");
    var fs = require("fs");
    var http = require("http");
    var https = require("https");
    var os = require("os");
    var path = require("path");
    var zlib = require("zlib");
    var state = {
      projects: [],
      activeProjectId: void 0,
      analysis: void 0,
      latestJob: void 0,
      domainModel: void 0
    };
    var statusItem;
    var projectsProvider;
    var operationsProvider;
    var analysisProvider;
    var domainModelProvider;
    var output;
    var extensionContext;
    var controlDeckPanel;
    var analysisPanel;
    var projectPanel;
    var domainModelPanel;
    var persistencePanel;
    var welcomePanel;
    var welcomeOpenedThisSession = false;
    function activate3(context) {
      extensionContext = context;
      output = vscode4.window.createOutputChannel("Renovatio");
      statusItem = vscode4.window.createStatusBarItem(vscode4.StatusBarAlignment.Left, 80);
      statusItem.command = "renovatio.selectProject";
      statusItem.show();
      projectsProvider = new ProjectsProvider();
      operationsProvider = new OperationsProvider();
      analysisProvider = new AnalysisProvider();
      domainModelProvider = new DomainModelProvider();
      context.subscriptions.push(
        output,
        statusItem,
        vscode4.window.registerTreeDataProvider("renovatio.projects", projectsProvider),
        vscode4.window.registerTreeDataProvider("renovatio.operations", operationsProvider),
        vscode4.window.registerTreeDataProvider("renovatio.analysis", analysisProvider),
        vscode4.window.registerTreeDataProvider("renovatio.domainModel", domainModelProvider),
        vscode4.commands.registerCommand("renovatio.refresh", refresh),
        vscode4.commands.registerCommand("renovatio.openWelcome", openWelcome),
        vscode4.commands.registerCommand("renovatio.selectProject", selectProject),
        vscode4.commands.registerCommand("renovatio.createProject", createProject),
        vscode4.commands.registerCommand("renovatio.analyzeSelectedPath", analyzeSelectedPath),
        vscode4.commands.registerCommand("renovatio.analyzeCobolSources", analyzeCobolSources),
        vscode4.commands.registerCommand("renovatio.analyzeWorkspace", analyzeWorkspace),
        vscode4.commands.registerCommand("renovatio.addCobolSourceRoot", addCobolSourceRoot),
        vscode4.commands.registerCommand("renovatio.removeCobolSourceRoot", removeCobolSourceRoot),
        vscode4.commands.registerCommand("renovatio.selectGeneratedOutputFolder", selectGeneratedOutputFolder),
        vscode4.commands.registerCommand("renovatio.openControlDeck", openControlDeck),
        vscode4.commands.registerCommand("renovatio.openDomainModel", openDomainModel),
        vscode4.commands.registerCommand("renovatio.openNativeDomainDiagram", openNativeDomainDiagram),
        vscode4.commands.registerCommand("renovatio.openNativePersistenceDiagram", openNativePersistenceDiagram),
        vscode4.commands.registerCommand("renovatio.openNativeArchitectureDiagram", openNativeArchitectureDiagram),
        vscode4.commands.registerCommand("renovatio.openPersistenceModel", openPersistenceModel),
        vscode4.commands.registerCommand("renovatio.openGeneratedCode", openGeneratedCode),
        vscode4.commands.registerCommand("renovatio.openFile", openFile),
        vscode4.commands.registerCommand("renovatio.openEvidence", openEvidence),
        vscode4.workspace.onDidChangeConfiguration((event) => {
          if (event.affectsConfiguration("renovatio")) {
            updateStatus();
            projectsProvider.refresh();
            operationsProvider.refresh();
            void refreshVisiblePanels();
          }
        }),
        vscode4.workspace.onDidChangeWorkspaceFolders(async () => {
          await syncActiveProjectWithWorkspace();
          await refresh();
        })
      );
      state.activeProjectId = context.workspaceState.get("renovatio.activeProjectId");
      void refresh();
      void openWelcome({ once: true });
    }
    function deactivate2() {
    }
    function config() {
      const cfg = vscode4.workspace.getConfiguration("renovatio");
      return {
        backendUrl: String(cfg.get("backendUrl") || "http://127.0.0.1:8081").replace(/\/$/, ""),
        role: String(cfg.get("role") || "ADMIN")
      };
    }
    function request(method, path2, body) {
      const { backendUrl, role } = config();
      const target = new URL(`${backendUrl}${path2}`);
      const transport = target.protocol === "https:" ? https : http;
      const payload = body === void 0 ? void 0 : JSON.stringify(body);
      const headers = { "X-Role": role };
      if (payload) {
        headers["Content-Type"] = "application/json";
        headers["Content-Length"] = Buffer.byteLength(payload);
      }
      return new Promise((resolve, reject) => {
        const req = transport.request(target, { method, headers }, (res) => {
          let raw = "";
          res.setEncoding("utf8");
          res.on("data", (chunk) => {
            raw += chunk;
          });
          res.on("end", () => {
            if (!res.statusCode || res.statusCode < 200 || res.statusCode >= 300) {
              reject(new Error(raw || `Renovatio API returned ${res.statusCode}`));
              return;
            }
            if (!raw) {
              resolve(void 0);
              return;
            }
            try {
              resolve(JSON.parse(raw));
            } catch {
              resolve(raw);
            }
          });
        });
        req.on("error", reject);
        if (payload) req.write(payload);
        req.end();
      });
    }
    async function refresh() {
      try {
        state.projects = await request("GET", "/api/workbench/projects");
        await syncActiveProjectWithWorkspace();
        if (!state.activeProjectId || !state.projects.some((project) => project.id === state.activeProjectId)) {
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
          const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
          if (project) await openProjectPanel(project);
        }
      } catch (error) {
        showError("Could not refresh Renovatio projects", error);
      }
    }
    async function refreshAnalysis() {
      if (!state.activeProjectId) {
        state.analysis = void 0;
        return;
      }
      try {
        state.analysis = await request("GET", `/api/projects/${encodeURIComponent(state.activeProjectId)}/workbench/analysis`);
      } catch (error) {
        output.appendLine(`Analysis refresh failed: ${message3(error)}`);
        state.analysis = void 0;
      }
    }
    async function refreshDomainModel() {
      if (!state.activeProjectId) {
        state.domainModel = void 0;
        return;
      }
      try {
        state.domainModel = await request("GET", `/api/projects/${encodeURIComponent(state.activeProjectId)}/workbench/domain-model`);
      } catch (error) {
        output.appendLine(`Domain model refresh failed: ${message3(error)}`);
        state.domainModel = void 0;
      }
    }
    async function selectProject(projectId) {
      await refresh();
      if (projectId) {
        const project = state.projects.find((candidate) => candidate.id === projectId);
        if (project) {
          const hydrated2 = await hydrateProject(project);
          await activateProject(hydrated2, { openWorkspace: false });
          await refreshAnalysis();
          await refreshDomainModel();
          analysisProvider.refresh();
          domainModelProvider.refresh();
          await openProjectPanel(hydrated2);
          return;
        }
      }
      const picks = state.projects.map((project) => ({
        label: project.name,
        description: project.workspacePath || project.cobolScanRoot || project.id,
        project
      }));
      const selected = await vscode4.window.showQuickPick(picks, { placeHolder: "Select Renovatio project" });
      if (!selected) return;
      const hydrated = await hydrateProject(selected.project);
      await activateProject(hydrated, { openWorkspace: false });
      await refreshAnalysis();
      await refreshDomainModel();
      analysisProvider.refresh();
      domainModelProvider.refresh();
      await openProjectPanel(hydrated);
    }
    async function activateProject(project, options = {}) {
      state.activeProjectId = project.id;
      await extensionContext.workspaceState.update("renovatio.activeProjectId", project.id);
      updateStatus();
      projectsProvider.refresh();
      operationsProvider.refresh();
      if (options.openWorkspace) {
        await ensureProjectWorkspaceFolder(project);
      }
    }
    async function ensureProjectWorkspaceFolder(project) {
      if (!project.workspacePath) return;
      const uri = vscode4.Uri.file(project.workspacePath);
      const current = vscode4.workspace.workspaceFolders || [];
      if (current.some((folder) => samePath(folder.uri.fsPath, uri.fsPath))) return;
      vscode4.workspace.updateWorkspaceFolders(current.length, 0, { uri, name: project.name });
    }
    async function analyzeSelectedPath(resource) {
      const resourceUri = resource instanceof vscode4.Uri ? resource : void 0;
      const project = await ensureActiveProject(resourceUri);
      if (!project) return;
      const scanRoot = resourceUri?.fsPath || vscode4.window.activeTextEditor?.document.uri.fsPath || await selectWorkspaceFolderPath();
      if (!scanRoot) {
        vscode4.window.showWarningMessage("Select a folder or file in Explorer before running Renovatio analysis.");
        return;
      }
      const workspaceRoot = workspaceFolderPathFor(vscode4.Uri.file(scanRoot)) || project.workspacePath || scanRoot;
      const ok = await confirmExternalScanRoot(project, scanRoot, workspaceRoot);
      if (!ok) return;
      await runAnalysis(project, scanRoot, workspaceRoot);
    }
    async function analyzeCobolSources() {
      const project = await ensureActiveProject();
      if (!project) return;
      const roots = await ensureCobolRootsForProject(project);
      if (!roots.length) return;
      const settings = workspaceSettings(project);
      const scanRoot = roots.length === 1 ? roots[0] : await pickCobolSourceRoot(roots);
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
      const roots = await ensureCobolRootsForProject(project);
      if (roots.length) {
        const scanRoot = roots.length === 1 ? roots[0] : await pickCobolSourceRoot(roots);
        if (scanRoot) await runAnalysis(project, scanRoot, workspaceSettings(project).workspaceFolderPath || project.workspacePath || scanRoot);
        return;
      }
      const workspacePath = await selectWorkspaceFolderPath();
      if (!workspacePath) {
        vscode4.window.showWarningMessage("Open or select a Renovatio workspace before analyzing.");
        return;
      }
      await runAnalysis(project, workspacePath, workspacePath);
    }
    async function runAnalysis(project, scanRoot, workspaceRoot) {
      const settings = workspaceSettings(project, workspaceRoot);
      const generatedRoot = settings.generatedRoot || project.javaOutputPath || null;
      const apiWorkspace = analysisWorkspacePath(project, scanRoot, workspaceRoot);
      await vscode4.window.withProgress({ location: vscode4.ProgressLocation.Notification, title: "Renovatio analysis", cancellable: false }, async (progress) => {
        progress.report({ message: `Using ${scanRoot}` });
        await activateProject(project, { openWorkspace: false });
        progress.report({ message: "Starting analyzer job" });
        const job = await request("POST", `/api/projects/${encodeURIComponent(project.id)}/jobs`, {
          operation: "analyze",
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
        vscode4.window.showInformationMessage(`Renovatio analyze ${completed.status || "completed"}: ${completed.id}`);
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
      const resourceUri = resource instanceof vscode4.Uri ? resource : void 0;
      const project = state.projects.length ? await ensureActiveProject(resourceUri) : void 0;
      const workspaceRoot = workspaceFolderPathFor(resourceUri) || workspaceSettings(project).workspaceFolderPath || void 0;
      let selectedUris = [];
      if (resourceUri) {
        selectedUris = [await directoryUriFor(resourceUri)];
      } else {
        selectedUris = await vscode4.window.showOpenDialog({
          canSelectFiles: false,
          canSelectFolders: true,
          canSelectMany: true,
          defaultUri: vscode4.Uri.file(workspaceRoot || os.homedir()),
          openLabel: "Add COBOL source root",
          title: "Select COBOL source root"
        }) || [];
      }
      if (!selectedUris.length) return;
      const settings = workspaceSettings(project, workspaceRoot);
      const selectedPaths = await Promise.all(selectedUris.map((uri) => resolveRealPath(uri.fsPath)));
      if (!project) {
        await createProjectFromCobolRoots(selectedPaths);
        return;
      }
      const mode = await chooseCobolImportMode(project, settings, selectedPaths);
      if (!mode || mode === "cancel") return;
      if (mode === "newProject") {
        await createProjectFromCobolRoots(selectedPaths);
        return;
      }
      const destinationRoots = settings.generatedRoots.length ? settings.generatedRoots : [defaultDestinationRoot(project, settings.targetLanguage)].filter(Boolean);
      await ensureRenovatioWorkspaceFolders(selectedPaths, destinationRoots, project);
      const additions = selectedPaths.map((fsPath) => path.normalize(fsPath));
      const next = mode === "replace" ? uniqueStrings2(additions) : uniqueStrings2([...settings.configuredCobolRoots, ...additions]);
      await rememberRenovatioWorkspace(project, next, destinationRoots);
      await updateWorkspaceSetting("cobolRoots", next, void 0, { target: "workspace" });
      if (destinationRoots.length) {
        await updateWorkspaceSetting("generatedRoots", uniqueStrings2(destinationRoots), void 0, { target: "workspace" });
        await updateWorkspaceSetting("generatedRoot", destinationRoots[0], void 0, { target: "workspace" });
      }
      vscode4.window.showInformationMessage(`${mode === "replace" ? "Replaced" : "Configured"} ${next.length} COBOL source root(s).`);
      projectsProvider.refresh();
      operationsProvider.refresh();
      await refreshVisiblePanels();
    }
    async function createProject(resource) {
      const resourceUri = resource instanceof vscode4.Uri ? resource : void 0;
      const workspaceRoot = workspaceFolderPathFor(resourceUri) || await selectWorkspaceFolderPath();
      let selectedUris = [];
      if (resourceUri) {
        selectedUris = [await directoryUriFor(resourceUri)];
      } else {
        selectedUris = await vscode4.window.showOpenDialog({
          canSelectFiles: false,
          canSelectFolders: true,
          canSelectMany: false,
          defaultUri: vscode4.Uri.file(workspaceRoot || os.homedir()),
          openLabel: "Create project from COBOL root",
          title: "Select COBOL source root for the new Renovatio project"
        }) || [];
      }
      if (!selectedUris.length) return;
      await createProjectFromCobolRoots(selectedUris.map((uri) => uri.fsPath));
    }
    async function removeCobolSourceRoot() {
      const project = await ensureActiveProject();
      if (!project) return;
      const settings = workspaceSettings(project);
      if (!settings.rawCobolRoots.length) {
        vscode4.window.showWarningMessage("No configured COBOL source roots to remove.");
        return;
      }
      const selected = await vscode4.window.showQuickPick(settings.rawCobolRoots.map((root, index) => ({
        label: root,
        description: settings.cobolRoots[index],
        root
      })), { placeHolder: "Remove COBOL source root" });
      if (!selected) return;
      await updateWorkspaceSetting("cobolRoots", settings.rawCobolRoots.filter((root) => root !== selected.root), settings.workspaceFolderPath);
      projectsProvider.refresh();
      operationsProvider.refresh();
      await refreshVisiblePanels();
    }
    async function chooseCobolImportMode(project, settings, selectedPaths) {
      const selected = selectedPaths.map((value2) => path.normalize(value2));
      const existing = settings.configuredCobolRoots.map((value2) => path.normalize(value2));
      const hasExistingWork = Boolean(
        existing.length || asArray(state.domainModel?.model?.nodes).length || state.analysis || project?.cobolScanRoot
      );
      if (!hasExistingWork) return "add";
      const alreadyKnown = selected.every((candidate) => existing.some((root) => samePath(candidate, root)));
      if (alreadyKnown) return "add";
      const action2 = await vscode4.window.showWarningMessage(
        `You selected a different COBOL source path for "${project?.name || "the current Renovatio project"}". Keep the current work separate or update this project?`,
        { modal: true, detail: `Selected:
${selected.join("\n")}

Current roots:
${existing.length ? existing.join("\n") : project?.cobolScanRoot || project?.workspacePath || "workspace fallback"}` },
        "Create New Project",
        "Add to Current Project",
        "Replace Current Roots",
        "Cancel"
      );
      if (action2 === "Create New Project") return "newProject";
      if (action2 === "Add to Current Project") return "add";
      if (action2 === "Replace Current Roots") {
        const confirm = await vscode4.window.showWarningMessage(
          "Replace the current COBOL roots for this VS Code workspace? Existing Renovatio backend/domain history is kept, but future analysis will update the active project from the new path.",
          { modal: true },
          "Replace Roots",
          "Cancel"
        );
        return confirm === "Replace Roots" ? "replace" : "cancel";
      }
      return "cancel";
    }
    async function createProjectFromCobolRoots(selectedPaths) {
      const resolvedPaths = await Promise.all(selectedPaths.map(resolveRealPath));
      const primaryRoot = resolvedPaths[0];
      if (!primaryRoot) return;
      const defaultName = path.basename(primaryRoot) || "renovatio-project";
      const name = await vscode4.window.showInputBox({
        title: "Create Renovatio project",
        prompt: "Name for the new Renovatio project",
        value: defaultName,
        ignoreFocusOut: true
      });
      if (!name) return;
      try {
        const settings = workspaceSettings();
        const created = await createBackendProject(name, primaryRoot);
        rememberProject(created);
        await activateProject(created, { openWorkspace: false });
        const destinationRoot = defaultDestinationRoot(created, settings.targetLanguage);
        const destinationRoots = [destinationRoot].filter(Boolean);
        await ensureRenovatioWorkspaceFolders(resolvedPaths, destinationRoots, created);
        await rememberRenovatioWorkspace(created, resolvedPaths, destinationRoots);
        await updateWorkspaceSetting("cobolRoots", resolvedPaths.map((root) => path.normalize(root)), void 0, { target: "workspace" });
        if (destinationRoots.length) {
          await updateWorkspaceSetting("generatedRoots", destinationRoots, void 0, { target: "workspace" });
          await updateWorkspaceSetting("generatedRoot", destinationRoots[0], void 0, { target: "workspace" });
        }
        await updateWorkspaceSetting("targetLanguage", settings.targetLanguage, void 0, { target: "workspace" });
        await updateWorkspaceSetting("targetPackage", settings.targetPackage, void 0, { target: "workspace" });
        await refresh();
        vscode4.window.showInformationMessage(`Created Renovatio project "${created.name || name}" for ${primaryRoot}.`);
        projectsProvider.refresh();
        operationsProvider.refresh();
        await refreshVisiblePanels();
      } catch (error) {
        showError("Could not create Renovatio project for the selected COBOL path", error);
      }
    }
    async function createBackendProject(name, workspacePath) {
      const targetPackage = workspaceSettings().targetPackage;
      const targetLanguage = workspaceSettings().targetLanguage;
      return request("POST", "/api/projects", {
        name,
        workspacePath: managedWorkspaceName(name, workspacePath),
        javaOutputPath: `generated/${targetLanguage}`,
        javaPackage: targetPackage,
        javaArchitecture: "layered"
      });
    }
    function managedWorkspaceName(name, workspacePath) {
      const base = path.basename(workspacePath) || name || "renovatio-project";
      return `vscode-${base}`.toLowerCase().replace(/[^a-z0-9._-]+/g, "-").replace(/^-+|-+$/g, "") || "vscode-renovatio-project";
    }
    function defaultDestinationRoot(project, targetLanguage = "java") {
      if (!project) return "";
      return path.normalize(project.javaOutputPath || path.join(project.workspacePath || "", "generated", targetLanguage));
    }
    async function ensureRenovatioWorkspaceFolders(sourceRoots, destinationRoots, project) {
      const sources = uniqueStrings2(asArray(sourceRoots).filter(Boolean).map((value2) => path.normalize(value2)));
      const destinations = uniqueStrings2(asArray(destinationRoots).filter(Boolean).map((value2) => path.normalize(value2)));
      for (const destination of destinations) {
        await vscode4.workspace.fs.createDirectory(vscode4.Uri.file(destination));
      }
      const folders = [...sources.map((root) => ({
        uri: vscode4.Uri.file(root),
        name: `source:${path.basename(root) || "cobol"}`
      })), ...destinations.map((root) => ({
        uri: vscode4.Uri.file(root),
        name: `dest:${path.basename(root) || "generated"}`
      }))];
      if (!folders.length) return;
      const current = vscode4.workspace.workspaceFolders || [];
      const additions = folders.filter((folder) => !current.some((existing) => samePath(existing.uri.fsPath, folder.uri.fsPath)));
      if (!additions.length) return;
      const ok = vscode4.workspace.updateWorkspaceFolders(current.length, 0, ...additions);
      if (!ok) {
        throw new Error("VS Code rejected the workspace folder update for Renovatio sources/destinations.");
      }
      await waitForWorkspaceFolders(additions.map((folder) => folder.uri.fsPath));
      output.appendLine(`Renovatio workspace folders added for ${project?.name || "project"}: ${additions.map((folder) => folder.uri.fsPath).join(", ")}`);
    }
    async function discoverCobolWorkspaceRoots() {
      const folders = asArray(vscode4.workspace.workspaceFolders).filter((folder) => !String(folder.name || "").startsWith("dest:"));
      const roots = [];
      for (const folder of folders) {
        if (await containsCobolArtifacts(folder.uri, 0)) {
          roots.push(path.normalize(folder.uri.fsPath));
        }
      }
      if (!roots.length && folders.length) {
        const matches = await vscode4.workspace.findFiles("**/*.{cbl,cob,cobol,cpy,copybook,jcl,job,proc}", "**/{node_modules,target,build,dist,out,.git}/**", 200);
        for (const match of matches) {
          const folder = vscode4.workspace.getWorkspaceFolder(match);
          if (folder && !String(folder.name || "").startsWith("dest:")) {
            roots.push(path.normalize(folder.uri.fsPath));
          }
        }
      }
      return uniqueStrings2(roots);
    }
    async function ensureCobolRootsForProject(project) {
      let settings = workspaceSettings(project);
      if (settings.configuredCobolRoots.length) {
        return settings.configuredCobolRoots;
      }
      let sourceRoots = await projectDefaultCobolRoots(project);
      if (!sourceRoots.length) {
        sourceRoots = await discoverCobolWorkspaceRoots();
      }
      if (!sourceRoots.length) {
        sourceRoots = rememberedLastSourceRoots();
      }
      if (!sourceRoots.length) {
        const selected = await vscode4.window.showOpenDialog({
          canSelectFiles: false,
          canSelectFolders: true,
          canSelectMany: true,
          defaultUri: vscode4.Uri.file(os.homedir()),
          openLabel: "Use COBOL source root",
          title: `Select COBOL source root for ${project?.name || "Renovatio"}`
        }) || [];
        sourceRoots = await Promise.all(selected.map((uri) => resolveRealPath(uri.fsPath)));
      }
      sourceRoots = uniqueStrings2(sourceRoots.map((root) => path.normalize(root)).filter(Boolean));
      if (!sourceRoots.length) {
        vscode4.window.showWarningMessage("Renovatio needs at least one COBOL source root before analysis.");
        return [];
      }
      settings = workspaceSettings(project);
      const destinationRoots = settings.generatedRoots.length ? settings.generatedRoots : [defaultDestinationRoot(project, settings.targetLanguage)].filter(Boolean);
      await ensureRenovatioWorkspaceFolders(sourceRoots, destinationRoots, project);
      await rememberRenovatioWorkspace(project, sourceRoots, destinationRoots);
      await updateWorkspaceSetting("cobolRoots", sourceRoots, void 0, { target: "workspace" });
      if (destinationRoots.length) {
        await updateWorkspaceSetting("generatedRoots", destinationRoots, void 0, { target: "workspace" });
        await updateWorkspaceSetting("generatedRoot", destinationRoots[0], void 0, { target: "workspace" });
      }
      projectsProvider.refresh();
      operationsProvider.refresh();
      await refreshVisiblePanels();
      return sourceRoots;
    }
    async function projectDefaultCobolRoots(project) {
      const candidates = uniqueStrings2([
        project?.cobolScanRoot,
        ...asArray(project?.sourceRoots),
        ...asArray(project?.cobolRoots)
      ].filter(Boolean).map((value2) => path.normalize(String(value2))));
      const roots = [];
      for (const candidate of candidates) {
        if (await containsCobolArtifacts(vscode4.Uri.file(candidate), 0)) {
          roots.push(candidate);
        }
      }
      return uniqueStrings2(roots);
    }
    async function rememberRenovatioWorkspace(project, sourceRoots, destinationRoots) {
      if (!project?.id) return;
      const value2 = {
        projectId: project.id,
        sourceRoots: uniqueStrings2(asArray(sourceRoots).filter(Boolean).map((value3) => path.normalize(value3))),
        destinationRoots: uniqueStrings2(asArray(destinationRoots).filter(Boolean).map((value3) => path.normalize(value3))),
        updatedAt: (/* @__PURE__ */ new Date()).toISOString()
      };
      await extensionContext.workspaceState.update(renovatioWorkspaceKey(project.id), value2);
      await extensionContext.globalState.update(renovatioWorkspaceKey(project.id), value2);
      await extensionContext.globalState.update("renovatio.lastCobolSourceRoots", value2.sourceRoots);
    }
    function rememberedWorkspace(project) {
      if (!project?.id || !extensionContext) return { sourceRoots: [], destinationRoots: [] };
      const key = renovatioWorkspaceKey(project.id);
      const value2 = extensionContext.workspaceState.get(key) || extensionContext.globalState.get(key) || {};
      return {
        sourceRoots: asArray(value2.sourceRoots).map((value3) => path.normalize(String(value3))).filter(Boolean),
        destinationRoots: asArray(value2.destinationRoots).map((value3) => path.normalize(String(value3))).filter(Boolean)
      };
    }
    function rememberedLastSourceRoots() {
      if (!extensionContext) return [];
      return asArray(extensionContext.globalState.get("renovatio.lastCobolSourceRoots")).map((value2) => path.normalize(String(value2))).filter(Boolean);
    }
    function renovatioWorkspaceKey(projectId) {
      return `renovatio.workspaceContext.${projectId}`;
    }
    function workspaceFoldersByPrefix(prefix) {
      return asArray(vscode4.workspace.workspaceFolders).filter((folder) => String(folder.name || "").startsWith(prefix)).map((folder) => folder.uri.fsPath);
    }
    async function waitForWorkspaceFolders(expectedPaths) {
      const expected = expectedPaths.map((value2) => path.normalize(value2));
      for (let attempt = 0; attempt < 20; attempt += 1) {
        const current = vscode4.workspace.workspaceFolders || [];
        if (expected.every((candidate) => current.some((folder) => samePath(folder.uri.fsPath, candidate)))) {
          return;
        }
        await sleep(50);
      }
    }
    async function resolveRealPath(fsPath) {
      try {
        return await fs.promises.realpath(fsPath);
      } catch {
        return fsPath;
      }
    }
    async function confirmExternalScanRoot(project, scanRoot, workspaceRoot) {
      const settings = workspaceSettings(project, workspaceRoot);
      const normalized = path.normalize(scanRoot);
      const knownRoots = uniqueStrings2([
        ...settings.configuredCobolRoots,
        project?.cobolScanRoot,
        project?.workspacePath
      ].filter(Boolean)).map((value2) => path.normalize(value2));
      if (!knownRoots.length) return true;
      const known = knownRoots.some((root) => samePath(normalized, root) || isInsidePath(normalized, root) || isInsidePath(root, normalized));
      if (known) return true;
      const action2 = await vscode4.window.showWarningMessage(
        `This path is not configured as a COBOL root for "${project?.name || "the active Renovatio project"}".`,
        { modal: true, detail: `Selected:
${normalized}

Configured/current:
${knownRoots.join("\n")}` },
        "Analyze Current Project",
        "Add Root First",
        "Cancel"
      );
      if (action2 === "Analyze Current Project") return true;
      if (action2 === "Add Root First") {
        const destinations = settings.generatedRoots.length ? settings.generatedRoots : [defaultDestinationRoot(project, settings.targetLanguage)].filter(Boolean);
        await ensureRenovatioWorkspaceFolders([normalized], destinations, project);
        const next = uniqueStrings2([...settings.configuredCobolRoots, normalized]);
        await rememberRenovatioWorkspace(project, next, destinations);
        await updateWorkspaceSetting("cobolRoots", next, void 0, { target: "workspace" });
        if (destinations.length) {
          await updateWorkspaceSetting("generatedRoots", uniqueStrings2(destinations), void 0, { target: "workspace" });
          await updateWorkspaceSetting("generatedRoot", destinations[0], void 0, { target: "workspace" });
        }
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
      const workspaceRoot = settings.workspaceFolderPath || project.workspacePath || os.homedir();
      const selected = await vscode4.window.showOpenDialog({
        canSelectFiles: false,
        canSelectFolders: true,
        canSelectMany: false,
        defaultUri: vscode4.Uri.file(settings.generatedRoot || workspaceRoot),
        openLabel: "Use future output folder",
        title: "Configure future output folder"
      });
      const uri = selected?.[0];
      if (!uri) return;
      const destinationRoot = path.normalize(uri.fsPath);
      await ensureRenovatioWorkspaceFolders(settings.configuredCobolRoots, [destinationRoot], project);
      const generatedRoots = uniqueStrings2([destinationRoot, ...settings.generatedRoots]);
      await rememberRenovatioWorkspace(project, settings.configuredCobolRoots, generatedRoots);
      await updateWorkspaceSetting("generatedRoots", generatedRoots, void 0, { target: "workspace" });
      await updateWorkspaceSetting("generatedRoot", destinationRoot, void 0, { target: "workspace" });
      projectsProvider.refresh();
      operationsProvider.refresh();
      await refreshVisiblePanels();
    }
    async function openGeneratedCode() {
      const project = await ensureActiveProject();
      if (!project) return;
      const generated = await loadGeneratedArtifacts(project);
      if (!generated.exists) {
        const action2 = await vscode4.window.showWarningMessage(`No future output folder found at ${generated.root}`, "Create Folder");
        if (action2) {
          const uri = vscode4.Uri.file(generated.root);
          await vscode4.workspace.fs.createDirectory(uri);
          await vscode4.commands.executeCommand("revealInExplorer", uri);
        }
        await openProjectPanel(project);
        return;
      }
      if (!generated.files.length) {
        await vscode4.commands.executeCommand("revealInExplorer", vscode4.Uri.file(generated.root));
        vscode4.window.showInformationMessage(`Future output exists but has no generated source files yet: ${generated.root}`);
        await openProjectPanel(project);
        return;
      }
      const first = generated.files[0];
      const document = await vscode4.workspace.openTextDocument(vscode4.Uri.file(path.join(generated.root, first.relativePath)));
      await vscode4.window.showTextDocument(document, { preview: false });
    }
    async function openFile(fsPath) {
      if (!fsPath) return;
      const document = await vscode4.workspace.openTextDocument(vscode4.Uri.file(String(fsPath)));
      await vscode4.window.showTextDocument(document, { preview: false });
    }
    async function openEvidence(sourceRef) {
      const parsed = parseSourceRef(sourceRef);
      if (!parsed.fsPath) return;
      const resolved = await resolveEvidencePath(parsed.fsPath);
      if (!resolved) {
        vscode4.window.showWarningMessage(`Could not resolve evidence source: ${parsed.fsPath}`);
        return;
      }
      const document = await vscode4.workspace.openTextDocument(vscode4.Uri.file(resolved));
      const editor = await vscode4.window.showTextDocument(document, { preview: false });
      if (parsed.line && parsed.line > 0) {
        const position = new vscode4.Position(Math.min(parsed.line - 1, document.lineCount - 1), 0);
        editor.selection = new vscode4.Selection(position, position);
        editor.revealRange(new vscode4.Range(position, position), vscode4.TextEditorRevealType.InCenter);
      }
    }
    function parseSourceRef(sourceRef) {
      const raw = String(sourceRef || "").trim();
      if (!raw) return { fsPath: "", line: void 0 };
      const match = raw.match(/^(.*?)(?::(\d+)(?::\d+)?)?$/);
      if (!match) return { fsPath: raw, line: void 0 };
      return {
        fsPath: match[1] || raw,
        line: match[2] ? Number(match[2]) : void 0
      };
    }
    async function resolveEvidencePath(fsPath) {
      const expanded = expandHome(fsPath);
      if (path.isAbsolute(expanded) && await pathExists(expanded)) return path.normalize(expanded);
      const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
      const settings = workspaceSettings(project);
      const roots = uniqueStrings2([
        ...settings.cobolRoots,
        settings.workspaceFolderPath,
        project?.cobolScanRoot,
        project?.workspacePath,
        ...asArray(vscode4.workspace.workspaceFolders).map((folder) => folder.uri.fsPath)
      ].filter(Boolean));
      for (const root of roots) {
        const candidate = path.normalize(path.join(root, expanded));
        if (await pathExists(candidate)) return candidate;
      }
      return path.isAbsolute(expanded) ? path.normalize(expanded) : "";
    }
    async function pathExists(fsPath) {
      try {
        await vscode4.workspace.fs.stat(vscode4.Uri.file(fsPath));
        return true;
      } catch {
        return false;
      }
    }
    async function ensureActiveProject(resourceUri) {
      if (!state.projects.length) await refresh();
      const resourceProject = resourceUri ? projectForResource(resourceUri) : void 0;
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
      let project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
      if (!project) {
        await selectProject();
        project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
      }
      if (!project) vscode4.window.showWarningMessage("Create or select a Renovatio project first.");
      return hydrateProject(project);
    }
    async function hydrateProject(project) {
      if (!project || project.workspacePath) return project;
      try {
        const full = await request("GET", `/api/projects/${encodeURIComponent(project.id)}`);
        const merged = { ...project, ...full };
        rememberProject(merged);
        return merged;
      } catch (error) {
        output.appendLine(`Could not load Renovatio project details for ${project.id}: ${message3(error)}`);
        return project;
      }
    }
    async function syncActiveProjectWithWorkspace() {
      const project = projectForOpenWorkspace();
      if (!project || project.id === state.activeProjectId) return;
      state.activeProjectId = project.id;
      await extensionContext.workspaceState.update("renovatio.activeProjectId", project.id);
    }
    function projectForOpenWorkspace() {
      const folders = vscode4.workspace.workspaceFolders || [];
      return state.projects.find((project) => folders.some((folder) => projectMatchesFolder(project, folder.uri.fsPath)));
    }
    function projectForResource(resourceUri) {
      const folderPath = workspaceFolderPathFor(resourceUri);
      if (!folderPath) return void 0;
      return state.projects.find((project) => projectMatchesFolder(project, folderPath));
    }
    function projectMatchesFolder(project, folderPath) {
      const remembered = rememberedWorkspace(project);
      const candidates = [
        project.workspacePath,
        project.cobolScanRoot,
        ...remembered.sourceRoots,
        ...remembered.destinationRoots
      ].filter(Boolean);
      return candidates.some((candidate) => samePath(candidate, folderPath) || isInsidePath(candidate, folderPath) || isInsidePath(folderPath, candidate));
    }
    function workspaceFolderPathFor(uri) {
      if (!uri) return void 0;
      const folder = vscode4.workspace.getWorkspaceFolder(uri);
      return folder?.uri.fsPath;
    }
    async function selectWorkspaceFolderPath() {
      const folders = vscode4.workspace.workspaceFolders || [];
      if (!folders.length) return void 0;
      if (folders.length === 1) return folders[0].uri.fsPath;
      const selected = await vscode4.window.showWorkspaceFolderPick({ placeHolder: "Select VS Code workspace folder for Renovatio analysis" });
      return selected?.uri.fsPath;
    }
    function workspaceSettings(project, workspaceRoot) {
      const folderPath = workspaceRoot || workspaceFolderForProject(project)?.uri.fsPath || vscode4.workspace.workspaceFolders?.[0]?.uri.fsPath || project?.workspacePath;
      const cfg = workspaceConfiguration(folderPath);
      const remembered = rememberedWorkspace(project);
      const targetLanguage = String(cfg.get("targetLanguage") || "java");
      const targetPackage = String(cfg.get("targetPackage") || "com.example.modernized");
      const rawCobolRoots = uniqueStrings2([
        ...arraySetting(cfg.get("cobolRoots")),
        ...remembered.sourceRoots,
        ...workspaceFoldersByPrefix("source:")
      ]);
      const configuredCobolRoots = rawCobolRoots.map((root) => resolveConfiguredPath(root, folderPath)).filter(Boolean);
      const fallbackCobolRoot = project?.cobolScanRoot || folderPath || project?.workspacePath;
      const rawGeneratedRoots = uniqueStrings2([
        ...arraySetting(cfg.get("generatedRoots")),
        ...remembered.destinationRoots,
        ...workspaceFoldersByPrefix("dest:")
      ]);
      const rawGeneratedRoot = String(cfg.get("generatedRoot") || `generated/${targetLanguage}`);
      const configuredGeneratedRoots = rawGeneratedRoots.map((root) => resolveConfiguredPath(root, folderPath || project?.workspacePath || project?.cobolScanRoot)).filter(Boolean);
      const configuredGeneratedRoot = configuredGeneratedRoots[0] || resolveConfiguredPath(rawGeneratedRoot || project?.javaOutputPath || `generated/${targetLanguage}`, folderPath || project?.workspacePath || project?.cobolScanRoot);
      const suggestedGeneratedRoot = resolveConfiguredPath(`generated/${targetLanguage}`, folderPath || project?.workspacePath || project?.cobolScanRoot);
      const generatedRootWarning = generatedRootConflict(configuredGeneratedRoot, configuredCobolRoots);
      const generatedRoot = generatedRootWarning ? suggestedGeneratedRoot : configuredGeneratedRoot;
      return {
        workspaceFolderPath: folderPath,
        rawCobolRoots,
        configuredCobolRoots,
        cobolRoots: configuredCobolRoots.length ? configuredCobolRoots : [fallbackCobolRoot].filter(Boolean),
        rawGeneratedRoots,
        configuredGeneratedRoots,
        generatedRoots: configuredGeneratedRoots.length ? configuredGeneratedRoots : [generatedRoot].filter(Boolean),
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
    function analysisWorkspacePath(project, scanRoot, fallbackWorkspaceRoot) {
      if (scanRoot) {
        try {
          const stat = fs.statSync(scanRoot);
          return stat.isDirectory() ? scanRoot : path.dirname(scanRoot);
        } catch {
          return scanRoot;
        }
      }
      return apiWorkspacePath(project, fallbackWorkspaceRoot);
    }
    function generatedRootConflict(generatedRoot, cobolRoots) {
      if (!generatedRoot || !cobolRoots.length) return "";
      const conflict = cobolRoots.find((root) => samePath(generatedRoot, root) || isInsidePath(root, generatedRoot));
      if (!conflict) return "";
      return `Configured future output overlaps a COBOL source root: ${conflict}`;
    }
    function workspaceFolderForProject(project) {
      const folders = vscode4.workspace.workspaceFolders || [];
      if (!project) return folders[0];
      return folders.find((folder) => projectMatchesFolder(project, folder.uri.fsPath)) || folders[0];
    }
    function arraySetting(value2) {
      return Array.isArray(value2) ? value2.map((item) => String(item).trim()).filter(Boolean) : [];
    }
    function resolveConfiguredPath(value2, basePath) {
      if (!value2) return "";
      const expanded = expandHome(String(value2));
      if (path.isAbsolute(expanded)) return path.normalize(expanded);
      if (!basePath) return path.normalize(expanded);
      return path.normalize(path.join(basePath, expanded));
    }
    function expandHome(value2) {
      if (value2 === "~") return os.homedir();
      if (value2.startsWith(`~${path.sep}`) || value2.startsWith("~/")) return path.join(os.homedir(), value2.slice(2));
      return value2;
    }
    async function updateWorkspaceSetting(key, value2, workspaceRoot, options = {}) {
      const cfg = workspaceConfiguration(workspaceRoot);
      const folder = workspaceRoot ? workspaceFolderForPath(workspaceRoot) : void 0;
      if (options.target === "workspace" && vscode4.workspace.workspaceFolders?.length) {
        await cfg.update(key, value2, vscode4.ConfigurationTarget.Workspace);
        return true;
      }
      if (folder) {
        await cfg.update(key, value2, vscode4.ConfigurationTarget.WorkspaceFolder);
        return true;
      }
      if (vscode4.workspace.workspaceFolders?.length) {
        await cfg.update(key, value2, vscode4.ConfigurationTarget.Workspace);
        return true;
      }
      if (options.optional) {
        output.appendLine(`Skipped writing renovatio.${key}; no VS Code workspace is open.`);
        return false;
      }
      const action2 = await vscode4.window.showWarningMessage(
        `Unable to save renovatio.${key} because no VS Code workspace is open.`,
        "Open Folder",
        "Save Globally",
        "Cancel"
      );
      if (action2 === "Open Folder" && workspaceRoot) {
        vscode4.workspace.updateWorkspaceFolders(0, 0, { uri: vscode4.Uri.file(workspaceRoot), name: path.basename(workspaceRoot) || "Renovatio" });
        return false;
      }
      if (action2 === "Save Globally") {
        await cfg.update(key, value2, vscode4.ConfigurationTarget.Global);
        return true;
      }
      return false;
    }
    function workspaceConfiguration(folderPath) {
      if (!folderPath) return vscode4.workspace.getConfiguration("renovatio");
      return vscode4.workspace.getConfiguration("renovatio", vscode4.Uri.file(folderPath));
    }
    function workspaceFolderForPath(folderPath) {
      if (!folderPath) return void 0;
      const folders = vscode4.workspace.workspaceFolders || [];
      return folders.find((folder) => samePath(folder.uri.fsPath, folderPath) || isInsidePath(folderPath, folder.uri.fsPath));
    }
    async function pickCobolSourceRoot(roots) {
      const selected = await vscode4.window.showQuickPick(roots.map((root) => ({
        label: path.basename(root) || root,
        description: root,
        root
      })), { placeHolder: "Select COBOL source root to analyze" });
      return selected?.root;
    }
    async function directoryUriFor(uri) {
      try {
        const stat = await vscode4.workspace.fs.stat(uri);
        if (stat.type === vscode4.FileType.Directory) return uri;
      } catch {
      }
      return vscode4.Uri.file(path.dirname(uri.fsPath));
    }
    function uniqueStrings2(values) {
      const seen = /* @__PURE__ */ new Set();
      return values.filter((value2) => {
        const key = String(value2);
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
        const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
        if (project) await openProjectPanel(project);
      }
    }
    function rememberProject(project) {
      const index = state.projects.findIndex((candidate) => candidate.id === project.id);
      if (index >= 0) {
        state.projects.splice(index, 1, project);
      } else {
        state.projects.push(project);
      }
    }
    async function waitForJob(jobId, progress, scanRoot) {
      let latest = state.latestJob;
      for (let attempt = 0; attempt < 120; attempt += 1) {
        await sleep(1e3);
        latest = await request("GET", `/api/jobs/${encodeURIComponent(jobId)}`);
        state.latestJob = latest;
        const percent = normalizeProgress(latest.progress);
        progress.report({ message: `${latest.status || "RUNNING"} ${percent}% \xB7 ${scanRoot}` });
        await refreshAnalysis();
        await refreshDomainModel();
        analysisProvider.refresh();
        domainModelProvider.refresh();
        if (analysisPanel) await openAnalysisPanel(scanRoot);
        if (domainModelPanel) await openDomainModel();
        if (persistencePanel) await openPersistenceModel();
        if (latest.status && !["PENDING", "RUNNING"].includes(String(latest.status).toUpperCase())) {
          return latest;
        }
      }
      output.appendLine(`Analyze job ${jobId} did not finish within the VS Code polling window.`);
      return latest || { id: jobId, status: "PENDING", progress: 0 };
    }
    function sleep(ms) {
      return new Promise((resolve) => setTimeout(resolve, ms));
    }
    async function openControlDeck() {
      if (!controlDeckPanel) {
        controlDeckPanel = vscode4.window.createWebviewPanel("renovatioControlDeck", "Renovatio Control Deck", vscode4.ViewColumn.One, { enableCommandUris: true });
        controlDeckPanel.onDidDispose(() => {
          controlDeckPanel = void 0;
        });
      }
      controlDeckPanel.reveal(vscode4.ViewColumn.One);
      const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
      const inventory = workbenchInventory(state.analysis, state.latestJob);
      const domainModel = state.domainModel?.model || {};
      const generated = await loadGeneratedArtifacts(project);
      controlDeckPanel.webview.html = renderPanel("Renovatio", project, `
    ${renderHero(project, "Control Deck", "Governed modernization cockpit for characterization, IR, decisions and later emission.")}
    ${renderModernizationPremises(inventory)}
    ${renderPaths(project, generated)}
    ${renderMetricGrid([
        ["COBOL files", inventory.sourceFiles ?? inventory.programs ?? 0],
        ["Programs", inventory.programs ?? 0],
        ["Copybooks", inventory.copybooks ?? 0],
        ["JCL", inventory.jcl ?? inventory.jclFiles ?? 0],
        ["Domain nodes", asArray(domainModel.nodes).length],
        ["Future output files", generated.files.length],
        ["Backend", config().backendUrl]
      ])}
    ${renderFutureOutput(generated)}
  `);
    }
    async function openProjectPanel(project) {
      if (!projectPanel) {
        projectPanel = vscode4.window.createWebviewPanel("renovatioProject", "Renovatio Project", vscode4.ViewColumn.One, { enableCommandUris: true });
        projectPanel.onDidDispose(() => {
          projectPanel = void 0;
        });
      }
      projectPanel.reveal(vscode4.ViewColumn.One);
      const inventory = workbenchInventory(state.analysis, state.latestJob);
      const generated = await loadGeneratedArtifacts(project);
      projectPanel.webview.html = renderPanel(project.name, project, `
    ${renderHero(project, "Project", "Workspace, COBOL source roots, analysis readiness and future output settings.")}
    ${renderPaths(project, generated)}
    ${renderModernizationPremises(inventory)}
    ${renderMetricGrid([
        ["Inventory", sumInventory(inventory)],
        ["Programs", inventory.programs ?? 0],
        ["Copybooks", inventory.copybooks ?? 0],
        ["Future output files", generated.files.length]
      ])}
    ${renderFutureOutput(generated)}
  `);
    }
    async function openAnalysisPanel(scanRoot) {
      if (!analysisPanel) {
        analysisPanel = vscode4.window.createWebviewPanel("renovatioAnalysis", "Renovatio Discovery", vscode4.ViewColumn.One, { enableCommandUris: true });
        analysisPanel.onDidDispose(() => {
          analysisPanel = void 0;
        });
      }
      analysisPanel.reveal(vscode4.ViewColumn.One);
      const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
      const inventory = workbenchInventory(state.analysis, state.latestJob);
      const job = state.latestJob;
      const settings = workspaceSettings(project);
      const activeScanRoot = scanRoot || settings.cobolRoots[0] || project?.cobolScanRoot || project?.workspacePath;
      const source = await loadCobolArtifacts(activeScanRoot);
      const progress = normalizeProgress(job?.progress ?? (job?.status === "COMPLETED" ? 1 : 0));
      analysisPanel.webview.html = renderPanel("Analysis", project, `
    ${renderHero(project, "Discovery report", "COBOL inventory, inferred persistence, data records and source evidence from the latest analysis.")}
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
        domainModelPanel = vscode4.window.createWebviewPanel("renovatioDomainModel", "Renovatio Domain Model", vscode4.ViewColumn.One, { enableCommandUris: true, enableScripts: true });
        domainModelPanel.onDidDispose(() => {
          domainModelPanel = void 0;
        });
      }
      domainModelPanel.reveal(vscode4.ViewColumn.One);
      const domain = state.domainModel;
      domainModelPanel.webview.html = renderPanel("Domain Model", project, `
    ${renderHero(project, "Domain Model", "Versioned business model projected from COBOL discovery evidence.")}
    ${renderNativeDiagramActions()}
    ${renderDomainModelOverview(domain)}
    ${renderDomainModelDiagram(domain)}
    ${renderInferredClassModel(domain, project)}
    ${renderDomainModelGraph(domain)}
    ${renderDomainModelGovernance(domain)}
  `);
    }
    async function openNativeDomainDiagram() {
      await runNativeDiagramCommand("domain", async () => {
        const project = await ensureActiveProject();
        if (!project) return;
        await refreshDomainModel();
        const artifact = await writeNativeDomainDiagramArtifact(project, state.domainModel);
        await openNativeDiagramArtifact(artifact, "renovatio.diagram.domain", "Renovatio native domain diagram");
      });
    }
    async function openNativePersistenceDiagram() {
      await runNativeDiagramCommand("persistence", async () => {
        const project = await ensureActiveProject();
        if (!project) return;
        await refreshDomainModel();
        const artifact = await writeNativePersistenceDiagramArtifact(project, state.domainModel);
        await openNativeDiagramArtifact(artifact, "renovatio.diagram.domain", "Renovatio native persistence diagram");
      });
    }
    async function openNativeArchitectureDiagram() {
      await runNativeDiagramCommand("architecture", async () => {
        const project = await ensureActiveProject();
        if (!project) return;
        await refreshDomainModel();
        const artifact = await writeNativeArchitectureDiagramArtifact(project, state.domainModel);
        await openNativeDiagramArtifact(artifact, "renovatio.diagram.architecture", "Renovatio native architecture diagram");
      });
    }
    async function runNativeDiagramCommand(kind, action2) {
      try {
        await action2();
      } catch (error) {
        const detail = message3(error);
        output.appendLine(`Native ${kind} diagram failed: ${detail}`);
        vscode4.window.showErrorMessage(`Renovatio native ${kind} diagram failed: ${detail}`);
      }
    }
    async function openNativeDiagramArtifact(artifact, viewType, label) {
      output.appendLine(`Opened ${label}: ${artifact.uri.fsPath}`);
      await vscode4.commands.executeCommand("vscode.openWith", artifact.uri, viewType, {
        preview: false,
        viewColumn: vscode4.ViewColumn.One
      });
    }
    async function openPersistenceModel() {
      const project = await ensureActiveProject();
      if (!project) return;
      await refreshDomainModel();
      if (!persistencePanel) {
        persistencePanel = vscode4.window.createWebviewPanel("renovatioPersistenceModel", "Renovatio Persistence Model", vscode4.ViewColumn.One, { enableCommandUris: true, enableScripts: true });
        persistencePanel.onDidDispose(() => {
          persistencePanel = void 0;
        });
      }
      persistencePanel.reveal(vscode4.ViewColumn.One);
      const domain = state.domainModel;
      persistencePanel.webview.html = renderPanel("Persistence Model", project, `
    ${renderHero(project, "Inferred Persistence", "ER-style table and file model inferred from COBOL I/O, DB2 access and record evidence.")}
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
    async function openWelcome(options = {}) {
      if (options.once && welcomeOpenedThisSession) return;
      welcomeOpenedThisSession = true;
      if (!welcomePanel) {
        welcomePanel = vscode4.window.createWebviewPanel("renovatioWelcome", "Welcome", vscode4.ViewColumn.One, { enableCommandUris: true });
        welcomePanel.onDidDispose(() => {
          welcomePanel = void 0;
        });
      }
      welcomePanel.reveal(vscode4.ViewColumn.One);
      const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
      welcomePanel.webview.html = renderPanel("Welcome", project, renderWelcome(project));
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
  .welcomeHero {
    min-height: 120px;
    display: grid;
    align-content: end;
    padding: 52px 0 24px;
  }
  .welcomeHero h1 {
    margin: 0;
    font-size: 32px;
    line-height: 1.1;
  }
  .welcomeHero p {
    margin: 8px 0 0;
    color: var(--vscode-descriptionForeground);
    font-size: 15px;
    font-weight: 700;
  }
  .welcomeGrid {
    display: grid;
    grid-template-columns: minmax(260px, 420px) minmax(320px, 1fr);
    gap: clamp(32px, 8vw, 120px);
    align-items: start;
  }
  .welcomeColumn h2 {
    margin: 0 0 12px;
    font-size: 18px;
  }
  .welcomeStart {
    display: grid;
    gap: 10px;
  }
  .welcomeStart a {
    display: grid;
    grid-template-columns: 22px 1fr;
    gap: 9px;
    align-items: center;
    width: fit-content;
    font-weight: 500;
  }
  .welcomeIcon {
    display: inline-grid;
    place-items: center;
    width: 18px;
    color: var(--vscode-textLink-foreground);
    font-size: 16px;
    font-weight: 700;
  }
  .recentList {
    display: grid;
    gap: 8px;
    margin-top: 28px;
  }
  .recentRow {
    display: grid;
    grid-template-columns: minmax(120px, auto) 1fr;
    gap: 12px;
    color: var(--vscode-descriptionForeground);
    font-size: 13px;
  }
  .welcomeCards {
    display: grid;
    gap: 16px;
  }
  .welcomeCard {
    display: grid;
    grid-template-columns: 22px 1fr;
    gap: 12px;
    padding: 13px 14px;
    border-radius: 4px;
    border: 1px solid var(--vscode-panel-border);
    background: var(--vscode-list-inactiveSelectionBackground, var(--vscode-sideBar-background));
    color: var(--vscode-foreground);
  }
  .welcomeCard:hover {
    border-color: var(--vscode-focusBorder);
    background: var(--vscode-list-hoverBackground, var(--vscode-sideBar-background));
    text-decoration: none;
  }
  .welcomeCard strong {
    display: block;
    margin-bottom: 4px;
  }
  .welcomeCard span {
    display: block;
    color: var(--vscode-descriptionForeground);
    font-weight: 400;
    line-height: 1.45;
  }
  .welcomeStatus {
    margin-top: 22px;
    display: grid;
    gap: 8px;
  }
  .welcomeStatus .kv {
    margin: 0;
    grid-template-columns: minmax(90px, 130px) 1fr;
  }
  @media (max-width: 760px) {
    .welcomeGrid { grid-template-columns: 1fr; gap: 30px; }
    .welcomeHero { padding-top: 24px; }
    .recentRow { grid-template-columns: 1fr; gap: 2px; }
  }
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
    function renderWelcome(project) {
      const settings = workspaceSettings(project);
      const backendUrl = config().backendUrl;
      const projectName = project?.name || "No project selected";
      const workspacePath = settings.workspaceFolderPath || project?.workspacePath || "Open a folder or create a project";
      const roots = settings.configuredCobolRoots.length ? settings.configuredCobolRoots.join(", ") : "Choose a COBOL source root";
      const destinationRoots = settings.generatedRoots.length ? settings.generatedRoots.join(", ") : "Choose a future output root";
      return `
    <section class="welcomeHero" aria-labelledby="renovatio-welcome-title">
      <div>
        <h1 id="renovatio-welcome-title">Renovatio Modernization</h1>
        <p>COBOL discovery, domain modeling and governed migration from VS Code</p>
      </div>
    </section>
    <section class="welcomeGrid">
      <div class="welcomeColumn">
        <h2>Start</h2>
        <div class="welcomeStart">
          ${welcomeAction("renovatio.createProject", "Create Project...", "\uFF0B")}
          ${welcomeAction("renovatio.addCobolSourceRoot", "Choose COBOL Source...", "\u25A3")}
          ${welcomeAction("renovatio.analyzeCobolSources", "Analyze COBOL Sources...", "\u25B7")}
          ${welcomeAction("renovatio.selectProject", "Select Existing Project...", "\u25C7")}
          ${welcomeAction("renovatio.refresh", "Refresh Backend State", "\u21BB")}
        </div>
        <div class="recentList" aria-label="Current Renovatio context">
          <h2>Current</h2>
          <div class="recentRow"><a href="command:renovatio.selectProject">${escapeHtml(projectName)}</a><span>${escapeHtml(workspacePath)}</span></div>
          <div class="recentRow"><a href="command:renovatio.addCobolSourceRoot">COBOL roots</a><span>${escapeHtml(roots)}</span></div>
          <div class="recentRow"><a href="command:renovatio.selectGeneratedOutputFolder">Future outputs</a><span>${escapeHtml(destinationRoots)}</span></div>
        </div>
      </div>
      <div class="welcomeColumn">
        <h2>Workflow</h2>
        <div class="welcomeCards">
          ${welcomeCard("renovatio.createProject", "Create a project from COBOL", "Pick the source directory first; Renovatio creates the backend project and stores that root in VS Code settings.", "1")}
          ${welcomeCard("renovatio.analyzeCobolSources", "Run discovery", "Parse COBOL, copybooks and JCL, then build inventory and domain evidence.", "2")}
          ${welcomeCard("renovatio.openDomainModel", "Inspect the domain model", "Review inferred use cases, repositories, records, relations and governance evidence.", "3")}
          ${welcomeCard("renovatio.openNativeArchitectureDiagram", "Open native diagrams", "Use the built-in diagram editor for domain, persistence and target architecture views.", "4")}
        </div>
        <div class="panel compact welcomeStatus">
          <div class="sectionTitle">Backend</div>
          <div class="kv"><span>API URL</span><code>${escapeHtml(backendUrl)}</code></div>
          <div class="kv"><span>Role</span><code>${escapeHtml(config().role)}</code></div>
          <div class="kv"><span>Expected port</span><code>${escapeHtml(new URL(backendUrl).port || "80")}</code></div>
        </div>
      </div>
    </section>
  `;
    }
    function welcomeAction(command, label, icon) {
      return `<a href="command:${command}"><span class="welcomeIcon">${escapeHtml(icon)}</span><span>${escapeHtml(label)}</span></a>`;
    }
    function welcomeCard(command, title, description, icon) {
      return `<a class="welcomeCard" href="command:${command}">
    <span class="welcomeIcon">${escapeHtml(icon)}</span>
    <span><strong>${escapeHtml(title)}</strong><span>${escapeHtml(description)}</span></span>
  </a>`;
    }
    function renderNativeDiagramActions() {
      return `<section class="panel">
    <div class="sectionTitle">Native diagrams</div>
    <p class="sectionLead">Open Renovatio diagrams with the built-in VS Code editor.</p>
    <div class="inlineAction">
      ${commandLink("renovatio.openNativeDomainDiagram", [], "Open domain diagram")}
      ${commandLink("renovatio.openNativePersistenceDiagram", [], "Open persistence diagram")}
      ${commandLink("renovatio.openNativeArchitectureDiagram", [], "Open architecture diagram")}
    </div>
  </section>`;
    }
    function renderPaths(project, generated) {
      const settings = workspaceSettings(project);
      const roots = settings.configuredCobolRoots.length ? settings.configuredCobolRoots.map((root) => `<code>${escapeHtml(root)}</code>`).join("") : "<code>Not configured</code>";
      const destinationRoots = settings.generatedRoots.length ? settings.generatedRoots.map((root) => `<code>${escapeHtml(root)}</code>`).join("") : "<code>Not configured</code>";
      return `<section class="panel">
    <div class="sectionTitle">Paths</div>
    <div class="paths">
      <div class="kv"><span>VS Code workspace</span><code>${escapeHtml(settings.workspaceFolderPath || project?.workspacePath || "Not configured")}</code></div>
      <div class="kv"><span>COBOL source roots</span><div class="stack">${roots}</div></div>
      <div class="kv"><span>Future output roots</span><div class="stack">${destinationRoots}</div></div>
      <div class="kv"><span>Target</span><div class="stack inline"><code>${escapeHtml(settings.targetLanguage)}</code><code>${escapeHtml(settings.targetPackage)}</code></div></div>
    </div>
  </section>`;
    }
    function renderModernizationPremises(inventory) {
      const hasInventory = sumInventory(inventory) > 0;
      return `<section class="panel compact">
    <div class="sectionTitle">Renovatio flow</div>
    <div class="flow">
      ${renderStage("01", "Characterize", "golden-master safety net", false)}
      ${renderStage("02", "Parse to neutral IR", "COBOL, copybooks, JCL", true)}
      ${renderStage("03", "Map relations", "calls, data, jobs, dependencies", hasInventory)}
      ${renderStage("04", "Decide", "target, architecture, persistence", false)}
      ${renderStage("05", "Emit later", "target code after approval", false)}
    </div>
  </section>`;
    }
    function renderStage(index, title, description, current) {
      return `<div class="stage ${current ? "current" : ""}">
    <span>${escapeHtml(index)}</span>
    <strong>${escapeHtml(title)}</strong>
    <small>${escapeHtml(description)}</small>
  </div>`;
    }
    function renderMetricGrid(metrics) {
      return `<div class="grid">${metrics.map(([label, value2]) => `<div class="metric"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value2)}</strong></div>`).join("")}</div>`;
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
      const status = job?.status || analysis?.status || (sumInventory(inventory) ? "COMPLETED" : "IDLE");
      const issueCount = Number(inventory?.errors || 0) + diagnostics.length;
      const parsedFiles = parsedFileCount(inventory);
      const lastJob = job?.id ? `<div class="statusLine">Latest job <code>${escapeHtml(job.id)}</code> \xB7 ${escapeHtml(status)} \xB7 ${Number(progress) || 0}%</div>` : '<div class="statusLine">No active job in this VS Code session. Refresh or run analysis to load the latest persisted evidence.</div>';
      return `<section class="panel">
    <div class="sectionTitle">Run result</div>
    <div class="progress"><span style="width:${Number(progress) || 0}%"></span></div>
    ${renderMetricGrid([
        ["Status", status],
        ["Parsed files", parsedFiles],
        ["Tables/files", discovery.repositories.length],
        ["Data records", discovery.recordNodes.length],
        ["Relations", discovery.relations.length],
        ["Issues", issueCount]
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
      ${discovery.repositories.slice(0, 12).map((repository) => renderRepositoryFinding(repository)).join("")}
    </div>
    ${discovery.repositories.length > 12 ? `<div class="statusLine">${discovery.repositories.length - 12} more persistence candidate(s) are available in the ER model view.</div>` : ""}
    <div class="inlineAction">
      ${commandLink("renovatio.openPersistenceModel", [], "Open ER model")}
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
      const withFields = repositories.filter((repository) => asArray(repository.properties).length).length;
      const unresolved = repositories.length - withFields;
      const mappedRecords = uniqueNodes(repositories.flatMap((repository) => asArray(repository.mappedNodes))).length;
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
      ${renderDiagramDebugControls("persistenceErd")}
    </div>
    <div class="erdViewport" data-debug-viewport="true">
      <svg id="persistenceErd" class="erdSvg" data-debug-diagram="true" viewBox="0 0 ${erd.width} ${erd.height}" role="img" aria-label="Inferred persistence ER diagram">
        ${erd.edges.map(renderErdEdge).join("")}
        ${erd.nodes.map(renderErdTable).join("")}
      </svg>
    </div>
    ${erd.notice ? `<div class="statusLine">${escapeHtml(erd.notice)}</div>` : ""}
  </section>`;
    }
    function buildPersistenceErd(domain) {
      const discovery = domainDiscovery(domain);
      const repositories = [...discovery.repositories].sort(compareRepositoriesForDiagram).slice(0, 12);
      if (!repositories.length) {
        return { width: 1080, height: 220, nodes: [], edges: [], notice: "" };
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
        x: left + index % columns * (cardWidth + gapX),
        y: top + Math.floor(index / columns) * (cardHeight + gapY),
        width: cardWidth,
        height: cardHeight
      }));
      const byId = new Map(nodes.map((node) => [node.source.logicalId || node.source.id, node]));
      const edges = [];
      const edgeIds = /* @__PURE__ */ new Set();
      for (const node of nodes) {
        for (const related of relatedPersistenceNodes(node.source, repositories)) {
          const to = byId.get(related.logicalId || related.id);
          if (!to || to === node) continue;
          const id = [node.source.logicalId || node.source.id, to.source.logicalId || to.source.id].sort().join("|");
          if (edgeIds.has(id)) continue;
          edgeIds.add(id);
          edges.push({ from: node, to });
        }
      }
      const rows = Math.ceil(nodes.length / columns);
      const height = top * 2 + rows * cardHeight + Math.max(0, rows - 1) * gapY;
      const notice = discovery.repositories.length > repositories.length ? `Showing ${repositories.length} of ${discovery.repositories.length} inferred persistence resources. Open the list below for the rest.` : "";
      return { width, height, nodes, edges, notice };
    }
    function relatedPersistenceNodes(repository, repositories) {
      const mappedIds = new Set(asArray(repository.mappedNodes).map((node) => node.id).filter(Boolean));
      const fieldNames = new Set(asArray(repository.properties).map((property) => normalizeName(property.name)).filter(Boolean));
      return repositories.filter((candidate) => {
        if (candidate === repository) return false;
        if (asArray(candidate.mappedNodes).some((node) => mappedIds.has(node.id))) return true;
        return asArray(candidate.properties).some((property) => fieldNames.has(normalizeName(property.name)));
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
    <rect class="erdTable${unresolved ? " unresolved" : ""}" x="${node.x}" y="${node.y}" width="${node.width}" height="${node.height}" rx="3" />
    <rect class="erdHeader" x="${node.x}" y="${node.y}" width="${node.width}" height="38" rx="3" />
    ${renderSvgLines(title, node.x + 12, node.y + 23, "erdTitle", 34, 1, 14, { editable: true })}
    <text class="erdKind" x="${node.x + 12}" y="${node.y + 57}">${escapeHtml(persistenceKind(repository))} \xB7 ${properties.length} field${properties.length === 1 ? "" : "s"} \xB7 ${asArray(repository.users).length} use${asArray(repository.users).length === 1 ? "" : "s"}</text>
    ${fields.length ? fields.map((property, index) => `<text class="erdField" x="${node.x + 14}" y="${node.y + 80 + index * 13}">${escapeHtml(compactField(property))}</text>`).join("") : renderSvgLines("Shape unresolved. No bound COBOL record fields yet.", node.x + 14, node.y + 82, "erdMeta", 44, 3, 14)}
    ${properties.length > fields.length ? `<text class="erdMeta" x="${node.x + 14}" y="${node.y + 80 + fields.length * 13}">+${properties.length - fields.length} more fields</text>` : ""}
  </g>`;
    }
    function renderPersistenceRepositoryList(domain) {
      const discovery = domainDiscovery(domain);
      if (!discovery.repositories.length) return "";
      return `<section class="panel">
    <div class="sectionTitle">Tables / files</div>
    <div class="domainGrid">
      ${discovery.repositories.map((repository) => renderRepositoryFinding(repository)).join("")}
    </div>
  </section>`;
    }
    function renderRepositoryFinding(repository) {
      const properties = repository.properties;
      const mappedNames = repository.mappedNodes.map((node) => node.name || node.id);
      const users = repository.users.map((node) => node.name || node.id);
      const evidence = mergedEvidence([repository, ...repository.mappedNodes]);
      return `<div class="nodeCard tableCard">
    <div class="artifactHeader">
      <strong>${escapeHtml(repository.name || repository.id)}</strong>
      <span>${properties.length} field${properties.length === 1 ? "" : "s"}</span>
    </div>
    <div class="nodeMeta">
      <span class="pill">${escapeHtml(persistenceKind(repository))}</span>
      <span class="pill">${Math.round(Number(repository.confidence || 0) * 100)}% confidence</span>
      ${repository.instances > 1 ? `<span class="pill">${repository.instances} references</span>` : ""}
      ${mappedNames.length ? `<span class="pill">maps to ${escapeHtml(mappedNames.slice(0, 2).join(", "))}</span>` : '<span class="pill">unmapped record</span>'}
    </div>
    ${properties.length ? renderPropertyGrid(properties, 18) : `<div class="empty">${escapeHtml(unresolvedShapeReason(repository))}</div>`}
    ${users.length ? `<div class="statusLine">Used by ${escapeHtml(users.slice(0, 4).join(", "))}${users.length > 4 ? ` and ${users.length - 4} more` : ""}</div>` : ""}
    ${renderEvidence(evidence)}
  </div>`;
    }
    function renderPersistenceDiagram(domain) {
      const diagram = buildPersistenceDiagram(domain);
      if (!diagram.nodes.length) return "";
      return `<div class="artifact">
    <div class="diagramToolbar">
      <div class="artifactHeader"><strong>Table map</strong><span>${diagram.repositories.length} shown</span></div>
      <div class="diagramLegend" aria-label="Persistence diagram legend">
        <span class="legendItem"><span class="legendSwatch"></span>USES</span>
        <span class="legendItem"><span class="legendSwatch maps"></span>MAPS_TO</span>
        <span class="legendItem"><span class="legendSwatch unresolved"></span>shape unresolved</span>
      </div>
      ${renderDiagramDebugControls("persistenceMap")}
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
        ${diagram.edges.map((edge) => renderDiagramEdge(edge, "persistence")).join("")}
        ${diagram.useCases.map((node) => renderDiagramNode(node, "use")).join("")}
        ${diagram.repositories.map((node) => renderDiagramNode(node, "repo")).join("")}
        ${diagram.records.map((node) => renderDiagramNode(node, "record")).join("")}
      </svg>
    </div>
    ${diagram.notice ? `<div class="statusLine">${escapeHtml(diagram.notice)}</div>` : ""}
  </div>`;
    }
    function renderPersistenceShapeGaps(discovery) {
      const unresolved = discovery.repositories.filter((repository) => !asArray(repository.properties).length).slice(0, 8);
      if (!unresolved.length) return "";
      return `<div class="artifact">
    <div class="artifactHeader"><strong>Tables without inferred fields</strong><span>${unresolved.length}</span></div>
    <div class="shapeGapList">
      ${unresolved.map((repository) => `<div class="shapeGap">
        <strong>${escapeHtml(repository.name || repository.id)}</strong>
        <span>${escapeHtml(unresolvedShapeReason(repository))}</span>
      </div>`).join("")}
    </div>
    ${discovery.repositories.filter((repository) => !asArray(repository.properties).length).length > unresolved.length ? `<div class="statusLine">${discovery.repositories.filter((repository) => !asArray(repository.properties).length).length - unresolved.length} more unresolved table shape(s) are available in the Domain Model view.</div>` : ""}
  </div>`;
    }
    function renderDataShapeFindings(domain, project) {
      const discovery = domainDiscovery(domain);
      const candidates = discovery.recordNodes.filter((node) => asArray(node.properties).length).slice(0, 10);
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
      ${candidates.map((node) => `<div class="nodeCard">
        <div class="artifactHeader">
          <strong>${escapeHtml(node.name || node.id)}</strong>
          <span>${asArray(node.properties).length} field${asArray(node.properties).length === 1 ? "" : "s"}</span>
        </div>
        <div class="nodeMeta">
          <span class="pill">${escapeHtml(humanize(node.kind || "record"))}</span>
          <span class="pill">${Math.round(Number(node.confidence || 0) * 100)}% confidence</span>
        </div>
        ${renderPropertyGrid(asArray(node.properties), 12)}
        ${renderEvidence(asArray(node.evidence))}
      </div>`).join("")}
    </div>
    ${discovery.recordNodes.length > candidates.length ? `<div class="statusLine">${discovery.recordNodes.length - candidates.length} more data shape(s) are available in the Domain Model view.</div>` : ""}
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
      const important = discovery.relations.filter((relation) => sameKind(relation.kind, "USES") || sameKind(relation.kind, "MAPS_TO") || sameKind(relation.kind, "CONTAINS"));
      const rows = (important.length ? important : discovery.relations).slice(0, 18);
      return `<section class="panel">
    <div class="sectionTitle">Relationship map</div>
    <div class="relationList">${rows.map((relation) => {
        const from = discovery.nodeById.get(relation.fromId);
        const to = discovery.nodeById.get(relation.toId);
        return `<div class="relationRow">
        <strong>${escapeHtml(from?.name || relation.fromId)}</strong>
        <span class="relationKind">${escapeHtml(humanize(relation.kind || "relates"))}</span>
        <strong>${escapeHtml(to?.name || relation.toId)}</strong>
      </div>`;
      }).join("")}</div>
    ${discovery.relations.length > rows.length ? `<div class="statusLine">${discovery.relations.length - rows.length} more relation(s) are available in the Domain Model view.</div>` : ""}
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
      ${errors ? `<div class="warning"><strong>Parser errors</strong><br>${escapeHtml(errors)} error(s) were reported by the inventory.</div>` : ""}
      ${diagnostics.map((diagnostic2) => `<div class="warning"><strong>${escapeHtml(diagnostic2.code)}</strong> \xB7 ${escapeHtml(diagnostic2.targetId)}<br>${escapeHtml(diagnostic2.message)}</div>`).join("")}
      ${suggestions.map((suggestion) => `<div class="warning"><strong>${escapeHtml(suggestion.name || suggestion.targetId)}</strong> \xB7 ${escapeHtml(suggestion.status || "pending")}<br>${escapeHtml(suggestion.targetType || "domain suggestion")}</div>`).join("")}
    </div>
  </section>`;
    }
    function renderPropertyGrid(properties, limit) {
      const visible = asArray(properties).slice(0, limit);
      if (!visible.length) return "";
      const hidden = properties.length - visible.length;
      return `<div class="fieldGrid">
    ${visible.map((property) => `<div class="field">
      <strong>${escapeHtml(property.name || "field")}</strong>
      <span>${escapeHtml(property.type || "unknown")}${property.required === false ? " optional" : ""}</span>
    </div>`).join("")}
    ${hidden > 0 ? `<div class="field"><strong>${hidden} more</strong><span>fields</span></div>` : ""}
  </div>`;
    }
    function domainDiscovery(domain) {
      const model = domain?.model || {};
      const nodes = asArray(model.nodes);
      const relations = asArray(model.relations);
      const nodeById = new Map(nodes.map((node) => [node.id, node]));
      const repositoryNodes = nodes.filter((node) => sameKind(node.kind, "REPOSITORY")).map((repository) => {
        const mappedNodes = relations.filter((relation) => sameKind(relation.kind, "MAPS_TO") && relation.fromId === repository.id).map((relation) => nodeById.get(relation.toId)).filter(Boolean);
        const users = relations.filter((relation) => sameKind(relation.kind, "USES") && relation.toId === repository.id).map((relation) => nodeById.get(relation.fromId)).filter(Boolean);
        return {
          ...repository,
          mappedNodes,
          users,
          properties: mergeProperties([asArray(repository.properties), ...mappedNodes.map((node) => asArray(node.properties))].flat())
        };
      });
      const repositories = mergeRepositories(repositoryNodes);
      const recordNodes = nodes.filter((node) => sameKind(node.kind, "ENTITY") || sameKind(node.kind, "VALUE_OBJECT") || sameKind(node.kind, "AGGREGATE")).sort((left, right) => asArray(right.properties).length - asArray(left.properties).length || String(left.name || left.id).localeCompare(String(right.name || right.id)));
      return { nodes, relations, nodeById, repositories, repositoryNodes, recordNodes };
    }
    function mergeRepositories(repositories) {
      const byResource = /* @__PURE__ */ new Map();
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
      return [...byResource.values()].sort((left, right) => String(left.name || left.id).localeCompare(String(right.name || right.id)));
    }
    function uniqueNodes(nodes) {
      const byId = /* @__PURE__ */ new Map();
      for (const node of asArray(nodes)) {
        const key = node?.id || node?.name;
        if (key && !byId.has(key)) byId.set(key, node);
      }
      return [...byId.values()];
    }
    function mergeProperties(properties) {
      const byName = /* @__PURE__ */ new Map();
      for (const property of asArray(properties)) {
        const key = String(property?.name || "").toUpperCase();
        if (!key || byName.has(key)) continue;
        byName.set(key, property);
      }
      return [...byName.values()].sort((left, right) => String(left.name).localeCompare(String(right.name)));
    }
    function mergedEvidence(nodes) {
      const evidenceByRef = /* @__PURE__ */ new Map();
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
      const name = String(repository?.name || repository?.id || "");
      if (/DB2 table/i.test(name)) return "DB2 table";
      if (/file record/i.test(name)) return "File record";
      return "Repository";
    }
    function domainDisplayName2(node, fallbackKind) {
      const raw = String(node?.name || node?.label || node?.id || "").trim();
      const kind = String(node?.kind || fallbackKind || "").toUpperCase();
      if (!raw || kind === "USE_CASE" || kind === "DOMAIN_SERVICE" || kind === "SERVICE") return raw;
      const normalized = raw.replace(/^DB2\s+table\s+/i, "").replace(/^file\s+record\s+/i, "").replace(/^(FD|WS|LS)-/i, "").replace(/-(FD|WS|LS)$/i, "").replace(/-(FILE|RECORD|REC|TABLE|DATA|IN|OUT|INPUT|OUTPUT)$/ig, "").replace(/^(FILE|RECORD|REC|TABLE|DATA|IN|OUT|INPUT|OUTPUT)-/ig, "").replace(/-{2,}/g, "-").replace(/^-|-$/g, "").trim();
      return titleCaseDomainName2(normalized || raw);
    }
    function titleCaseDomainName2(value2) {
      return String(value2 || "").split(/[-_\s]+/).filter(Boolean).map((token) => {
        const lower = token.toLowerCase();
        if (/^[A-Z0-9]{2,}$/.test(token) && token.length <= 4) return token;
        return lower.charAt(0).toUpperCase() + lower.slice(1);
      }).join(" ");
    }
    function parsedFileCount(inventory) {
      const source = Number(inventory?.sourceFiles ?? inventory?.programs ?? 0) || 0;
      const copybooks = Number(inventory?.copybooks ?? 0) || 0;
      const jcl = Number(inventory?.jcl ?? inventory?.jclFiles ?? 0) || 0;
      const total = source + copybooks + jcl;
      return total || sumInventory(inventory);
    }
    function workbenchInventory(analysis, job) {
      return {
        ...analysis?.inventory || {},
        ...jobInventory(job)
      };
    }
    function jobInventory(job) {
      const result = job?.result || {};
      const summary = result.summary || result.analysis?.summary || {};
      const inventory = {};
      for (const key of ["sourceFiles", "programs", "copybooks", "jcl", "jclFiles"]) {
        const value2 = Number(summary[key]);
        if (Number.isFinite(value2) && value2 > 0) inventory[key] = value2;
      }
      return inventory;
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
    <div class="fileStrip">${source.files.slice(0, 12).map((file) => `<span class="fileChip">${escapeHtml(file.relativePath)}</span>`).join("")}</div>
    ${renderFileList(source.files, "Open in editor")}
  </section>`;
    }
    function renderFutureOutput(generated) {
      const root = generated.root || "Not configured";
      const notice = generated.notice ? `<div class="warning">${escapeHtml(generated.notice)}</div>` : "";
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
      return `<div class="modelMetaBar" title="Canonical hash: ${escapeHtml(domain.canonicalHash || "not saved")}">
    <span>Revision ${escapeHtml(domain.revision ?? 0)}</span>
    <span>${nodes.length} nodes</span>
    <span>${relations.length} relations</span>
    <span>Saved ${escapeHtml(formatSavedAt(domain.savedAt))}</span>
  </div>`;
    }
    function formatSavedAt(value2) {
      if (!value2) return "not saved yet";
      if (Array.isArray(value2)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value2;
        if (year && month && day) {
          return `${padDate(year, 4)}-${padDate(month)}-${padDate(day)} ${padDate(hour)}:${padDate(minute)}:${padDate(second)}`;
        }
      }
      return String(value2).replace(/[TZ]/g, " ").replace(/\.\d+/, "").trim();
    }
    function padDate(value2, width = 2) {
      return String(value2).padStart(width, "0");
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
      ${renderDiagramDebugControls("domainDiagram")}
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
        ${diagram.edges.map(renderDiagramEdge).join("")}
        ${diagram.useCases.map((node) => renderDiagramNode(node, "use")).join("")}
        ${diagram.repositories.map((node) => renderDiagramNode(node, "repo")).join("")}
        ${diagram.records.map((node) => renderDiagramNode(node, "record")).join("")}
      </svg>
    </div>
    ${diagram.evidence.length ? `<div class="artifact">
      <div class="artifactHeader"><strong>Source evidence</strong><span>${diagram.evidence.length}</span></div>
      <div class="fileStrip">${diagram.evidence.slice(0, 10).map((item) => openEvidenceLink(item.sourceRef || item.provenance, item.sourceRef || "Open evidence")).join("")}</div>
    </div>` : ""}
    ${diagram.notice ? `<div class="statusLine">${escapeHtml(diagram.notice)}</div>` : ""}
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
    async function writeNativeDomainDiagramArtifact(project, domain) {
      const uri = await nativeDiagramUri(project, "domain-model.renovatio-domain.json");
      const previous = await readJsonArtifact(uri);
      const next = nativeDomainDocument(domain);
      preserveDiagramLayout(next, previous);
      const text = JSON.stringify(next, null, 2) + "\n";
      const openDoc = vscode4.workspace.textDocuments.find((d) => d.uri.toString() === uri.toString());
      if (openDoc) {
        const edit = new vscode4.WorkspaceEdit();
        const fullRange = new vscode4.Range(0, 0, openDoc.lineCount, 0);
        edit.replace(uri, fullRange, text);
        await vscode4.workspace.applyEdit(edit);
        await openDoc.save();
      } else {
        await vscode4.workspace.fs.writeFile(uri, Buffer.from(text, "utf8"));
      }
      return { uri };
    }
    async function writeNativePersistenceDiagramArtifact(project, domain) {
      const uri = await nativeDiagramUri(project, "persistence-model.renovatio-domain.json");
      const previous = await readJsonArtifact(uri);
      const next = nativePersistenceDocument(project, domain);
      preserveDiagramLayout(next, previous);
      await vscode4.workspace.fs.writeFile(uri, Buffer.from(JSON.stringify(next, null, 2) + "\n", "utf8"));
      return { uri };
    }
    async function writeNativeArchitectureDiagramArtifact(project, domain) {
      const uri = await nativeDiagramUri(project, "architecture.renovatio-arch.json");
      const previous = await readJsonArtifact(uri);
      const next = nativeArchitectureDocument(project, domain);
      preserveArchitectureLayout(next, previous);
      await vscode4.workspace.fs.writeFile(uri, Buffer.from(JSON.stringify(next, null, 2) + "\n", "utf8"));
      return { uri };
    }
    async function nativeDiagramUri(project, suffix) {
      const rootPath = workspaceSettings(project).workspaceFolderPath || project?.workspacePath || extensionContext.globalStorageUri.fsPath;
      const diagramsDir = vscode4.Uri.joinPath(vscode4.Uri.file(rootPath), ".renovatio", "diagrams");
      await vscode4.workspace.fs.createDirectory(diagramsDir);
      return vscode4.Uri.joinPath(diagramsDir, `${sanitizeFileName(project?.name || "renovatio")}-${suffix}`);
    }
    function nativeDomainDocument(domain) {
      const discovery = domainDiscovery(domain);
      if (discovery.repositories.length || discovery.recordNodes.length) {
        const nodes2 = [];
        const nodeIds2 = /* @__PURE__ */ new Set();
        const addNode2 = (n, fallbackKind) => {
          const src = n?.source || n;
          const id = src?.logicalId || src?.id;
          if (!id || nodeIds2.has(id)) return;
          nodeIds2.add(id);
          nodes2.push({
            id,
            kind: src.kind || fallbackKind,
            name: domainDisplayName2(src, fallbackKind),
            physicalName: src.name || src.label || id,
            properties: asArray(src.properties),
            tableName: src.tableName,
            sourceDataset: src.sourceDataset,
            confidence: src.confidence,
            origin: src.origin,
            evidence: asArray(src.evidence)
          });
        };
        const useCases2 = uniqueNodes(discovery.repositories.flatMap((repository) => asArray(repository.users))).filter((node) => sameKind(node.kind, "USE_CASE") || String(node.id || "").startsWith("use-case:"));
        const records2 = uniqueNodes(discovery.repositories.flatMap((repository) => asArray(repository.mappedNodes)));
        useCases2.forEach((n) => addNode2(n, "USE_CASE"));
        discovery.repositories.forEach((n) => addNode2(n, "REPOSITORY"));
        records2.forEach((n) => addNode2(n, "ENTITY"));
        const relations = [];
        const relKeys = /* @__PURE__ */ new Set();
        const addRelation = (fromId, toId, kind, label) => {
          if (!fromId || !toId || fromId === toId || !nodeIds2.has(fromId) || !nodeIds2.has(toId)) return;
          const key = `${fromId}->${toId}:${kind}`;
          if (relKeys.has(key)) return;
          relKeys.add(key);
          relations.push({ id: `rel:${key}`, fromId, toId, kind, label });
        };
        for (const repository of discovery.repositories) {
          const repositoryId = repository.logicalId || repository.id;
          for (const user of asArray(repository.users)) {
            addRelation(user.id, repositoryId, "USES", "uses");
          }
          for (const record of asArray(repository.mappedNodes)) {
            addRelation(repositoryId, record.id, "MAPS_TO", "maps");
          }
        }
        return {
          diagramKind: "domain",
          projection: "domain",
          revision: domain?.revision || 0,
          savedAt: domain?.savedAt,
          nodes: nodes2,
          relations,
          invariants: asArray(domain?.model?.invariants),
          layout: {},
          excludedNodeIds: []
        };
      }
      const repositories = discovery.repositories;
      const useCases = uniqueNodes([
        ...repositories.flatMap((r) => asArray(r.users)),
        ...discovery.nodes.filter((n) => sameKind(n.kind, "USE_CASE") || sameKind(n.kind, "SERVICE"))
      ]);
      const records = uniqueNodes([
        ...repositories.flatMap((r) => asArray(r.mappedNodes)),
        ...discovery.recordNodes.filter((n) => sameKind(n.kind, "ENTITY") || asArray(n.properties).length > 0)
      ]);
      const nodes = [];
      const nodeIds = /* @__PURE__ */ new Set();
      const addNode = (node) => {
        const id = node?.logicalId || node?.id;
        if (!id || nodeIds.has(id)) return;
        nodeIds.add(id);
        nodes.push({
          ...node,
          id,
          name: domainDisplayName2(node),
          physicalName: node.name || node.label || id,
          properties: asArray(node.properties),
          evidence: asArray(node.evidence)
        });
      };
      useCases.forEach(addNode);
      repositories.forEach(addNode);
      records.forEach(addNode);
      return {
        diagramKind: "domain",
        projection: "domain",
        revision: domain?.revision || 0,
        savedAt: domain?.savedAt,
        nodes,
        relations: [],
        invariants: asArray(domain?.model?.invariants),
        layout: {},
        excludedNodeIds: []
      };
    }
    function nativePersistenceDocument(project, domain) {
      const discovery = domainDiscovery(domain);
      const sqlFacts = collectPersistenceSqlFacts(project, discovery);
      const nodes = persistenceNodes(discovery, sqlFacts);
      const relations = persistenceRelations(discovery, nodes, sqlFacts);
      return {
        diagramKind: "persistence",
        projection: "persistence",
        revision: domain?.revision || 0,
        savedAt: domain?.savedAt,
        nodes,
        relations,
        invariants: [],
        layout: {},
        excludedNodeIds: []
      };
    }
    async function readJsonArtifact(uri) {
      try {
        const bytes = await vscode4.workspace.fs.readFile(uri);
        return JSON.parse(Buffer.from(bytes).toString("utf8"));
      } catch (error) {
        return void 0;
      }
    }
    function preserveDiagramLayout(next, previous) {
      if (!previous || !previous.layout || typeof previous.layout !== "object") return;
      const nodeIds = new Set(asArray(next.nodes).map((node) => String(node.id)));
      const layout = {};
      for (const [id, position] of Object.entries(previous.layout)) {
        if (!nodeIds.has(String(id))) continue;
        const x = Number(position?.x);
        const y = Number(position?.y);
        if (Number.isFinite(x) && Number.isFinite(y)) layout[id] = { x, y };
      }
      if (Object.keys(layout).length) next.layout = layout;
    }
    function preserveArchitectureLayout(next, previous) {
      const previousLayout = previous?.profile?.layout || previous?.layout;
      if (!previousLayout || typeof previousLayout !== "object") return;
      const nodeIds = /* @__PURE__ */ new Set([
        ...asArray(next.canvas).map((node) => String(node.id)),
        ...Object.keys(next.profile?.packageRoots || {}).map((layer) => `architecture-layer:${layer}`)
      ]);
      const layout = {};
      for (const [id, position] of Object.entries(previousLayout)) {
        if (!nodeIds.has(String(id))) continue;
        const x = Number(position?.x);
        const y = Number(position?.y);
        if (Number.isFinite(x) && Number.isFinite(y)) layout[id] = { x, y };
      }
      if (Object.keys(layout).length) {
        next.profile = next.profile || {};
        next.profile.layout = layout;
      }
    }
    var NON_PERSISTENCE_VERB_NAMES = /* @__PURE__ */ new Set([
      // CICS FILE-CONTROL verbs
      "OPEN",
      "CLOSE",
      "READ",
      "WRITE",
      "REWRITE",
      "DELETE",
      "START",
      "UNLOCK",
      "BROWSE",
      "ENDBR",
      "RESETBR",
      // Embedded SQL statement verbs
      "SELECT",
      "INSERT",
      "UPDATE",
      "FETCH",
      "SET",
      "DECLARE",
      "EXEC"
    ]);
    function isFileControlVerbStub(node) {
      if (hasPersistenceShape(node)) return false;
      const name = String(node?.name ?? node?.id ?? "").trim().toUpperCase();
      return !name || NON_PERSISTENCE_VERB_NAMES.has(name);
    }
    function persistenceNodes(discovery, sqlFacts) {
      const candidates = [
        ...asArray(discovery.repositories).filter((node) => !isFileControlVerbStub(node)),
        ...asArray(discovery.recordNodes).filter((node) => hasPersistenceShape(node)),
        ...asArray(discovery.repositories).flatMap((repository) => asArray(repository.mappedNodes)).filter((node) => hasPersistenceShape(node)),
        ...[...sqlFacts.tables.keys()].map((table) => ({ id: `table:${table}`, kind: "REPOSITORY", name: table, tableName: table }))
      ];
      const byTable = /* @__PURE__ */ new Map();
      for (const candidate of candidates) {
        const id = String(candidate.logicalId || candidate.id || candidate.name);
        if (!id) continue;
        const tableName = candidate.tableName || persistenceTableName(candidate);
        const key = normalizeName(tableName || candidate.name || id);
        const existing = byTable.get(key);
        const node = {
          id,
          kind: "TABLE",
          name: tableName || candidate.name || id,
          tableName,
          sourceDataset: candidate.sourceDataset,
          properties: persistenceProperties(candidate, sqlFacts),
          confidence: candidate.confidence,
          origin: candidate.origin,
          evidence: candidate.evidence
        };
        if (!existing) {
          byTable.set(key, node);
          continue;
        }
        byTable.set(key, {
          ...existing,
          properties: mergeProperties([...asArray(existing.properties), ...asArray(node.properties)]),
          evidence: mergedEvidence([existing, node]),
          confidence: Math.max(Number(existing.confidence || 0), Number(node.confidence || 0))
        });
      }
      return [...byTable.values()].sort((left, right) => String(left.tableName || left.name || left.id).localeCompare(String(right.tableName || right.name || right.id)));
    }
    function hasPersistenceShape(node) {
      return Boolean(node?.tableName || node?.sourceDataset || asArray(node?.properties).some((property) => property?.columnName || property?.sourceColumn || property?.sourceDataset || property?.isKey));
    }
    function persistenceTableName(node) {
      if (node?.tableName) return node.tableName;
      const name = String(node?.name || node?.id || "");
      return name.replace(/^DB2 table\s+/i, "").replace(/^file record\s+/i, "").trim();
    }
    function persistenceProperties(node, sqlFacts) {
      const tableName = persistenceTableName(node);
      const explicit = asArray(node?.properties).map((property) => ({
        ...property,
        columnName: property.columnName || property.sourceColumn || property.name,
        isKey: Boolean(property.isKey || looksLikePrimaryKey(property, tableName, node?.name))
      }));
      const existing = new Set(explicit.map((property) => normalizeName(property.columnName || property.name)));
      const inferred = asArray(sqlFacts.tables.get(normalizeName(tableName))).filter((column) => !existing.has(normalizeName(column))).map((column) => ({
        name: column,
        type: "unknown",
        required: false,
        evidence: [],
        columnName: column,
        isKey: looksLikePrimaryKey({ name: column, columnName: column }, tableName, node?.name)
      }));
      return mergeProperties([...explicit, ...inferred]);
    }
    function looksLikePrimaryKey(property, tableName, nodeName) {
      const name = normalizeName(property?.columnName || property?.sourceColumn || property?.name);
      const table = normalizeSingular(tableName || nodeName);
      if (!name) return false;
      return name === "ID" || name === `${table}ID` || name === `${table}KEY` || name === `${table}NO` || name === `${table}NUMBER` || name.endsWith("ID") && table && name === `${table}ID`;
    }
    function persistenceRelations(discovery, nodes, sqlFacts) {
      const nodeIds = new Set(nodes.map((node) => String(node.id)));
      const relationIds = /* @__PURE__ */ new Set();
      const relations = [];
      for (const relation of asArray(discovery.relations)) {
        if (!relation || !nodeIds.has(String(relation.fromId)) || !nodeIds.has(String(relation.toId))) continue;
        if (!relation.foreignKey && relation.kind !== "MAPS_TO" && relation.kind !== "ASSOCIATES_WITH") continue;
        const id = relation.foreignKey ? String(`fk:${relation.fromId}:${relation.foreignKey.property}->${relation.toId}:${relation.foreignKey.referencesProperty}`) : String(`rel:${relation.fromId}->${relation.toId}:${relation.kind}`);
        if (relationIds.has(id)) continue;
        relationIds.add(id);
        relations.push({
          id,
          fromId: relation.fromId,
          toId: relation.toId,
          kind: relation.kind || "ASSOCIATES_WITH",
          sourceCardinality: relation.sourceCardinality || "ZERO_OR_MORE",
          targetCardinality: relation.targetCardinality || "ONE",
          foreignKey: relation.foreignKey
        });
      }
      for (const relation of inferForeignKeyRelations(nodes, sqlFacts)) {
        const key = normalizedForeignKeyRelationKey(relation);
        if (relationIds.has(key)) continue;
        relationIds.add(key);
        relations.push(relation);
      }
      return relations;
    }
    function inferForeignKeyRelations(nodes, sqlFacts) {
      const relations = [];
      const nodeByTable = new Map(nodes.map((node) => [normalizeName(node.tableName || node.name), node]));
      const sqlRelatedNodeIds = /* @__PURE__ */ new Set();
      for (const join of sqlFacts.joins) {
        const left = nodeByTable.get(normalizeName(join.leftTable));
        const right = nodeByTable.get(normalizeName(join.rightTable));
        if (!left || !right || left.id === right.id) continue;
        sqlRelatedNodeIds.add(left.id);
        sqlRelatedNodeIds.add(right.id);
        const leftKey = primaryKeyProperty(left);
        const rightKey = primaryKeyProperty(right);
        const leftIsPrimary = leftKey && normalizeName(leftKey.columnName || leftKey.name) === normalizeName(join.leftColumn);
        const rightIsPrimary = rightKey && normalizeName(rightKey.columnName || rightKey.name) === normalizeName(join.rightColumn);
        const orientation = persistenceJoinOrientation(left, right, join, leftIsPrimary, rightIsPrimary);
        const source = orientation.source;
        const target = orientation.target;
        const sourceColumn = orientation.sourceColumn;
        const targetColumn = orientation.targetColumn;
        relations.push({
          id: `fk:${source.id}:${sourceColumn}->${target.id}:${targetColumn}`,
          fromId: source.id,
          toId: target.id,
          kind: "ASSOCIATES_WITH",
          sourceCardinality: "ZERO_OR_MORE",
          targetCardinality: "ONE",
          foreignKey: {
            property: sourceColumn,
            referencesProperty: targetColumn
          }
        });
      }
      for (const source of nodes) {
        for (const property of asArray(source.properties)) {
          if (property.isKey) continue;
          for (const target of nodes) {
            if (source.id === target.id) continue;
            const targetKey = primaryKeyProperty(target);
            if (!targetKey || !looksLikeForeignKey(property, target, targetKey)) continue;
            if (!shouldInferPropertyForeignKey(source, target, sqlRelatedNodeIds)) continue;
            relations.push({
              id: `fk:${source.id}:${property.name}->${target.id}:${targetKey.name}`,
              fromId: source.id,
              toId: target.id,
              kind: "ASSOCIATES_WITH",
              sourceCardinality: property.required ? "ONE_OR_MORE" : "ZERO_OR_MORE",
              targetCardinality: "ONE",
              foreignKey: {
                property: property.name,
                referencesProperty: targetKey.name
              }
            });
            break;
          }
        }
      }
      return relations;
    }
    function normalizedForeignKeyRelationKey(relation) {
      return [
        relation.fromId,
        relation.toId,
        normalizeName(relation.foreignKey?.property),
        normalizeName(relation.foreignKey?.referencesProperty)
      ].join(":");
    }
    function shouldInferPropertyForeignKey(source, target, sqlRelatedNodeIds) {
      const sourceTable = normalizeSingular(source?.tableName || source?.name);
      const targetTable = normalizeSingular(target?.tableName || target?.name);
      return sourceTable.includes(targetTable) || sqlRelatedNodeIds.has(source.id);
    }
    function persistenceJoinOrientation(left, right, join, leftIsPrimary, rightIsPrimary) {
      const leftTable = normalizeSingular(left?.tableName || left?.name);
      const rightTable = normalizeSingular(right?.tableName || right?.name);
      const leftColumn = normalizeName(join.leftColumn);
      const rightColumn = normalizeName(join.rightColumn);
      const leftColumnNamesLeftTable = leftColumn.includes(leftTable);
      const rightColumnNamesRightTable = rightColumn.includes(rightTable);
      if (leftColumnNamesLeftTable && !rightColumnNamesRightTable) {
        return { source: right, target: left, sourceColumn: join.rightColumn, targetColumn: join.leftColumn };
      }
      if (rightColumnNamesRightTable && !leftColumnNamesLeftTable) {
        return { source: left, target: right, sourceColumn: join.leftColumn, targetColumn: join.rightColumn };
      }
      if (leftIsPrimary && !rightIsPrimary) {
        return { source: right, target: left, sourceColumn: join.rightColumn, targetColumn: join.leftColumn };
      }
      if (rightIsPrimary && !leftIsPrimary) {
        return { source: left, target: right, sourceColumn: join.leftColumn, targetColumn: join.rightColumn };
      }
      return { source: right, target: left, sourceColumn: join.rightColumn, targetColumn: join.leftColumn };
    }
    function collectPersistenceSqlFacts(project, discovery) {
      const facts = { tables: /* @__PURE__ */ new Map(), joins: [] };
      const rootPath = workspaceSettings(project).workspaceFolderPath || project?.workspacePath;
      if (!rootPath) return facts;
      const sourceRefs = /* @__PURE__ */ new Set();
      for (const node of asArray(discovery.nodes)) {
        for (const evidence of asArray(node.evidence)) {
          if (evidence?.sourceRef) sourceRefs.add(String(evidence.sourceRef));
        }
        for (const property of asArray(node.properties)) {
          for (const evidence of asArray(property.evidence)) {
            if (evidence?.sourceRef) sourceRefs.add(String(evidence.sourceRef));
          }
        }
      }
      for (const sourceRef of sourceRefs) {
        const sourcePath = resolveEvidencePath(rootPath, sourceRef);
        if (!sourcePath) continue;
        try {
          for (const statement of extractExecSqlStatements(fs.readFileSync(sourcePath, "utf8"))) {
            collectSqlStatementFacts(statement, facts);
          }
        } catch (error) {
        }
      }
      return facts;
    }
    function resolveEvidencePath(rootPath, sourceRef) {
      const cleanRef = String(sourceRef || "").split("#")[0].split(":")[0];
      const candidates = [
        path.resolve(rootPath, cleanRef),
        path.resolve(rootPath, "demo/cics-genapp", cleanRef),
        path.resolve(rootPath, "demo/cics-genapp/base/src", path.basename(cleanRef)),
        path.resolve(rootPath, "base/src", path.basename(cleanRef))
      ];
      return candidates.find((candidate) => fs.existsSync(candidate) && fs.statSync(candidate).isFile());
    }
    function extractExecSqlStatements(source) {
      const statements = [];
      const matcher = /EXEC\s+SQL([\s\S]*?)END-EXEC/gi;
      let match;
      while ((match = matcher.exec(source || "")) !== null) {
        const sql = match[1].replace(/\s+/g, " ").trim();
        if (sql) statements.push(sql);
      }
      return statements;
    }
    function collectSqlStatementFacts(statement, facts) {
      const sql = String(statement || "").replace(/\s+/g, " ").trim();
      collectInsertColumns(sql, facts);
      collectUpdateColumns(sql, facts);
      collectSelectColumns(sql, facts);
      collectJoinColumns(sql, facts);
    }
    function rememberTableColumn(facts, table, column) {
      const tableKey = normalizeName(table);
      const columnName = normalizeSqlColumn(column);
      if (!tableKey || !columnName || columnName.startsWith(":")) return;
      const columns = facts.tables.get(tableKey) || [];
      if (!columns.some((value2) => normalizeName(value2) === normalizeName(columnName))) columns.push(columnName);
      facts.tables.set(tableKey, columns);
    }
    function collectInsertColumns(sql, facts) {
      const matcher = /\bINSERT\s+INTO\s+([A-Z0-9_]+)\s*\(([\s\S]*?)\)\s*VALUES\b/ig;
      let match;
      while ((match = matcher.exec(sql)) !== null) {
        splitSqlList(match[2]).forEach((column) => rememberTableColumn(facts, match[1], column));
      }
    }
    function collectUpdateColumns(sql, facts) {
      const match = /\bUPDATE\s+([A-Z0-9_]+)\s+SET\s+([\s\S]*?)(?:\bWHERE\b|$)/i.exec(sql);
      if (!match) return;
      splitSqlList(match[2]).map((value2) => value2.split("=")[0]).forEach((column) => rememberTableColumn(facts, match[1], column));
    }
    function collectSelectColumns(sql, facts) {
      const match = /\bSELECT\s+([\s\S]*?)\s+\bFROM\s+([\s\S]*?)(?:\s+\bWHERE\b|\s+\bORDER\b|\s+\bGROUP\b|$)/i.exec(sql);
      if (!match) return;
      const tables = splitSqlList(match[2]).map((table) => normalizeSqlColumn(table.split(/\s+/)[0]));
      if (tables.length === 1) {
        splitSqlList(match[1]).forEach((column) => rememberTableColumn(facts, tables[0], column.includes(".") ? column.split(".").pop() : column));
        return;
      }
      splitSqlList(match[1]).filter((column) => column.includes(".")).forEach((column) => {
        const [table, field] = column.split(".");
        rememberTableColumn(facts, table, field);
      });
    }
    function collectJoinColumns(sql, facts) {
      const matcher = /\b([A-Z][A-Z0-9_]*)\.([A-Z][A-Z0-9_]*)\s*=\s*([A-Z][A-Z0-9_]*)\.([A-Z][A-Z0-9_]*)\b/ig;
      let match;
      while ((match = matcher.exec(sql)) !== null) {
        rememberTableColumn(facts, match[1], match[2]);
        rememberTableColumn(facts, match[3], match[4]);
        facts.joins.push({
          leftTable: normalizeSqlColumn(match[1]),
          leftColumn: normalizeSqlColumn(match[2]),
          rightTable: normalizeSqlColumn(match[3]),
          rightColumn: normalizeSqlColumn(match[4])
        });
      }
    }
    function splitSqlList(value2) {
      return String(value2 || "").split(",").map((item) => normalizeSqlColumn(item)).filter(Boolean);
    }
    function normalizeSqlColumn(value2) {
      return String(value2 || "").replace(/\b(DISTINCT|AS)\b/ig, " ").replace(/["'`]/g, "").replace(/\([^)]*\)/g, "").trim().split(/\s+/)[0].replace(/[^A-Za-z0-9_:-]/g, "");
    }
    function primaryKeyProperty(node) {
      return asArray(node?.properties).find((property) => property.isKey) || asArray(node?.properties).find((property) => looksLikePrimaryKey(property, node?.tableName, node?.name));
    }
    function looksLikeForeignKey(property, target, targetKey) {
      const propertyName = normalizeName(property?.columnName || property?.sourceColumn || property?.name);
      const keyName = normalizeName(targetKey?.columnName || targetKey?.sourceColumn || targetKey?.name);
      const targetTable = normalizeSingular(target?.tableName || target?.name);
      if (!propertyName || !targetTable || !keyName) return false;
      if (propertyName === keyName) return keyName.includes(targetTable);
      return propertyName === `${targetTable}${keyName}` || propertyName === `${targetTable}ID` || propertyName === `${targetTable}KEY` || propertyName === `${targetTable}NO` || propertyName === `${targetTable}NUMBER` || propertyName.includes(targetTable) && (propertyName.endsWith(keyName) || propertyName.endsWith("ID"));
    }
    function normalizeSingular(value2) {
      const normalized = normalizeName(value2);
      return normalized.endsWith("S") ? normalized.slice(0, -1) : normalized;
    }
    function nativeArchitectureDocument(project, domain) {
      const projection = classModelProjection(domain, { componentLimit: Infinity, classLimit: Infinity, repositoryLimit: Infinity });
      const canvas = projection.items.map((item) => ({
        id: item.id,
        layer: nativeArchitectureLayer(item.packageName),
        kind: "COMPONENT",
        label: item.name || item.id,
        packageName: nativeArchitecturePackage(project, item.packageName),
        className: item.name || item.id,
        componentId: item.sourceId || item.logicalId || item.id
      }));
      return {
        profile: {
          style: "HEXAGONAL",
          packageRoots: {
            "inbound-adapter": `${nativeTargetPackage(project)}.adapter.in`,
            "inbound-port": `${nativeTargetPackage(project)}.port.in`,
            application: `${nativeTargetPackage(project)}.application`,
            domain: `${nativeTargetPackage(project)}.domain`,
            "outbound-port": `${nativeTargetPackage(project)}.port.out`,
            "outbound-adapter": `${nativeTargetPackage(project)}.adapter.out.persistence`
          },
          suffixes: {
            "inbound-adapter": "Controller",
            "inbound-port": "Port",
            application: "UseCase",
            domain: "",
            "outbound-port": "Port",
            "outbound-adapter": "Adapter"
          },
          dependencyRules: [
            { fromLayer: "inbound-adapter", toLayer: "inbound-port", allowed: true, reason: "Driving adapters call inbound ports." },
            { fromLayer: "inbound-port", toLayer: "application", allowed: true, reason: "Inbound ports expose application use cases." },
            { fromLayer: "application", toLayer: "domain", allowed: true, reason: "Use cases coordinate domain behavior." },
            { fromLayer: "application", toLayer: "outbound-port", allowed: true, reason: "Application core depends on outbound ports." },
            { fromLayer: "outbound-adapter", toLayer: "outbound-port", allowed: true, reason: "Driven adapters implement outbound ports." },
            { fromLayer: "domain", toLayer: "inbound-adapter", allowed: false, reason: "Domain model must not depend on driving adapters." },
            { fromLayer: "domain", toLayer: "outbound-adapter", allowed: false, reason: "Domain model must not depend on driven adapters." }
          ],
          layout: {},
          excludedNodeIds: []
        },
        canvas,
        dependencyDiagnostics: []
      };
    }
    function nativeArchitectureLayer(packageName) {
      const normalized = String(packageName || "").toLowerCase();
      if (normalized.includes("application")) return "service";
      if (normalized.includes("infrastructure") || normalized.includes("persistence")) return "persistence";
      return "model";
    }
    function nativeArchitecturePackage(project, packageName) {
      return `${nativeTargetPackage(project)}.${String(packageName || "domain").replace(/[_-]+/g, ".")}`;
    }
    function nativeTargetPackage(project) {
      return workspaceSettings(project).targetPackage || "com.example.modernized";
    }
    function classModelProjection(domain, limits) {
      const componentLimit = limits?.componentLimit ?? 18;
      const classLimit = limits?.classLimit ?? 28;
      const repositoryLimit = limits?.repositoryLimit ?? 28;
      const discovery = domainDiscovery(domain);
      const taken = /* @__PURE__ */ new Set();
      const itemBySourceId = /* @__PURE__ */ new Map();
      const componentNodes = uniqueNodes([
        ...discovery.repositories.flatMap((repository) => asArray(repository.users)),
        ...discovery.nodes.filter((node) => sameKind(node.kind, "SERVICE") || sameKind(node.kind, "USE_CASE"))
      ]).slice(0, componentLimit);
      const classNodes = uniqueNodes(discovery.repositories.flatMap((repository) => asArray(repository.mappedNodes))).sort((left, right) => asArray(right.properties).length - asArray(left.properties).length || String(left.name || left.id).localeCompare(String(right.name || right.id))).slice(0, classLimit);
      const repositoryNodes = discovery.repositories.filter((repository) => asArray(repository.properties).length || asArray(repository.mappedNodes).length || asArray(repository.users).length).sort(compareRepositoriesForDiagram).slice(0, repositoryLimit);
      const items = [
        ...componentNodes.map((node, index) => projectUmlItem(node, "application", "component", taken, index)),
        ...classNodes.map((node, index) => projectUmlItem(node, "domain_model", classStereotype(node), taken, index)),
        ...repositoryNodes.map((node, index) => projectUmlItem(node, "infrastructure_persistence", repositoryStereotype(node), taken, index))
      ];
      for (const item of items) {
        if (item.sourceId) itemBySourceId.set(item.sourceId, item);
        if (item.logicalId) itemBySourceId.set(item.logicalId, item);
      }
      const relations = [];
      const relationIds = /* @__PURE__ */ new Set();
      for (const repository of repositoryNodes) {
        const repositoryItem = itemBySourceId.get(repository.logicalId || repository.id);
        if (!repositoryItem) continue;
        for (const user of asArray(repository.users)) {
          const componentItem = itemBySourceId.get(user.id);
          if (componentItem) addMermaidRelation(relations, relationIds, componentItem.id, repositoryItem.id, "..>", "uses adapter");
          for (const mapped of asArray(repository.mappedNodes)) {
            const classItem = itemBySourceId.get(mapped.id);
            if (componentItem && classItem) addMermaidRelation(relations, relationIds, componentItem.id, classItem.id, "..>", "uses");
          }
        }
        for (const mapped of asArray(repository.mappedNodes)) {
          const classItem = itemBySourceId.get(mapped.id);
          if (classItem) {
            addMermaidRelation(relations, relationIds, classItem.id, repositoryItem.id, "-->", "persists via");
            addMermaidRelation(relations, relationIds, repositoryItem.id, classItem.id, "..>", "maps record");
          }
        }
      }
      return {
        packages: ["application", "domain_model", "infrastructure_persistence"],
        items,
        relations
      };
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
      if (stereotype === "component") {
        return [`+source ${mermaidMemberName(node?.name || node?.id || "program")}`];
      }
      if (stereotype === "repository" || stereotype === "fileAdapter") {
        const mapped = asArray(node?.mappedNodes).map((candidate) => mermaidMemberName(candidate.name || candidate.id)).slice(0, 4);
        return mapped.length ? mapped.map((name) => `+maps ${name}`) : [`+resource ${mermaidMemberName(node?.name || node?.id || "repository")}`];
      }
      return properties.length ? properties.slice(0, 12).map((property) => `+${mermaidType(property.type)} ${mermaidMemberName(property.name || "field")}`) : [`+source ${mermaidMemberName(node?.name || node?.id || "record")}`];
    }
    function umlOperationsFor(stereotype, properties) {
      if (stereotype === "component") return ["+execute()", "+coordinate()"];
      if (stereotype === "repository") return ["+load()", "+save()", "+query()"];
      if (stereotype === "fileAdapter") return ["+read()", "+write()"];
      return properties.length ? ["+validate()", "+toDTO()"] : ["+discoverShape()"];
    }
    function classStereotype(node) {
      const kind = String(node?.kind || "").toUpperCase();
      if (kind.includes("VALUE")) return "valueObject";
      if (kind.includes("AGGREGATE")) return "aggregate";
      return "entity";
    }
    function repositoryStereotype(node) {
      return persistenceKind(node) === "DB2 table" ? "repository" : "fileAdapter";
    }
    function addMermaidRelation(relations, relationIds, from, to, operator, label) {
      const id = `${from}:${operator}:${to}:${label}`;
      if (relationIds.has(id)) return;
      relationIds.add(id);
      relations.push({ from, to, operator, label });
    }
    function uniqueMermaidIdentifier(value2, fallback, taken) {
      const words = String(value2 || fallback).replace(/[^A-Za-z0-9]+/g, " ").trim().split(/\s+/).filter(Boolean);
      let base = words.map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join("") || fallback;
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
    function mermaidMemberName(value2) {
      const normalized = String(value2 || "value").replace(/[^A-Za-z0-9_]+/g, "_").replace(/^_+|_+$/g, "");
      return normalized || "value";
    }
    function mermaidType(value2) {
      return mermaidMemberName(value2 || "unknown");
    }
    function sanitizeFileName(value2) {
      return String(value2 || "renovatio").toLowerCase().replace(/[^a-z0-9._-]+/g, "-").replace(/^-+|-+$/g, "") || "renovatio";
    }
    function buildDomainDiagram(domain) {
      const discovery = domainDiscovery(domain);
      if (!discovery.nodes.length) {
        return { width: 1080, height: 220, nodes: [], useCases: [], repositories: [], records: [], edges: [], evidence: [] };
      }
      const repositories = discovery.repositories.filter((repository) => asArray(repository.properties).length || asArray(repository.mappedNodes).length || asArray(repository.users).length).sort((left, right) => asArray(right.properties).length - asArray(left.properties).length || asArray(right.users).length - asArray(left.users).length || String(left.name || left.id).localeCompare(String(right.name || right.id))).slice(0, 8);
      const selectedRepositories = repositories.length ? repositories : discovery.repositories.slice(0, 8);
      const selectedRepositoryIds = new Set(selectedRepositories.map((repository) => repository.logicalId || repository.id));
      const useCases = uniqueNodes(selectedRepositories.flatMap((repository) => asArray(repository.users))).filter((node) => sameKind(node.kind, "USE_CASE") || String(node.id || "").startsWith("use-case:")).slice(0, 8);
      const records = uniqueNodes([
        ...selectedRepositories.flatMap((repository) => asArray(repository.mappedNodes)),
        ...discovery.recordNodes.filter((node) => asArray(node.properties).length)
      ]).slice(0, 8);
      const width = 1080;
      const top = 54;
      const slot = 134;
      const boxHeight = 106;
      const rowCount = Math.max(1, useCases.length, selectedRepositories.length, records.length);
      const height = top + rowCount * slot + 30;
      const diagramUseCases = layoutDiagramColumn(useCases, "use", 24, top, 282, boxHeight, slot);
      const diagramRepositories = layoutDiagramColumn(selectedRepositories, "repo", 386, top, 300, boxHeight, slot);
      const diagramRecords = layoutDiagramColumn(records, "record", 746, top, 310, boxHeight, slot);
      const useById = new Map(diagramUseCases.map((node) => [node.source.id, node]));
      const repoById = new Map(diagramRepositories.map((node) => [node.source.logicalId || node.source.id, node]));
      const recordById = new Map(diagramRecords.map((node) => [node.source.id, node]));
      const edges = [];
      const edgeIds = /* @__PURE__ */ new Set();
      for (const repository of selectedRepositories) {
        const repoNode = repoById.get(repository.logicalId || repository.id);
        if (!repoNode) continue;
        for (const user of asArray(repository.users)) {
          const useNode = useById.get(user.id);
          if (useNode) addDiagramEdge(edges, edgeIds, useNode, repoNode, "uses");
        }
        for (const record of asArray(repository.mappedNodes)) {
          const recordNode = recordById.get(record.id);
          if (recordNode) addDiagramEdge(edges, edgeIds, repoNode, recordNode, "maps");
        }
      }
      const evidence = mergedEvidence([...useCases, ...selectedRepositories, ...records]);
      const totalInteresting = discovery.repositories.length + discovery.recordNodes.filter((node) => asArray(node.properties).length).length;
      const shownInteresting = selectedRepositoryIds.size + records.length;
      const notice = totalInteresting > shownInteresting ? `Showing the most connected ${shownInteresting} of ${totalInteresting} persistence/data-shape candidates. The full model remains below.` : "";
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
        return { width: 1080, height: 220, nodes: [], useCases: [], repositories: [], records: [], edges: [], notice: "" };
      }
      const repositoriesWithFields = discovery.repositories.filter((repository) => asArray(repository.properties).length).sort(compareRepositoriesForDiagram).slice(0, 6);
      const repositoriesWithoutFields = discovery.repositories.filter((repository) => !asArray(repository.properties).length).sort(compareRepositoriesForDiagram).slice(0, 4);
      const selectedRepositories = uniqueNodes([...repositoriesWithFields, ...repositoriesWithoutFields]).slice(0, 10);
      const useCases = uniqueNodes(selectedRepositories.flatMap((repository) => asArray(repository.users))).filter((node) => sameKind(node.kind, "USE_CASE") || String(node.id || "").startsWith("use-case:")).slice(0, 10);
      const records = uniqueNodes(selectedRepositories.flatMap((repository) => asArray(repository.mappedNodes))).sort((left, right) => asArray(right.properties).length - asArray(left.properties).length || String(left.name || left.id).localeCompare(String(right.name || right.id))).slice(0, 10);
      const width = 1080;
      const top = 54;
      const slot = 134;
      const boxHeight = 106;
      const rowCount = Math.max(1, useCases.length, selectedRepositories.length, records.length);
      const height = top + rowCount * slot + 30;
      const diagramUseCases = layoutDiagramColumn(useCases, "use", 24, top, 282, boxHeight, slot);
      const diagramRepositories = layoutDiagramColumn(selectedRepositories, "repo", 386, top, 300, boxHeight, slot);
      const diagramRecords = layoutDiagramColumn(records, "record", 746, top, 310, boxHeight, slot);
      const useById = new Map(diagramUseCases.map((node) => [node.source.id, node]));
      const repoById = new Map(diagramRepositories.map((node) => [node.source.logicalId || node.source.id, node]));
      const recordById = new Map(diagramRecords.map((node) => [node.source.id, node]));
      const edges = [];
      const edgeIds = /* @__PURE__ */ new Set();
      for (const repository of selectedRepositories) {
        const repoNode = repoById.get(repository.logicalId || repository.id);
        if (!repoNode) continue;
        for (const user of asArray(repository.users)) {
          const useNode = useById.get(user.id);
          if (useNode) addDiagramEdge(edges, edgeIds, useNode, repoNode, "uses");
        }
        for (const record of asArray(repository.mappedNodes)) {
          const recordNode = recordById.get(record.id);
          if (recordNode) addDiagramEdge(edges, edgeIds, repoNode, recordNode, "maps");
        }
      }
      const unresolvedCount = discovery.repositories.filter((repository) => !asArray(repository.properties).length).length;
      const notice = unresolvedCount ? `${unresolvedCount} table/resource shape(s) still have no inferred fields. They were detected from I/O, but no COBOL record shape was confidently bound yet.` : "";
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
      const leftDb = persistenceKind(left) === "DB2 table" ? 1 : 0;
      const rightDb = persistenceKind(right) === "DB2 table" ? 1 : 0;
      return rightDb - leftDb || asArray(right.properties).length - asArray(left.properties).length || asArray(right.users).length - asArray(left.users).length || String(left.name || left.id).localeCompare(String(right.name || right.id));
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
      return String(node?.source?.logicalId || node?.source?.id || node?.source?.name || "");
    }
    function renderDiagramEdge(edge, markerScope = "domain") {
      const x1 = edge.from.x + edge.from.width;
      const y1 = edge.from.y + edge.from.height / 2;
      const x2 = edge.to.x;
      const y2 = edge.to.y + edge.to.height / 2;
      const className = edge.kind === "maps" ? "diagramEdge maps" : "diagramEdge";
      const marker = markerScope === "persistence" ? edge.kind === "maps" ? "persistenceArrowMaps" : "persistenceArrowUses" : edge.kind === "maps" ? "arrowMaps" : "arrowUses";
      return `<path class="${className}" data-edge="true" data-from="${escapeHtml(diagramNodeId(edge.from))}" data-to="${escapeHtml(diagramNodeId(edge.to))}" d="M ${x1} ${y1} C ${x1 + 46} ${y1}, ${x2 - 46} ${y2}, ${x2} ${y2}" marker-end="url(#${marker})" />`;
    }
    function renderDiagramNode(node, variant) {
      const source = node.source;
      const title = source.name || source.id;
      const properties = asArray(source.properties);
      const meta = diagramNodeMeta(source, variant);
      const unresolved = variant === "repo" && !asArray(source.properties).length;
      const boxClass = variant === "repo" ? `diagramBox repo${unresolved ? " unresolved" : ""}` : variant === "record" ? "diagramBox record" : "diagramBox";
      return `<g class="debugNode" data-node-id="${escapeHtml(diagramNodeId(node))}" data-x="${node.x}" data-y="${node.y}" data-width="${node.width}" data-height="${node.height}">
    <title>${escapeHtml(title)}</title>
    <rect class="${boxClass}" x="${node.x}" y="${node.y}" width="${node.width}" height="${node.height}" rx="3" />
    ${renderSvgLines(title, node.x + 12, node.y + 22, "diagramTitle", variant === "record" ? 30 : 28, 2, 15, { editable: true })}
    <text class="diagramMeta" x="${node.x + 12}" y="${node.y + 55}">${escapeHtml(meta)}</text>
    ${properties.slice(0, 3).map((property, index) => `<text class="diagramField" x="${node.x + 12}" y="${node.y + 74 + index * 14}">${escapeHtml(compactField(property))}</text>`).join("")}
    ${properties.length > 3 ? `<text class="diagramMeta" x="${node.x + 12}" y="${node.y + 74 + 3 * 14}">+${properties.length - 3} more fields</text>` : ""}
  </g>`;
    }
    function diagramNodeMeta(source, variant) {
      if (variant === "repo") {
        const refs = Number(source.instances || 1);
        return `${persistenceKind(source)} \xB7 ${asArray(source.properties).length} fields \xB7 ${refs} ref${refs === 1 ? "" : "s"}`;
      }
      if (variant === "record") {
        return `${humanize(source.kind || "record")} \xB7 ${asArray(source.properties).length} fields`;
      }
      return humanize(source.kind || "use case");
    }
    function renderSvgLines(value2, x, y, className, maxChars, maxLines, lineHeight, options = {}) {
      return wrapText(value2, maxChars, maxLines).map((line, index) => `<text class="${className}"${options.editable && index === 0 ? ' data-editable-label="true"' : ""} x="${x}" y="${y + index * lineHeight}">${escapeHtml(line)}</text>`).join("");
    }
    function wrapText(value2, maxChars, maxLines) {
      const normalized = String(value2 || "").replace(/\s+/g, " ").trim();
      if (!normalized) return [""];
      const chunks = [];
      let remaining = normalized;
      while (remaining.length && chunks.length < maxLines) {
        if (remaining.length <= maxChars) {
          chunks.push(remaining);
          remaining = "";
          break;
        }
        let cut = remaining.lastIndexOf(" ", maxChars);
        if (cut < Math.floor(maxChars * 0.55)) cut = remaining.lastIndexOf("-", maxChars);
        if (cut < Math.floor(maxChars * 0.55)) cut = maxChars;
        chunks.push(remaining.slice(0, cut).trim());
        remaining = remaining.slice(cut).replace(/^[-\s]+/, "");
      }
      if (remaining && chunks.length) {
        chunks[chunks.length - 1] = `${chunks[chunks.length - 1].replace(/[.\s]+$/, "")}...`;
      }
      return chunks;
    }
    function compactField(property) {
      return `${property.name || "field"}: ${property.type || "unknown"}`;
    }
    function unresolvedShapeReason(repository) {
      const mapped = asArray(repository?.mappedNodes);
      if (mapped.length) {
        return "Shape unresolved: Renovatio linked this table to a COBOL record candidate, but that record has no parsed child fields yet. Check copybook resolution and DATA DIVISION/group parsing for the mapped record.";
      }
      const users = asArray(repository?.users);
      if (users.length) {
        return "Shape unresolved: table access was detected from COBOL I/O, but no matching COBOL group/host-variable record was confidently bound as its column shape.";
      }
      return "Shape unresolved: repository/table candidate exists in the DomainModel, but the current evidence has no source program and no mapped COBOL record shape.";
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
      const nodeById = new Map(nodes.map((node) => [node.id, node]));
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
      const grouped = /* @__PURE__ */ new Map();
      for (const node of nodes) {
        const kind = node.kind || "UNKNOWN";
        if (!grouped.has(kind)) grouped.set(kind, []);
        grouped.get(kind).push(node);
      }
      return [...grouped.entries()].sort(([left], [right]) => left.localeCompare(right)).map(([kind, values]) => `
    <div class="artifact">
      <div class="artifactHeader"><strong>${escapeHtml(humanize(kind))}</strong><span>${values.length}</span></div>
      <div class="domainGrid">
        ${values.map(renderDomainNode).join("")}
      </div>
    </div>`).join("");
    }
    function renderDomainNode(node) {
      const properties = asArray(node.properties);
      const evidence = asArray(node.evidence);
      return `<div class="nodeCard">
    <strong>${escapeHtml(node.name || node.id)}</strong>
    <div class="nodeMeta">
      <span class="pill">${escapeHtml(node.kind || "node")}</span>
      <span class="pill">${escapeHtml(node.origin || "UNKNOWN")}</span>
      <span class="pill">${Math.round(Number(node.confidence || 0) * 100)}%</span>
    </div>
    ${properties.length ? `<div class="pillRow">${properties.slice(0, 8).map((property) => `<span class="pill">${escapeHtml(property.name)}: ${escapeHtml(property.type)}</span>`).join("")}</div>` : ""}
    ${renderEvidence(evidence)}
    <code>${escapeHtml(node.id)}</code>
  </div>`;
    }
    function renderRelations(relations, nodeById) {
      if (!relations.length) return '<div class="empty">No relations persisted yet.</div>';
      return `<div class="relationList">${relations.map((relation) => {
        const from = nodeById.get(relation.fromId);
        const to = nodeById.get(relation.toId);
        return `<div class="relationRow">
      <strong>${escapeHtml(from?.name || relation.fromId)}</strong>
      <span class="relationKind">${escapeHtml(humanize(relation.kind || "relates"))}</span>
      <strong>${escapeHtml(to?.name || relation.toId)}</strong>
    </div>`;
      }).join("")}</div>`;
    }
    function renderInvariants(invariants, nodeById) {
      if (!invariants.length) return '<div class="empty">No business invariants persisted yet.</div>';
      return `<div class="domainGrid">${invariants.map((invariant) => {
        const subject = nodeById.get(invariant.subjectId);
        return `<div class="nodeCard">
      <strong>${escapeHtml(invariant.expression || invariant.id)}</strong>
      <div class="nodeMeta">
        <span class="pill">${escapeHtml(subject?.name || invariant.subjectId)}</span>
        <span class="pill">${escapeHtml(invariant.origin || "UNKNOWN")}</span>
      </div>
      ${renderEvidence(asArray(invariant.evidence))}
    </div>`;
      }).join("")}</div>`;
    }
    function renderDomainModelGovernance(domain) {
      const diagnostics = asArray(domain?.diagnostics);
      const suggestions = asArray(domain?.suggestions);
      return `<section class="panel">
    <div class="sectionTitle">Governance</div>
    ${diagnostics.length ? `<div class="relationList">${diagnostics.map((diagnostic2) => `<div class="warning"><strong>${escapeHtml(diagnostic2.code)}</strong> \xB7 ${escapeHtml(diagnostic2.targetId)}<br>${escapeHtml(diagnostic2.message)}</div>`).join("")}</div>` : '<div class="empty">No DomainModel diagnostics.</div>'}
    ${suggestions.length ? `<div class="artifact">
      <div class="artifactHeader"><strong>Suggestions</strong><span>${suggestions.length}</span></div>
      <div class="relationList">${suggestions.map((suggestion) => `<div class="relationRow">
        <strong>${escapeHtml(suggestion.name || suggestion.targetId)}</strong>
        <span class="relationKind">${escapeHtml(suggestion.targetType || "suggestion")}</span>
        <span class="pill">${escapeHtml(suggestion.status || "pending")}</span>
      </div>`).join("")}</div>
    </div>` : ""}
  </section>`;
    }
    function renderEvidence(evidence) {
      if (!evidence.length) return "";
      return `<div class="evidenceList">${evidence.slice(0, 3).map((item) => `<code title="${escapeHtml(item.rationale || "")}">${escapeHtml(item.sourceRef || item.provenance || "evidence")}</code>`).join("")}</div>`;
    }
    function renderFileList(files, actionLabel) {
      if (!files.length) return "";
      return `<div class="fileList">${files.map((file) => `<div class="fileRow">
    <strong>${escapeHtml(file.relativePath)}</strong>
    <span>${escapeHtml(file.language)}</span>
    ${openFileLink(file.fsPath, actionLabel)}
  </div>`).join("")}</div>`;
    }
    function openFileLink(fsPath, label) {
      return commandLink("renovatio.openFile", [fsPath], label);
    }
    function openEvidenceLink(sourceRef, label) {
      return commandLink("renovatio.openEvidence", [sourceRef], label);
    }
    function commandLink(command, args, label) {
      const encoded = encodeURIComponent(JSON.stringify(args));
      return `<a href="command:${command}?${encoded}">${escapeHtml(label)}</a>`;
    }
    async function loadGeneratedArtifacts(project) {
      const settings = workspaceSettings(project);
      const root = settings.generatedRoot;
      if (!root) return { root: "", exists: false, files: [] };
      const configurationWarning = settings.generatedRootWarning ? `${settings.generatedRootWarning}. Using ${settings.generatedRoot} instead.` : "";
      const rootUri = vscode4.Uri.file(root);
      try {
        const stat = await vscode4.workspace.fs.stat(rootUri);
        if (stat.type !== vscode4.FileType.Directory) return { root, exists: false, files: [], notice: configurationWarning };
      } catch {
        return { root, exists: false, files: [], notice: configurationWarning };
      }
      if (await containsCobolArtifacts(rootUri, 0)) {
        return {
          root,
          exists: true,
          files: [],
          warning: "The future output folder appears to contain COBOL/JCL input files. Future output must be separate from COBOL source roots."
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
      if (!root) return { root: "", exists: false, files: [] };
      const rootUri = vscode4.Uri.file(root);
      try {
        const stat = await vscode4.workspace.fs.stat(rootUri);
        if (stat.type !== vscode4.FileType.Directory) return { root, exists: false, files: [] };
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
        entries = await vscode4.workspace.fs.readDirectory(uri);
      } catch {
        return;
      }
      entries.sort(([leftName, leftType], [rightName, rightType]) => {
        if (leftType !== rightType) return leftType === vscode4.FileType.Directory ? -1 : 1;
        return leftName.localeCompare(rightName);
      });
      for (const [name, type] of entries) {
        if (files.length >= 16) break;
        const child = vscode4.Uri.joinPath(uri, name);
        if (type === vscode4.FileType.Directory) {
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
        entries = await vscode4.workspace.fs.readDirectory(uri);
      } catch {
        return;
      }
      entries.sort(([leftName, leftType], [rightName, rightType]) => {
        if (leftType !== rightType) return leftType === vscode4.FileType.Directory ? -1 : 1;
        return leftName.localeCompare(rightName);
      });
      for (const [name, type] of entries) {
        if (files.length >= 20) break;
        const child = vscode4.Uri.joinPath(uri, name);
        if (type === vscode4.FileType.Directory) {
          if (!shouldSkipPreviewDirectory(name)) {
            await collectCobolFiles(child, root, files, depth + 1);
          }
        } else if (isCobolArtifact(name)) {
          files.push(child);
        }
      }
    }
    async function containsCobolArtifacts(uri, depth) {
      if (depth > 8) return false;
      let entries = [];
      try {
        entries = await vscode4.workspace.fs.readDirectory(uri);
      } catch {
        return false;
      }
      for (const [name, type] of entries) {
        if (type === vscode4.FileType.Directory) {
          if (!shouldSkipPreviewDirectory(name) && await containsCobolArtifacts(vscode4.Uri.joinPath(uri, name), depth + 1)) {
            return true;
          }
        } else if (isCobolArtifact(name)) {
          return true;
        }
      }
      return false;
    }
    function shouldSkipPreviewDirectory(name) {
      return name.startsWith(".") || ["node_modules", "target", "build", "dist", "out"].includes(name);
    }
    function generatedOutputPath(project) {
      if (!project) return "";
      return workspaceSettings(project).generatedRoot;
    }
    function isPreviewableSource(name) {
      return /\.(java|kt|py|js|ts|xml|json|ya?ml)$/i.test(name);
    }
    function isCobolArtifact(name) {
      return /\.(cbl|cob|cobol|cpy|copybook|jcl|job|proc)$/i.test(name);
    }
    function languageForFile(filePath) {
      const extension = path.extname(filePath).replace(".", "").toLowerCase();
      return extension || "text";
    }
    function sumInventory(inventory) {
      return Object.values(inventory || {}).reduce((total, value2) => total + (Number(value2) || 0), 0);
    }
    function asArray(value2) {
      return Array.isArray(value2) ? value2 : [];
    }
    function normalizeProgress(value2) {
      const numeric = Number(value2);
      if (!Number.isFinite(numeric)) return 0;
      if (numeric <= 1) return Math.round(numeric * 100);
      return Math.round(Math.min(numeric, 100));
    }
    function humanize(key) {
      return String(key).replace(/([a-z])([A-Z])/g, "$1 $2").replace(/[_-]+/g, " ");
    }
    function normalizeName(value2) {
      return String(value2 || "").toUpperCase().replace(/[^A-Z0-9]/g, "");
    }
    function sameKind(value2, expected) {
      return String(value2 || "").toUpperCase() === String(expected || "").toUpperCase();
    }
    function samePath(left, right) {
      if (!left || !right) return false;
      return path.resolve(left) === path.resolve(right);
    }
    function isInsidePath(candidate, parent) {
      if (!candidate || !parent) return false;
      const relative = path.relative(path.resolve(parent), path.resolve(candidate));
      return Boolean(relative) && !relative.startsWith("..") && !path.isAbsolute(relative);
    }
    function updateStatus() {
      const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
      const settings = workspaceSettings(project);
      statusItem.text = project ? `$(tools) Renovatio: ${project.name}` : "$(tools) Renovatio";
      statusItem.tooltip = project ? `Workspace: ${settings.workspaceFolderPath || project.workspacePath || "not configured"}
COBOL roots: ${settings.configuredCobolRoots.length ? settings.configuredCobolRoots.join(", ") : "not configured"}` : "Select Renovatio project";
    }
    var ProjectsProvider = class {
      constructor() {
        this._onDidChangeTreeData = new vscode4.EventEmitter();
        this.onDidChangeTreeData = this._onDidChangeTreeData.event;
      }
      refresh() {
        this._onDidChangeTreeData.fire();
      }
      getTreeItem(item) {
        return item;
      }
      getChildren(item) {
        if (item?.children) return item.children;
        const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
        if (!project) {
          const create = new vscode4.TreeItem("Create Renovatio project", vscode4.TreeItemCollapsibleState.None);
          create.description = "from COBOL source root";
          create.iconPath = new vscode4.ThemeIcon("new-folder");
          create.command = { command: "renovatio.createProject", title: "Create Renovatio Project" };
          const select = new vscode4.TreeItem("Select Renovatio project", vscode4.TreeItemCollapsibleState.None);
          select.iconPath = new vscode4.ThemeIcon("folder-active");
          select.command = { command: "renovatio.selectProject", title: "Select Renovatio Project" };
          return [create, select];
        }
        const settings = workspaceSettings(project);
        const workspace4 = new vscode4.TreeItem(path.basename(settings.workspaceFolderPath || project.workspacePath || "Workspace"), vscode4.TreeItemCollapsibleState.None);
        workspace4.description = settings.workspaceFolderPath || project.workspacePath || "";
        workspace4.iconPath = new vscode4.ThemeIcon("root-folder");
        workspace4.tooltip = settings.workspaceFolderPath || project.workspacePath || "VS Code workspace";
        const switchProject = new vscode4.TreeItem("Switch project...", vscode4.TreeItemCollapsibleState.None);
        switchProject.description = `${state.projects.length} available`;
        switchProject.iconPath = new vscode4.ThemeIcon("folder-active");
        switchProject.tooltip = "Choose a different Renovatio project";
        switchProject.command = { command: "renovatio.selectProject", title: "Select Renovatio Project" };
        const projectItem = new vscode4.TreeItem(project.name, vscode4.TreeItemCollapsibleState.None);
        projectItem.description = "Active project";
        projectItem.contextValue = "renovatioProject";
        projectItem.iconPath = new vscode4.ThemeIcon("repo");
        projectItem.tooltip = 'The currently active Renovatio project \u2014 use "Switch project..." above to change it';
        const sourceRootItems = settings.configuredCobolRoots.map((root) => {
          const rootItem = new vscode4.TreeItem(path.basename(root) || root, vscode4.TreeItemCollapsibleState.None);
          rootItem.description = root;
          rootItem.iconPath = new vscode4.ThemeIcon("folder");
          rootItem.tooltip = `Analyze COBOL source root
${root}`;
          rootItem.command = { command: "renovatio.analyzeSelectedPath", title: "Analyze COBOL Source Root", arguments: [vscode4.Uri.file(root)] };
          return rootItem;
        });
        const addRoot = new vscode4.TreeItem("Add COBOL source root", vscode4.TreeItemCollapsibleState.None);
        addRoot.iconPath = new vscode4.ThemeIcon("add");
        addRoot.command = { command: "renovatio.addCobolSourceRoot", title: "Add COBOL Source Root" };
        sourceRootItems.push(addRoot);
        if (settings.rawCobolRoots.length) {
          const removeRoot = new vscode4.TreeItem("Remove COBOL source root", vscode4.TreeItemCollapsibleState.None);
          removeRoot.iconPath = new vscode4.ThemeIcon("remove");
          removeRoot.command = { command: "renovatio.removeCobolSourceRoot", title: "Remove COBOL Source Root" };
          sourceRootItems.push(removeRoot);
        }
        const sourcesLabel = settings.configuredCobolRoots.length ? `COBOL source roots (${settings.configuredCobolRoots.length})` : "COBOL source roots (workspace fallback)";
        const sources = new vscode4.TreeItem(sourcesLabel, vscode4.TreeItemCollapsibleState.Expanded);
        sources.iconPath = new vscode4.ThemeIcon("references");
        sources.children = sourceRootItems;
        const generated = new vscode4.TreeItem(path.basename(settings.generatedRoot || "Future output"), vscode4.TreeItemCollapsibleState.None);
        generated.description = settings.generatedRoot;
        generated.iconPath = new vscode4.ThemeIcon("file-code");
        generated.tooltip = settings.generatedRoot;
        generated.command = { command: "renovatio.selectGeneratedOutputFolder", title: "Configure Future Output Folder" };
        const target = new vscode4.TreeItem(`${settings.targetLanguage}`, vscode4.TreeItemCollapsibleState.None);
        target.description = settings.targetPackage;
        target.iconPath = new vscode4.ThemeIcon("symbol-namespace");
        return [switchProject, projectItem, workspace4, sources, generated, target];
      }
    };
    var OperationsProvider = class {
      constructor() {
        this._onDidChangeTreeData = new vscode4.EventEmitter();
        this.onDidChangeTreeData = this._onDidChangeTreeData.event;
      }
      refresh() {
        this._onDidChangeTreeData.fire();
      }
      getTreeItem(item) {
        return item;
      }
      getChildren() {
        const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
        const settings = workspaceSettings(project);
        const operations = [
          {
            label: "Create Renovatio project",
            description: "from COBOL source root",
            icon: "new-folder",
            command: "renovatio.createProject"
          },
          {
            label: "Select Renovatio project",
            description: project?.name || "",
            icon: "folder-active",
            command: "renovatio.selectProject"
          },
          {
            label: "Add COBOL source root",
            description: settings.workspaceFolderPath || "",
            icon: "add",
            command: "renovatio.addCobolSourceRoot"
          },
          {
            label: "Configure future output",
            description: settings.rawGeneratedRoot || "",
            icon: "file-code",
            command: "renovatio.selectGeneratedOutputFolder"
          },
          {
            label: "Analyze COBOL sources",
            description: settings.configuredCobolRoots.length ? `${settings.configuredCobolRoots.length} configured` : "workspace fallback",
            icon: "run-all",
            command: "renovatio.analyzeCobolSources"
          },
          {
            label: "Analyze Explorer selection",
            description: "Explorer context",
            icon: "play",
            command: "renovatio.analyzeSelectedPath"
          },
          {
            label: "Open control deck",
            description: "summary dashboard",
            icon: "dashboard",
            command: "renovatio.openControlDeck"
          },
          {
            label: "Open domain model",
            description: state.domainModel?.revision ? `revision ${state.domainModel.revision}` : "seeded after discovery",
            icon: "symbol-structure",
            command: "renovatio.openDomainModel"
          },
          {
            label: "Open native domain diagram",
            description: state.domainModel?.revision ? `revision ${state.domainModel.revision}` : "run discovery first",
            icon: "type-hierarchy",
            command: "renovatio.openNativeDomainDiagram"
          },
          {
            label: "Open native persistence diagram",
            description: domainDiscovery(state.domainModel).repositories.length ? "repositories and records" : "run discovery first",
            icon: "database",
            command: "renovatio.openNativePersistenceDiagram"
          },
          {
            label: "Open native architecture diagram",
            description: "target layers",
            icon: "circuit-board",
            command: "renovatio.openNativeArchitectureDiagram"
          },
          {
            label: "Open inferred persistence",
            description: domainDiscovery(state.domainModel).repositories.length ? "ER model" : "seeded after discovery",
            icon: "database",
            command: "renovatio.openPersistenceModel"
          },
          {
            label: "Open future output",
            description: generatedOutputPath(project),
            icon: "file-code",
            command: "renovatio.openGeneratedCode"
          },
          {
            label: "Refresh Renovatio",
            description: config().backendUrl,
            icon: "refresh",
            command: "renovatio.refresh"
          }
        ];
        return operations.map((operation) => {
          const item = new vscode4.TreeItem(operation.label, vscode4.TreeItemCollapsibleState.None);
          item.description = operation.description;
          item.iconPath = new vscode4.ThemeIcon(operation.icon);
          item.command = { command: operation.command, title: operation.label };
          return item;
        });
      }
    };
    var AnalysisProvider = class {
      constructor() {
        this._onDidChangeTreeData = new vscode4.EventEmitter();
        this.onDidChangeTreeData = this._onDidChangeTreeData.event;
      }
      refresh() {
        this._onDidChangeTreeData.fire();
      }
      getTreeItem(item) {
        return item;
      }
      getChildren(item) {
        if (item?.children) return item.children;
        const inventory = workbenchInventory(state.analysis, state.latestJob);
        const entries = Object.entries(inventory);
        const discovery = domainDiscovery(state.domainModel);
        const result = [];
        if (discovery.repositories.length) {
          const group = new vscode4.TreeItem(`Inferred persistence (${discovery.repositories.length})`, vscode4.TreeItemCollapsibleState.Expanded);
          group.iconPath = new vscode4.ThemeIcon("database");
          group.command = { command: "renovatio.openPersistenceModel", title: "Open Persistence Model" };
          group.children = discovery.repositories.slice(0, 12).map((repository) => {
            const item2 = new vscode4.TreeItem(repository.name || repository.id, vscode4.TreeItemCollapsibleState.None);
            item2.description = `${repository.properties.length} fields`;
            item2.tooltip = `${repository.name || repository.id}
${repository.properties.map((property) => property.name).slice(0, 12).join(", ")}`;
            item2.iconPath = new vscode4.ThemeIcon(persistenceKind(repository) === "DB2 table" ? "database" : "file-binary");
            item2.command = { command: "renovatio.openPersistenceModel", title: "Open Persistence Model" };
            return item2;
          });
          result.push(group);
        }
        const shapedRecords = discovery.recordNodes.filter((node) => asArray(node.properties).length);
        if (shapedRecords.length) {
          const group = new vscode4.TreeItem(`Data records (${shapedRecords.length})`, vscode4.TreeItemCollapsibleState.Collapsed);
          group.iconPath = new vscode4.ThemeIcon("symbol-structure");
          group.command = { command: "renovatio.openDomainModel", title: "Open Domain Model" };
          group.children = shapedRecords.slice(0, 12).map((record) => {
            const item2 = new vscode4.TreeItem(record.name || record.id, vscode4.TreeItemCollapsibleState.None);
            item2.description = `${asArray(record.properties).length} fields`;
            item2.tooltip = `${record.name || record.id}
${asArray(record.properties).map((property) => property.name).slice(0, 12).join(", ")}`;
            item2.iconPath = new vscode4.ThemeIcon("symbol-field");
            item2.command = { command: "renovatio.openDomainModel", title: "Open Domain Model" };
            return item2;
          });
          result.push(group);
        }
        if (entries.length) {
          const group = new vscode4.TreeItem(`Parsed inventory (${parsedFileCount(inventory)})`, vscode4.TreeItemCollapsibleState.Collapsed);
          group.iconPath = new vscode4.ThemeIcon("list-tree");
          group.children = entries.map(([key, value2]) => {
            const item2 = new vscode4.TreeItem(`${humanize(key)}: ${value2}`, vscode4.TreeItemCollapsibleState.None);
            item2.iconPath = new vscode4.ThemeIcon(key.toLowerCase().includes("jcl") ? "terminal" : "symbol-file");
            return item2;
          });
          result.push(group);
        }
        if (!result.length) {
          const item2 = new vscode4.TreeItem("No analysis loaded", vscode4.TreeItemCollapsibleState.None);
          item2.iconPath = new vscode4.ThemeIcon("warning");
          item2.command = { command: "renovatio.analyzeCobolSources", title: "Analyze COBOL Sources" };
          return [item2];
        }
        return result;
      }
    };
    var DomainModelProvider = class {
      constructor() {
        this._onDidChangeTreeData = new vscode4.EventEmitter();
        this.onDidChangeTreeData = this._onDidChangeTreeData.event;
      }
      refresh() {
        this._onDidChangeTreeData.fire();
      }
      getTreeItem(item) {
        return item;
      }
      getChildren(item) {
        if (item?.children) return item.children;
        const project = state.projects.find((candidate) => candidate.id === state.activeProjectId);
        if (!project) {
          const select = new vscode4.TreeItem("Select Renovatio project", vscode4.TreeItemCollapsibleState.None);
          select.iconPath = new vscode4.ThemeIcon("folder-active");
          select.command = { command: "renovatio.selectProject", title: "Select Renovatio Project" };
          return [select];
        }
        const domain = state.domainModel;
        if (!domain) {
          const refreshItem = new vscode4.TreeItem("Domain model not loaded", vscode4.TreeItemCollapsibleState.None);
          refreshItem.iconPath = new vscode4.ThemeIcon("refresh");
          refreshItem.command = { command: "renovatio.refresh", title: "Refresh Renovatio" };
          return [refreshItem];
        }
        const model = domain.model || {};
        const nodes = asArray(model.nodes);
        const relations = asArray(model.relations);
        const invariants = asArray(model.invariants);
        const suggestions = asArray(domain.suggestions);
        if (!nodes.length && !relations.length && !invariants.length) {
          const analyze = new vscode4.TreeItem("No DomainModel persisted yet", vscode4.TreeItemCollapsibleState.None);
          analyze.description = "run discovery";
          analyze.iconPath = new vscode4.ThemeIcon("symbol-structure");
          analyze.command = { command: "renovatio.analyzeCobolSources", title: "Analyze COBOL Sources" };
          return [analyze];
        }
        const root = new vscode4.TreeItem(`Revision ${domain.revision || 0}`, vscode4.TreeItemCollapsibleState.None);
        root.description = `${nodes.length} nodes \xB7 ${relations.length} relations`;
        root.iconPath = new vscode4.ThemeIcon("repo");
        root.command = { command: "renovatio.openDomainModel", title: "Open Domain Model" };
        const nodeGroup = new vscode4.TreeItem(`Nodes (${nodes.length})`, vscode4.TreeItemCollapsibleState.Expanded);
        nodeGroup.iconPath = new vscode4.ThemeIcon("symbol-class");
        nodeGroup.children = nodes.map((node) => {
          const child = new vscode4.TreeItem(node.name || node.id, vscode4.TreeItemCollapsibleState.None);
          child.description = node.kind || "";
          child.tooltip = `${node.id}
${Math.round(Number(node.confidence || 0) * 100)}% confidence`;
          child.iconPath = new vscode4.ThemeIcon(domainIconForKind(node.kind));
          child.command = { command: "renovatio.openDomainModel", title: "Open Domain Model" };
          return child;
        });
        const relationGroup = new vscode4.TreeItem(`Relations (${relations.length})`, vscode4.TreeItemCollapsibleState.Collapsed);
        relationGroup.iconPath = new vscode4.ThemeIcon("references");
        relationGroup.children = relations.map((relation) => {
          const child = new vscode4.TreeItem(relation.kind || "relation", vscode4.TreeItemCollapsibleState.None);
          child.description = `${relation.fromId} -> ${relation.toId}`;
          child.iconPath = new vscode4.ThemeIcon("arrow-right");
          child.command = { command: "renovatio.openDomainModel", title: "Open Domain Model" };
          return child;
        });
        const invariantGroup = new vscode4.TreeItem(`Invariants (${invariants.length})`, vscode4.TreeItemCollapsibleState.Collapsed);
        invariantGroup.iconPath = new vscode4.ThemeIcon("shield");
        invariantGroup.children = invariants.map((invariant) => {
          const child = new vscode4.TreeItem(invariant.expression || invariant.id, vscode4.TreeItemCollapsibleState.None);
          child.description = invariant.subjectId || "";
          child.iconPath = new vscode4.ThemeIcon("symbol-boolean");
          child.command = { command: "renovatio.openDomainModel", title: "Open Domain Model" };
          return child;
        });
        const result = [root, nodeGroup, relationGroup, invariantGroup];
        if (suggestions.length) {
          const suggestionGroup = new vscode4.TreeItem(`Suggestions (${suggestions.length})`, vscode4.TreeItemCollapsibleState.Collapsed);
          suggestionGroup.iconPath = new vscode4.ThemeIcon("sparkle");
          suggestionGroup.children = suggestions.map((suggestion) => {
            const child = new vscode4.TreeItem(suggestion.name || suggestion.targetId, vscode4.TreeItemCollapsibleState.None);
            child.description = suggestion.status || "pending";
            child.iconPath = new vscode4.ThemeIcon("lightbulb");
            child.command = { command: "renovatio.openDomainModel", title: "Open Domain Model" };
            return child;
          });
          result.push(suggestionGroup);
        }
        return result;
      }
    };
    function domainIconForKind(kind) {
      const normalized = String(kind || "").toUpperCase();
      if (normalized.includes("USE_CASE")) return "run";
      if (normalized.includes("REPOSITORY")) return "database";
      if (normalized.includes("EXTERNAL")) return "plug";
      if (normalized.includes("VALUE")) return "symbol-value";
      if (normalized.includes("SERVICE")) return "gear";
      return "symbol-class";
    }
    function showError(prefix, error) {
      const text = `${prefix}: ${message3(error)}`;
      output.appendLine(text);
      vscode4.window.showErrorMessage(text);
    }
    function message3(error) {
      return error instanceof Error ? error.message : String(error);
    }
    function escapeHtml(value2) {
      return String(value2).replace(/[&<>"']/g, (char) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
      })[char]);
    }
    module2.exports = { activate: activate3, deactivate: deactivate2 };
  }
});

// src/extension.ts
var extension_exports = {};
__export(extension_exports, {
  activate: () => activate2,
  deactivate: () => deactivate
});
module.exports = __toCommonJS(extension_exports);
var vscode3 = __toESM(require("vscode"));

// src/model.ts
var LAYER_NODE_PREFIX = "architecture-layer:";
var MVC_LAYERS = ["controller", "service", "model", "persistence"];
var HEXAGONAL_LAYERS = ["inbound-adapter", "inbound-port", "application", "domain", "outbound-port", "outbound-adapter"];
var PACKAGE_WIDTH = 300;
var PACKAGE_HEADER_HEIGHT = 48;
var PACKAGE_PADDING = 18;
var COMPONENT_WIDTH = PACKAGE_WIDTH - PACKAGE_PADDING * 2;
var COMPONENT_HEIGHT = 104;
var COMPONENT_GAP = 14;
var PACKAGE_GAP = 48;
function parseDiagramDocument(text, fileName) {
  const raw = parseJsonObject(text);
  const kind = detectKind(raw, fileName);
  return { kind, raw, model: toDiagramModel(kind, raw) };
}
function applyDiagramEvent(parsed, event) {
  const raw = structuredClone(parsed.raw);
  if (event.type === "nodeMoved") {
    applyNodeMoved(parsed.kind, raw, event.id, event.x, event.y);
  } else if (event.type === "layoutChanged") {
    applyLayoutChanged(parsed.kind, raw, event.positions);
  } else if (event.type === "nodesPruned") {
    applyNodesPruned(parsed.kind, raw, event.ids);
  } else if (event.type === "edgeCreated") {
    applyEdgeCreated(parsed.kind, raw, event.source, event.target);
  } else if (event.type === "edgeReconnected") {
    applyEdgeReconnected(parsed.kind, raw, event.id, event.source, event.target);
  } else if (event.type === "edgeLabelChanged") {
    applyEdgeLabelChanged(parsed.kind, raw, event.id, event.label);
  } else if (event.type === "edgesDeleted") {
    applyEdgesDeleted(parsed.kind, raw, event.ids);
  } else if (event.type === "architectureStyleChanged") {
    applyArchitectureStyleChanged(parsed.kind, raw, event.style);
  }
  return { kind: parsed.kind, raw, model: toDiagramModel(parsed.kind, raw) };
}
function formatDiagramDocument(raw) {
  return `${JSON.stringify(raw, null, 2)}
`;
}
function parseJsonObject(text) {
  if (!text.trim()) {
    return {};
  }
  const parsed = JSON.parse(text);
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    throw new Error("Renovatio diagram files must contain a JSON object.");
  }
  return parsed;
}
function detectKind(raw, fileName) {
  if (raw.diagramKind === "persistence" || raw.projection === "persistence" || fileName.includes("persistence")) {
    return "persistence";
  }
  if (fileName.endsWith(".renovatio-domain.json")) {
    return "domain";
  }
  if (fileName.endsWith(".renovatio-arch.json")) {
    return "architecture";
  }
  if (Array.isArray(raw.nodes) && Array.isArray(raw.edges)) {
    return "diagram";
  }
  if (Array.isArray(raw.nodes) && Array.isArray(raw.relations)) {
    return "domain";
  }
  if (Array.isArray(raw.canvas) || raw.profile?.dependencyRules) {
    return "architecture";
  }
  return "diagram";
}
function toDiagramModel(kind, raw) {
  if (kind === "domain" || kind === "persistence") {
    return domainToDiagram(raw);
  }
  if (kind === "architecture") {
    return architectureToDiagram(raw);
  }
  return plainDiagram(raw);
}
function normalizeConceptName(value2) {
  return String(value2 ?? "").trim().toUpperCase().replace(/[^A-Z0-9]/g, "");
}
function domainDisplayName(value2, kind) {
  const raw = String(value2 ?? "").trim();
  const normalizedKind = String(kind ?? "").toUpperCase();
  if (!raw || normalizedKind === "USE_CASE" || normalizedKind === "DOMAIN_SERVICE" || normalizedKind === "SERVICE") {
    return raw;
  }
  const cleaned = raw.replace(/^DB2\s+table\s+/i, "").replace(/^file\s+record\s+/i, "").replace(/^(FD|WS|LS)-/i, "").replace(/-(FD|WS|LS)$/i, "").replace(/-(FILE|RECORD|REC|TABLE|DATA|IN|OUT|INPUT|OUTPUT)$/ig, "").replace(/^(FILE|RECORD|REC|TABLE|DATA|IN|OUT|INPUT|OUTPUT)-/ig, "").replace(/-{2,}/g, "-").replace(/^-|-$/g, "").trim();
  return titleCaseDomainName(cleaned || raw);
}
function titleCaseDomainName(value2) {
  return value2.split(/[-_\s]+/).filter(Boolean).map((token) => {
    const lower = token.toLowerCase();
    if (/^[A-Z0-9]{2,}$/.test(token) && token.length <= 4) return token;
    return lower.charAt(0).toUpperCase() + lower.slice(1);
  }).join(" ");
}
function mergeNodeProperties(props) {
  const seen = /* @__PURE__ */ new Map();
  for (const p of props) {
    if (!p || !p.name) continue;
    const key = String(p.name).trim().toUpperCase();
    if (!seen.has(key)) {
      seen.set(key, { ...p });
    } else {
      const existing = seen.get(key);
      if (p.isKey) existing.isKey = true;
      if (p.isForeignKey) existing.isForeignKey = true;
      if (!existing.type && p.type) existing.type = p.type;
    }
  }
  return Array.from(seen.values());
}
function domainToDiagram(raw) {
  const excluded = new Map((raw.excludedNodeIds ?? []).map((value2) => [String(value2.id), value2.reason]));
  const aliasMap = /* @__PURE__ */ new Map();
  const bySemanticKey = /* @__PURE__ */ new Map();
  const canonicalRawNodes = [];
  for (const rawNode of safeArray(raw.nodes)) {
    if (!rawNode?.id) continue;
    const kind = String(rawNode.kind ?? "ENTITY").toUpperCase();
    if (kind === "AGGREGATE" || kind === "EXTERNAL_SYSTEM") continue;
    const name = String(rawNode.name ?? rawNode.label ?? rawNode.id).trim();
    const isSharedConcept = kind === "REPOSITORY" || kind === "ENTITY" || kind === "VALUE_OBJECT";
    const semanticKey = isSharedConcept ? `${kind}:${normalizeConceptName(name)}` : `id:${rawNode.id}`;
    const existing = bySemanticKey.get(semanticKey);
    if (!existing) {
      const copy = { ...rawNode, name, properties: safeArray(rawNode.properties) };
      bySemanticKey.set(semanticKey, copy);
      aliasMap.set(String(rawNode.id), String(copy.id));
      canonicalRawNodes.push(copy);
    } else {
      aliasMap.set(String(rawNode.id), String(existing.id));
      existing.properties = mergeNodeProperties([...existing.properties, ...safeArray(rawNode.properties)]);
      if (rawNode.tableName && !existing.tableName) existing.tableName = rawNode.tableName;
      if (rawNode.sourceDataset && !existing.sourceDataset) existing.sourceDataset = rawNode.sourceDataset;
      if (typeof rawNode.confidence === "number") {
        existing.confidence = Math.max(Number(existing.confidence ?? 0), rawNode.confidence);
      }
    }
  }
  const foreignKeys = /* @__PURE__ */ new Map();
  safeArray(raw.relations).forEach((relation) => {
    const property = relation.foreignKey?.property;
    const fromId = aliasMap.get(String(relation.fromId)) ?? String(relation.fromId);
    if (!property || !fromId) return;
    const set = foreignKeys.get(fromId) ?? /* @__PURE__ */ new Set();
    set.add(String(property));
    foreignKeys.set(fromId, set);
  });
  const nodes = canonicalRawNodes.map((node, index) => {
    const id = String(node.id ?? `domain-node:${index}`);
    const nodeForeignKeys = foreignKeys.get(id) ?? /* @__PURE__ */ new Set();
    return {
      id,
      kind: String(node.kind ?? "ENTITY"),
      label: domainDisplayName(node.name ?? node.label ?? id, node.kind),
      group: String(node.kind ?? "ENTITY"),
      x: numberOrUndefined(raw.layout?.[id]?.x ?? node.x),
      y: numberOrUndefined(raw.layout?.[id]?.y ?? node.y),
      data: {
        diagramKind: raw.diagramKind ?? raw.projection,
        properties: safeArray(node.properties).map((property) => ({
          ...property,
          isForeignKey: nodeForeignKeys.has(String(property.name))
        })),
        tableName: node.tableName,
        sourceDataset: node.sourceDataset,
        confidence: node.confidence,
        origin: node.origin,
        physicalName: node.physicalName ?? node.name,
        excluded: excluded.has(id),
        exclusionReason: excluded.get(id) ?? ""
      }
    };
  });
  const seenEdges = /* @__PURE__ */ new Set();
  const edges = [];
  safeArray(raw.relations).forEach((relation, index) => {
    const source = aliasMap.get(String(relation.fromId)) ?? String(relation.fromId);
    const target = aliasMap.get(String(relation.toId)) ?? String(relation.toId);
    if (!source || !target || source === target) return;
    const edgeKey = `${source}->${target}:${relation.kind ?? ""}`;
    if (seenEdges.has(edgeKey)) return;
    seenEdges.add(edgeKey);
    edges.push({
      id: String(relation.id ?? `domain-relation:${index}`),
      source,
      target,
      kind: String(relation.kind ?? "ASSOCIATES_WITH"),
      label: relation.label ? String(relation.label) : relation.foreignKey?.property ? String(relation.foreignKey.property) : void 0,
      data: {
        associationLabel: relation.foreignKey ? `${relation.foreignKey.property} -> ${relation.foreignKey.referencesProperty}` : relation.kind,
        sourceCardinality: relation.sourceCardinality,
        targetCardinality: relation.targetCardinality,
        foreignKey: relation.foreignKey
      }
    });
  });
  return { nodes, edges };
}
function architectureToDiagram(raw) {
  const profile = raw.profile ?? {};
  const style = architectureStyle(profile.style ?? raw.style);
  const canvas = safeArray(raw.canvas);
  const rules = safeArray(raw.dependencyRules ?? profile.dependencyRules);
  const diagnostics = safeArray(raw.dependencyDiagnostics);
  const layers = collectLayers(canvas, rules, profile, style);
  const layerSlots = /* @__PURE__ */ new Map();
  const excluded = new Map(safeArray(profile.excludedNodeIds ?? raw.excludedNodeIds).map((value2) => [String(value2.id), value2.reason]));
  const PACKAGE_COLUMNS = 3;
  const columnCursorY = new Array(PACKAGE_COLUMNS).fill(24);
  const layerNodes = layers.map((layer, index) => {
    const id = layerNodeId(layer);
    const count = canvas.filter((node2) => architectureVisualLayer(node2, style) === layer).length;
    const height = PACKAGE_HEADER_HEIGHT + PACKAGE_PADDING + Math.max(count, 1) * COMPONENT_HEIGHT + Math.max(count - 1, 0) * COMPONENT_GAP + PACKAGE_PADDING;
    const column = index % PACKAGE_COLUMNS;
    const defaultX = 24 + column * (PACKAGE_WIDTH + PACKAGE_GAP);
    const defaultY = columnCursorY[column];
    const node = {
      id,
      kind: "ARCHITECTURE_LAYER",
      label: layer,
      group: layer,
      x: numberOrUndefined(profile.layout?.[id]?.x ?? raw.layout?.[id]?.x) ?? defaultX,
      y: numberOrUndefined(profile.layout?.[id]?.y ?? raw.layout?.[id]?.y) ?? defaultY,
      width: PACKAGE_WIDTH,
      height,
      data: {
        architectureStyle: style,
        layer,
        layerRole: architectureLayerRole(layer, style),
        packageName: profile.packageRoots?.[layer] ?? profile.packageRoots?.base ?? "",
        className: profile.suffixes?.[layer] ?? "Layer",
        componentCount: count,
        diagnostics: diagnostics.filter((diagnostic2) => diagnostic2.fromLayer === layer || diagnostic2.toLayer === layer)
      }
    };
    columnCursorY[column] += height + PACKAGE_GAP;
    return node;
  });
  const componentNodes = canvas.map((node, index) => {
    const id = String(node.id ?? `architecture-node:${index}`);
    const layer = architectureVisualLayer(node, style);
    const slot = layerSlots.get(layer) ?? 0;
    layerSlots.set(layer, slot + 1);
    const stored = {
      x: numberOrUndefined(profile.layout?.[id]?.x ?? raw.layout?.[id]?.x),
      y: numberOrUndefined(profile.layout?.[id]?.y ?? raw.layout?.[id]?.y)
    };
    const storedLooksRelative = stored.x !== void 0 && stored.y !== void 0 && stored.x >= 0 && stored.x <= PACKAGE_WIDTH - COMPONENT_WIDTH && stored.y >= PACKAGE_HEADER_HEIGHT && stored.y <= 2e3;
    return {
      id,
      kind: String(node.kind ?? "COMPONENT"),
      label: String(node.label ?? node.className ?? id),
      group: layer,
      parentId: layerNodeId(layer),
      width: COMPONENT_WIDTH,
      height: COMPONENT_HEIGHT,
      x: storedLooksRelative ? stored.x : PACKAGE_PADDING,
      y: storedLooksRelative ? stored.y : PACKAGE_HEADER_HEIGHT + PACKAGE_PADDING + slot * (COMPONENT_HEIGHT + COMPONENT_GAP),
      data: {
        layer,
        packageName: node.packageName,
        className: node.className,
        componentId: node.componentId,
        architectureStyle: style,
        layerRole: architectureLayerRole(layer, style),
        excluded: excluded.has(id),
        exclusionReason: excluded.get(id) ?? "",
        diagnostics: diagnostics.filter((diagnostic2) => diagnostic2.fromLayer === layer || diagnostic2.toLayer === layer)
      }
    };
  });
  const edges = rules.map((rule, index) => ({
    id: `architecture-rule:${rule.fromLayer}:${rule.toLayer}:${index}`,
    source: layerNodeId(String(rule.fromLayer)),
    target: layerNodeId(String(rule.toLayer)),
    kind: rule.allowed ? "ALLOWED_DEPENDENCY" : "DENIED_DEPENDENCY",
    label: String(rule.reason ?? ""),
    data: {
      allowed: Boolean(rule.allowed),
      fromLayer: rule.fromLayer,
      toLayer: rule.toLayer
    }
  })).filter((edge) => edge.source && edge.target);
  return { nodes: [...layerNodes, ...componentNodes], edges };
}
function plainDiagram(raw) {
  return {
    nodes: safeArray(raw.nodes).map((node, index) => ({
      id: String(node.id ?? `node:${index}`),
      kind: String(node.kind ?? "NODE"),
      label: String(node.label ?? node.id ?? `Node ${index + 1}`),
      group: node.group,
      x: numberOrUndefined(node.x),
      y: numberOrUndefined(node.y),
      data: node.data
    })),
    edges: safeArray(raw.edges).map((edge, index) => ({
      id: String(edge.id ?? `edge:${index}`),
      source: String(edge.source),
      target: String(edge.target),
      kind: String(edge.kind ?? "EDGE"),
      label: edge.label,
      data: edge.data
    })).filter((edge) => edge.source && edge.target)
  };
}
function applyNodeMoved(kind, raw, id, x, y) {
  if (id.startsWith(LAYER_NODE_PREFIX) && kind !== "architecture") {
    return;
  }
  if (kind === "domain" || kind === "persistence") {
    raw.layout = raw.layout ?? {};
    raw.layout[id] = { x, y };
    return;
  }
  if (kind === "architecture") {
    raw.profile = raw.profile ?? {};
    raw.profile.layout = raw.profile.layout ?? {};
    raw.profile.layout[id] = { x, y };
    return;
  }
  const node = safeArray(raw.nodes).find((candidate) => candidate.id === id);
  if (node) {
    node.x = x;
    node.y = y;
  }
}
function applyLayoutChanged(kind, raw, positions) {
  for (const [id, position] of Object.entries(positions ?? {})) {
    const x = numberOrUndefined(position?.x);
    const y = numberOrUndefined(position?.y);
    if (x === void 0 || y === void 0) {
      continue;
    }
    applyNodeMoved(kind, raw, id, x, y);
  }
}
function applyNodesPruned(kind, raw, ids) {
  const prunableIds = ids.filter((id) => !id.startsWith(LAYER_NODE_PREFIX));
  if (!prunableIds.length) {
    return;
  }
  const target = kind === "architecture" ? raw.profile = raw.profile ?? {} : raw;
  target.excludedNodeIds = safeArray(target.excludedNodeIds);
  const existing = new Set(target.excludedNodeIds.map((value2) => String(value2.id)));
  for (const id of prunableIds) {
    if (!existing.has(id)) {
      target.excludedNodeIds.push({ id, reason: "Excluded from VS Code custom editor" });
    }
  }
}
function applyEdgeCreated(kind, raw, source, target) {
  if (kind !== "domain" && kind !== "persistence") {
    return;
  }
  raw.relations = safeArray(raw.relations);
  raw.relations.push({
    id: `relation:${source}:${target}:${Date.now()}`,
    fromId: source,
    toId: target,
    kind: "ASSOCIATES_WITH",
    sourceCardinality: "ONE",
    targetCardinality: "ONE"
  });
}
function applyEdgeReconnected(kind, raw, id, source, target) {
  if (kind !== "domain" && kind !== "persistence") {
    return;
  }
  const relation = safeArray(raw.relations).find((value2) => String(value2.id) === id);
  if (!relation) {
    return;
  }
  relation.fromId = source;
  relation.toId = target;
}
function applyEdgeLabelChanged(kind, raw, id, label) {
  if (kind !== "domain" && kind !== "persistence") {
    return;
  }
  const relation = safeArray(raw.relations).find((value2) => String(value2.id) === id);
  if (!relation) {
    return;
  }
  relation.label = label;
}
function applyEdgesDeleted(kind, raw, ids) {
  if (kind !== "domain" && kind !== "persistence") {
    return;
  }
  const idSet = new Set(ids);
  raw.relations = safeArray(raw.relations).filter((relation) => !idSet.has(String(relation.id)));
}
function applyArchitectureStyleChanged(kind, raw, style) {
  if (kind !== "architecture") {
    return;
  }
  const next = architectureStyle(style);
  raw.profile = raw.profile ?? {};
  const basePackage = inferBasePackage(raw.profile.packageRoots);
  raw.profile.style = next;
  raw.profile.packageRoots = architecturePackageRoots(next, basePackage);
  raw.profile.suffixes = architectureSuffixes(next);
  raw.profile.dependencyRules = architectureDependencyRules(next);
  delete raw.profile.layout;
}
function collectLayers(canvas, rules, profile, style) {
  const preferred = architectureLayerOrder(style);
  const discovered = Array.from(new Set([
    ...canvas.map((node) => architectureVisualLayer(node, style)),
    ...rules.flatMap((rule) => [rule.fromLayer, rule.toLayer]),
    ...Object.keys(profile.packageRoots ?? {}),
    ...Object.keys(profile.suffixes ?? {}),
    ...preferred
  ].filter(Boolean).map(String)));
  return [
    ...preferred.filter((layer) => discovered.includes(layer)),
    ...discovered.filter((layer) => !preferred.includes(layer))
  ];
}
function layerNodeId(layer) {
  return `${LAYER_NODE_PREFIX}${layer}`;
}
function safeArray(value2) {
  return Array.isArray(value2) ? value2 : [];
}
function numberOrUndefined(value2) {
  return typeof value2 === "number" && Number.isFinite(value2) ? value2 : void 0;
}
function architectureStyle(value2) {
  return value2 === "HEXAGONAL" ? "HEXAGONAL" : "LAYERED_MVC";
}
function architectureLayerOrder(style) {
  return style === "HEXAGONAL" ? HEXAGONAL_LAYERS : MVC_LAYERS;
}
function architectureLayerRole(layer, style) {
  if (style === "HEXAGONAL") {
    const roles2 = {
      "inbound-adapter": "Driving adapter",
      "inbound-port": "Inbound port",
      application: "Use case / application service",
      domain: "Domain model",
      "outbound-port": "Outbound port",
      "outbound-adapter": "Driven adapter"
    };
    return roles2[layer] ?? layer;
  }
  const roles = {
    controller: "Controller layer",
    service: "Service layer",
    model: "Model layer",
    persistence: "Persistence layer"
  };
  return roles[layer] ?? layer;
}
function inferBasePackage(packageRoots) {
  const roots = Object.values(packageRoots ?? {}).filter(Boolean).map(String);
  const first = roots[0] ?? "com.example.modernized";
  return first.replace(/\.(adapter\.(in|out\.persistence)|port\.(in|out)|application|domain|controller|service|model|persistence)$/, "").replace(/\.(adapter|out)$/, "");
}
function architecturePackageRoots(style, basePackage) {
  if (style === "HEXAGONAL") {
    return {
      "inbound-adapter": `${basePackage}.adapter.in`,
      "inbound-port": `${basePackage}.port.in`,
      application: `${basePackage}.application`,
      domain: `${basePackage}.domain`,
      "outbound-port": `${basePackage}.port.out`,
      "outbound-adapter": `${basePackage}.adapter.out.persistence`
    };
  }
  return {
    controller: `${basePackage}.controller`,
    service: `${basePackage}.service`,
    model: `${basePackage}.model`,
    persistence: `${basePackage}.persistence`
  };
}
function architectureSuffixes(style) {
  return style === "HEXAGONAL" ? {
    "inbound-adapter": "Controller",
    "inbound-port": "Port",
    application: "UseCase",
    domain: "",
    "outbound-port": "Port",
    "outbound-adapter": "Adapter"
  } : { controller: "Controller", service: "Service", model: "", persistence: "Repository" };
}
function architectureDependencyRules(style) {
  if (style === "HEXAGONAL") {
    return [
      { fromLayer: "inbound-adapter", toLayer: "inbound-port", allowed: true, reason: "Driving adapters call inbound ports." },
      { fromLayer: "inbound-port", toLayer: "application", allowed: true, reason: "Inbound ports expose application use cases." },
      { fromLayer: "application", toLayer: "domain", allowed: true, reason: "Use cases coordinate domain behavior." },
      { fromLayer: "application", toLayer: "outbound-port", allowed: true, reason: "Application core depends on outbound ports." },
      { fromLayer: "outbound-adapter", toLayer: "outbound-port", allowed: true, reason: "Driven adapters implement outbound ports." },
      { fromLayer: "domain", toLayer: "inbound-adapter", allowed: false, reason: "Domain must not depend on driving adapters." },
      { fromLayer: "domain", toLayer: "outbound-adapter", allowed: false, reason: "Domain must not depend on driven adapters." },
      { fromLayer: "application", toLayer: "outbound-adapter", allowed: false, reason: "Application core talks to ports, not adapter implementations." }
    ];
  }
  return [
    { fromLayer: "controller", toLayer: "service", allowed: true, reason: "Controllers call services." },
    { fromLayer: "service", toLayer: "model", allowed: true, reason: "Services own model orchestration." },
    { fromLayer: "service", toLayer: "persistence", allowed: true, reason: "Services call persistence repositories." },
    { fromLayer: "model", toLayer: "controller", allowed: false, reason: "Model must not depend on controllers." },
    { fromLayer: "persistence", toLayer: "controller", allowed: false, reason: "Persistence must not depend on controllers." }
  ];
}
function architectureVisualLayer(node, style) {
  const rawLayer = String(node?.layer ?? "").toLowerCase();
  if (style !== "HEXAGONAL") {
    if (rawLayer === "application") return "service";
    if (rawLayer === "domain") return "model";
    if (rawLayer === "outbound-adapter") return "persistence";
    if (rawLayer === "inbound-adapter") return "controller";
    return rawLayer || "model";
  }
  if (HEXAGONAL_LAYERS.includes(rawLayer)) {
    return rawLayer;
  }
  const kind = String(node?.kind ?? "").toUpperCase();
  const text = `${node?.label ?? ""} ${node?.className ?? ""} ${node?.packageName ?? ""} ${node?.componentId ?? ""}`.toLowerCase();
  if (kind === "INBOUND_PORT" || text.includes("inbound port") || text.includes(".port.in")) return "inbound-port";
  if (kind === "OUTBOUND_PORT" || text.includes("outbound port") || text.includes(".port.out")) return "outbound-port";
  if (kind === "USE_CASE" || kind === "SERVICE" || rawLayer === "service" || rawLayer === "application") return "application";
  if (kind === "ENTITY" || kind === "VALUE" || kind === "VALUE_OBJECT" || rawLayer === "model" || rawLayer === "domain") return "domain";
  if (kind === "ADAPTER" || kind === "COMPONENT") {
    if (rawLayer === "controller" || text.includes("controller") || text.includes("inbound") || text.includes(".adapter.in")) return "inbound-adapter";
    return "outbound-adapter";
  }
  if (rawLayer === "persistence" || text.includes("repository") || text.includes("persistence")) return "outbound-adapter";
  return "domain";
}

// src/extension.ts
var legacyExtension = __toESM(require_legacyExtension());

// src/workspaceManifest.ts
var vscode = __toESM(require("vscode"));
var WORKSPACE_MANIFEST_RELATIVE_PATH = ".renovatio/workspace.renovatio.json";
var DEFAULT_INCLUDE = ["**/*.cbl", "**/*.cob", "**/*.cpy", "**/*.jcl"];
var DEFAULT_EXCLUDE = ["**/target/**", "**/.git/**"];
var RenovatioWorkspaceManifestService = class {
  constructor(output) {
    this.output = output;
    this.disposables.push(
      this.diagnostics,
      vscode.workspace.onDidOpenTextDocument((document) => this.validateDocumentIfManifest(document)),
      vscode.workspace.onDidSaveTextDocument((document) => this.validateDocumentIfManifest(document)),
      vscode.workspace.onDidChangeWorkspaceFolders(() => void this.validateWorkspace())
    );
  }
  output;
  diagnostics = vscode.languages.createDiagnosticCollection("renovatio-workspace");
  disposables = [];
  dispose() {
    this.disposables.forEach((disposable) => disposable.dispose());
  }
  async initializeWorkspace() {
    const folder = await this.pickWorkspaceFolder();
    if (!folder) return;
    const manifestUri = this.manifestUri(folder);
    if (await exists(manifestUri)) {
      const overwrite = await vscode.window.showWarningMessage(
        "Renovatio workspace manifest already exists. Overwrite it?",
        { modal: true },
        "Overwrite"
      );
      if (overwrite !== "Overwrite") return;
    }
    const defaults = this.defaultsFromSettings(folder);
    const projectId = await vscode.window.showInputBox({
      title: "Renovatio: Initialize Workspace",
      prompt: "Project id used for local Renovatio artifacts",
      value: defaults.projectId,
      validateInput: (value2) => /^[a-zA-Z0-9._-]+$/.test(value2.trim()) ? void 0 : "Use letters, numbers, dot, underscore or dash."
    });
    if (!projectId) return;
    const manifest = this.createDefaultManifest(folder, projectId.trim(), defaults);
    await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(folder.uri, ".renovatio"));
    await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(folder.uri, ".renovatio", "diagrams"));
    await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(folder.uri, ".renovatio", "evidence"));
    await vscode.workspace.fs.writeFile(manifestUri, encodeJson(manifest));
    this.output.appendLine(`Created Renovatio workspace manifest: ${manifestUri.fsPath}`);
    await this.openWorkspaceManifest(folder);
    await this.validateWorkspace(folder);
  }
  async openWorkspaceManifest(folder) {
    const targetFolder = folder ?? await this.pickWorkspaceFolder();
    if (!targetFolder) return;
    const uri = this.manifestUri(targetFolder);
    if (!await exists(uri)) {
      const action2 = await vscode.window.showInformationMessage(
        "No Renovatio workspace manifest exists for this workspace.",
        "Initialize Workspace"
      );
      if (action2 === "Initialize Workspace") {
        await this.initializeWorkspace();
      }
      return;
    }
    const document = await vscode.workspace.openTextDocument(uri);
    await vscode.window.showTextDocument(document, { preview: false });
  }
  async validateWorkspace(folder) {
    const folders = folder ? [folder] : vscode.workspace.workspaceFolders ?? [];
    if (!folders.length) {
      vscode.window.showWarningMessage("Open a VS Code workspace before validating Renovatio artifacts.");
      return false;
    }
    let valid = true;
    for (const workspaceFolder of folders) {
      const manifestUri = this.manifestUri(workspaceFolder);
      if (!await exists(manifestUri)) {
        this.diagnostics.delete(manifestUri);
        continue;
      }
      const text = decodeBytes(await vscode.workspace.fs.readFile(manifestUri));
      const diagnostics = this.validateManifestText(text);
      this.diagnostics.set(manifestUri, diagnostics);
      valid = valid && diagnostics.length === 0;
    }
    if (valid) {
      vscode.window.showInformationMessage("Renovatio workspace validation passed.");
    } else {
      vscode.window.showWarningMessage("Renovatio workspace validation found issues. See Problems.");
    }
    return valid;
  }
  async formatArtifacts() {
    const active = vscode.window.activeTextEditor?.document;
    if (active && isRenovatioJson(active.uri)) {
      await this.formatDocument(active);
      return;
    }
    const files = await vscode.workspace.findFiles("**/.renovatio/**/*.json", "**/{node_modules,target,.git}/**");
    if (!files.length) {
      vscode.window.showInformationMessage("No Renovatio JSON artifacts found to format.");
      return;
    }
    let formatted = 0;
    for (const uri of files) {
      const document = await vscode.workspace.openTextDocument(uri);
      if (await this.formatDocument(document, { silent: true })) formatted += 1;
    }
    vscode.window.showInformationMessage(`Formatted ${formatted} Renovatio artifact(s).`);
  }
  async load(folder) {
    const targetFolder = folder ?? vscode.workspace.workspaceFolders?.[0];
    if (!targetFolder) return void 0;
    const uri = this.manifestUri(targetFolder);
    if (!await exists(uri)) return void 0;
    const text = decodeBytes(await vscode.workspace.fs.readFile(uri));
    return parseJsonc(text);
  }
  async update(mutator, folder) {
    const targetFolder = folder ?? vscode.workspace.workspaceFolders?.[0];
    if (!targetFolder) return void 0;
    const uri = this.manifestUri(targetFolder);
    if (!await exists(uri)) {
      await this.initializeWorkspace();
      if (!await exists(uri)) return void 0;
    }
    const manifest = await this.load(targetFolder);
    if (!manifest) return void 0;
    mutator(manifest);
    await vscode.workspace.fs.writeFile(uri, encodeJson(manifest));
    await this.validateWorkspace(targetFolder);
    return manifest;
  }
  async formatDocument(document, options = {}) {
    try {
      const parsed = parseJsonc(document.getText());
      const formatted = `${JSON.stringify(parsed, null, 2)}
`;
      if (formatted === document.getText()) return true;
      const edit = new vscode.WorkspaceEdit();
      const end = document.lineCount === 0 ? new vscode.Position(0, 0) : document.lineAt(document.lineCount - 1).rangeIncludingLineBreak.end;
      edit.replace(document.uri, new vscode.Range(new vscode.Position(0, 0), end), formatted);
      const applied = await vscode.workspace.applyEdit(edit);
      if (!applied) throw new Error("VS Code rejected the format edit.");
      await document.save();
      return true;
    } catch (error) {
      if (!options.silent) {
        vscode.window.showErrorMessage(`Could not format Renovatio artifact: ${message(error)}`);
      }
      return false;
    }
  }
  validateDocumentIfManifest(document) {
    if (!document.uri.fsPath.endsWith(WORKSPACE_MANIFEST_RELATIVE_PATH)) return;
    this.diagnostics.set(document.uri, this.validateManifestText(document.getText()));
  }
  validateManifestText(text) {
    let parsed;
    try {
      parsed = parseJsonc(text);
    } catch (error) {
      return [diagnostic(`Invalid JSON: ${message(error)}`)];
    }
    return validateManifest(parsed).map(diagnostic);
  }
  createDefaultManifest(folder, projectId, defaults) {
    const targetLanguage = defaults.targetLanguage;
    const targetRoot = defaults.generatedRoots[0] ?? `generated/${targetLanguage}`;
    return {
      version: "1",
      projectId,
      source: {
        language: "cobol",
        roots: defaults.cobolRoots,
        include: DEFAULT_INCLUDE,
        exclude: DEFAULT_EXCLUDE
      },
      targets: [{
        language: targetLanguage,
        root: targetRoot,
        package: defaults.targetPackage,
        framework: targetLanguage === "java" ? "spring" : void 0
      }],
      artifacts: {
        domainModel: `.renovatio/diagrams/${projectId}.renovatio-domain.json`,
        persistenceModel: `.renovatio/diagrams/${projectId}-persistence.renovatio-domain.json`,
        architecture: `.renovatio/diagrams/${projectId}.renovatio-arch.json`,
        migrationMap: ".renovatio/migration-map.renovatio.json",
        evidenceDir: ".renovatio/evidence"
      },
      backend: {
        url: defaults.backendUrl,
        environment: "local",
        healthEndpoint: "/actuator/health",
        capabilitiesEndpoint: "/api/capabilities",
        allowLocalProcessControl: true
      },
      llm: {
        provider: "ollama",
        model: "codellama:13b",
        purpose: "cobol-reverse-engineering",
        temperature: 0.1,
        maxTokens: 8192,
        cacheEnabled: true,
        promptProfile: "cobol.domain.entities.v1",
        fallbackModel: null
      }
    };
  }
  defaultsFromSettings(folder) {
    const config = vscode.workspace.getConfiguration("renovatio", folder.uri);
    const targetLanguage = String(config.get("targetLanguage") || "java");
    const generatedRoot = String(config.get("generatedRoot") || `generated/${targetLanguage}`);
    const generatedRoots = uniqueStrings([
      ...stringArray(config.get("generatedRoots")),
      generatedRoot
    ]).map((value2) => relativePath(folder, value2));
    const cobolRoots = stringArray(config.get("cobolRoots")).map((value2) => relativePath(folder, value2));
    return {
      projectId: sanitizeProjectId(folder.name),
      cobolRoots: cobolRoots.length ? cobolRoots : ["src/mainframe", "copybooks"],
      generatedRoots: generatedRoots.length ? generatedRoots : [`generated/${targetLanguage}`],
      targetLanguage,
      targetPackage: String(config.get("targetPackage") || "com.example.modernized"),
      backendUrl: String(config.get("backendUrl") || "http://127.0.0.1:8081").replace(/\/$/, "")
    };
  }
  async pickWorkspaceFolder() {
    const folders = vscode.workspace.workspaceFolders ?? [];
    if (folders.length === 0) {
      vscode.window.showWarningMessage("Open a VS Code workspace before using Renovatio workspace commands.");
      return void 0;
    }
    if (folders.length === 1) return folders[0];
    const selected = await vscode.window.showQuickPick(
      folders.map((folder) => ({ label: folder.name, description: folder.uri.fsPath, folder })),
      { placeHolder: "Select the workspace folder for Renovatio artifacts" }
    );
    return selected?.folder;
  }
  manifestUri(folder) {
    return vscode.Uri.joinPath(folder.uri, ...WORKSPACE_MANIFEST_RELATIVE_PATH.split("/"));
  }
};
function validateManifest(value2) {
  const issues = [];
  if (!isRecord(value2)) return ["Manifest must be a JSON object."];
  requireString(value2, "version", issues, ["1"]);
  requireString(value2, "projectId", issues);
  const source = requireObject(value2, "source", issues);
  if (source) {
    requireString(source, "language", issues, ["cobol"]);
    requireStringArray(source, "roots", issues);
    requireStringArray(source, "include", issues);
    requireStringArray(source, "exclude", issues);
  }
  const targets = value2.targets;
  if (!Array.isArray(targets) || targets.length === 0) {
    issues.push("targets must contain at least one target.");
  } else {
    targets.forEach((target, index) => {
      if (!isRecord(target)) {
        issues.push(`targets[${index}] must be an object.`);
        return;
      }
      requireString(target, "language", issues);
      requireString(target, "root", issues);
    });
  }
  const artifacts = requireObject(value2, "artifacts", issues);
  if (artifacts) {
    for (const key of ["domainModel", "persistenceModel", "architecture", "migrationMap", "evidenceDir"]) {
      requireWorkspaceRelativePath(artifacts, key, issues);
    }
  }
  const backend = requireObject(value2, "backend", issues);
  if (backend) {
    requireString(backend, "url", issues);
    requireString(backend, "environment", issues);
    if (backend.healthEndpoint !== void 0) requireString(backend, "healthEndpoint", issues);
    if (backend.capabilitiesEndpoint !== void 0) requireString(backend, "capabilitiesEndpoint", issues);
    if (backend.commands !== void 0 && !isRecord(backend.commands)) {
      issues.push("backend.commands must be an object.");
    }
    if (typeof backend.allowLocalProcessControl !== "boolean") {
      issues.push("backend.allowLocalProcessControl must be a boolean.");
    }
  }
  const llm = requireObject(value2, "llm", issues);
  if (llm) {
    requireString(llm, "provider", issues);
    requireString(llm, "model", issues);
    requireString(llm, "purpose", issues);
    requireNumber(llm, "temperature", issues);
    requireNumber(llm, "maxTokens", issues);
    requireString(llm, "promptProfile", issues);
    if (typeof llm.cacheEnabled !== "boolean") {
      issues.push("llm.cacheEnabled must be a boolean.");
    }
    if (llm.fallbackModel !== null && llm.fallbackModel !== void 0 && typeof llm.fallbackModel !== "string") {
      issues.push("llm.fallbackModel must be a string or null.");
    }
  }
  return issues;
}
function requireObject(target, key, issues) {
  const value2 = target[key];
  if (!isRecord(value2)) {
    issues.push(`${key} must be an object.`);
    return void 0;
  }
  return value2;
}
function requireString(target, key, issues, allowed) {
  const value2 = target[key];
  if (typeof value2 !== "string" || value2.trim() === "") {
    issues.push(`${key} must be a non-empty string.`);
    return;
  }
  if (allowed && !allowed.includes(value2)) {
    issues.push(`${key} must be one of: ${allowed.join(", ")}.`);
  }
}
function requireNumber(target, key, issues) {
  if (typeof target[key] !== "number" || !Number.isFinite(target[key])) {
    issues.push(`${key} must be a finite number.`);
  }
}
function requireStringArray(target, key, issues) {
  const value2 = target[key];
  if (!Array.isArray(value2) || value2.some((entry) => typeof entry !== "string" || entry.trim() === "")) {
    issues.push(`${key} must be an array of non-empty strings.`);
  }
}
function requireWorkspaceRelativePath(target, key, issues) {
  const value2 = target[key];
  if (typeof value2 !== "string" || value2.trim() === "") {
    issues.push(`artifacts.${key} must be a workspace-relative path.`);
    return;
  }
  if (value2.startsWith("/") || /^[a-zA-Z]:[\\/]/.test(value2)) {
    issues.push(`artifacts.${key} must be workspace-relative, not absolute.`);
  }
}
function diagnostic(messageText) {
  return new vscode.Diagnostic(
    new vscode.Range(new vscode.Position(0, 0), new vscode.Position(0, 1)),
    messageText,
    vscode.DiagnosticSeverity.Error
  );
}
function parseJsonc(text) {
  return JSON.parse(stripJsonComments(text));
}
function stripJsonComments(text) {
  let output = "";
  let inString = false;
  let escaped = false;
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index];
    const next = text[index + 1];
    if (inString) {
      output += char;
      if (escaped) {
        escaped = false;
      } else if (char === "\\") {
        escaped = true;
      } else if (char === '"') {
        inString = false;
      }
      continue;
    }
    if (char === '"') {
      inString = true;
      output += char;
      continue;
    }
    if (char === "/" && next === "/") {
      while (index < text.length && text[index] !== "\n") {
        output += " ";
        index += 1;
      }
      output += "\n";
      continue;
    }
    if (char === "/" && next === "*") {
      output += "  ";
      index += 2;
      while (index < text.length && !(text[index] === "*" && text[index + 1] === "/")) {
        output += text[index] === "\n" ? "\n" : " ";
        index += 1;
      }
      output += "  ";
      index += 1;
      continue;
    }
    output += char;
  }
  return output;
}
function relativePath(folder, value2) {
  const normalized = value2.replace(/\\/g, "/");
  const root = folder.uri.fsPath.replace(/\\/g, "/");
  if (normalized.startsWith(`${root}/`)) return normalized.slice(root.length + 1);
  return normalized;
}
function stringArray(value2) {
  return Array.isArray(value2) ? value2.filter((entry) => typeof entry === "string") : [];
}
function uniqueStrings(values) {
  return [...new Set(values.filter((value2) => value2.trim().length > 0))];
}
function sanitizeProjectId(value2) {
  const sanitized = value2.trim().toLowerCase().replace(/[^a-z0-9._-]+/g, "-").replace(/^-+|-+$/g, "");
  return sanitized || "renovatio-workspace";
}
function isRecord(value2) {
  return Boolean(value2 && typeof value2 === "object" && !Array.isArray(value2));
}
function isRenovatioJson(uri) {
  return uri.fsPath.endsWith(".renovatio.json") || uri.fsPath.endsWith(".renovatio-domain.json") || uri.fsPath.endsWith(".renovatio-arch.json") || uri.fsPath.endsWith(WORKSPACE_MANIFEST_RELATIVE_PATH);
}
async function exists(uri) {
  try {
    await vscode.workspace.fs.stat(uri);
    return true;
  } catch {
    return false;
  }
}
function encodeJson(value2) {
  return new TextEncoder().encode(`${JSON.stringify(value2, null, 2)}
`);
}
function decodeBytes(value2) {
  return new TextDecoder("utf-8").decode(value2);
}
function message(error) {
  return error instanceof Error ? error.message : String(error);
}

// src/backendControl.ts
var vscode2 = __toESM(require("vscode"));
var import_node_child_process = require("node:child_process");
var RenovatioBackendControlCenter = class {
  constructor(manifestService, output) {
    this.manifestService = manifestService;
    this.output = output;
    this.disposables.push(this.onDidChangeTreeDataEmitter);
  }
  manifestService;
  output;
  onDidChangeTreeDataEmitter = new vscode2.EventEmitter();
  onDidChangeTreeData = this.onDidChangeTreeDataEmitter.event;
  disposables = [];
  snapshot = { status: "unknown" };
  dispose() {
    this.disposables.forEach((disposable) => disposable.dispose());
  }
  refresh() {
    this.onDidChangeTreeDataEmitter.fire(void 0);
  }
  getTreeItem(element) {
    return element;
  }
  async getChildren(element) {
    const manifest = await this.manifest();
    if (!manifest) {
      return [
        action("Initialize Renovatio Workspace", "renovatio.initializeWorkspace", "required before backend config"),
        action("Open Workspace Manifest", "renovatio.openWorkspaceManifest")
      ];
    }
    if (!element) {
      return [
        section("Backend Connection", "backend"),
        section("LLM Reverse Engineering", "llm"),
        section("Server Control", "server")
      ];
    }
    if (element.id === "backend") {
      return [
        value("URL", manifest.backend.url),
        value("Environment", manifest.backend.environment),
        value("Health", healthLabel(this.snapshot), this.snapshot.error),
        value("Version", this.snapshot.version ?? "unknown"),
        value("Last check", this.snapshot.checkedAt?.toLocaleString() ?? "never"),
        action("Test Connection", "renovatio.testBackendConnection", manifest.backend.healthEndpoint ?? "/actuator/health"),
        action("Open Settings", "renovatio.openBackendSettings"),
        action("Open Logs", "renovatio.openBackendLogs"),
        action("Reload Config", "renovatio.reloadBackendConfiguration")
      ];
    }
    if (element.id === "llm") {
      return [
        value("Provider", manifest.llm.provider),
        value("Model", manifest.llm.model),
        value("Fallback", manifest.llm.fallbackModel ?? "none"),
        value("Prompt profile", manifest.llm.promptProfile),
        value("Cache", manifest.llm.cacheEnabled ? "enabled" : "disabled"),
        value("Smoke test", this.snapshot.llmSmoke ?? "not run"),
        action("Configure Model", "renovatio.configureLlmModel"),
        action("Test Reverse Engineering", "renovatio.testLlmReverseEngineering"),
        action("Clear LLM Cache", "renovatio.clearLlmCache"),
        action("Open Prompt Profile", "renovatio.openPromptProfile"),
        action("Compare Against Golden Fixture", "renovatio.compareLlmOutputAgainstGoldenFixture")
      ];
    }
    return [
      action("Start Backend", "renovatio.startBackend", controlState(manifest)),
      action("Stop Backend", "renovatio.stopBackend", controlState(manifest)),
      action("Restart Backend", "renovatio.restartBackend", controlState(manifest)),
      action("Reload Config", "renovatio.reloadBackendConfiguration")
    ];
  }
  register(context) {
    context.subscriptions.push(
      vscode2.window.registerTreeDataProvider("renovatio.backend", this),
      vscode2.commands.registerCommand("renovatio.openBackendSettings", () => this.manifestService.openWorkspaceManifest()),
      vscode2.commands.registerCommand("renovatio.testBackendConnection", () => this.testBackendConnection()),
      vscode2.commands.registerCommand("renovatio.openBackendLogs", () => this.output.show()),
      vscode2.commands.registerCommand("renovatio.startBackend", () => this.runBackendCommand("start")),
      vscode2.commands.registerCommand("renovatio.stopBackend", () => this.runBackendCommand("stop")),
      vscode2.commands.registerCommand("renovatio.restartBackend", () => this.runBackendCommand("restart")),
      vscode2.commands.registerCommand("renovatio.reloadBackendConfiguration", () => this.runBackendCommand("reloadConfig")),
      vscode2.commands.registerCommand("renovatio.configureLlmModel", () => this.configureLlmModel()),
      vscode2.commands.registerCommand("renovatio.testLlmReverseEngineering", () => this.testLlmReverseEngineering()),
      vscode2.commands.registerCommand("renovatio.clearLlmCache", () => this.clearLlmCache()),
      vscode2.commands.registerCommand("renovatio.openPromptProfile", () => this.openPromptProfile()),
      vscode2.commands.registerCommand("renovatio.compareLlmOutputAgainstGoldenFixture", () => this.compareLlmOutputAgainstGoldenFixture())
    );
  }
  async testBackendConnection() {
    const manifest = await this.manifest();
    if (!manifest) return;
    const endpoint = manifest.backend.healthEndpoint ?? "/actuator/health";
    const url = joinUrl(manifest.backend.url, endpoint);
    this.output.appendLine(`[backend] GET ${url}`);
    try {
      const response = await fetchWithTimeout(url, 6e3);
      const body = await response.text();
      if (!response.ok) throw new Error(`HTTP ${response.status}: ${body.slice(0, 240)}`);
      const parsed = parseMaybeJson(body);
      this.snapshot = {
        status: healthStatus(parsed),
        checkedAt: /* @__PURE__ */ new Date(),
        version: versionFrom(parsed),
        error: void 0,
        llmSmoke: this.snapshot.llmSmoke
      };
      this.output.appendLine(`[backend] health ok: ${body.slice(0, 500)}`);
      vscode2.window.showInformationMessage(`Renovatio backend health: ${this.snapshot.status}`);
    } catch (error) {
      this.snapshot = {
        status: "unhealthy",
        checkedAt: /* @__PURE__ */ new Date(),
        error: message2(error),
        llmSmoke: this.snapshot.llmSmoke
      };
      this.output.appendLine(`[backend] health failed: ${message2(error)}`);
      vscode2.window.showWarningMessage(`Renovatio backend unreachable: ${message2(error)}`);
    }
    this.refresh();
  }
  async configureLlmModel() {
    const manifest = await this.manifest();
    if (!manifest) return;
    const provider = await vscode2.window.showInputBox({ title: "Renovatio LLM Provider", value: manifest.llm.provider });
    if (!provider) return;
    const model = await vscode2.window.showInputBox({ title: "Renovatio LLM Model", value: manifest.llm.model });
    if (!model) return;
    const fallbackModel = await vscode2.window.showInputBox({
      title: "Renovatio fallback model",
      value: manifest.llm.fallbackModel ?? "",
      prompt: "Leave empty to disable fallback."
    });
    await this.manifestService.update((next) => {
      next.llm.provider = provider.trim();
      next.llm.model = model.trim();
      next.llm.fallbackModel = fallbackModel?.trim() || null;
    });
    this.refresh();
  }
  async testLlmReverseEngineering() {
    const manifest = await this.manifest();
    if (!manifest) return;
    const endpoint = "/api/llm/reverse-engineering/smoke-test";
    const url = joinUrl(manifest.backend.url, endpoint);
    this.output.appendLine(`[llm] POST ${url}`);
    try {
      const response = await fetchWithTimeout(url, 1e4, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ provider: manifest.llm.provider, model: manifest.llm.model, promptProfile: manifest.llm.promptProfile })
      });
      const body = await response.text();
      if (response.status === 404 || response.status === 405) {
        this.snapshot = { ...this.snapshot, llmSmoke: "unsupported by backend" };
        vscode2.window.showInformationMessage("LLM smoke test endpoint is not available in this backend.");
      } else if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${body.slice(0, 240)}`);
      } else {
        this.snapshot = { ...this.snapshot, llmSmoke: "passed" };
        vscode2.window.showInformationMessage("LLM reverse-engineering smoke test passed.");
      }
      this.output.appendLine(`[llm] smoke response: ${body.slice(0, 500)}`);
    } catch (error) {
      this.snapshot = { ...this.snapshot, llmSmoke: `failed: ${message2(error)}` };
      this.output.appendLine(`[llm] smoke failed: ${message2(error)}`);
      vscode2.window.showWarningMessage(`LLM smoke test failed: ${message2(error)}`);
    }
    this.refresh();
  }
  async clearLlmCache() {
    await this.postOptional("/api/llm/cache/clear", "LLM cache clear");
  }
  async openPromptProfile() {
    const manifest = await this.manifest();
    if (!manifest) return;
    const files = await vscode2.workspace.findFiles(`**/${manifest.llm.promptProfile}*`, "**/{node_modules,target,.git}/**", 5);
    if (!files.length) {
      vscode2.window.showInformationMessage(`Prompt profile not found in workspace: ${manifest.llm.promptProfile}`);
      return;
    }
    await vscode2.window.showTextDocument(await vscode2.workspace.openTextDocument(files[0]));
  }
  async compareLlmOutputAgainstGoldenFixture() {
    this.snapshot = { ...this.snapshot, llmSmoke: "golden comparison unsupported by backend" };
    vscode2.window.showInformationMessage("Golden fixture comparison is not exposed by the backend yet.");
    this.refresh();
  }
  async runBackendCommand(kind) {
    const manifest = await this.manifest();
    if (!manifest) return;
    if (kind !== "reloadConfig" && !canControlServer(manifest)) {
      vscode2.window.showWarningMessage("Backend process control is available only for local/dev workspaces with allowLocalProcessControl enabled.");
      return;
    }
    const command = manifest.backend.commands?.[kind] ?? defaultCommand(kind, manifest);
    if (!command) {
      vscode2.window.showInformationMessage(`No ${kind} command configured in the Renovatio workspace manifest.`);
      return;
    }
    const confirmed = await vscode2.window.showWarningMessage(
      `Run Renovatio backend ${kind} command?

${command}`,
      { modal: true },
      "Run Command"
    );
    if (confirmed !== "Run Command") return;
    await this.runShellCommand(kind, command);
  }
  async runShellCommand(kind, command) {
    const cwd = vscode2.workspace.workspaceFolders?.[0]?.uri.fsPath;
    this.output.show(true);
    this.output.appendLine(`[backend:${kind}] ${command}`);
    if (kind === "start") {
      const terminal = vscode2.window.createTerminal({ name: "Renovatio Backend", cwd });
      terminal.show();
      terminal.sendText(command);
      this.output.appendLine("[backend:start] command sent to VS Code terminal");
      return;
    }
    await new Promise((resolve) => {
      const child = (0, import_node_child_process.exec)(command, { cwd, timeout: 12e4 }, (error, stdout, stderr) => {
        if (stdout) this.output.appendLine(stdout.trimEnd());
        if (stderr) this.output.appendLine(stderr.trimEnd());
        if (error) {
          this.output.appendLine(`[backend:${kind}] failed: ${error.message}`);
          vscode2.window.showWarningMessage(`Backend ${kind} failed: ${error.message}`);
        } else {
          this.output.appendLine(`[backend:${kind}] completed`);
          vscode2.window.showInformationMessage(`Backend ${kind} command completed.`);
        }
        resolve();
      });
      child.stdout?.on("data", (chunk) => this.output.append(String(chunk)));
      child.stderr?.on("data", (chunk) => this.output.append(String(chunk)));
    });
  }
  async postOptional(endpoint, label) {
    const manifest = await this.manifest();
    if (!manifest) return;
    const url = joinUrl(manifest.backend.url, endpoint);
    try {
      const response = await fetchWithTimeout(url, 8e3, { method: "POST" });
      if (response.status === 404 || response.status === 405) {
        vscode2.window.showInformationMessage(`${label} endpoint is not available in this backend.`);
        return;
      }
      if (!response.ok) throw new Error(`HTTP ${response.status}: ${(await response.text()).slice(0, 240)}`);
      vscode2.window.showInformationMessage(`${label} completed.`);
    } catch (error) {
      vscode2.window.showWarningMessage(`${label} failed: ${message2(error)}`);
    }
  }
  async manifest() {
    const manifest = await this.manifestService.load();
    if (!manifest) {
      vscode2.window.setStatusBarMessage("Renovatio workspace manifest not found.", 5e3);
    }
    return manifest;
  }
};
var BackendTreeItem = class extends vscode2.TreeItem {
  constructor(id, label, collapsibleState = vscode2.TreeItemCollapsibleState.None) {
    super(label, collapsibleState);
    this.id = id;
  }
  id;
};
function section(label, id) {
  const item = new BackendTreeItem(id, label, vscode2.TreeItemCollapsibleState.Expanded);
  item.contextValue = "renovatioBackendSection";
  return item;
}
function value(label, description, tooltip) {
  const item = new BackendTreeItem(label, label);
  item.description = description;
  item.tooltip = tooltip ?? `${label}: ${description ?? ""}`;
  return item;
}
function action(label, command, description) {
  const item = new BackendTreeItem(command, label);
  item.description = description;
  item.command = { command, title: label };
  item.contextValue = "renovatioBackendAction";
  return item;
}
function healthLabel(snapshot) {
  return snapshot.status === "healthy" ? "healthy" : snapshot.status === "unhealthy" ? "unhealthy" : "unknown";
}
function controlState(manifest) {
  return canControlServer(manifest) ? "enabled" : "disabled by environment/safety";
}
function canControlServer(manifest) {
  return ["local", "dev"].includes(manifest.backend.environment) && manifest.backend.allowLocalProcessControl === true;
}
function defaultCommand(kind, manifest) {
  if (kind === "start") return "./mvnw -pl renovatio-api spring-boot:run";
  if (kind === "reloadConfig") return `curl -X POST ${joinUrl(manifest.backend.url, "/api/admin/reload")}`;
  return void 0;
}
function joinUrl(base, endpoint) {
  const cleanBase = base.replace(/\/$/, "");
  const cleanEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
  return `${cleanBase}${cleanEndpoint}`;
}
async function fetchWithTimeout(url, timeoutMs, init = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timeout);
  }
}
function parseMaybeJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
function healthStatus(value2) {
  if (typeof value2 === "string") return value2 ? "healthy" : "unknown";
  if (!value2 || typeof value2 !== "object") return "unknown";
  const status = String(value2.status ?? "").toUpperCase();
  return status === "UP" || status === "OK" || status === "HEALTHY" ? "healthy" : "unhealthy";
}
function versionFrom(value2) {
  if (!value2 || typeof value2 !== "object") return void 0;
  const record = value2;
  const direct = record.version ?? record.buildVersion;
  if (typeof direct === "string") return direct;
  const components = record.components;
  if (components && typeof components === "object") {
    const info = components.info;
    if (info && typeof info === "object") {
      const details = info.details;
      if (details && typeof details === "object" && typeof details.version === "string") {
        return details.version;
      }
    }
  }
  return void 0;
}
function message2(error) {
  return error instanceof Error ? error.message : String(error);
}

// src/extension.ts
var DOMAIN_VIEW_TYPE = "renovatio.diagram.domain";
var ARCHITECTURE_VIEW_TYPE = "renovatio.diagram.architecture";
function activate2(context) {
  const output = vscode3.window.createOutputChannel("Renovatio Diagrams");
  const backendOutput = vscode3.window.createOutputChannel("Renovatio Backend");
  legacyExtension.activate(context);
  const provider = new RenovatioDiagramEditorProvider(context, output);
  const tree = new RenovatioWelcomeTree(context);
  const manifestService = new RenovatioWorkspaceManifestService(output);
  const backendControl = new RenovatioBackendControlCenter(manifestService, backendOutput);
  backendControl.register(context);
  context.subscriptions.push(
    output,
    backendOutput,
    manifestService,
    backendControl,
    vscode3.window.registerCustomEditorProvider(DOMAIN_VIEW_TYPE, provider, { webviewOptions: { retainContextWhenHidden: true } }),
    vscode3.window.registerCustomEditorProvider(ARCHITECTURE_VIEW_TYPE, provider, { webviewOptions: { retainContextWhenHidden: true } }),
    vscode3.window.registerTreeDataProvider("renovatio.views.welcome", tree),
    vscode3.commands.registerCommand("renovatio.initializeWorkspace", () => manifestService.initializeWorkspace()),
    vscode3.commands.registerCommand("renovatio.openWorkspaceManifest", () => manifestService.openWorkspaceManifest()),
    vscode3.commands.registerCommand("renovatio.validateWorkspace", () => manifestService.validateWorkspace()),
    vscode3.commands.registerCommand("renovatio.formatArtifacts", () => manifestService.formatArtifacts()),
    vscode3.commands.registerCommand("renovatio.openDomainSample", () => openSample(context, "sample.renovatio-domain.json")),
    vscode3.commands.registerCommand("renovatio.openArchitectureSample", () => openSample(context, "sample.renovatio-arch.json"))
  );
  void manifestService.validateWorkspace();
}
function deactivate() {
}
var RenovatioDiagramEditorProvider = class {
  constructor(context, output) {
    this.context = context;
    this.output = output;
  }
  context;
  output;
  updateQueue = Promise.resolve();
  mutatingDocuments = /* @__PURE__ */ new Map();
  async resolveCustomTextEditor(document, webviewPanel, token) {
    webviewPanel.webview.options = {
      enableScripts: true,
      localResourceRoots: [vscode3.Uri.joinPath(this.context.extensionUri, "dist")]
    };
    webviewPanel.webview.html = this.htmlFor(webviewPanel.webview);
    const postModel = () => {
      if (token.isCancellationRequested) {
        return;
      }
      const parsed = this.parseForWebview(document);
      this.output.appendLine(`Rendering ${parsed.kind} diagram: ${document.uri.fsPath} (${parsed.model.nodes.length} nodes, ${parsed.model.edges.length} edges)`);
      webviewPanel.webview.postMessage({
        type: "setModel",
        documentKind: parsed.kind,
        hasSavedLayout: hasSavedLayout(parsed),
        model: parsed.model
      });
    };
    const changeSubscription = vscode3.workspace.onDidChangeTextDocument((event) => {
      if (event.document.uri.toString() === document.uri.toString()) {
        if (this.isMutatingDocument(document.uri)) {
          return;
        }
        postModel();
      }
    });
    webviewPanel.onDidDispose(() => changeSubscription.dispose());
    webviewPanel.webview.onDidReceiveMessage(async (message3) => {
      if (!message3) {
        return;
      }
      if (message3.type === "ready") {
        postModel();
        return;
      }
      if (message3.type === "error") {
        const detail = String(message3.message ?? "Unknown webview error");
        this.output.appendLine(`Webview error in ${document.uri.fsPath}: ${detail}`);
        vscode3.window.showErrorMessage(`Renovatio diagram webview error: ${detail}`);
        return;
      }
      if (message3.type !== "diagramEvent") {
        return;
      }
      await this.enqueueDiagramUpdate(document, message3.event, postModel);
    });
    postModel();
  }
  parseForWebview(document) {
    try {
      return parseDiagramDocument(document.getText(), document.fileName);
    } catch (error) {
      const detail = error instanceof Error ? error.message : String(error);
      return {
        kind: "diagram",
        raw: {},
        model: {
          nodes: [{
            id: "parse-error",
            kind: "ERROR",
            label: "Invalid Renovatio JSON",
            x: 80,
            y: 80,
            data: { properties: [{ name: "error", type: detail, required: true }] }
          }],
          edges: []
        }
      };
    }
  }
  async enqueueDiagramUpdate(document, event, postModel) {
    if (!isDocumentMutationEvent(event)) {
      return;
    }
    this.updateQueue = this.updateQueue.catch(() => void 0).then(async () => {
      try {
        const parsed = parseDiagramDocument(document.getText(), document.fileName);
        const next = applyDiagramEvent(parsed, event);
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
        vscode3.window.showErrorMessage(`Unable to update Renovatio diagram: ${detail}`);
      }
    });
    await this.updateQueue;
  }
  beginDocumentMutation(uri) {
    const key = uri.toString();
    this.mutatingDocuments.set(key, (this.mutatingDocuments.get(key) ?? 0) + 1);
  }
  endDocumentMutation(uri) {
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
  isMutatingDocument(uri) {
    return (this.mutatingDocuments.get(uri.toString()) ?? 0) > 0;
  }
  async replaceDocument(document, text) {
    if (document.getText() === text) {
      return;
    }
    const edit = new vscode3.WorkspaceEdit();
    const start = new vscode3.Position(0, 0);
    const end = document.lineCount === 0 ? start : document.lineAt(document.lineCount - 1).rangeIncludingLineBreak.end;
    edit.replace(document.uri, new vscode3.Range(start, end), text);
    const applied = await vscode3.workspace.applyEdit(edit);
    if (!applied) {
      throw new Error("VS Code rejected the diagram document edit.");
    }
    await document.save();
  }
  htmlFor(webview) {
    const nonce = nonceValue();
    const scriptUri = webview.asWebviewUri(vscode3.Uri.joinPath(this.context.extensionUri, "dist", "webview.js"));
    const styleUri = webview.asWebviewUri(vscode3.Uri.joinPath(this.context.extensionUri, "dist", "webview.css"));
    const csp = [
      `default-src 'none'`,
      `img-src ${webview.cspSource} data:`,
      `style-src ${webview.cspSource} 'unsafe-inline'`,
      `script-src 'nonce-${nonce}'`
    ].join("; ");
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
};
function hasSavedLayout(parsed) {
  const layout = parsed.kind === "architecture" ? parsed.raw.profile?.layout ?? parsed.raw.layout : parsed.raw.layout;
  return Boolean(layout && typeof layout === "object" && Object.keys(layout).length > 0);
}
function isLayoutOnlyEvent(event) {
  return Boolean(event && typeof event === "object" && event.type === "layoutChanged");
}
function isDocumentMutationEvent(event) {
  if (!event || typeof event !== "object") {
    return false;
  }
  const type = event.type;
  return type === "nodeMoved" || type === "layoutChanged" || type === "nodesPruned" || type === "edgeCreated" || type === "edgeReconnected" || type === "edgeLabelChanged" || type === "edgesDeleted" || type === "architectureStyleChanged";
}
var RenovatioWelcomeTree = class {
  constructor(context) {
    this.context = context;
  }
  context;
  getTreeItem(element) {
    return element;
  }
  getChildren() {
    return [
      new RenovatioTreeItem("Initialize Renovatio Workspace", "renovatio.initializeWorkspace", ".renovatio/workspace.renovatio.json"),
      new RenovatioTreeItem("Open Workspace Manifest", "renovatio.openWorkspaceManifest", "local contract"),
      new RenovatioTreeItem("Validate Renovatio Workspace", "renovatio.validateWorkspace", "schemas and manifest"),
      new RenovatioTreeItem("Format Renovatio Artifacts", "renovatio.formatArtifacts", "JSON/JSONC"),
      new RenovatioTreeItem("Open Native Domain Diagram", "renovatio.openNativeDomainDiagram", "current DomainModel"),
      new RenovatioTreeItem("Open Native Persistence Diagram", "renovatio.openNativePersistenceDiagram", "repositories and records"),
      new RenovatioTreeItem("Open Native Architecture Diagram", "renovatio.openNativeArchitectureDiagram", "target layers"),
      new RenovatioTreeItem("Open Domain sample", "renovatio.openDomainSample", "renovatio-domain.json"),
      new RenovatioTreeItem("Open Architecture sample", "renovatio.openArchitectureSample", "renovatio-arch.json"),
      new RenovatioTreeItem("Custom editors are active for *.renovatio-domain.json and *.renovatio-arch.json")
    ];
  }
};
var RenovatioTreeItem = class extends vscode3.TreeItem {
  constructor(label, commandId, description) {
    super(label, vscode3.TreeItemCollapsibleState.None);
    this.description = description;
    this.tooltip = label;
    if (commandId) {
      this.command = { command: commandId, title: label };
      this.contextValue = "renovatioAction";
    }
  }
};
async function openSample(context, fileName) {
  const uri = vscode3.Uri.joinPath(context.extensionUri, "examples", fileName);
  const document = await vscode3.workspace.openTextDocument(uri);
  await vscode3.window.showTextDocument(document, { preview: false });
  await vscode3.commands.executeCommand(
    "vscode.openWith",
    uri,
    fileName.endsWith("arch.json") ? ARCHITECTURE_VIEW_TYPE : DOMAIN_VIEW_TYPE
  );
}
function nonceValue() {
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  let value2 = "";
  for (let i = 0; i < 32; i += 1) {
    value2 += alphabet.charAt(Math.floor(Math.random() * alphabet.length));
  }
  return value2;
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  activate,
  deactivate
});
//# sourceMappingURL=extension.js.map
