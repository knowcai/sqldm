package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.MetricDefinition;
import org.example.entity.Topic;
import org.example.entity.SysUser;
import org.example.repository.MetricRepository;
import org.example.repository.TopicRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final MetricRepository metricRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建指标
     */
    @Transactional
    public MetricDefinition createMetric(MetricDefinition metric) {
        // 检查指标名称是否已存在
        if (metricRepository.findByMetricNameAndIsDeletedFalse(metric.getMetricName()).isPresent()) {
            throw new RuntimeException("指标名称已存在: " + metric.getMetricName());
        }
        
        // 设置主题信息
        if (metric.getTopicId() != null) {
            Topic topic = topicRepository.findById(metric.getTopicId())
                    .orElseThrow(() -> new RuntimeException("主题不存在: " + metric.getTopicId()));
            metric.setTopicName(topic.getTopicName());
        }
        
        // 获取当前用户
        String currentUser = getCurrentUsername();
        metric.setCreatedBy(currentUser);
        metric.setUpdatedBy(currentUser);
        metric.setIsDeleted(false);
        
        return metricRepository.save(metric);
    }

    /**
     * 更新指标
     */
    @Transactional
    public MetricDefinition updateMetric(Long id, MetricDefinition metric) {
        MetricDefinition existing = metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));

        // 检查新名称是否与其他指标冲突
        if (!existing.getMetricName().equals(metric.getMetricName())) {
            if (metricRepository.findByMetricNameAndIsDeletedFalse(metric.getMetricName()).isPresent()) {
                throw new RuntimeException("指标名称已存在: " + metric.getMetricName());
            }
        }

        existing.setMetricName(metric.getMetricName());
        existing.setMetricType(metric.getMetricType());
        existing.setMainTable(metric.getMainTable());
        existing.setJoinTables(metric.getJoinTables());
        existing.setJoinFields(metric.getJoinFields());
        existing.setDimensionFields(metric.getDimensionFields());
        existing.setFilterFields(metric.getFilterFields());
        existing.setDescription(metric.getDescription());
        existing.setUpdatedBy(getCurrentUsername());

        return metricRepository.save(existing);
    }

    /**
     * 删除指标（逻辑删除）
     */
    @Transactional
    public void deleteMetric(Long id) {
        MetricDefinition metric = metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));
        
        metric.setIsDeleted(true);
        metric.setUpdatedBy(getCurrentUsername());
        metricRepository.save(metric);
    }

    /**
     * 获取所有未删除的指标
     */
    public List<MetricDefinition> getAllMetrics() {
        return metricRepository.findByIsDeletedFalseOrderByCreatedTimeDesc();
    }

    /**
     * 根据ID获取指标
     */
    public MetricDefinition getMetricById(Long id) {
        return metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));
    }

    /**
     * 根据类型获取指标
     */
    public List<MetricDefinition> getMetricsByType(String type) {
        return metricRepository.findByMetricTypeAndIsDeletedFalse(type);
    }

    /**
     * 搜索指标
     */
    public List<MetricDefinition> searchMetrics(String keyword) {
        return metricRepository.searchMetrics(keyword);
    }
    
    /**
     * 根据主题获取指标
     */
    public List<MetricDefinition> getMetricsByTopic(Long topicId) {
        return metricRepository.findByTopicIdAndIsDeletedFalseOrderByCreatedTimeDesc(topicId);
    }
    
    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }

    /**
     * 根据指标配置生成SQL
     */
    public String generateSqlFromMetric(Long id) {
        MetricDefinition metric = getMetricById(id);
        
        StringBuilder sql = new StringBuilder();
        
        // SELECT 部分
        sql.append("SELECT ");
        
        // 如果有维度字段，添加到SELECT
        if (metric.getDimensionFields() != null && !metric.getDimensionFields().trim().isEmpty()) {
            String[] dimensions = metric.getDimensionFields().split(",");
            for (int i = 0; i < dimensions.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(dimensions[i].trim());
            }
        } else {
            sql.append("*");
        }
        
        // 如果是统计类型，添加聚合函数
        if ("STATISTICS".equals(metric.getMetricType())) {
            sql.append(", COUNT(*) as total_count");
        }
        
        // FROM 主表
        sql.append("\nFROM ").append(metric.getMainTable());
        
        // JOIN 关联表
        if (metric.getJoinTables() != null && !metric.getJoinTables().trim().isEmpty() && !"[]".equals(metric.getJoinTables().trim())) {
            try {
                List<Map<String, String>> joinTables = objectMapper.readValue(
                    metric.getJoinTables(), 
                    new TypeReference<List<Map<String, String>>>() {}
                );
                
                List<Map<String, String>> joinFields = new ArrayList<>();
                if (metric.getJoinFields() != null && !metric.getJoinFields().trim().isEmpty() && !"[]".equals(metric.getJoinFields().trim())) {
                    joinFields = objectMapper.readValue(
                        metric.getJoinFields(), 
                        new TypeReference<List<Map<String, String>>>() {}
                    );
                }
                
                for (Map<String, String> joinTable : joinTables) {
                    String tableName = joinTable.get("table");
                    String alias = joinTable.getOrDefault("alias", tableName);
                    sql.append("\nLEFT JOIN ").append(tableName).append(" ").append(alias);
                    
                    // 添加ON条件
                    for (Map<String, String> joinField : joinFields) {
                        String mainField = joinField.get("mainField");
                        String joinFieldVal = joinField.get("joinField");
                        if (mainField != null && joinFieldVal != null) {
                            sql.append("\n  ON ").append(mainField).append(" = ").append(joinFieldVal);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("解析JOIN信息失败: " + e.getMessage());
                // 如果解析失败，忽略JOIN
            }
        }
        
        // WHERE 过滤条件
        if (metric.getFilterFields() != null && !metric.getFilterFields().trim().isEmpty() && !"[]".equals(metric.getFilterFields().trim())) {
            try {
                List<Map<String, String>> filters = objectMapper.readValue(
                    metric.getFilterFields(), 
                    new TypeReference<List<Map<String, String>>>() {}
                );
                
                if (!filters.isEmpty()) {
                    sql.append("\nWHERE ");
                    for (int i = 0; i < filters.size(); i++) {
                        if (i > 0) sql.append("\n  AND ");
                        
                        Map<String, String> filter = filters.get(i);
                        String field = filter.get("field");
                        String operator = filter.get("operator");
                        String value = filter.get("value");
                        
                        if (field != null && operator != null && value != null) {
                            // 判断值是否需要加引号
                            if ("LIKE".equals(operator)) {
                                sql.append(field).append(" ").append(operator).append(" '%").append(value).append("%'");
                            } else if (operator.matches("[=<>!]+")) {
                                // 判断是否是数字
                                if (value.matches("^-?\\d+(\\.\\d+)?$")) {
                                    sql.append(field).append(" ").append(operator).append(" ").append(value);
                                } else {
                                    sql.append(field).append(" ").append(operator).append(" '").append(value).append("'");
                                }
                            } else {
                                sql.append(field).append(" ").append(operator).append(" ").append(value);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("解析过滤条件失败: " + e.getMessage());
                // 如果解析失败，忽略过滤条件
            }
        }
        
        // GROUP BY（统计类型且有维度字段）
        if ("STATISTICS".equals(metric.getMetricType()) && 
            metric.getDimensionFields() != null && !metric.getDimensionFields().trim().isEmpty()) {
            sql.append("\nGROUP BY ").append(metric.getDimensionFields());
        }
        
        // ORDER BY
        sql.append("\nORDER BY ");
        if (metric.getDimensionFields() != null && !metric.getDimensionFields().trim().isEmpty()) {
            sql.append(metric.getDimensionFields().split(",")[0].trim());
        } else {
            sql.append("1");
        }
        sql.append(" DESC");
        
        return sql.toString();
    }
}
