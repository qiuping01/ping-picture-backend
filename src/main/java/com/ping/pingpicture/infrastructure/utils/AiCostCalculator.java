package com.ping.pingpicture.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * AI 审图 - 成本计算工具类
 */
@Slf4j
public class AiCostCalculator {

    /**
     * 输入Token单价（元/百万Token）
     */
    private static final BigDecimal INPUT_PRICE_PER_MILLION = new BigDecimal("0.8");

    /**
     * 输出Token单价（元/百万Token）
     */
    private static final BigDecimal OUTPUT_PRICE_PER_MILLION = new BigDecimal("2.0");

    /**
     * 百万基数
     */
    private static final BigDecimal MILLION = new BigDecimal("1000000");

    /**
     * 计算单次调用成本
     *
     * @param inputTokens  输入Token数
     * @param outputTokens 输出Token数
     * @return 成本（元），保留6位小数
     */
    public static BigDecimal calculateCost(Long inputTokens, Long outputTokens) {
        if (inputTokens == null || outputTokens == null) {
            log.warn("Token数为空，无法计算成本");
            return null;
        }

        BigDecimal input = new BigDecimal(inputTokens);
        BigDecimal output = new BigDecimal(outputTokens);

        // cost = (input × 0.8 + output × 2.0) / 1,000,000
        BigDecimal cost = input.multiply(INPUT_PRICE_PER_MILLION)
                .add(output.multiply(OUTPUT_PRICE_PER_MILLION))
                .divide(MILLION, 6, RoundingMode.HALF_UP);

        return cost;
    }

    /**
     * 批量计算成本（用于统计）
     *
     * @param totalInputTokens  总输入Token数
     * @param totalOutputTokens 总输出Token数
     * @return 总成本（元）
     */
    public static BigDecimal calculateTotalCost(Long totalInputTokens, Long totalOutputTokens) {
        return calculateCost(totalInputTokens, totalOutputTokens);
    }
}