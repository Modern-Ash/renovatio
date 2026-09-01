import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import StepDiff from '../StepDiff'

describe('StepDiff', () => {
  it('renders the dry run preview instead of a placeholder', () => {
    render(
      <StepDiff
        data={{
          dryRunPreview: 'Dry run preview\n- Parse COBOL programs',
          dryRunResult: {
            changes: {
              enabledSteps: 1,
              skippedSteps: 1,
              dryRun: true,
              javaOutputDirectory: '/tmp/workspace/generated-java-stubs',
              javaDirectoryStructure: ['A.java', 'B.java']
            },
            selectedSteps: ['Parse COBOL programs'],
            skippedSteps: ['Generate tests']
          },
          planSteps: [
            { description: 'Parse COBOL programs', enabled: true },
            { description: 'Generate tests', enabled: false }
          ]
        }}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    )

    expect(screen.getByText(/dry run preview/i, { selector: 'pre' })).toBeTruthy()
    expect(screen.getByText(/^parse cobol programs$/i)).toBeTruthy()
    expect(screen.getByText(/^enabled steps$/i)).toBeTruthy()
    expect(screen.getByText(/Java files output/i)).toBeTruthy()
    expect(screen.getByText(/generated-java-stubs/i)).toBeTruthy()
    expect(screen.getByText(/selected steps:\s*1/i)).toBeTruthy()
  })
})
