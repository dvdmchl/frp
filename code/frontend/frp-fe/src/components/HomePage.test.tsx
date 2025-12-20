import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { HomePage } from './HomePage'
import type { UserDto } from '../api/models/UserDto'

describe('HomePage Component', () => {
  it('renders welcome message with user name', () => {
    const user: UserDto = { id: 1, email: 'test@example.com', fullName: 'Test User', activeSchema: 'schema1' }
    render(<HomePage user={user} />)

    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Vítej, Test User!')
    expect(screen.getByText('Tohle je základní rozhraní pro přihlášeného uživatele.')).toBeInTheDocument()
  })
})
