// src/main/java/net/urosk/upravnikpredstavnik/data/entity/Building.java
package net.urosk.upravnikpredstavnik.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "buildings")
public class Building {
    @Id
    private String id;
    @Indexed(unique = true)
    private String name;
    private String address;
}