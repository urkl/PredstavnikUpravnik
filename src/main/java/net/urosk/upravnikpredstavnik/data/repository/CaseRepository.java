// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/repository/CaseRepository.java
package net.urosk.upravnikpredstavnik.data.repository;

import net.urosk.upravnikpredstavnik.data.entity.Case;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CaseRepository extends MongoRepository<Case, String> {
    List<Case> findByStatus(String status);
    List<Case> findByAuthorId(String authorId);
    List<Case> findFirst10ByAuthorIdOrderByCreatedDateDesc(String authorId);
    List<Case> findAllByStatusNot(String status);
    List<Case> findFirst10ByAuthorIdAndStatusNotOrderByCreatedDateDesc(String authorId, String status);
    Page<Case> findByStatus(String status, Pageable pageable);

}