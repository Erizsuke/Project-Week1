package com.yourorg.taskmanager.assignment.repository;

import com.yourorg.taskmanager.assignment.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByProject_Id(UUID projectId);
}
