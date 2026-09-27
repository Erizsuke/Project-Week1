package com.yourorg.taskmanager.assignment.repository;

import com.yourorg.taskmanager.assignment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByTask_Id(UUID taskId);
    List<Submission> findBySubmittedBy_Id(UUID userId);
}
