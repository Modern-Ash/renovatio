import {
    AbstractViewContribution,
    FrontendApplication,
    FrontendApplicationContribution
} from '@theia/core/lib/browser';
import {
    Command,
    CommandRegistry,
    MAIN_MENU_BAR,
    MenuModelRegistry,
    MenuPath
} from '@theia/core/lib/common';
import { injectable } from '@theia/core/shared/inversify';
import { RenovatioWorkbenchWidget } from './renovatio-workbench-widget';

export const RENOVATIO_CATEGORY = 'Renovatio';
export const RENOVATIO_MENU: MenuPath = [...MAIN_MENU_BAR, 'renovatio'];

export const OPEN_RENOVATIO_WORKBENCH: Command = {
    id: 'renovatio.workbench.open',
    label: 'Open Workbench Overview',
    category: RENOVATIO_CATEGORY
};

@injectable()
export class RenovatioWorkbenchContribution
    extends AbstractViewContribution<RenovatioWorkbenchWidget>
    implements FrontendApplicationContribution {

    constructor() {
        super({
            widgetId: RenovatioWorkbenchWidget.ID,
            widgetName: RenovatioWorkbenchWidget.LABEL,
            defaultWidgetOptions: { area: 'main' }
        });
    }

    registerCommands(commands: CommandRegistry): void {
        commands.registerCommand(OPEN_RENOVATIO_WORKBENCH, {
            execute: () => this.openView({ reveal: true, activate: true })
        });
    }

    registerMenus(menus: MenuModelRegistry): void {
        menus.registerSubmenu(RENOVATIO_MENU, RENOVATIO_CATEGORY);
        menus.registerMenuAction(RENOVATIO_MENU, {
            commandId: OPEN_RENOVATIO_WORKBENCH.id,
            order: '0'
        });
    }

    onStart(_app: FrontendApplication): void {
        void this.openView({ reveal: true, activate: false });
    }
}
