import type { Meta, StoryObj } from '@storybook/react-vite'
import { ErrorDisplay } from './ErrorDisplay'
import { I18nextProvider } from 'react-i18next'
import i18n from '../../i18n'

const meta: Meta<typeof ErrorDisplay> = {
  title: 'UIComponent/ErrorDisplay',
  component: ErrorDisplay,
  decorators: [
    (Story) => (
      <I18nextProvider i18n={i18n}>
        <div className="max-w-md p-4 bg-white">
          <Story />
        </div>
      </I18nextProvider>
    ),
  ],
}

export default meta

export const SimpleError: StoryObj = {
  args: {
    error: {
      message: 'Something went wrong on the server.',
    },
  },
}

export const ErrorWithStackTrace: StoryObj = {
  args: {
    error: {
      message: 'Unexpected Exception',
      stackTrace:
        'java.lang.NullPointerException: Cannot invoke "String.toLowerCase()" because "input" is null\n  at com.example.Service.process(Service.java:42)\n  at com.example.Controller.handle(Controller.java:15)',
    },
  },
}
