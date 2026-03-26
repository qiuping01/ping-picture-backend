package com.ping.pingpicture.infrastructure.api.qwen;

import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.ResponseFormat;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import com.ping.pingpicture.infrastructure.api.qwen.model.AuditImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;

@Component
public class ImageAuditWithStructuredOutput {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    static {
        // 根据实际地域设置，默认北京地域无需修改，新加坡地域需改为 https://dashscope-intl.aliyuncs.com/api/v1
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";
    }

    /**
     * 审核图片并返回结构化对象（含标签和分类）
     *
     * @param imageUrl 图片公网 URL
     * @return 审核结果对象
     * @throws Exception 调用失败或 JSON 解析失败时抛出
     */
    public AuditImageResponse auditImage(String imageUrl) throws Exception {
        // 1. 构建系统消息（包含标签分类要求）
        String systemPrompt = "你是一个图片内容审核与标签分类助手。请根据图片内容完成以下任务：\n"
                + "1. 审核图片是否正常、是否危险、是否通过，并给出不通过原因和简短描述。\n"
                + "2. 为图片打上合适的标签（可多选）并选择一个最匹配的分类。\n\n"
                + "【输出格式】\n"
                + "你必须输出一个JSON对象，包含以下字段：\n"
                + "{\n"
                + "  \"isNormal\": \"yes/no\",\n"
                + "  \"isDangerous\": \"yes/no\",\n"
                + "  \"noPassReason\": \"string，不通过时填写原因\",\n"
                + "  \"isPass\": \"yes/no\",\n"
                + "  \"description\": \"string，图片内容简短描述\",\n"
                + "  \"tags\": [\"标签1\", \"标签2\", ...],\n"
                + "  \"category\": \"分类\"\n"
                + "}\n\n"
                + "【可用分类】（只能选择一个）\n"
                + "人物, 风景, 动物, 植物, 建筑, 科技, 美食, 运动, 艺术, 生活, 其他\n\n"
                + "【可用标签】（可多选，从以下列表中选择）\n"
                + "艺术, 简约, 复古, 萌系, 酷炫, 唯美, 人物, 动物, 宠物, 植物, 花卉, \n"
                + "风景, 山川, 城市夜景, 建筑, 美食, 甜点, 科技, 运动, 生活, 校园, 旅行, 文化, \n"
                + "背景, 封面, 欢乐, 创意, 搞笑\n\n"
                + "只输出JSON，不要包含任何其他文本或解释。";

        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", systemPrompt)))
                .build();

        // 2. 构建用户消息：包含图片 URL 和文本指令（再次强调 JSON）
        String userText = "请分析图片并严格按照上述 JSON 格式输出审核结果。";
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("image", imageUrl),
                        Collections.singletonMap("text", userText)
                ))
                .build();

        // 3. 开启 JSON 结构化输出
        ResponseFormat jsonFormat = ResponseFormat.builder()
                .type("json_object")
                .build();

        // 4. 构建请求参数（使用多模态模型，如 qwen-vl-plus）
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 建议从环境变量读取 API Key，避免硬编码
                .apiKey(apiKey)
                .model("qwen-vl-plus")
                .messages(Arrays.asList(systemMessage, userMessage))
                .responseFormat(jsonFormat)
                .build();

        // 5. 调用模型
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationResult result = conv.call(param);

        // 6. 提取 JSON 字符串
        String jsonOutput = result.getOutput()
                .getChoices().get(0)
                .getMessage().getContent().get(0)
                .get("text").toString();
        System.out.println("原始JSON: " + jsonOutput);

        // 7. 使用 Hutool 解析为 AuditImageResponse 对象
        try {
            return JSONUtil.toBean(jsonOutput, AuditImageResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("解析AI返回的JSON失败: " + jsonOutput, e);
        }
    }
}