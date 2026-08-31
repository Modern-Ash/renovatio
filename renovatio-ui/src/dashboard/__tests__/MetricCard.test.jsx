import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import MetricCard from '../MetricCard'

describe('MetricCard', () => {
  it('renders without crashing', () => {
    const { container } = render(
      <MetricCard title="Lines of Code" value="1234" icon="📝" />
    )
    expect(container.innerHTML).toContain('Lines of Code')
  })
})
