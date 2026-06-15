package org.sqldm.util;

import org.sqldm.entity.MetricDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricDiffHelperTest {

    @Test
    void diffFields_detectsChanges() {
        MetricDefinition before = new MetricDefinition();
        before.setMetricName("A");
        before.setMetricCode("CODE_A");
        before.setSqlTemplate("SELECT 1");

        MetricDefinition after = new MetricDefinition();
        after.setMetricName("B");
        after.setMetricCode("CODE_A");
        after.setSqlTemplate("SELECT 2");

        List<String> changed = MetricDiffHelper.diffFields(before, after);
        assertTrue(changed.contains("指标名称"));
        assertTrue(changed.contains("SQL模版"));
        assertFalse(changed.contains("指标编码"));
    }
}
