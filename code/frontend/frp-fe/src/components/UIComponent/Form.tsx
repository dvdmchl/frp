import React from 'react'

type FormProps = React.FormHTMLAttributes<HTMLFormElement> & {
  children: React.ReactNode
}

export function Form({ children, className = '', ...props }: FormProps) {
  return (
    <form
      className={`max-w-md mx-auto mt-12 p-8 bg-bgForm rounded-2xl shadow-2xl flex flex-col gap-6 ${className}`}
      {...props}
    >
      {children}
    </form>
  )
}
