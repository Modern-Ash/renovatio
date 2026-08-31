import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import StepMetrics from '../StepMetrics'

describe('StepMetrics', () => {
  it('shows the analysis summary values from the completed analysis', () => {
    render(
      <StepMetrics
        data={{
          analysisSummary: {
            sourceFiles: 43,
            copybooks: 85,
            programs: 43
          },
          analysisWorkspace: 'Bank-of-Z',
          analysisMessage: 'Parsed 43 COBOL source file(s) and 85 copybook(s) from Bank-of-Z'
        }}
        onNext={vi.fn()}
        onBack={vi.fn()}
      />
    )

    expect(screen.getByText(/COBOL Source Files/i).parentElement?.textContent).toContain('43')
    expect(screen.getByText(/Copybooks/i).parentElement?.textContent).toContain('85')
    expect(screen.getByText(/Parsed Programs/i).parentElement?.textContent).toContain('43')
    expect(screen.getByText(/Files Scanned/i).parentElement?.textContent).toContain('128')
    expect(screen.getByText(/Workspace:/i).parentElement?.textContent).toContain('Bank-of-Z')
    expect(screen.getByText(/Latest message:/i).parentElement?.textContent).toContain(
      'Parsed 43 COBOL source file(s) and 85 copybook(s) from Bank-of-Z'
    )
  })
})
