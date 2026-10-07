import { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../../../api/core/ApiError'
import type { AccAccountDto } from '../../../api/models/AccAccountDto'
import type { AccNodeDto } from '../../../api/models/AccNodeDto'
import type { ErrorDto } from '../../../api/models/ErrorDto'

export const flattenNodes = (nodes: AccNodeDto[]): AccNodeDto[] =>
  nodes.flatMap((node) => [node, ...flattenNodes(node.children ?? [])])

export const flattenAccounts = (nodes: AccNodeDto[]): AccAccountDto[] =>
  flattenNodes(nodes).flatMap((node) => (node.account ? [node.account] : []))

export const toErrorDto = (error: unknown, fallbackMessage: string): ErrorDto =>
  error instanceof ApiError && error.errorDto ? error.errorDto : { message: fallbackMessage }

export const formatDateTime = (value: string | undefined) => (value ? new Date(value).toLocaleString() : '—')

/**
 * Runs API calls with a shared busy flag and turns their failures into an ErrorDto for ErrorDisplay.
 * `run` resolves to true when the action succeeded.
 */
export const useApiAction = (fallbackMessageKey: string) => {
  const { t } = useTranslation()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<ErrorDto | null>(null)

  const run = useCallback(
    async (action: () => Promise<unknown>): Promise<boolean> => {
      setBusy(true)
      setError(null)
      try {
        await action()
        return true
      } catch (caughtError) {
        setError(toErrorDto(caughtError, t(fallbackMessageKey)))
        return false
      } finally {
        setBusy(false)
      }
    },
    [fallbackMessageKey, t],
  )

  return { busy, error, run }
}
