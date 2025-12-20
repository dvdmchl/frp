import React from 'react'
import { Link } from 'react-router-dom'

export function HeaderTitle({ children }: Readonly<{ children: React.ReactNode }>) {
  return <h1 className="text-lg font-bold text-left text-textHeaderTitle">{children}</h1>
}

export function H1Title({ children }: Readonly<{ children: React.ReactNode }>) {
  return <h1 className="text-3xl font-extrabold text-center text-textPrimary mb-4">{children}</h1>
}

export function H2Title({ children }: Readonly<{ children: React.ReactNode }>) {
  return <h2 className="text-3xl font-extrabold text-center text-textPrimary mb-4">{children}</h2>
}

export function TextError({ message }: Readonly<{ message: string }>) {
  return <div className="text-textError text-sm text-center my-2">{message}</div>
}

export function TextSuccess({ message }: Readonly<{ message: string }>) {
  return <div className="text-green-600 text-sm text-center my-2">{message}</div>
}

export function Paragraph({ children }: Readonly<{ children: React.ReactNode }>) {
  return <p className="text-textPrimary text-base leading-relaxed mb-4">{children}</p>
}

type LinkTextProps = {
  children: React.ReactNode
  to?: string // Pro vnitřní route (react-router)
  href?: string // Pro externí odkaz
  className?: string
}

export function LinkText({ children, to, href, className = '' }: Readonly<LinkTextProps>) {
  const baseClasses = 'text-textLink hover:underline cursor-pointer'

  if (to) {
    // Interní odkaz – react-router Link
    return (
      <Link to={to} className={`${baseClasses} ${className}`}>
        {children}
      </Link>
    )
  }

  if (href) {
    // Externí odkaz – standardní a s ikonou
    return (
      <a
        href={href}
        target="_blank"
        rel="noopener noreferrer"
        className={`${baseClasses} underline decoration-dotted text-secondary`}
      >
        {children} <span aria-hidden="true">↗</span>
      </a>
    )
  }

  return <span className={baseClasses}>{children}</span>
}
