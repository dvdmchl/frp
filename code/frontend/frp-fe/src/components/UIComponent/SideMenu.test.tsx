import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { SideMenu } from './SideMenu'

describe('SideMenu Component', () => {
  const items = [
    { key: 'item1', label: 'Item 1' },
    { key: 'item2', label: 'Item 2' },
    { key: 'item3', label: 'Item 3' },
  ]

  it('renders all items', () => {
    render(<SideMenu items={items} selected="item1" onSelect={() => {}} />)
    items.forEach((item) => {
      expect(screen.getByText(item.label)).toBeInTheDocument()
    })
  })

  it('highlights selected item', () => {
    render(<SideMenu items={items} selected="item2" onSelect={() => {}} />)
    const selectedItem = screen.getByText('Item 2')
    const unselectedItem = screen.getByText('Item 1')

    expect(selectedItem).toHaveClass('bg-primary')
    expect(unselectedItem).not.toHaveClass('bg-primary')
  })

  it('calls onSelect when item is clicked', () => {
    const handleSelect = vi.fn()
    render(<SideMenu items={items} selected="item1" onSelect={handleSelect} />)

    fireEvent.click(screen.getByText('Item 3'))
    expect(handleSelect).toHaveBeenCalledWith('item3')
  })
})
