package com.giseop.comebot.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private boolean enabled = false;
    private boolean sendHold = false;
    private boolean sendFilled = true;
    private boolean sendRejected = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSendHold() {
        return sendHold;
    }

    public void setSendHold(boolean sendHold) {
        this.sendHold = sendHold;
    }

    public boolean isSendFilled() {
        return sendFilled;
    }

    public void setSendFilled(boolean sendFilled) {
        this.sendFilled = sendFilled;
    }

    public boolean isSendRejected() {
        return sendRejected;
    }

    public void setSendRejected(boolean sendRejected) {
        this.sendRejected = sendRejected;
    }
}
