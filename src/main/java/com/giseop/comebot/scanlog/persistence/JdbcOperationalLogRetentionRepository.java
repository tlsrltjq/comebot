package com.giseop.comebot.scanlog.persistence;

import com.giseop.comebot.scanlog.repository.OperationalLogRetentionRepository;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOperationalLogRetentionRepository implements OperationalLogRetentionRepository {

    private static final String SUMMARY_SQL = """
            INSERT INTO candidate_scan_daily_summary (
                summary_date,
                exchange,
                market,
                decision,
                reason,
                scan_count,
                selected_count,
                first_scanned_at,
                last_scanned_at,
                updated_at
            )
            SELECT
                (date_trunc('day', scanned_at AT TIME ZONE 'UTC'))::date AS summary_date,
                exchange,
                market,
                decision,
                coalesce(reason, '') AS reason,
                count(*) AS scan_count,
                sum(CASE WHEN decision = 'SELECTED' THEN 1 ELSE 0 END) AS selected_count,
                min(scanned_at) AS first_scanned_at,
                max(scanned_at) AS last_scanned_at,
                now() AS updated_at
            FROM candidate_scan_log
            WHERE scanned_at < ?
            GROUP BY summary_date, exchange, market, decision, coalesce(reason, '')
            ON CONFLICT (summary_date, exchange, market, decision, reason)
            DO UPDATE SET
                scan_count = candidate_scan_daily_summary.scan_count + EXCLUDED.scan_count,
                selected_count = candidate_scan_daily_summary.selected_count + EXCLUDED.selected_count,
                first_scanned_at = LEAST(
                    candidate_scan_daily_summary.first_scanned_at,
                    EXCLUDED.first_scanned_at
                ),
                last_scanned_at = GREATEST(
                    candidate_scan_daily_summary.last_scanned_at,
                    EXCLUDED.last_scanned_at
                ),
                updated_at = EXCLUDED.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcOperationalLogRetentionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int summarizeCandidateScansBefore(Instant cutoff) {
        return jdbcTemplate.update(SUMMARY_SQL, Timestamp.from(cutoff));
    }

    @Override
    public int deleteCandidateScansBefore(Instant cutoff) {
        return jdbcTemplate.update("DELETE FROM candidate_scan_log WHERE scanned_at < ?", Timestamp.from(cutoff));
    }

    @Override
    public int deleteTradingFlowHistoryBefore(Instant cutoff) {
        return jdbcTemplate.update("DELETE FROM trading_flow_history WHERE created_at < ?", Timestamp.from(cutoff));
    }
}
