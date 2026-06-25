package com.giseop.comebot.market.candle.provider;

import com.giseop.comebot.market.candle.domain.StockCandleImportManifest;
import com.giseop.comebot.market.candle.domain.StockCandleRow;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StockCandleCsvImporter {

    private static final List<String> REQUIRED_COLUMNS = List.of("timestamp", "open", "high", "low", "close", "volume");

    public List<StockCandleRow> load(StockCandleImportManifest manifest) {
        if (manifest == null) {
            throw new IllegalArgumentException("manifest must not be null");
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(manifest.dataFile());
        } catch (IOException exception) {
            throw new IllegalArgumentException("failed to read stock candle csv: " + manifest.dataFile(), exception);
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("stock candle csv must include a header");
        }

        Map<String, Integer> header = parseHeader(lines.getFirst());
        List<StockCandleRow> rows = new ArrayList<>();
        Instant previousTimestamp = null;
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            StockCandleRow row = parseRow(header, line, index + 1);
            if (previousTimestamp != null && !row.timestamp().isAfter(previousTimestamp)) {
                throw new IllegalArgumentException("timestamps must be strictly increasing at line " + (index + 1));
            }
            if (row.timestamp().isBefore(manifest.since()) || !row.timestamp().isBefore(manifest.until())) {
                throw new IllegalArgumentException("timestamp is outside manifest range at line " + (index + 1));
            }
            rows.add(row);
            previousTimestamp = row.timestamp();
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("stock candle csv must include at least one data row");
        }
        return List.copyOf(rows);
    }

    private Map<String, Integer> parseHeader(String headerLine) {
        String[] columns = split(headerLine);
        Map<String, Integer> header = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            header.put(columns[i].trim().toLowerCase(), i);
        }
        for (String required : REQUIRED_COLUMNS) {
            if (!header.containsKey(required)) {
                throw new IllegalArgumentException("missing required csv column: " + required);
            }
        }
        return header;
    }

    private StockCandleRow parseRow(Map<String, Integer> header, String line, int lineNumber) {
        String[] values = split(line);
        try {
            return new StockCandleRow(
                    Instant.parse(value(header, values, "timestamp")),
                    decimal(header, values, "open"),
                    decimal(header, values, "high"),
                    decimal(header, values, "low"),
                    decimal(header, values, "close"),
                    decimal(header, values, "volume")
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("invalid stock candle csv row at line " + lineNumber
                    + ": " + exception.getMessage(), exception);
        }
    }

    private BigDecimal decimal(Map<String, Integer> header, String[] values, String column) {
        return new BigDecimal(value(header, values, column));
    }

    private String value(Map<String, Integer> header, String[] values, String column) {
        int index = header.get(column);
        if (index >= values.length || values[index].isBlank()) {
            throw new IllegalArgumentException(column + " must not be blank");
        }
        return values[index].trim();
    }

    private String[] split(String line) {
        return line.split(",", -1);
    }
}
