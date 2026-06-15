package org.sqldm.util;

import org.sqldm.config.SqldmProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class MetricCodeValidator {

    private final SqldmProperties properties;

    public MetricCodeValidator(SqldmProperties properties) {
        this.properties = properties;
    }

    public void validate(String metricCode) {
        if (!properties.isMetricCodePatternEnabled() || !StringUtils.hasText(metricCode)) {
            return;
        }
        Pattern pattern = Pattern.compile(properties.getMetricCodePattern());
        if (!pattern.matcher(metricCode).matches()) {
            throw new RuntimeException("指标编码不符合规范，需匹配: " + properties.getMetricCodePattern());
        }
    }
}
