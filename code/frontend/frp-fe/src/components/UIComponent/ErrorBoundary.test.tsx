import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

const Thrower = ({ error }: { error: Error }) => {
  throw error
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders children when nothing throws', () => {
    render(
      <ErrorBoundary>
        <span>content</span>
      </ErrorBoundary>,
    )

    expect(screen.getByText('content')).toBeInTheDocument()
  })

  it('shows the error message when a child throws', () => {
    render(
      <ErrorBoundary>
        <Thrower error={new Error('Broken tree')} />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    expect(screen.getByText('Broken tree')).toBeInTheDocument()
  })

  it('shows a fallback message when the thrown error has no message', () => {
    render(
      <ErrorBoundary>
        <Thrower error={new Error('')} />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Unknown error')).toBeInTheDocument()
  })
})
