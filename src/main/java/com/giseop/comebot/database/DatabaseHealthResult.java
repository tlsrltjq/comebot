package com.giseop.comebot.database;

public record DatabaseHealthResult(
        boolean connected,
        String database
) {
}
