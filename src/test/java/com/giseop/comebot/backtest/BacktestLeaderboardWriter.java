package com.giseop.comebot.backtest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

final class BacktestLeaderboardWriter {

    private BacktestLeaderboardWriter() {
    }

    static void writeCsv(Path path, List<BacktestLeaderboardRow> rows) throws IOException {
        Files.createDirectories(path.getParent());
        String body = rows.stream()
                .map(BacktestLeaderboardRow::toCsv)
                .collect(Collectors.joining(System.lineSeparator()));
        String content = BacktestLeaderboardRow.csvHeader() + System.lineSeparator()
                + body + System.lineSeparator();
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    static void writeMarkdown(Path path, List<BacktestLeaderboardRow> rows) throws IOException {
        Files.createDirectories(path.getParent());
        String body = rows.stream()
                .map(BacktestLeaderboardRow::toMarkdownRow)
                .collect(Collectors.joining(System.lineSeparator()));
        String content = BacktestLeaderboardRow.markdownHeader() + System.lineSeparator()
                + body + System.lineSeparator();
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
