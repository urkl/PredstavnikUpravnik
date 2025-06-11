// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/repository/CaseRepository.java
package net.urosk.upravnikpredstavnik.data.repository;

import net.urosk.upravnikpredstavnik.data.entity.Case;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CaseRepository extends MongoRepository<Case, String> {
    List<Case> findByStatus(String status);
    List<Case> findByAuthorId(String authorId);
}