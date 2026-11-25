package com.tasktracker.backend.repository;

import com.tasktracker.backend.model.Task;
import com.tasktracker.backend.model.TaskStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedToId(Long userId, Sort sort);
    List<Task> findByTeamId(Long teamId, Sort sort);

    @Query("""
           SELECT t FROM Task t
           WHERE (:status IS NULL OR t.status = :status)
             AND (:teamId IS NULL OR t.team.id = :teamId)
             AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId)
             AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
           """)
    List<Task> searchTasks(
            @Param("status") TaskStatus status,
            @Param("teamId") Long teamId,
            @Param("assignedToId") Long assignedToId,
            @Param("keyword") String keyword,
            Sort sort
    );
}
