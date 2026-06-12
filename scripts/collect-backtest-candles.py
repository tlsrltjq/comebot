#!/usr/bin/env python3
"""Collect normalized public candle caches for Java backtests.

The output JSON schema intentionally matches Upbit minute candle field names so
the existing CandleSeries loader can read both Upbit and Binance caches.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


UPBIT_BASE = "https://api.upbit.com"
BINANCE_BASE = "https://api.binance.com"
UNITS = (1, 3, 5, 15)
UPBIT_LIMIT = 200
BINANCE_LIMIT = 1000
MAX_HTTP_ATTEMPTS = 5


def main() -> int:
    args = parse_args()
    output_dir = Path(args.output_dir)
    units = parse_units(args.units)
    since_ms = parse_utc(args.since)
    until_ms = parse_utc(args.until) if args.until else int(time.time() * 1000)

    upbit_markets = resolve_upbit_markets(args.upbit_markets, args.upbit_top)
    binance_symbols = resolve_binance_symbols(args.binance_symbols, args.binance_top)

    jobs = []
    for market in upbit_markets:
        for unit in units:
            jobs.append(("upbit", market, unit))
    for symbol in binance_symbols:
        for unit in units:
            jobs.append(("binance", symbol, unit))

    log(
        f"collecting jobs={len(jobs)} upbit={len(upbit_markets)} "
        f"binance={len(binance_symbols)} units={','.join(map(str, units))}"
    )
    if args.dry_run:
        for exchange, market, unit in jobs:
            log(f"dry-run {exchange} {market} {unit}m")
        return 0

    output_dir.mkdir(parents=True, exist_ok=True)
    for exchange, market, unit in jobs:
        path = output_path(output_dir, market, unit, since_ms, until_ms)
        if path.exists() and not args.overwrite:
            log(f"skip existing {path}")
            continue
        log(f"collect {exchange} {market} {unit}m -> {path}")
        if exchange == "upbit":
            candles = fetch_upbit_candles(market, unit, since_ms, until_ms, args.request_delay_sec)
        else:
            candles = fetch_binance_candles(market, unit, since_ms, until_ms, args.request_delay_sec)
        write_json(path, candles)
        log(f"wrote {path} candles={len(candles)}")

    write_manifest(output_dir, args, units, upbit_markets, binance_symbols, since_ms, until_ms)
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", default=".backtest_cache")
    parser.add_argument("--since", required=True, help="UTC start, e.g. 2025-06-12T00:00:00Z")
    parser.add_argument("--until", help="UTC end, defaults to now")
    parser.add_argument("--units", default="1,3,5,15")
    parser.add_argument("--upbit-markets", default="ALL_KRW")
    parser.add_argument("--binance-symbols", default="ALL_USDT")
    parser.add_argument("--upbit-top", type=int, default=30)
    parser.add_argument("--binance-top", type=int, default=30)
    parser.add_argument("--request-delay-sec", type=float, default=0.15)
    parser.add_argument("--overwrite", action="store_true", help="recollect files that already exist")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def parse_units(raw: str) -> list[int]:
    units = [int(part.strip()) for part in raw.split(",") if part.strip()]
    unsupported = [unit for unit in units if unit not in UNITS]
    if unsupported:
        raise SystemExit(f"unsupported units: {unsupported}; supported={UNITS}")
    return units


def parse_utc(raw: str) -> int:
    value = raw.strip()
    if value.endswith("Z"):
        value = value[:-1] + "+00:00"
    dt = datetime.fromisoformat(value)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return int(dt.astimezone(timezone.utc).timestamp() * 1000)


def iso_utc(ms: int) -> str:
    return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def log(message: str) -> None:
    print(message, flush=True)


def request_json(url: str) -> object:
    request = urllib.request.Request(url, headers={"User-Agent": "comebot-backtest-collector/1.0"})
    last_error = None
    for attempt in range(1, MAX_HTTP_ATTEMPTS + 1):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.loads(response.read().decode("utf-8"))
        except (ConnectionResetError, TimeoutError, urllib.error.URLError, urllib.error.HTTPError) as exc:
            last_error = exc
            if isinstance(exc, urllib.error.HTTPError) and exc.code not in (429, 500, 502, 503, 504):
                raise
            if attempt == MAX_HTTP_ATTEMPTS:
                break
            sleep_sec = min(8.0, 0.5 * (2 ** (attempt - 1)))
            print(f"request failed attempt={attempt}/{MAX_HTTP_ATTEMPTS}; retrying in {sleep_sec:.1f}s: {exc}", file=sys.stderr)
            time.sleep(sleep_sec)
    raise RuntimeError(f"request failed after {MAX_HTTP_ATTEMPTS} attempts: {url}") from last_error


def resolve_upbit_markets(raw: str, top: int) -> list[str]:
    if raw != "ALL_KRW":
        return split_csv(raw)
    markets = request_json(f"{UPBIT_BASE}/v1/market/all?isDetails=false")
    krw = sorted(item["market"] for item in markets if item.get("market", "").startswith("KRW-"))
    tickers = []
    for chunk in chunks(krw, 100):
        query = urllib.parse.urlencode({"markets": ",".join(chunk)})
        rows = request_json(f"{UPBIT_BASE}/v1/ticker?{query}")
        tickers.extend(rows)
        time.sleep(0.2)
    ranked = sorted(tickers, key=lambda row: float(row.get("acc_trade_price_24h", 0)), reverse=True)
    return [row["market"] for row in ranked[:top]]


def resolve_binance_symbols(raw: str, top: int) -> list[str]:
    if raw != "ALL_USDT":
        return split_csv(raw)
    exchange_info = request_json(f"{BINANCE_BASE}/api/v3/exchangeInfo")
    tradable = {
        item["symbol"]
        for item in exchange_info.get("symbols", [])
        if item.get("status") == "TRADING"
        and item.get("quoteAsset") == "USDT"
        and item.get("isSpotTradingAllowed")
    }
    tickers = request_json(f"{BINANCE_BASE}/api/v3/ticker/24hr")
    ranked = sorted(
        (row for row in tickers if row.get("symbol") in tradable),
        key=lambda row: float(row.get("quoteVolume", 0)),
        reverse=True,
    )
    return [row["symbol"] for row in ranked[:top]]


def split_csv(raw: str) -> list[str]:
    return [part.strip().upper() for part in raw.split(",") if part.strip()]


def chunks(values: list[str], size: int) -> list[list[str]]:
    return [values[index:index + size] for index in range(0, len(values), size)]


def fetch_upbit_candles(market: str, unit: int, since_ms: int, until_ms: int, delay_sec: float) -> list[dict[str, object]]:
    candles = []
    cursor = until_ms
    while cursor > since_ms:
        to = urllib.parse.quote(datetime.fromtimestamp(cursor / 1000, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%S"))
        url = f"{UPBIT_BASE}/v1/candles/minutes/{unit}?market={market}&count={UPBIT_LIMIT}&to={to}"
        rows = request_json(url)
        if not rows:
            break
        oldest_ms = cursor
        for row in rows:
            candle_ms = int(row["timestamp"])
            oldest_ms = min(oldest_ms, candle_ms)
            if since_ms <= candle_ms < until_ms:
                candles.append(normalize_upbit(row))
        next_cursor = oldest_ms - 1
        if next_cursor >= cursor:
            break
        cursor = next_cursor
        time.sleep(delay_sec)
    return sorted(unique_by_time(candles), key=lambda row: row["candle_date_time_utc"])


def fetch_binance_candles(symbol: str, unit: int, since_ms: int, until_ms: int, delay_sec: float) -> list[dict[str, object]]:
    candles = []
    interval = f"{unit}m"
    cursor = since_ms
    while cursor < until_ms:
        query = urllib.parse.urlencode({
            "symbol": symbol,
            "interval": interval,
            "limit": BINANCE_LIMIT,
            "startTime": cursor,
            "endTime": until_ms,
        })
        rows = request_json(f"{BINANCE_BASE}/api/v3/klines?{query}")
        if not rows:
            break
        for row in rows:
            open_ms = int(row[0])
            if since_ms <= open_ms < until_ms:
                candles.append(normalize_binance(symbol, row))
        next_cursor = int(rows[-1][0]) + unit * 60 * 1000
        if next_cursor <= cursor:
            break
        cursor = next_cursor
        time.sleep(delay_sec)
    return sorted(unique_by_time(candles), key=lambda row: row["candle_date_time_utc"])


def normalize_upbit(row: dict[str, object]) -> dict[str, object]:
    return {
        "market": row["market"],
        "candle_date_time_utc": row["candle_date_time_utc"],
        "opening_price": row["opening_price"],
        "high_price": row["high_price"],
        "low_price": row["low_price"],
        "trade_price": row["trade_price"],
        "candle_acc_trade_price": row["candle_acc_trade_price"],
        "candle_acc_trade_volume": row["candle_acc_trade_volume"],
    }


def normalize_binance(symbol: str, row: list[object]) -> dict[str, object]:
    return {
        "market": symbol,
        "candle_date_time_utc": iso_without_z(int(row[0])),
        "opening_price": row[1],
        "high_price": row[2],
        "low_price": row[3],
        "trade_price": row[4],
        "candle_acc_trade_price": row[7],
        "candle_acc_trade_volume": row[5],
    }


def iso_without_z(ms: int) -> str:
    return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")


def unique_by_time(candles: list[dict[str, object]]) -> list[dict[str, object]]:
    deduped = {}
    for candle in candles:
        deduped[candle["candle_date_time_utc"]] = candle
    return list(deduped.values())


def output_path(output_dir: Path, market: str, unit: int, since_ms: int, until_ms: int) -> Path:
    since = datetime.fromtimestamp(since_ms / 1000, tz=timezone.utc).strftime("%Y%m%d")
    until = datetime.fromtimestamp(until_ms / 1000, tz=timezone.utc).strftime("%Y%m%d")
    return output_dir / f"{market}_{unit}m_{since}_{until}.json"


def write_json(path: Path, candles: list[dict[str, object]]) -> None:
    tmp = path.with_suffix(path.suffix + ".tmp")
    with tmp.open("w", encoding="utf-8") as handle:
        json.dump(candles, handle, ensure_ascii=False, separators=(",", ":"))
    tmp.replace(path)


def write_manifest(
        output_dir: Path,
        args: argparse.Namespace,
        units: list[int],
        upbit_markets: list[str],
        binance_symbols: list[str],
        since_ms: int,
        until_ms: int,
) -> None:
    manifest = {
        "collected_at_utc": iso_utc(int(time.time() * 1000)),
        "since_utc": iso_utc(since_ms),
        "until_utc": iso_utc(until_ms),
        "units": units,
        "upbit_markets": upbit_markets,
        "binance_symbols": binance_symbols,
        "request_delay_sec": args.request_delay_sec,
    }
    write_json(output_dir / "manifest.json", manifest)


if __name__ == "__main__":
    sys.exit(main())
