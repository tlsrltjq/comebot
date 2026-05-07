import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import { App } from './App';

function renderApp(initialEntry = '/') {
  const router = createMemoryRouter(
    [
      {
        path: '/',
        element: <App />,
        children: [{ index: true, element: <div>content</div> }],
      },
    ],
    { initialEntries: [initialEntry] },
  );

  return render(<RouterProvider router={router} />);
}

describe('App', () => {
  afterEach(() => {
    cleanup();
  });

  it('defaults to Upbit exchange mode', () => {
    renderApp();

    expect(screen.getByRole('button', { name: 'UPBIT' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'BINANCE' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('reads and updates exchange mode from the URL query', async () => {
    renderApp('/?exchange=binance');

    expect(screen.getByRole('button', { name: 'BINANCE' })).toHaveAttribute('aria-pressed', 'true');

    await userEvent.click(screen.getByRole('button', { name: 'UPBIT' }));

    expect(screen.getByRole('button', { name: 'UPBIT' })).toHaveAttribute('aria-pressed', 'true');
  });
});
