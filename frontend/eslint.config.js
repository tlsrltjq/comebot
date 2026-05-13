import js from '@eslint/js';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

const restrictedSecretNames = [
  'accessKey',
  'accessToken',
  'apiKey',
  'botToken',
  'chatId',
  'password',
  'secret',
  'secretKey',
  'token',
];

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'node_modules'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['src/**/*.{ts,tsx}'],
    languageOptions: {
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      'no-restricted-globals': [
        'error',
        {
          name: 'localStorage',
          message: 'Do not store trading or notification state in browser localStorage.',
        },
        {
          name: 'sessionStorage',
          message: 'Do not store trading or notification state in browser sessionStorage.',
        },
      ],
      'id-denylist': ['error', ...restrictedSecretNames],
      'no-restricted-syntax': [
        'error',
        {
          selector: 'Literal[value="REAL_TRADING"]',
          message: 'The web UI must not add or expose REAL_TRADING.',
        },
        {
          selector: 'TemplateElement[value.raw=/REAL_TRADING/]',
          message: 'The web UI must not add or expose REAL_TRADING.',
        },
        {
          selector: 'CallExpression[callee.object.name="api"][callee.property.name="executeCandidate"]',
          message: 'The web UI must not call candidate execution from React.',
        },
        {
          selector: 'CallExpression[callee.object.name="api"][callee.property.name="runTradingFlow"]',
          message: 'The web UI must not call trading execution from React.',
        },
      ],
    },
  },
  {
    files: ['src/**/*.{ts,tsx}'],
    ignores: ['src/**/*.test.{ts,tsx}'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: 'Literal[value=/\\/api\\/candidates\\/execute/]',
          message: 'The web UI must not add candidate execution endpoints.',
        },
        {
          selector: 'TemplateElement[value.raw=/\\/api\\/candidates\\/execute/]',
          message: 'The web UI must not add candidate execution endpoints.',
        },
        {
          selector: 'Literal[value=/\\/api\\/trading-flow\\/run/]',
          message: 'The web UI must not add trading execution endpoints.',
        },
        {
          selector: 'TemplateElement[value.raw=/\\/api\\/trading-flow\\/run/]',
          message: 'The web UI must not add trading execution endpoints.',
        },
        {
          selector: 'Literal[value=/\\/api\\/portfolio\\/positions\\/(buy|buy-selected|manual-buy)/]',
          message: 'The web UI must not add manual BUY endpoints.',
        },
        {
          selector: 'TemplateElement[value.raw=/\\/api\\/portfolio\\/positions\\/(buy|buy-selected|manual-buy)/]',
          message: 'The web UI must not add manual BUY endpoints.',
        },
      ],
    },
  },
  {
    files: ['src/**/*.{ts,tsx}'],
    ignores: ['src/shared/api/client.ts'],
    rules: {
      'no-restricted-globals': [
        'error',
        {
          name: 'fetch',
          message: 'Use shared/api/client.ts instead of direct fetch calls.',
        },
        {
          name: 'localStorage',
          message: 'Do not store trading or notification state in browser localStorage.',
        },
        {
          name: 'sessionStorage',
          message: 'Do not store trading or notification state in browser sessionStorage.',
        },
      ],
    },
  },
  {
    files: ['*.config.{js,ts}', 'eslint.config.js'],
    rules: {
      'id-denylist': 'off',
      'no-restricted-syntax': 'off',
    },
  },
);
