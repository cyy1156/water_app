package com.waterapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话记录实体类
 */
@Data
@TableName("chat_record")
public class ChatRecord {
    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色：user-用户, assistant-AI
     */
    private String role;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

