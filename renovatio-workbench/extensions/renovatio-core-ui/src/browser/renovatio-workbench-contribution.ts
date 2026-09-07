import {
    AbstractViewContribution,
    FrontendApplication,
    FrontendApplicationContribution,
    KeybindingRegistry
} from '@theia/core/lib/browser';
import {
    Command,
    CommandRegistry,
    MAIN_MENU_BAR,
    MenuModelRegistry,
    MenuPath
} from '@theia/core/lib/common';
import { injectable } from '@theia/core/shared/inversify';
import { RenovatioAreaId, RenovatioShellWidget } from './renovatio-shell-widget';

export const RENOVATIO_CATEGORY = 'Renovatio';
export const RENOVATIO_MENU: MenuPath = [...MAIN_MENU_BAR, 'renovatio'];

export const OPEN_RENOVATIO_WORKBENCH: Command = {
    id: 'renovatio.workbench.open',
    label: 'Open Workbench Overview',
    category: RENOVATIO_CATEGORY
};

const AREA_COMMANDS: ReadonlyArray<{ area: RenovatioAreaId; command: Command; keybinding: string }> = [
    { area: 'project', command: { id: 'renovatio.shell.project', label: 'Open Project Area', category: RENOVATIO_CATEGORY }, keybinding: 'ctrlcmd+alt+1' },
    { area: 'analysis', command: { id: 'renovatio.shell.analysis', label: 'Open Analysis Area', category: RENOVATIO_CATEGORY }, keybinding: 'ctrlcmd+alt+2' },
    { area: 'domain', command: { id: 'renovatio.shell.domain', label: 'Open Domain Area', category: RENOVATIO_CATEGORY }, keybinding: 'ctrlcmd+alt+3' },
    { area: 'architecture', command: { id: 'renovatio.shell.architecture', label: 'Open Architecture Area', category: RENOVATIO_CATEGORY }, keybinding: 'ctrlcmd+alt+4' },
    { area: 'shadow', command: { id: 'renovatio.shell.shadow', label: 'Open Shadow Area', category: RENOVATIO_CATEGORY }, keybinding: 'ctrlcmd+alt+5' },
    { area: 'ai', command: { id: 'renovatio.shell.ai', label: 'Open AI Area', category: RENOVATIO_CATEGORY }, keybinding: 'ctrlcmd+alt+6' },
    { area: 'equivalence', command: { id: 'renovatio.shell.equivalence', label: 'Open Equivalence Area', category: RENOVATIO_CATEGORY }, keybinding: 'ctrlcmd+alt+7' }
];

@injectable()
export class RenovatioWorkbenchContribution
    extends AbstractViewContribution<RenovatioShellWidget>
    implements FrontendApplicationContribution {

    constructor() {
        super({
            widgetId: RenovatioShellWidget.ID,
            widgetName: RenovatioShellWidget.LABEL,
            defaultWidgetOptions: { area: 'main' }
        });
    }

    registerCommands(commands: CommandRegistry): void {
        commands.registerCommand(OPEN_RENOVATIO_WORKBENCH, {
            execute: () => this.openArea('project')
        });
        for (const { area, command } of AREA_COMMANDS) {
            commands.registerCommand(command, { execute: () => this.openArea(area) });
        }
    }

    registerKeybindings(keybindings: KeybindingRegistry): void {
        for (const { command, keybinding } of AREA_COMMANDS) {
            keybindings.registerKeybinding({ command: command.id, keybinding });
        }
    }

    registerMenus(menus: MenuModelRegistry): void {
        menus.registerSubmenu(RENOVATIO_MENU, RENOVATIO_CATEGORY);
        menus.registerMenuAction(RENOVATIO_MENU, { commandId: OPEN_RENOVATIO_WORKBENCH.id, order: '0' });
        AREA_COMMANDS.forEach(({ command }, index) => {
            menus.registerMenuAction(RENOVATIO_MENU, { commandId: command.id, order: `1${index}` });
        });
    }

    async openArea(area: RenovatioAreaId): Promise<void> {
        const widget = await this.openView({ reveal: true, activate: true });
        widget.activateArea(area);
    }

    onStart(_app: FrontendApplication): void {
        void this.openArea('project');
    }
}
