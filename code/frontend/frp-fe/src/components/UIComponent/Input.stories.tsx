import type { Meta, StoryObj } from '@storybook/react-vite'
import { InputText, InputEmail, InputPassword } from './Input'
import { I18nextProvider } from 'react-i18next'
import i18n from '../../i18n'

const meta: Meta = {
  title: 'UIComponent/Input',
  component: InputText,
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

export const AllInputs: StoryObj = {
  render: () => (
    <div className="flex flex-col gap-4">
      <InputText
        id="text"
        name="text"
        labelTranslationKey="profile.fullName"
        placeholderTranslationKey="profile.fullName"
      />
      <InputEmail />
      <InputPassword />
    </div>
  ),
}
