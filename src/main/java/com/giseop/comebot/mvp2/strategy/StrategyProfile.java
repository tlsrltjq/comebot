package com.giseop.comebot.mvp2.strategy;

public enum StrategyProfile {
    STABLE("안정형", "Stable"),
    AGGRESSIVE("공격형", "Aggressive"),
    DEFENSIVE("수비형", "Defensive");

    private final String koreanName;
    private final String englishName;

    StrategyProfile(String koreanName, String englishName) {
        this.koreanName = koreanName;
        this.englishName = englishName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getDisplayName() {
        return koreanName + "(" + englishName + ")";
    }
}
