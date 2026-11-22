package com.tasktracker.backend.repository;

import com.tasktracker.backend.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTaskIdOrderByUploadedAtDesc(Long taskId);
}