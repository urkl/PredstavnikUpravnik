// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/entity/Case.java
package net.urosk.upravnikpredstavnik.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "cases")
public class Case {
    @Id
    private String id;
    private String title;
    private String description;
    private String status;
    @DBRef
    private User author;
    @DBRef
    private User assignedTo;
    private List<Comment> comments = new ArrayList<>();
    private List<Subtask> subtasks = new ArrayList<>();
    private List<AttachedFile> attachedFiles = new ArrayList<>();

    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;

}