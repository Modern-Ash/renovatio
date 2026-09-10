import React from 'react'
import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import CobolTooltip from '../CobolTooltip'

describe('CobolTooltip', () => {
  it('renders children', () => {
    const { container } = render(
      <CobolTooltip term="COPYBOOK">Test content</CobolTooltip>
    )
    expect(container.textContent).toContain('Test content')
  })
})
