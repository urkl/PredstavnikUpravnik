// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/repository/UserRepository.java
package net.urosk.upravnikpredstavnik.data.repository;

import net.urosk.upravnikpredstavnik.data.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findByRolesIn(Set<String> roles); // NOVO
}