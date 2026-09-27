package com.yourorg.taskmanager.assignment.repository;

import com.yourorg.taskmanager.assignment.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findBySprint_Id(UUID sprintId);
    List<Task> findByAssignedTo_Id(UUID userId);
}
