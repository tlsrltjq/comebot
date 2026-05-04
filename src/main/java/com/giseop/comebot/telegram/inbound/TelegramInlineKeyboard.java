package com.giseop.comebot.telegram.inbound;

import java.util.List;

public record TelegramInlineKeyboard(
        List<List<Button>> inline_keyboard
) {

    public static TelegramInlineKeyboard mainMenu() {
        return new TelegramInlineKeyboard(List.of(
                List.of(new Button("상태 보기", "STATUS")),
                List.of(new Button("자동 실행", "AUTO"), new Button("매매 조건", "CONDITIONS")),
                List.of(new Button("손익", "PNL"), new Button("포트폴리오", "PORTFOLIO")),
                List.of(new Button("후보 보기", "CANDIDATES")),
                List.of(new Button("BTC 이력", "HISTORY:KRW-BTC"), new Button("ETH 이력", "HISTORY:KRW-ETH")),
                List.of(new Button("보유 포지션", "POSITIONS")),
                List.of(new Button("리스크", "RISK"), new Button("안전장치", "SAFETY")),
                List.of(new Button("도움말", "HELP"))
        ));
    }

    public record Button(
            String text,
            String callback_data
    ) {
    }
}
