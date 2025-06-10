package net.urosk.upravnikpredstavnik.data.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class AttachedFile {

    private String fileName;
    private String mimeType;
    private String comment;

    // Vsebino datoteke bomo shranili kot Binary, kar je optimizirano za MongoDB.
    @Field("content")
    private byte[] content;

    public AttachedFile(String fileName, String mimeType, byte[] content) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.content = content;
    }
}