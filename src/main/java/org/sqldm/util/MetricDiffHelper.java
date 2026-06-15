package org.sqldm.util;

import org.sqldm.entity.MetricDefinition;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MetricDiffHelper {

    private MetricDiffHelper() {
    }

    public static List<String> diffFields(MetricDefinition before, MetricDefinition after) {
        List<String> changed = new ArrayList<>();
        if (before == null || after == null) {
            return changed;
        }
        compare(changed, "指标名称", before.getMetricName(), after.getMetricName());
        compare(changed, "指标编码", before.getMetricCode(), after.getMetricCode());
        compare(changed, "业务口径", before.getBusinessCaliber(), after.getBusinessCaliber());
        compare(changed, "统计周期", before.getStatPeriod(), after.getStatPeriod());
        compare(changed, "主题", before.getTopicName(), after.getTopicName());
        if (!Objects.equals(before.getTopicId(), after.getTopicId())) {
            changed.add("所属主题域");
        }
        compare(changed, "负责人", before.getOwner(), after.getOwner());
        compare(changed, "状态", before.getStatus(), after.getStatus());
        compare(changed, "数据源", before.getDataSource(), after.getDataSource());
        compare(changed, "SQL模版", before.getSqlTemplate(), after.getSqlTemplate());
        compare(changed, "参数定义", before.getParamDefinition(), after.getParamDefinition());
        return changed;
    }

    private static void compare(List<String> changed, String label, String a, String b) {
        String left = StringUtils.hasText(a) ? a.trim() : "";
        String right = StringUtils.hasText(b) ? b.trim() : "";
        if (!left.equals(right)) {
            changed.add(label);
        }
    }
}
