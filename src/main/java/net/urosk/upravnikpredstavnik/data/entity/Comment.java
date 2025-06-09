// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/entity/Comment.java
package net.urosk.upravnikpredstavnik.data.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

@Data
public class Comment {
    @DBRef
    private User author;
    private String content;
    private LocalDateTime timestamp;
}