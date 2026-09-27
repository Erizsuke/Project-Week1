package com.yourorg.taskmanager.project.repository;

import com.yourorg.taskmanager.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
}
