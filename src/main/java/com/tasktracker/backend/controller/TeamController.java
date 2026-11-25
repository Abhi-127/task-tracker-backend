package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.InviteMemberRequest;
import com.tasktracker.backend.dto.TaskResponse;
import com.tasktracker.backend.dto.TeamRequest;
import com.tasktracker.backend.dto.TeamResponse;
import com.tasktracker.backend.exception.ApiException;
import com.tasktracker.backend.model.Team;
import com.tasktracker.backend.model.User;
import com.tasktracker.backend.repository.TaskRepository;
import com.tasktracker.backend.repository.TeamRepository;
import com.tasktracker.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public TeamController(
            TeamRepository teamRepository,
            UserRepository userRepository,
            TaskRepository taskRepository
    ) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TeamRequest request
    ) {
        Team team = new Team();
        team.setName(request.name().trim());
        team.setDescription(request.description());
        team.setCreatedBy(currentUser);
        team.getMembers().add(currentUser);

        Team saved = teamRepository.save(team);
        return ResponseEntity.status(HttpStatus.CREATED).body(TeamResponse.from(saved));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams(@AuthenticationPrincipal User currentUser) {
        return teamRepository.findByMembersId(currentUser.getId())
                .stream()
                .map(TeamResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found"));
        return ResponseEntity.ok(TeamResponse.from(team));
    }

    @PostMapping("/{id}/invite")
    @Transactional
    public ResponseEntity<TeamResponse> inviteMember(
            @PathVariable Long id,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found"));
        User userToInvite = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User with this email not found"));

        team.getMembers().add(userToInvite);
        Team updated = teamRepository.save(team);
        return ResponseEntity.ok(TeamResponse.from(updated));
    }

    @PostMapping("/{id}/join")
    @Transactional
    public ResponseEntity<TeamResponse> joinTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found"));

        team.getMembers().add(currentUser);
        Team updated = teamRepository.save(team);
        return ResponseEntity.ok(TeamResponse.from(updated));
    }

    @GetMapping("/{id}/tasks")
    @Transactional(readOnly = true)
    public List<TaskResponse> getTeamTasks(@PathVariable Long id) {
        if (!teamRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Team not found");
        }
        return taskRepository.findByTeamId(id, Sort.by("createdAt").descending())
                .stream()
                .map(TaskResponse::from)
                .toList();
    }
}