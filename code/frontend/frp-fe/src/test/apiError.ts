import { ApiError } from '../api/core/ApiError'

export const apiError = (message: string, status = 400) =>
  new ApiError(
    { method: 'GET', url: '/api' } as never,
    { url: '/api', ok: false, status, statusText: 'Error', body: { message } },
    message,
  )
