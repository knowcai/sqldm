package org.example.util;

import org.example.config.SqldmProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricCodeValidatorTest {

    @Test
    void acceptsValidCode() {
        SqldmProperties props = new SqldmProperties();
        props.setMetricCodePatternEnabled(true);
        MetricCodeValidator validator = new MetricCodeValidator(props);
        assertDoesNotThrow(() -> validator.validate("ORDER_CNT_D"));
    }

    @Test
    void rejectsInvalidCode() {
        SqldmProperties props = new SqldmProperties();
        props.setMetricCodePatternEnabled(true);
        MetricCodeValidator validator = new MetricCodeValidator(props);
        assertThrows(RuntimeException.class, () -> validator.validate("bad-code"));
    }
}
