import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { HeaderTitle, H1Title, H2Title, TextError, TextSuccess, Paragraph, LinkText } from './Text'

describe('Text Components', () => {
  it('HeaderTitle renders children correctly', () => {
    render(<HeaderTitle>Header Content</HeaderTitle>)
    const element = screen.getByRole('heading', { level: 1 })
    expect(element).toHaveTextContent('Header Content')
    expect(element).toHaveClass('text-textHeaderTitle')
  })

  it('H1Title renders children correctly', () => {
    render(<H1Title>H1 Content</H1Title>)
    const element = screen.getByRole('heading', { level: 1 })
    expect(element).toHaveTextContent('H1 Content')
    expect(element).toHaveClass('text-3xl')
  })

  it('H2Title renders children correctly', () => {
    render(<H2Title>H2 Content</H2Title>)
    const element = screen.getByRole('heading', { level: 2 })
    expect(element).toHaveTextContent('H2 Content')
    expect(element).toHaveClass('text-3xl')
  })

  it('TextError renders message correctly', () => {
    render(<TextError message="Error occurred" />)
    const element = screen.getByText('Error occurred')
    expect(element).toHaveClass('text-textError')
  })

  it('TextSuccess renders message correctly', () => {
    render(<TextSuccess message="Success!" />)
    const element = screen.getByText('Success!')
    expect(element).toHaveClass('text-green-600')
  })

  it('Paragraph renders children correctly', () => {
    render(<Paragraph>Paragraph text</Paragraph>)
    const element = screen.getByText('Paragraph text')
    expect(element.tagName).toBe('P')
    expect(element).toHaveClass('text-textPrimary')
  })

  describe('LinkText', () => {
    it('renders internal link using React Router', () => {
      render(
        <MemoryRouter>
          <LinkText to="/internal">Internal Link</LinkText>
        </MemoryRouter>,
      )
      const link = screen.getByRole('link', { name: 'Internal Link' })
      expect(link).toHaveAttribute('href', '/internal')
      expect(link).toHaveClass('text-textLink')
    })

    it('renders external link correctly', () => {
      render(<LinkText href="https://example.com">External Link</LinkText>)
      const link = screen.getByRole('link', { name: /External Link/i })
      expect(link).toHaveAttribute('href', 'https://example.com')
      expect(link).toHaveAttribute('target', '_blank')
      expect(link).toHaveAttribute('rel', 'noopener noreferrer')
      expect(link).toHaveTextContent('↗') // Checks for the icon arrow
    })

    it('renders span when no to or href is provided', () => {
      render(<LinkText>Just Text</LinkText>)
      const element = screen.getByText('Just Text')
      expect(element.tagName).toBe('SPAN')
      expect(element).toHaveClass('text-textLink')
    })
  })
})
