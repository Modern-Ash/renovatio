import { useState } from 'react'
import { describe, it, expect, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import StepFolder from '../StepFolder'

function Harness() {
  const [data, setData] = useState({ workspacePath: '' })

  return (
    <StepFolder
      data={data}
      onChange={(next) => setData((current) => ({ ...current, ...next }))}
      onNext={vi.fn()}
    />
  )
}

function makeSelectedFile(name, contents, relativePath) {
  const file = new File([contents], name)
  Object.defineProperty(file, 'webkitRelativePath', {
    value: relativePath
  })
  return file
}

describe('StepFolder', () => {
  it('accepts a folder that contains COBOL source files', async () => {
    const { container } = render(<Harness />)

    const folderInput = container.querySelector('input[type="file"]')
    const files = [
      makeSelectedFile(
        'sample.cbl',
        '       IDENTIFICATION DIVISION.\n       PROGRAM-ID. SAMPLE.\n',
        'demo-workspace/src/cbl/sample.cbl'
      ),
      makeSelectedFile(
        'copybook.cpy',
        '       01  SAMPLE-ITEM PIC X(10).\n',
        'demo-workspace/includes/copybook.cpy'
      )
    ]

    fireEvent.change(folderInput, {
      target: { files }
    })

    expect(
      await screen.findByText(/Found 1 COBOL program file\(s\) and 1 copybook\(s\)\./i)
    ).toBeTruthy()
    expect(
      await screen.findByText(/Folder browsing scans the selected folder and all its subdirectories/i)
    ).toBeTruthy()
    expect(screen.getByPlaceholderText('/path/to/cobol/workspace').value).toBe('')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Next: Analyze/i }).disabled).toBe(false)
    })
  })

  it('rejects a folder that does not look like COBOL workspace', async () => {
    const { container } = render(<Harness />)

    const folderInput = container.querySelector('input[type="file"]')
    const file = makeSelectedFile('notes.txt', 'hello world', 'demo-workspace/notes.txt')

    fireEvent.change(folderInput, {
      target: { files: [file] }
    })

    expect(
      await screen.findByText(/No COBOL files found/i)
    ).toBeTruthy()

    expect(screen.getByRole('button', { name: /Next: Analyze/i }).disabled).toBe(true)
  })
})
