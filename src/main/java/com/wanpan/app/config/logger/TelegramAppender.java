package com.wanpan.app.config.logger;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class TelegramAppender<E> extends UnsynchronizedAppenderBase<E> {

    private static final String SEND_MESSAGE_API_URL = "https://api.telegram.org/bot%s/sendMessage";

    @Setter
    protected Layout<E> layout;

    @Setter
    protected String token;

    @Setter
    protected String chatId;

    private RestTemplate restTemplate = new RestTemplate();
    private HttpHeaders headers = new HttpHeaders();

    private TimeBaseEventEvaluator eventEvaluator = new TimeBaseEventEvaluator();

    public TelegramAppender() {
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    }

    @Override
    protected void append(E event) {
        if (!this.isStarted())
            return;

        if (this.eventEvaluator.evaluate()) {
            this.sendTelegramMessage(event);
        }
    }

    void sendTelegramMessage(E event) {
        String messageToSend = layout.doLayout(event);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("chat_id", this.chatId);
        body.add("text", messageToSend);
        restTemplate.postForEntity(String.format(SEND_MESSAGE_API_URL, this.token),
                new HttpEntity<>(body, this.headers), String.class);
    }

    private final class TimeBaseEventEvaluator {

        private long beforeTime;
        private long intervalTime = 60000;

        boolean evaluate() {
            long current = System.currentTimeMillis();
            long backupBeforeTime = this.beforeTime;
            if ((current - backupBeforeTime) > intervalTime) {
                this.beforeTime = current;
                return true;
            }
            return false;
        }
    }
}
