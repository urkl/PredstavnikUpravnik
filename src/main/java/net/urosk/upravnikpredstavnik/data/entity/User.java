// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/entity/User.java
package net.urosk.upravnikpredstavnik.data.entity;

import lombok.Data;
import net.urosk.upravnikpredstavnik.data.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Data
@Document(collection = "users")
public class User implements Serializable {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String email;
    private Role role;
    private boolean activated;
}