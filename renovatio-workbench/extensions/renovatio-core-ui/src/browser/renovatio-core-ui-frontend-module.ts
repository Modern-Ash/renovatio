import {
    bindViewContribution,
    FrontendApplicationContribution,
    WidgetFactory
} from '@theia/core/lib/browser';
import { ContainerModule, interfaces } from '@theia/core/shared/inversify';
import { RenovatioWorkbenchContribution } from './renovatio-workbench-contribution';
import { RenovatioShellWidget } from './renovatio-shell-widget';
import './style/renovatio-workbench.css';

export default new ContainerModule((bind: interfaces.Bind) => {
    bindViewContribution(bind, RenovatioWorkbenchContribution);
    bind(FrontendApplicationContribution).toService(RenovatioWorkbenchContribution);
    bind(RenovatioShellWidget).toSelf();
    bind(WidgetFactory).toDynamicValue(context => ({
        id: RenovatioShellWidget.ID,
        createWidget: () => context.container.get<RenovatioShellWidget>(RenovatioShellWidget)
    })).inSingletonScope();
});
