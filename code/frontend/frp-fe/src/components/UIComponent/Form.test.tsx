import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Form } from './Form'

describe('Form Component', () => {
  it('renders children correctly', () => {
    render(
      <Form data-testid="test-form">
        <input type="text" />
        <button>Submit</button>
      </Form>,
    )

    const form = screen.getByTestId('test-form')
    expect(form).toBeInTheDocument()
    expect(screen.getByRole('button')).toBeInTheDocument()
  })

  it('applies default classes', () => {
    render(<Form data-testid="test-form">Content</Form>)
    const form = screen.getByTestId('test-form')
    expect(form).toHaveClass('max-w-md', 'mx-auto', 'bg-bgForm')
  })

  it('accepts additional className', () => {
    render(
      <Form className="custom-class" data-testid="test-form">
        Content
      </Form>,
    )
    const form = screen.getByTestId('test-form')
    expect(form).toHaveClass('custom-class')
    // Should still have default classes
    expect(form).toHaveClass('bg-bgForm')
  })

  it('passes other props to the form element', () => {
    const handleSubmit = () => {}
    render(
      <Form onSubmit={handleSubmit} data-testid="test-form">
        Content
      </Form>,
    )
    // Note: We can't easily check the function prop equality, but we can check attributes
    // However, onSubmit is an event handler. Let's check a standard attribute.
    render(
      <Form action="/submit" data-testid="test-form-action">
        Content
      </Form>,
    )
    expect(screen.getByTestId('test-form-action')).toHaveAttribute('action', '/submit')
  })
})
