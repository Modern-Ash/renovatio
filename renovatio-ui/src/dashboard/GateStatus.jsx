const gateColors = {
  passed: 'bg-green-100 text-green-800',
  failed: 'bg-red-100 text-red-800',
  pending: 'bg-yellow-100 text-yellow-800',
  skipped: 'bg-gray-100 text-gray-800'
};

function GateStatus({ program, gates }) {
  return (
    <div className="card">
      <h3 className="font-semibold mb-3">{program}</h3>
      <div className="flex gap-2">
        {Object.entries(gates).map(([gate, status]) => (
          <span
            key={gate}
            className={`badge ${gateColors[status] || gateColors.pending}`}
          >
            {gate}: {status}
          </span>
        ))}
      </div>
    </div>
  )
}

export default GateStatus
