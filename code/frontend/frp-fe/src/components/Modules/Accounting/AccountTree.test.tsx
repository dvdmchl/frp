import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { AccountTree } from './AccountTree'
import { AccountingService } from '../../../api/services/AccountingService'
import { vi, describe, it, expect, beforeEach } from 'vitest'

// Mock services
vi.mock('../../../api/services/AccountingService')
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}))

// Mock Flowbite components to simplify DOM
vi.mock('flowbite-react', async () => {
  // const actual = await vi.importActual('flowbite-react') // Don't use actual to avoid issues
  function ModalMock({ show, onClose, children }: any) {
    return show ? (
      <div role="dialog">
        <button onClick={onClose}>Close</button>
        {children}
      </div>
    ) : null
  }
  ModalMock.displayName = 'ModalMock'
  function ModalMockBody({ children }: any) {
    return <div>{children}</div>
  }
  ModalMockBody.displayName = 'ModalMockBody'
  ;(ModalMock as any).Body = ModalMockBody

  return {
    Modal: ModalMock,
    ModalBody: ModalMockBody,
    Button: (props: any) => <button {...props} />,
    Spinner: () => <div>Spinner</div>,
    Checkbox: (props: any) => <input type="checkbox" {...props} />,
    Label: ({ value, children, ...props }: any) => <label {...props}>{value || children}</label>,
    Select: (props: any) => <select {...props} />,
    Textarea: (props: any) => <textarea {...props} />,
  }
})

describe('AccountTree', () => {
  const mockTree = [
    {
      id: 1,
      account: { id: 1, name: 'Assets', accountType: 'ASSET' },
      children: [
        {
          id: 2,
          parentId: 1,
          account: { id: 2, name: 'Cash', accountType: 'ASSET', currencyCode: 'USD', balance: 100 },
          children: [],
        },
      ],
    },
  ]

  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(AccountingService.getTree).mockResolvedValue(mockTree as any)
    vi.mocked(AccountingService.getAllCurrencies).mockResolvedValue([])
  })

  it('renders tree correctly', async () => {
    render(<AccountTree />)

    await waitFor(() => {
      expect(screen.getByText('Assets')).toBeInTheDocument()
      expect(screen.getByText('Cash')).toBeInTheDocument()
    })
  })

  it('opens create modal on add button click', async () => {
    render(<AccountTree />)
    await waitFor(() => expect(screen.getByText('Assets')).toBeInTheDocument())

    // Find the main "Create" button. It text is 'account.create'.
    // But there might be multiple buttons with same text if I reused text key?
    // In AccountTree, main button is 'account.create', submit button in form is 'account.create'.
    // But form is closed.

    fireEvent.click(screen.getByText('account.create'))

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText('account.createTitle')).toBeInTheDocument()
    })
  })

  it('calls delete API on delete click', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<AccountTree />)
    await waitFor(() => expect(screen.getByText('Assets')).toBeInTheDocument())

    const deleteButtons = screen.getAllByText('X')
    fireEvent.click(deleteButtons[0])

    await waitFor(() => {
      expect(AccountingService.deleteAccount).toHaveBeenCalledWith(1)
    })
  })
})
