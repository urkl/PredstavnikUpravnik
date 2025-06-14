package net.urosk.upravnikpredstavnik.data.entity;

import lombok.Getter;

@Getter
public enum Priority {
    REMINDER("Opomnik", "#616161"),   // Siva
    LOW("Nizka", "#4CAF50"),      // Zelena
    MEDIUM("Srednja", "#FFC107"), // Oranžna
    HIGH("Visoka", "#F44336");     // Rdeča

    private final String displayName;
    private final String color;

    Priority(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
}