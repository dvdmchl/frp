import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import Footer from './Footer'

describe('Footer Component', () => {
  it('renders correctly', () => {
    render(<Footer />)
    const currentYear = new Date().getFullYear().toString()
    expect(screen.getByText((content: string) => content.includes(currentYear))).toBeInTheDocument()
    expect(screen.getByText(/Family Resource Planner/i)).toBeInTheDocument()
  })
})
