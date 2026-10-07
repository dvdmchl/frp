import { act, renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { apiError } from '../../../test/apiError'
import { flattenAccounts, flattenNodes, formatDateTime, toErrorDto, useApiAction } from './accountingUtils'

const translate = (key: string) => key
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: translate }),
}))

const tree = [
  {
    id: 1,
    isPlaceholder: true,
    account: { id: 10, name: 'Assets' },
    children: [{ id: 2, account: { id: 20, name: 'Cash' } }],
  },
  { id: 3 },
]

describe('accountingUtils', () => {
  it('flattens nested nodes depth first', () => {
    expect(flattenNodes(tree).map((node) => node.id)).toEqual([1, 2, 3])
  })

  it('flattens accounts and skips nodes without an account', () => {
    expect(flattenAccounts(tree).map((account) => account.name)).toEqual(['Assets', 'Cash'])
  })

  it('uses the error dto of an api error', () => {
    expect(toErrorDto(apiError('Rejected'), 'fallback')).toEqual({ message: 'Rejected' })
  })

  it('uses the fallback message for other errors', () => {
    expect(toErrorDto(new Error('boom'), 'fallback')).toEqual({ message: 'fallback' })
  })

  it('shows a dash for a missing date time', () => {
    expect(formatDateTime(undefined)).toBe('—')
  })

  it('formats a date time in the local format', () => {
    const value = '2026-10-01T08:30:00Z'
    expect(formatDateTime(value)).toBe(new Date(value).toLocaleString())
  })

  it('reports success of an action', async () => {
    const { result } = renderHook(() => useApiAction('fallback.key'))

    let succeeded = false
    await act(async () => {
      succeeded = await result.current.run(() => Promise.resolve())
    })

    expect(succeeded).toBe(true)
    expect(result.current.error).toBeNull()
    expect(result.current.busy).toBe(false)
  })

  it('keeps the error of a failed action', async () => {
    const { result } = renderHook(() => useApiAction('fallback.key'))

    let succeeded = true
    await act(async () => {
      succeeded = await result.current.run(() => Promise.reject(new Error('boom')))
    })

    expect(succeeded).toBe(false)
    expect(result.current.error).toEqual({ message: 'fallback.key' })
  })
})
