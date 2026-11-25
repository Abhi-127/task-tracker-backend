package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.AttachmentResponse;
import com.tasktracker.backend.dto.CommentRequest;
import com.tasktracker.backend.dto.CommentResponse;
import com.tasktracker.backend.exception.ApiException;
import com.tasktracker.backend.model.Attachment;
import com.tasktracker.backend.model.Comment;
import com.tasktracker.backend.model.Task;
import com.tasktracker.backend.model.User;
import com.tasktracker.backend.repository.AttachmentRepository;
import com.tasktracker.backend.repository.CommentRepository;
import com.tasktracker.backend.repository.TaskRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks/{taskId}")
public class CollaborationController {

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public CollaborationController(
            TaskRepository taskRepository,
            CommentRepository commentRepository,
            AttachmentRepository attachmentRepository
    ) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @PostMapping("/comments")
    @Transactional
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CommentRequest request
    ) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));

        Comment comment = new Comment();
        comment.setContent(request.content().trim());
        comment.setTask(task);
        comment.setAuthor(currentUser);

        Comment saved = commentRepository.save(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(saved));
    }

    @GetMapping("/comments")
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(@PathVariable Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @PostMapping("/attachments")
    @Transactional
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file cannot be empty");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String cleanName = Paths.get(originalName).getFileName().toString();
            String storedName = UUID.randomUUID() + "_" + cleanName;
            Path targetLocation = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = new Attachment();
            attachment.setFileName(cleanName);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setFilePath(targetLocation.toString());
            attachment.setTask(task);
            attachment.setUploadedBy(currentUser);

            Attachment saved = attachmentRepository.save(attachment);
            return ResponseEntity.status(HttpStatus.CREATED).body(AttachmentResponse.from(saved));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file: " + ex.getMessage());
        }
    }

    @GetMapping("/attachments")
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(@PathVariable Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return attachmentRepository.findByTaskIdOrderByUploadedAtDesc(taskId)
                .stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId
    ) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));

        if (!attachment.getTask().getId().equals(taskId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment does not belong to this task");
        }

        try {
            Path filePath = Paths.get(attachment.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "File not found on server");
            }

            String contentType = attachment.getFileType() != null
                    ? attachment.getFileType()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download file");
        }
    }
}
