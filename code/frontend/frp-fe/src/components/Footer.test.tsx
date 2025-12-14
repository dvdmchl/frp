import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import Footer from './Footer';

describe('Footer Component', () => {
  it('renders current year and copyright text', () => {
    render(<Footer />);
    const currentYear = new Date().getFullYear().toString();
    expect(screen.getByText((content) => content.includes(currentYear))).toBeInTheDocument();
    expect(screen.getByText(/Family Resource Planner/)).toBeInTheDocument();
  });
});
