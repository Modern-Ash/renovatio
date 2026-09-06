import {
    bindViewContribution,
    FrontendApplicationContribution,
    WidgetFactory
} from '@theia/core/lib/browser';
import { ContainerModule, interfaces } from '@theia/core/shared/inversify';
import { RenovatioWorkbenchContribution } from './renovatio-workbench-contribution';
import { RenovatioWorkbenchWidget } from './renovatio-workbench-widget';
import './style/renovatio-workbench.css';

export default new ContainerModule((bind: interfaces.Bind) => {
    bindViewContribution(bind, RenovatioWorkbenchContribution);
    bind(FrontendApplicationContribution).toService(RenovatioWorkbenchContribution);
    bind(RenovatioWorkbenchWidget).toSelf();
    bind(WidgetFactory).toDynamicValue(context => ({
        id: RenovatioWorkbenchWidget.ID,
        createWidget: () => context.container.get<RenovatioWorkbenchWidget>(RenovatioWorkbenchWidget)
    })).inSingletonScope();
});
