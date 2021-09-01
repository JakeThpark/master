package com.wanpan.app.dto.reebonz;

import lombok.Data;

@Data
public class UpdateCommentReply {
    private long userId;
    private long replyId;
    private String content;
    private String createdAt;
}
