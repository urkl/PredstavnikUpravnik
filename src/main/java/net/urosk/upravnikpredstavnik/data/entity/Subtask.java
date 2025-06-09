// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/entity/Subtask.java
package net.urosk.upravnikpredstavnik.data.entity;

import lombok.Data;

@Data
public class Subtask {
    private String task;
    private boolean completed;
}