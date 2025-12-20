import type { Meta, StoryObj } from '@storybook/react-vite'
import { Form } from './Form'
import { InputText, InputEmail } from './Input'
import { Button } from 'flowbite-react'
import { I18nextProvider } from 'react-i18next'
import i18n from '../../i18n'

const meta: Meta = {
  title: 'UIComponent/Form',
  decorators: [
    (Story) => (
      <I18nextProvider i18n={i18n}>
        <Story />
      </I18nextProvider>
    ),
  ],
}

export default meta

export const ExampleForm: StoryObj = {
  render: () => (
    <Form onSubmit={(e) => e.preventDefault()}>
      <InputText
        id="name"
        name="name"
        labelTranslationKey="profile.fullName"
        placeholderTranslationKey="profile.fullName"
      />
      <InputEmail />
      <Button type="submit">Submit</Button>
    </Form>
  ),
}
