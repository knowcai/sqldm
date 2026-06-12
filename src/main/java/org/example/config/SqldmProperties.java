package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sqldm")
public class SqldmProperties {

    private String metricCodePattern = "^[A-Z][A-Z0-9_]{2,49}$";
    private boolean metricCodePatternEnabled = true;
    private Webhook webhook = new Webhook();

    @Data
    public static class Webhook {
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 5000;
    }
}
