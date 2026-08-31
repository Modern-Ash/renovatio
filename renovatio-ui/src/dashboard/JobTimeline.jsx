const statusColors = {
  COMPLETED: 'bg-green-500',
  RUNNING: 'bg-blue-500',
  PENDING: 'bg-yellow-500',
  FAILED: 'bg-red-500'
};

function JobTimeline({ jobs }) {
  return (
    <div className="card">
      <h3 className="font-semibold mb-4">Job Timeline</h3>
      {jobs.length === 0 ? (
        <p className="text-gray-500">No jobs yet</p>
      ) : (
        <div className="space-y-3">
          {jobs.map(job => (
            <div key={job.id} className="flex items-center gap-3">
              <div className={`w-3 h-3 rounded-full ${statusColors[job.status] || 'bg-gray-400'}`} />
              <div className="flex-1">
                <p className="font-medium">{job.operation}</p>
                <p className="text-sm text-gray-500">
                  {new Date(job.createdAt).toLocaleString()}
                </p>
              </div>
              <span className="text-sm text-gray-600">{job.status}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default JobTimeline
