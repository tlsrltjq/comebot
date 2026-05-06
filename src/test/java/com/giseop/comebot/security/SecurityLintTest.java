package com.giseop.comebot.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SecurityLintTest {

    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final List<Path> SCAN_ROOTS = List.of(
            ROOT.resolve("src/main/java"),
            ROOT.resolve("src/main/resources"),
            ROOT.resolve("scripts"),
            ROOT.resolve("docs"),
            ROOT.resolve("README.md"),
            ROOT.resolve("AGENTS.md"),
            ROOT.resolve(".env.example"),
            ROOT.resolve("docker-compose.yml")
    );

    private static final Pattern TELEGRAM_BOT_TOKEN = Pattern.compile("\\b\\d{6,}:[A-Za-z0-9_-]{20,}\\b");
    private static final Pattern AWS_ACCESS_KEY = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");
    private static final Pattern PRIVATE_KEY_HEADER = Pattern.compile("-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----");
    private static final Pattern UPBIT_ACCESS_KEY_ASSIGNMENT = Pattern.compile("(?i)(upbit|access)[._-]?(key|token)[ \\t]*=[ \\t]*[^$\\s][^\\r\\n]*");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile("(?i)(secret|password|bot[-_]?token|chat[-_]?id)[ \\t]*=[ \\t]*[^$\\s][^\\r\\n]*");

    @Test
    void envFileMustStayIgnoredAndUntracked() throws Exception {
        String gitignore = Files.readString(ROOT.resolve(".gitignore"), StandardCharsets.UTF_8);
        assertThat(gitignore.lines().map(String::trim)).contains(".env");

        Process process = new ProcessBuilder("git", "ls-files", ".env")
                .directory(ROOT.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        assertThat(exitCode).isZero();
        assertThat(output.trim()).isEmpty();
    }

    @Test
    void trackedFilesMustNotContainLiteralSecrets() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : trackedTextFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            String normalizedPath = ROOT.relativize(file).toString().replace('\\', '/');

            addIfMatches(violations, normalizedPath, "telegram bot token literal", TELEGRAM_BOT_TOKEN, content);
            addIfMatches(violations, normalizedPath, "AWS access key literal", AWS_ACCESS_KEY, content);
            addIfMatches(violations, normalizedPath, "private key literal", PRIVATE_KEY_HEADER, content);

            if (!normalizedPath.endsWith(".env.example") && !normalizedPath.endsWith(".java")) {
                addIfMatches(violations, normalizedPath, "hardcoded sensitive assignment", SECRET_ASSIGNMENT, content);
                addIfMatches(violations, normalizedPath, "hardcoded Upbit key assignment", UPBIT_ACCESS_KEY_ASSIGNMENT, content);
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void sensitiveApplicationPropertiesMustUseEnvironmentPlaceholders() throws IOException {
        Path applicationProperties = ROOT.resolve("src/main/resources/application.properties");
        List<String> violations = new ArrayList<>();

        for (String line : Files.readAllLines(applicationProperties, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                continue;
            }

            String key = trimmed.substring(0, trimmed.indexOf('=')).toLowerCase(Locale.ROOT);
            String value = trimmed.substring(trimmed.indexOf('=') + 1);
            boolean sensitive = key.contains("password")
                    || key.contains("token")
                    || key.contains("secret")
                    || key.contains("chat-id")
                    || key.contains("access-key");

            if (sensitive && !value.startsWith("${")) {
                violations.add(trimmed);
            }
            if (sensitive && value.matches(".*:[A-Za-z0-9_-]+}.*")) {
                violations.add("non-empty default for sensitive property: " + trimmed);
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void logStatementsMustNotPrintSensitiveIdentifiers() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : javaMainFiles()) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            String normalizedPath = ROOT.relativize(file).toString().replace('\\', '/');

            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.contains("log.") && containsSensitiveIdentifier(lower)) {
                    violations.add(normalizedPath + ":" + (index + 1) + " " + line.trim());
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void realTradingImplementationMustNotBeAdded() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : javaMainFiles()) {
            String normalizedPath = ROOT.relativize(file).toString().replace('\\', '/');
            String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
            String content = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

            if (fileName.contains("realtrading") || fileName.contains("real_trading")) {
                violations.add(normalizedPath);
            }
            if (content.contains("havingvalue = \"real_trading\"")
                    || content.contains("executionmode.real_trading")) {
                violations.add(normalizedPath);
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void scheduledTasksMustNotUseFixedRate() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : javaMainFiles()) {
            String normalizedPath = ROOT.relativize(file).toString().replace('\\', '/');
            String content = Files.readString(file, StandardCharsets.UTF_8);

            if (content.contains("@Scheduled") && content.contains("fixedRate")) {
                violations.add(normalizedPath);
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void upbitAuthenticationConfigurationMustNotBeAdded() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : trackedTextFiles()) {
            String normalizedPath = ROOT.relativize(file).toString().replace('\\', '/');
            String content = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

            if (content.contains("upbit_access_key")
                    || content.contains("upbit_secret_key")
                    || content.contains("upbit.access-key")
                    || content.contains("upbit.secret-key")) {
                violations.add(normalizedPath);
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void binanceAuthenticationConfigurationMustNotBeAdded() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : trackedTextFiles()) {
            String normalizedPath = ROOT.relativize(file).toString().replace('\\', '/');
            String content = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

            if (content.contains("binance_api_key")
                    || content.contains("binance_secret_key")
                    || content.contains("binance.api-key")
                    || content.contains("binance.secret-key")) {
                violations.add(normalizedPath);
            }
        }

        assertThat(violations).isEmpty();
    }

    private static List<Path> trackedTextFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path scanRoot : SCAN_ROOTS) {
            if (!Files.exists(scanRoot)) {
                continue;
            }
            if (Files.isRegularFile(scanRoot)) {
                files.add(scanRoot);
                continue;
            }
            try (Stream<Path> stream = Files.walk(scanRoot)) {
                stream.filter(Files::isRegularFile)
                        .filter(SecurityLintTest::isTextFile)
                        .forEach(files::add);
            }
        }
        return files;
    }

    private static List<Path> javaMainFiles() throws IOException {
        Path mainJava = ROOT.resolve("src/main/java");
        if (!Files.exists(mainJava)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(mainJava)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static boolean isTextFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".java")
                || fileName.endsWith(".properties")
                || fileName.endsWith(".md")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml")
                || fileName.endsWith(".bat")
                || fileName.equals(".env.example")
                || fileName.equals("docker-compose.yml")
                || fileName.equals("AGENTS.md")
                || fileName.equals("README.md");
    }

    private static void addIfMatches(
            List<String> violations,
            String path,
            String reason,
            Pattern pattern,
            String content
    ) {
        if (pattern.matcher(content).find()) {
            violations.add(path + ": " + reason);
        }
    }

    private static boolean containsSensitiveIdentifier(String lowerLine) {
        return lowerLine.contains("bottoken")
                || lowerLine.contains("bot-token")
                || lowerLine.contains("chatid")
                || lowerLine.contains("chat-id")
                || lowerLine.contains("password")
                || lowerLine.contains("secret")
                || lowerLine.contains("accesskey")
                || lowerLine.contains("access-key");
    }
}
