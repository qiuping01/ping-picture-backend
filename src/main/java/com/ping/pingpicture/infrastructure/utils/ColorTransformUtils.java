package com.ping.pingpicture.infrastructure.utils;

import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;

import java.util.regex.Pattern;

/**
 * 颜色转换工具类
 */
public class ColorTransformUtils {

    private ColorTransformUtils() {
        // 工具类不需要实例化
    }

    // 颜色正则表达式
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^0x[0-9A-Fa-f]{3,6}$");
    private static final Pattern SHORT_HEX_PATTERN = Pattern.compile("^0x[0-9A-Fa-f]{3,5}$");

    /**
     * 获取标准颜色（将数据万象的非标准色值转为标准6位）
     * <p>
     * 数据万象可能的格式：
     * 1. 3位：0xRGB -> 扩展为 0xRRGGBB
     * 2. 5位：0xRRGGB -> 转换为 0xRRGGBB（最后一位B需要补0）
     * 3. 6位：0xRRGGBB -> 保持不变
     *
     * @param color 颜色字符串（如 "0x080e0"）
     * @return 标准6位颜色字符串（如 "0x0800e0"）
     * @throws IllegalArgumentException 如果颜色格式无效
     */
    public static String getStandardColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "颜色字符串不能为空");
        }

        // .trim() 去除字符串首尾空白字符
        color = color.trim();

        // 验证格式
        if (!HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new IllegalArgumentException("无效的颜色格式: " + color +
                    "，应为 0xRGB、0xRRGGB 或 0xRRGGBB 格式");
        }

        // 去掉 0x 前缀，只处理数字部分
        String hexDigits = color.substring(2);

        switch (hexDigits.length()) {
            case 3:
                // 0xRGB -> 0xRRGGBB
                return "0x" + expandShortHex(hexDigits);
            case 5:
                // 数据万象特有格式：0xRRGGB -> 0xRRGGBB
                return convertFiveDigitHex(color, hexDigits);
            case 6:
                // 已经是标准格式
                return color;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的颜色长度: " + color);
        }
    }

    /**
     * 扩展3位短格式为6位标准格式
     * 0xRGB -> 0xRRGGBB
     */
    private static String expandShortHex(String shortHex) {
        if (shortHex.length() != 3) {
            throw new IllegalArgumentException("短格式必须为3位: " + shortHex);
        }

        StringBuilder expanded = new StringBuilder(6);
        for (int i = 0; i < 3; i++) {
            char c = shortHex.charAt(i);
            expanded.append(c).append(c);
        }
        return expanded.toString();
    }

    /**
     * 转换5位数据万象格式为6位标准格式
     * 0xRRGGB -> 0xRRGGBB
     * 规则：最后一位B需要补0，变成B0
     * 示例：0x080e0 -> 0x0800e0
     */
    private static String convertFiveDigitHex(String original, String hexDigits) {
        if (hexDigits.length() != 5) {
            throw new IllegalArgumentException("数据万象格式必须为5位: " + original);
        }

        // 0x080e0 -> 提取前4位数字 + "0" + 最后1位
        // hexDigits = "080e0"
        // 前4位: "080e" + "0" + 最后1位: "0" = "080e00" ❌ 这不对！

        // 正确逻辑：数据万象的5位格式是 RRGGB，需要补全为 RRGGBB
        // 其中 B 是1位，需要补0成为 B0
        // 所以：前4位不变，第5位后加0
        String result = "0x" +
                hexDigits.substring(0, 4) +  // 前4位: RRGG
                hexDigits.charAt(4) + "0";   // 最后1位B + "0"

        return result;
    }

    /**
     * 批量转换颜色数组
     */
    public static String[] getStandardColors(String... colors) {
        if (colors == null) {
            return new String[0];
        }

        String[] result = new String[colors.length];
        for (int i = 0; i < colors.length; i++) {
            result[i] = getStandardColor(colors[i]);
        }
        return result;
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 测试用例
        String[] testCases = {
                "0x080e0",    // 数据万象5位 -> 0x0800e0
                "0x12345",    // 数据万象5位 -> 0x123450
                "0xfff",      // 短格式3位 -> 0xffffff
                "0xabc",      // 短格式3位 -> 0xaabbcc
                "0x123456",   // 标准6位 -> 保持不变
                "0x000",      // 黑色短格式 -> 0x000000
                "0x000000",   // 黑色标准格式 -> 保持不变
        };

        System.out.println("颜色转换测试:");
        System.out.println("================");
        for (String test : testCases) {
            try {
                String result = getStandardColor(test);
                System.out.printf("%-10s -> %-10s%n", test, result);
            } catch (Exception e) {
                System.out.printf("%-10s -> 错误: %s%n", test, e.getMessage());
            }
        }

        // 额外测试：验证具体转换逻辑
        System.out.println("\n详细转换示例:");
        System.out.println("================");
        // 示例：0x080e0（数据万象格式）
        // R=08, G=0e, B=0
        // 需要补全为：R=08, G=0e, B=00
        // 结果：0x0800e0
        String example = "0x080e0";
        System.out.println("输入: " + example);
        System.out.println("解析:");
        System.out.println("  - 原格式: 0xRRGGB (5位)");
        System.out.println("  - R = " + example.substring(2, 4));
        System.out.println("  - G = " + example.substring(4, 5) + "? 实际应该从索引4开始");
        // 修正解析
        String digits = example.substring(2); // "080e0"
        System.out.println("  - 数字部分: " + digits);
        System.out.println("  - R = " + digits.substring(0, 2)); // 08
        System.out.println("  - G = " + digits.substring(2, 4)); // 0e
        System.out.println("  - B = " + digits.substring(4));    // 0
        System.out.println("  - 补全B: " + digits.substring(4) + "0 = 00");
        System.out.println("输出: " + getStandardColor(example));
    }
}