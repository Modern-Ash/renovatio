import { useState, useEffect } from 'react'
import { getActionItems, updateActionItemStatus } from '../api/client'

const severityColors = {
  CRITICAL: 'badge-critical',
  HIGH: 'badge-high',
  MEDIUM: 'badge-medium',
  LOW: 'badge-low'
};

function StepReview({ projectId, data, onNext, onBack }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  const fetchItems = async () => {
    try {
      const data = await getActionItems(projectId || 'default')
      setItems(data)
    } catch (error) {
      console.error('Failed to fetch action items:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchItems()
  }, [projectId])

  const handleStatusChange = async (id, status) => {
    try {
      await updateActionItemStatus(id, status)
      fetchItems()
    } catch (error) {
      console.error('Failed to update status:', error)
    }
  }

  if (loading) {
    return <div className="text-center p-8">Loading action items...</div>
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Review Action Items</h2>
      
      {items.length === 0 ? (
        <div className="card text-center py-8">
          <p className="text-gray-500">No action items to review</p>
        </div>
      ) : (
        <div className="space-y-3 mb-6">
          {items.map(item => (
            <div key={item.id} className="border rounded-lg p-4">
              <div className="flex items-start justify-between">
                <div>
                  <span className={`badge ${severityColors[item.severity] || 'badge-medium'}`}>
                    {item.severity}
                  </span>
                  <p className="mt-2 text-gray-700">{item.reason}</p>
                  {item.requiredHumanAction && (
                    <p className="mt-1 text-sm text-gray-500">
                      Action: {item.requiredHumanAction}
                    </p>
                  )}
                </div>
                {item.reviewStatus === 'PENDING' && (
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleStatusChange(item.id, 'ACCEPTED')}
                      className="btn btn-success text-sm"
                    >
                      Accept
                    </button>
                    <button
                      onClick={() => handleStatusChange(item.id, 'REJECTED')}
                      className="btn btn-danger text-sm"
                    >
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
          ))}
        </div>
      )}

      <div className="flex justify-between">
        <button onClick={onBack} className="btn btn-secondary">
          ← Back
        </button>
        <button onClick={onNext} className="btn btn-primary">
          Next: Export →
        </button>
      </div>
    </div>
  )
}

export default StepReview
