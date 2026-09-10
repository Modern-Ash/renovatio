import { expect, test } from '@playwright/test';
import { architectureScenarios, mockCobolModernizationFlow } from './fixtures/cobol-modernization-flow.mjs';

test('Theia guides COBOL source scan through modeling, architecture, shadow review and approved target generation', async ({ page }) => {
    await mockCobolModernizationFlow(page);

    await page.goto('/');

    const activityRail = page.getByRole('navigation', { name: 'Renovatio activity areas' });

    await expect(page.getByRole('heading', { name: 'RENOVATIO / CONTROL DECK' })).toBeVisible();
    await expect(activityRail.getByRole('button', { name: /Project/ })).toHaveAttribute('aria-pressed', 'true');

    const projectAssets = page.getByRole('navigation', { name: 'Project assets' });
    await expect(projectAssets).toContainText('COBOL sources');
    await expect(projectAssets.getByRole('button', { name: 'PAYROLL.CBL' })).toBeVisible();
    await expect(page.getByRole('textbox', { name: 'PAYROLL.CBL content' })).toHaveValue(/CALCULATE-PAYROLL/);
    await expect(page.getByLabel('Source explorer')).toContainText('Source explorer · READY');
    await expect(page.getByRole('list', { name: 'Outline of PAYROLL.CBL' })).toContainText('EMPLOYEE-RECORD');
    await expect(page.getByRole('list', { name: 'Outline of PAYROLL.CBL' })).toContainText('ir://payroll/procedure/calculate-payroll');

    await activityRail.getByRole('button', { name: /Analysis/ }).click();
    await expect(page.getByLabel('Analysis inventory')).toContainText('cobol: 1');
    await expect(page.getByLabel('Analysis inventory')).toContainText('analysis-2026-09-08');

    await activityRail.getByRole('button', { name: /Domain/ }).click();
    await expect(page.getByLabel('Business DomainModel editor')).toContainText('Employee Payroll');
    await expect(page.getByLabel('Domain provenance and history')).toContainText('src/PAYROLL.CBL#EMPLOYEE-RECORD');
    await expect(page.getByLabel('Business DomainModel editor')).toContainText('Net pay equals gross pay minus tax withholding.');

    await activityRail.getByRole('button', { name: /Architecture/ }).click();
    await expect(page.getByLabel('Architecture Canvas editor')).toContainText('HEXAGONAL');
    await expect(page.getByLabel('Artifact and package manifest preview')).toContainText('PayrollService.java');
    await expect(page.getByLabel('Architecture profile inspector').getByLabel('Reason').nth(1)).toHaveValue('Domain cannot depend on adapters.');

    await activityRail.getByRole('button', { name: /Shadow/ }).click();
    await expect(page.getByLabel('Shadow diff and impact analysis')).toContainText('COBOL → IR → DomainModel → Architecture → Java');
    await expect(page.getByLabel('Manifest diff before generation')).toContainText('PayrollService.java');
    await expect(page.getByLabel('Manifest diff before generation')).toContainText('All DomainModel evidence resolves to known source paths.');

    await activityRail.getByRole('button', { name: /Changes/ }).click();
    const changeSetPanel = page.getByLabel('Reviewable change sets approval and rollback');
    await expect(changeSetPanel).toContainText('DomainModel revision 2 saved');
    await expect(changeSetPanel).toContainText('Architecture profile revision 3 saved');
    await expect(page.getByRole('button', { name: 'Apply approved manifest' })).toBeDisabled();

    await page.getByRole('button', { name: 'Approve diff' }).click();
    await expect(page.getByRole('button', { name: 'Apply approved manifest' })).toBeEnabled();
    await expect(page.getByText(/Approved manifest sha256:/)).toBeVisible();

    await page.getByRole('button', { name: 'Apply approved manifest' }).click();
    await expect(changeSetPanel.locator('.renovatio-change-card.state-applied')).toContainText('Generate reviewed PayrollService target');

    await activityRail.getByRole('button', { name: /Equivalence/ }).click();
    await expect(page.getByLabel('Integrated Equivalence Lab')).toContainText('PayrollService.java');
    await expect(page.getByLabel('Equivalence promotion gate and readiness')).toContainText('promotable: true');
    await expect(page.getByLabel('Async execution progress logs and cancellation')).toContainText('Outputs matched');
});

test('Theia previews Java manifests for every supported COBOL target architecture', async ({ page }) => {
    await mockCobolModernizationFlow(page);

    await page.goto('/');

    const activityRail = page.getByRole('navigation', { name: 'Renovatio activity areas' });
    await activityRail.getByRole('button', { name: /Architecture/ }).click();

    const architectureCanvas = page.getByLabel('Architecture Canvas editor');
    const architectureStage = page.getByLabel('Editable architecture canvas');
    const styleSelector = page.getByRole('group', { name: 'Architecture style' });
    const manifestPreview = page.getByLabel('Artifact and package manifest preview');

    await expect(architectureCanvas).toContainText('HEXAGONAL');

    for (const scenario of architectureScenarios) {
        const styleButton = styleSelector.getByRole('button', { name: scenario.label, exact: true });

        await styleButton.click();
        await expect(styleButton).toHaveAttribute('aria-pressed', 'true');
        await expect(architectureStage).toContainText(scenario.expectedPackage);
        await expect(manifestPreview).toContainText(scenario.expectedServicePath);
        await expect(manifestPreview).toContainText(scenario.expectedModelPath);
    }
});
