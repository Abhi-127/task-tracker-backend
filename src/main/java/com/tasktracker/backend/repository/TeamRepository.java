package com.tasktracker.backend.repository;

import com.tasktracker.backend.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByMembersId(Long userId);
}
