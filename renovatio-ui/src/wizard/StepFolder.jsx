import { useState } from 'react'

function StepFolder({ data, onChange, onNext }) {
  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Select COBOL Workspace</h2>
      <p className="text-gray-600 mb-4">
        Enter the path to your COBOL workspace folder.
      </p>
      <input
        type="text"
        placeholder="/path/to/cobol/workspace"
        value={data.workspacePath || ''}
        onChange={(e) => onChange({ workspacePath: e.target.value })}
        className="input mb-4"
      />
      <div className="flex justify-end">
        <button
          onClick={onNext}
          disabled={!data.workspacePath}
          className="btn btn-primary"
        >
          Next: Analyze →
        </button>
      </div>
    </div>
  )
}

export default StepFolder
