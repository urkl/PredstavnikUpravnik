// FILE: src/main/java/net/urosk/upravnikpredstavnik/data/repository/BuildingRepository.java
package net.urosk.upravnikpredstavnik.data.repository;

import net.urosk.upravnikpredstavnik.data.entity.Building;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional; // NOV UVOZ

public interface BuildingRepository extends MongoRepository<Building, String> {
    Optional<Building> findByName(String name); // NOV DODATEK
}