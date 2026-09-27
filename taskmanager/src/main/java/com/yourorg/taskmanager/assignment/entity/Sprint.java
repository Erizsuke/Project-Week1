package com.yourorg.taskmanager.assignment.entity;

import com.yourorg.taskmanager.project.entity.Project;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sprints")
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "sprint_number")
    private Integer sprintNumber;

    @Column(name = "planned_task_count")
    private Integer plannedTaskCount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SprintStatus status;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Integer getSprintNumber() { return sprintNumber; }
    public void setSprintNumber(Integer sprintNumber) { this.sprintNumber = sprintNumber; }

    public Integer getPlannedTaskCount() { return plannedTaskCount; }
    public void setPlannedTaskCount(Integer plannedTaskCount) { this.plannedTaskCount = plannedTaskCount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public SprintStatus getStatus() { return status; }
    public void setStatus(SprintStatus status) { this.status = status; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }
}
