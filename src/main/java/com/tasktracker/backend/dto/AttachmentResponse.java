package com.tasktracker.backend.dto;

import com.tasktracker.backend.model.Attachment;
import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        String fileName,
        String fileType,
        Long fileSize,
        Long taskId,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime uploadedAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                attachment.getTask().getId(),
                attachment.getUploadedBy().getId(),
                attachment.getUploadedBy().getName(),
                attachment.getUploadedAt()
        );
    }
}
