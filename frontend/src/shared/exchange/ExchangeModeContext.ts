import { createContext, useContext } from 'react';
import type { ExchangeMode } from '../api/types';

export interface ExchangeModeContextValue {
  exchange: ExchangeMode;
  setExchange: (exchange: ExchangeMode) => void;
}

export const ExchangeModeContext = createContext<ExchangeModeContextValue>({
  exchange: 'UPBIT',
  setExchange: () => {},
});

export function useExchangeMode() {
  return useContext(ExchangeModeContext);
}

export function exchangeParam(exchange: ExchangeMode) {
  return exchange.toLowerCase();
}
