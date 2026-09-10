import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Projects from './pages/Projects'
import ProjectDetail from './pages/ProjectDetail'
import Wizard from './pages/Wizard'
import ProfilesPolicies from './pages/ProfilesPolicies'

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/projects" element={<Projects />} />
          <Route path="/projects/:id" element={<ProjectDetail />} />
          <Route path="/wizard" element={<Wizard />} />
          <Route path="/wizard/:projectId" element={<Wizard />} />
          <Route path="/reusable-assets" element={<ProfilesPolicies />} />
        </Routes>
      </Layout>
    </Router>
  )
}

export default App
