package com.yourorg.taskmanager.project.repository;

import com.yourorg.taskmanager.project.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {

    List<ProjectDocument> findAllByProject_IdOrderByUploadedAtDesc(UUID projectId);

    Optional<ProjectDocument> findByIdAndProject_Id(UUID id, UUID projectId);

    @Query("select d from ProjectDocument d where d.project.id = :projectId and "
            + "(lower(d.originalFilename) like lower(concat('%', :query, '%')) "
            + "or lower(d.extractedText) like lower(concat('%', :query, '%'))) "
            + "order by d.uploadedAt desc")
    List<ProjectDocument> searchInProject(@Param("projectId") UUID projectId,
                                          @Param("query") String query);
}