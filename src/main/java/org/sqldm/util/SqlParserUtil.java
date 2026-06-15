package org.sqldm.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL解析工具类
 * 解析常规SELECT语句，提取表、字段、关联、过滤条件等信息
 */
public class SqlParserUtil {

    /**
     * 解析SQL语句
     */
    public static Map<String, Object> parseSql(String sql) {
        Map<String, Object> result = new HashMap<>();
        
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }

        sql = sql.trim().replaceAll("\\s+", " ");
        
        // 提取主表
        String mainTable = extractMainTable(sql);
        result.put("mainTable", mainTable);
        
        // 提取关联表和关联字段
        List<Map<String, String>> joinTables = extractJoinTables(sql);
        result.put("joinTables", joinTables);
        
        List<Map<String, String>> joinFields = extractJoinFields(sql);
        result.put("joinFields", joinFields);
        
        // 提取WHERE条件
        List<Map<String, String>> filterFields = extractWhereConditions(sql);
        result.put("filterFields", filterFields);
        
        // 提取GROUP BY字段（维度字段）
        List<String> dimensionFields = extractGroupByFields(sql);
        result.put("dimensionFields", dimensionFields);
        
        // 提取SELECT字段
        List<String> selectFields = extractSelectFields(sql);
        result.put("selectFields", selectFields);
        
        return result;
    }

    /**
     * 提取主表名
     */
    private static String extractMainTable(String sql) {
        // 匹配 FROM 后的第一个表名
        Pattern pattern = Pattern.compile("\\bFROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 提取JOIN表信息
     */
    private static List<Map<String, String>> extractJoinTables(String sql) {
        List<Map<String, String>> joins = new ArrayList<>();
        
        // 匹配 JOIN table_name [AS] alias 或 JOIN table_name alias
        Pattern pattern = Pattern.compile(
            "\\b(?:LEFT|RIGHT|INNER|OUTER|CROSS)?\\s*JOIN\\s+(\\w+)(?:\\s+(?:AS\\s+)?(\\w+))?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(sql);
        
        while (matcher.find()) {
            Map<String, String> join = new HashMap<>();
            join.put("table", matcher.group(1));
            join.put("alias", matcher.group(2) != null ? matcher.group(2) : matcher.group(1));
            joins.add(join);
        }
        
        return joins;
    }

    /**
     * 提取关联字段（ON条件）
     */
    private static List<Map<String, String>> extractJoinFields(String sql) {
        List<Map<String, String>> fields = new ArrayList<>();
        
        // 匹配 ON 条件
        Pattern pattern = Pattern.compile(
            "\\bON\\s+(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(sql);
        
        while (matcher.find()) {
            Map<String, String> field = new HashMap<>();
            field.put("mainField", matcher.group(1) + "." + matcher.group(2));
            field.put("joinField", matcher.group(3) + "." + matcher.group(4));
            fields.add(field);
        }
        
        return fields;
    }

    /**
     * 提取WHERE条件
     */
    private static List<Map<String, String>> extractWhereConditions(String sql) {
        List<Map<String, String>> conditions = new ArrayList<>();
        
        // 提取WHERE子句
        Pattern wherePattern = Pattern.compile(
            "\\bWHERE\\s+(.+?)(?:\\bGROUP\\b|\\bORDER\\b|\\bLIMIT\\b|\\bHAVING\\b|$)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher whereMatcher = wherePattern.matcher(sql);
        
        if (whereMatcher.find()) {
            String whereClause = whereMatcher.group(1).trim();
            
            // 解析简单的条件（支持 AND 分隔的多个条件）
            String[] conditionParts = whereClause.split("\\s+AND\\s+", -1);
            
            for (String part : conditionParts) {
                part = part.trim();
                
                // 匹配 field operator value
                Pattern conditionPattern = Pattern.compile(
                    "(\\w+(?:\\.\\w+)?)\\s*(>=|<=|!=|<>|>|<|=|LIKE|IN|BETWEEN)\\s*(.+?)(?:\\s+AND\\s+|$)",
                    Pattern.CASE_INSENSITIVE
                );
                Matcher conditionMatcher = conditionPattern.matcher(part);
                
                if (conditionMatcher.find()) {
                    Map<String, String> condition = new HashMap<>();
                    condition.put("field", conditionMatcher.group(1));
                    condition.put("operator", conditionMatcher.group(2).toUpperCase());
                    String value = conditionMatcher.group(3).trim();
                    // 移除引号
                    value = value.replaceAll("^['\"]|['\"]$", "");
                    condition.put("value", value);
                    conditions.add(condition);
                }
            }
        }
        
        return conditions;
    }

    /**
     * 提取GROUP BY字段（维度字段）
     */
    private static List<String> extractGroupByFields(String sql) {
        List<String> fields = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(
            "\\bGROUP\\s+BY\\s+([^;]+?)(?:\\bORDER\\b|\\bLIMIT\\b|\\bHAVING\\b|$)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(sql);
        
        if (matcher.find()) {
            String groupByClause = matcher.group(1).trim();
            String[] fieldArray = groupByClause.split(",");
            
            for (String field : fieldArray) {
                fields.add(field.trim());
            }
        }
        
        return fields;
    }

    /**
     * 提取SELECT字段
     */
    private static List<String> extractSelectFields(String sql) {
        List<String> fields = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(
            "\\bSELECT\\s+(.+?)\\s+\\bFROM\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(sql);
        
        if (matcher.find()) {
            String selectClause = matcher.group(1).trim();
            String[] fieldArray = selectClause.split(",");
            
            for (String field : fieldArray) {
                String trimmedField = field.trim();
                // 移除聚合函数，只保留字段名
                trimmedField = trimmedField.replaceAll("^(COUNT|SUM|AVG|MAX|MIN)\\s*\\([^)]*\\)\\s*(?:AS\\s+)?", "");
                if (!trimmedField.isEmpty()) {
                    fields.add(trimmedField);
                }
            }
        }
        
        return fields;
    }
}
