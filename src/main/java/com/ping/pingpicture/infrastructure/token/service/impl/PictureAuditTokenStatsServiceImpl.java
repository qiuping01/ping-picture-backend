package com.ping.pingpicture.infrastructure.token.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ping.pingpicture.infrastructure.token.entity.PictureAuditTokenStats;
import com.ping.pingpicture.infrastructure.token.service.PictureAuditTokenStatsService;
import com.ping.pingpicture.infrastructure.token.mapper.PictureAuditTokenStatsMapper;
import org.springframework.stereotype.Service;

/**
* @author 21877
* @description 针对表【picture_audit_token_stats(图片 AI 调用 token 统计表)】的数据库操作Service实现
* @createDate 2026-04-09 15:12:10
*/
@Service
public class PictureAuditTokenStatsServiceImpl extends ServiceImpl<PictureAuditTokenStatsMapper, PictureAuditTokenStats>
    implements PictureAuditTokenStatsService{

}




