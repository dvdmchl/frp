import type { Meta, StoryObj } from '@storybook/react-vite'
import { HeaderTitle, H1Title, H2Title, Paragraph, LinkText, TextError, TextSuccess } from './Text'
import { MemoryRouter } from 'react-router-dom'
import { Paths } from '../../constants/Paths'

const meta: Meta = {
  title: 'UIComponent/Text',
  decorators: [
    (Story) => (
      <MemoryRouter>
        <Story />
      </MemoryRouter>
    ),
  ],
}

export default meta

export const AllTexts: StoryObj = {
  render: () => (
    <div className="flex flex-col gap-4">
      <HeaderTitle>Header Title</HeaderTitle>
      <H1Title>H1 Title</H1Title>
      <H2Title>H2 Title</H2Title>
      <Paragraph>This is a paragraph of text.</Paragraph>
      <LinkText to={Paths.HOME}>Internal Link</LinkText>
      <LinkText href="https://google.com">External Link</LinkText>
      <TextError message="This is an error message" />
      <TextSuccess message="This is a success message" />
    </div>
  ),
}
