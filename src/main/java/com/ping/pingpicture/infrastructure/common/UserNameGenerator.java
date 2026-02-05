package com.ping.pingpicture.infrastructure.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ping.pingpicture.infrastructure.mapper.UserMapper;
import com.ping.pingpicturebackend.model.entity.User;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户名生成器 - Spring Bean版本
 */
@Component
public class UserNameGenerator {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 生成唯一用户名
     */
    public String generateUniqueUserName(String baseUserName) {
        String finalUserName;
        int maxRetry = 5;
        
        for (int i = 0; i < maxRetry; i++) {
            String randomSuffix = RandomStringUtils.randomNumeric(4);
            finalUserName = baseUserName + randomSuffix;
            
            if (!isUserNameExists(finalUserName)) {
                return finalUserName;
            }
        }
        
        return generateUniqueUserNameWithTimestamp(baseUserName);
    }
    
    private boolean isUserNameExists(String userName) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userName", userName);
        return userMapper.selectCount(queryWrapper) > 0;
    }
    
    private String generateUniqueUserNameWithTimestamp(String baseUserName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uniqueSuffix = timestamp.substring(timestamp.length() - 8) + 
                             RandomStringUtils.randomNumeric(2);
        return baseUserName + uniqueSuffix;
    }
}