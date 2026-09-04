import { Link, useLocation } from 'react-router-dom'

const navigation = [
  { name: 'Dashboard', href: '/', icon: '📊' },
  { name: 'Projects', href: '/projects', icon: '📁' },
  { name: 'Wizard', href: '/wizard', icon: '🧙' },
  { name: 'Profiles & Policies', href: '/reusable-assets', icon: '◈' },
]

function Sidebar() {
  const location = useLocation()

  return (
    <div className="sidebar">
      <div className="p-4">
        <h1 className="text-xl font-bold">Renovatio</h1>
        <p className="text-sm text-gray-400">Migration Management</p>
      </div>
      <nav className="mt-4">
        {navigation.map((item) => (
          <Link
            key={item.name}
            to={item.href}
            className={`sidebar-link ${location.pathname === item.href ? 'active' : ''}`}
          >
            <span className="mr-2">{item.icon}</span>
            {item.name}
          </Link>
        ))}
      </nav>
    </div>
  )
}

export default Sidebar
