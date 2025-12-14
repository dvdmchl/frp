import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { InputText, InputEmail, InputPassword } from './Input';

// Mock react-i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

describe('InputText', () => {
  it('renders with label and placeholder', () => {
    render(<InputText id="test-input" name="test" labelTranslationKey="label.key" placeholderTranslationKey="placeholder.key" />);
    
    expect(screen.getByLabelText('label.key')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('placeholder.key')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveAttribute('type', 'text');
  });

  it('forwards ref', () => {
    const ref = React.createRef<HTMLInputElement>();
    render(<InputText id="test-ref" name="test-ref" labelTranslationKey="label" placeholderTranslationKey="place" ref={ref} />);
    expect(ref.current).toBeInstanceOf(HTMLInputElement);
  });
});

describe('InputEmail', () => {
  it('renders email input', () => {
    render(<InputEmail />);
    expect(screen.getByLabelText('login.email')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveAttribute('type', 'email');
  });
});

describe('InputPassword', () => {
  it('renders password input', () => {
    const { container } = render(<InputPassword />);
    expect(screen.getByLabelText('login.password')).toBeInTheDocument();
    const input = container.querySelector('input[type="password"]');
    expect(input).toBeInTheDocument();
  });
});
