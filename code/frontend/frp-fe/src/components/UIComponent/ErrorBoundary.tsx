import React, { Component, ErrorInfo, ReactNode } from 'react'
import { ErrorDisplay } from './ErrorDisplay'
import { ApiError } from '../../api/core/ApiError'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  }

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo)
  }

  public render() {
    if (this.state.hasError) {
      // Create a fake ApiError to reuse ErrorDisplay
      const apiError: ApiError = {
        name: 'UI Error',
        message: this.state.error?.message || 'Unknown error',
        stack: this.state.error?.stack,
      } as any

      return (
        <div className="p-4 border border-red-200 rounded bg-red-50">
          <h3 className="text-red-700 font-bold mb-2">Something went wrong</h3>
          <ErrorDisplay error={apiError} />
        </div>
      )
    }

    return this.props.children
  }
}
