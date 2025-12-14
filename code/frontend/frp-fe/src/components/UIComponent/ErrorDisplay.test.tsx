import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ErrorDisplay } from './ErrorDisplay';
import type { ErrorDto } from '../../api/models/ErrorDto';

// Mock translations
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => {
        if (key === 'error.unknown') return 'Unknown error';
        if (key === 'error.showStackTrace') return 'Show Stack Trace';
        if (key === 'error.hideStackTrace') return 'Hide Stack Trace';
        return key;
    },
  }),
}));

describe('ErrorDisplay Component', () => {
  it('renders nothing when error is null', () => {
    const { container } = render(<ErrorDisplay error={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders error message', () => {
    const error: ErrorDto = { message: 'Something went wrong' };
    render(<ErrorDisplay error={error} />);
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('renders unknown error when message is missing', () => {
     // @ts-ignore - explicitly testing partial object
    const error: ErrorDto = { stackTrace: 'trace' };
    render(<ErrorDisplay error={error} />);
    expect(screen.getByText('Unknown error')).toBeInTheDocument();
  });

  it('toggles stack trace visibility', () => {
    const error: ErrorDto = {
        message: 'Error',
        stackTrace: 'Error details...',
    };
    render(<ErrorDisplay error={error} />);

    // Initially hidden
    expect(screen.queryByText('Error details...')).not.toBeInTheDocument();

    // Click show
    const button = screen.getByRole('button', { name: 'Show Stack Trace' });
    fireEvent.click(button);

    // Now visible
    expect(screen.getByText('Error details...')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hide Stack Trace' })).toBeInTheDocument();

    // Click hide
    fireEvent.click(button);
    expect(screen.queryByText('Error details...')).not.toBeInTheDocument();
  });
});
