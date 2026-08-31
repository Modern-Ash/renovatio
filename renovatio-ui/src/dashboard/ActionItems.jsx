import { updateActionItemStatus } from '../api/client'

const severityColors = {
  CRITICAL: 'badge-critical',
  HIGH: 'badge-high',
  MEDIUM: 'badge-medium',
  LOW: 'badge-low'
};

function ActionItem({ item, onStatusChange }) {
  const handleAccept = async () => {
    await updateActionItemStatus(item.id, 'ACCEPTED');
    onStatusChange();
  };

  const handleReject = async () => {
    await updateActionItemStatus(item.id, 'REJECTED');
    onStatusChange();
  };

  return (
    <div className="border rounded-lg p-4 mb-3">
      <div className="flex items-start justify-between">
        <div>
          <span className={`badge ${severityColors[item.severity] || 'badge-medium'}`}>
            {item.severity}
          </span>
          <p className="mt-2 text-sm text-gray-700">{item.reason}</p>
          {item.requiredHumanAction && (
            <p className="mt-1 text-xs text-gray-500">
              Action: {item.requiredHumanAction}
            </p>
          )}
        </div>
        {item.reviewStatus === 'PENDING' && (
          <div className="flex gap-2">
            <button onClick={handleAccept} className="btn btn-success text-sm">
              Accept
            </button>
            <button onClick={handleReject} className="btn btn-danger text-sm">
              Reject
            </button>
          </div>
        )}
        {item.reviewStatus !== 'PENDING' && (
          <span className={`badge ${item.reviewStatus === 'ACCEPTED' ? 'badge-low' : 'badge-critical'}`}>
            {item.reviewStatus}
          </span>
        )}
      </div>
    </div>
  )
}

function ActionItems({ items, onStatusChange }) {
  return (
    <div className="card">
      <h3 className="font-semibold mb-4">Action Items</h3>
      {items.length === 0 ? (
        <p className="text-gray-500">No action items</p>
      ) : (
        <div className="space-y-3">
          {items.map(item => (
            <ActionItem key={item.id} item={item} onStatusChange={onStatusChange} />
          ))}
        </div>
      )}
    </div>
  )
}

export default ActionItems
