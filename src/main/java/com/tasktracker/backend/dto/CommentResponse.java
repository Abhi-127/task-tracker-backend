package com.tasktracker.backend.dto;

import com.tasktracker.backend.model.Comment;
import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        Long taskId,
        Long authorId,
        String authorName,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getTask().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getCreatedAt()
        );
    }
}
