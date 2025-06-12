// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/entity/User.java
package net.urosk.upravnikpredstavnik.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Document(collection = "users")
public class User implements Serializable {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String email;
    private Set<String> roles = new HashSet<>();
    @DBRef //
    private Set<Building> managedBuildings = new HashSet<>(); // NOV DODATEK
    private boolean activated;
}