package com.giseop.comebot.telegram.inbound;

import java.util.List;

public record TelegramInlineKeyboard(
        List<List<Button>> inline_keyboard
) {

    public static TelegramInlineKeyboard mainMenu() {
        return new TelegramInlineKeyboard(List.of(
                List.of(new Button("상태 보기", "STATUS")),
                List.of(new Button("KRW-BTC 실행", "RUN:KRW-BTC"), new Button("KRW-ETH 실행", "RUN:KRW-ETH")),
                List.of(new Button("BTC 이력 보기", "HISTORY:KRW-BTC"), new Button("ETH 이력 보기", "HISTORY:KRW-ETH")),
                List.of(new Button("도움말", "HELP"))
        ));
    }

    public record Button(
            String text,
            String callback_data
    ) {
    }
}
