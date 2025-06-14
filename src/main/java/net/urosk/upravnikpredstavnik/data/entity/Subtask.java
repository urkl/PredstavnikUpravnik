// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/entity/Subtask.java
package net.urosk.upravnikpredstavnik.data.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor

@NoArgsConstructor
public class Subtask {
    private String task;
    private boolean completed;

}