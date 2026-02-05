package com.ping.pingpicture.infrastructure.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.ping.pingpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.ping.pingpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.ping.pingpicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.ping.pingpicture.infrastructure.exception.BusinessException;
import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import com.ping.pingpicture.infrastructure.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    //读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态 - %s 占位
    public static final String GET_OUT_PAINTING_TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/tasks/%s";


    /**
     * 创建扩图任务
     *
     * @param createOutPaintingTaskRequest 请求参数
     * @return 任务响应
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey) // 请求头必须包含 ApiKey
                // 必须开启异步处理，设置为 enable
                .header("X-DashScope-Async", "enable")    // 为何必填参数还需要显式指定？ A: 利于扩展
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));// 一定要使用hutool
        // 处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求失败，状态码：{}，响应内容：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图请求失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                log.error("请求失败，错误码：{}，错误信息：{}", errorCode, response.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId 任务 id
     * @return 任务响应
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        // 处理响应
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey) // 请求头必须包含 ApiKey
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常，响应内容：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图请求失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
