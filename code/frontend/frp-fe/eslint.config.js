import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import react from 'eslint-plugin-react'
import prettier from 'eslint-plugin-prettier'
import configPrettier from 'eslint-config-prettier'

export default tseslint.config(
  {
    ignores: ['dist', 'coverage', 'src/api'],
  },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      'react': react,
      'prettier': prettier,
    },
    settings: {
      react: {
        version: 'detect',
      },
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      ...react.configs.recommended.rules,
      ...react.configs['jsx-runtime'].rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      'prettier/prettier': 'error',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      '@typescript-eslint/no-explicit-any': 'warn',
      'react/prop-types': 'off',

      // Prevent inline styles and magic strings for routes
      'no-restricted-syntax': [
        'error',
        {
          selector: 'JSXAttribute[name.name="style"]',
          message: 'Inline styles are not allowed. Use Tailwind CSS or UIComponent library.',
        },
        {
          selector: 'JSXAttribute[name.name="path"] > Literal',
          message: 'Magic string literals in Route paths are not allowed. Use a constant.',
        },
        {
          selector: 'JSXAttribute[name.name="to"] > Literal',
          message: 'Magic string literals in Navigate or Link "to" props are not allowed. Use a constant.',
        },
        {
          selector: 'CallExpression[callee.name="navigate"] > Literal',
          message: 'Magic string literals in navigate() calls are not allowed. Use a constant.',
        },
      ],
    },
  },
  {
    files: ['**/*.test.{ts,tsx}', 'src/test/**/*'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: 'JSXAttribute[name.name="style"]',
          message: 'Inline styles are not allowed. Use Tailwind CSS or UIComponent library.',
        },
      ],
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },
  configPrettier,
)
