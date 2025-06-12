// FILE: src/main/java/net/urosk/upravnikpredstavnik/DataInitializer.java
package net.urosk.upravnikpredstavnik;

import net.urosk.upravnikpredstavnik.data.entity.Building;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays; // NOV UVOZ
import java.util.HashSet; // NOV UVOZ

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final BuildingRepository buildingRepository;

    public DataInitializer(UserRepository userRepository, CaseRepository caseRepository, BuildingRepository buildingRepository) {
        this.userRepository = userRepository;
        this.caseRepository = caseRepository;
        this.buildingRepository = buildingRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.deleteAll();
            caseRepository.deleteAll();
            buildingRepository.deleteAll();

            User stanovalec = new User();
            stanovalec.setName("Janez Novak");
            stanovalec.setEmail("janez.novak@example.com");
            stanovalec.setRoles(Set.of("ROLE_STANOVALEC"));
            stanovalec.setActivated(true);
            userRepository.save(stanovalec);

            User upravnik = new User();
            upravnik.setName("Ana Kos");
            upravnik.setEmail("ana.kos@example.com");
            upravnik.setRoles(Set.of("ROLE_UPRAVNIK"));
            upravnik.setActivated(true);
            userRepository.save(upravnik);

            User predstavnik = new User();
            predstavnik.setName("Marko Horvat");
            predstavnik.setEmail("marko.horvat@example.com");
            predstavnik.setRoles(Set.of("ROLE_PREDSTAVNIK", "ROLE_UPRAVNIK"));
            predstavnik.setActivated(true);
            userRepository.save(predstavnik);

            User administrator = new User();
            administrator.setName("Admin User");
            administrator.setEmail("uros.kristan@gmail.com");
            administrator.setRoles(Set.of("ROLE_ADMINISTRATOR", "ROLE_PREDSTAVNIK", "ROLE_UPRAVNIK", "ROLE_STANOVALEC"));
            administrator.setActivated(true);
            userRepository.save(administrator);


            Building building1 = new Building();
            building1.setName("Sončni Dvor");
            building1.setAddress("Sončna ulica 1, Ljubljana");
            buildingRepository.save(building1);

            Building building2 = new Building();
            building2.setName("Mestna Rezidenca");
            building2.setAddress("Glavna cesta 15, Maribor");
            buildingRepository.save(building2);

            Building building3 = new Building();
            building3.setName("Gorski Apartmaji");
            building3.setAddress("Planinska pot 3, Bled");
            buildingRepository.save(building3);

            // SPREMENJENO NA SET
            upravnik.setManagedBuildings(new HashSet<>(Arrays.asList(building1, building2, building3)));
            userRepository.save(upravnik);

            // SPREMENJENO NA SET
            predstavnik.setManagedBuildings(Set.of(building1));
            userRepository.save(predstavnik);

            // SPREMENJENO NA SET
            stanovalec.setManagedBuildings(Set.of(building2));
            userRepository.save(stanovalec);


            Case case1 = new Case();
            case1.setTitle("Menjava žarnice v 2. nadstropju");
            case1.setDescription("Žarnica na hodniku pred stanovanjem št. 12 ne deluje.");
            case1.setStatus("PREDLOG");
            case1.setAuthor(stanovalec);
            case1.setAssignedTo(upravnik);
            case1.setCreatedDate(LocalDateTime.now().minusDays(2));
            case1.setLastModifiedDate(LocalDateTime.now().minusDays(2));
            case1.setBuildings(Set.of(building2)); // SPREMENJENO NA SET
            case1.setStartDate(LocalDateTime.now().plusDays(5));
            case1.setEndDate(LocalDateTime.now().plusDays(7));
            caseRepository.save(case1);

            Case case2 = new Case();
            case2.setTitle("Čiščenje kolesarnice");
            case2.setDescription("Kolesarnica potrebuje temeljito čiščenje in ureditev.");
            case2.setStatus("V_DELU");
            case2.setAuthor(predstavnik);
            case2.setAssignedTo(upravnik);
            case2.setCreatedDate(LocalDateTime.now().minusDays(10));
            case2.setLastModifiedDate(LocalDateTime.now().minusDays(1));
            case2.setBuildings(Set.of(building1)); // SPREMENJENO NA SET
            case2.setStartDate(LocalDateTime.now().minusDays(3));
            case2.setEndDate(LocalDateTime.now().plusDays(2));
            caseRepository.save(case2);

            Case case3 = new Case();
            case3.setTitle("Popravilo vhodnih vrat");
            case3.setDescription("Vhodna vrata se ne zapirajo pravilno in ostajajo priprta.");
            case3.setStatus("ZAKLJUCENO");
            case3.setAuthor(stanovalec);
            case3.setAssignedTo(upravnik);
            case3.setCreatedDate(LocalDateTime.now().minusMonths(1));
            case3.setLastModifiedDate(LocalDateTime.now().minusWeeks(2));
            case3.setBuildings(new HashSet<>(Arrays.asList(building2, building3))); // SPREMENJENO NA SET
            case3.setStartDate(LocalDateTime.now().minusMonths(1).withDayOfMonth(5));
            case3.setEndDate(LocalDateTime.now().minusWeeks(3));
            caseRepository.save(case3);

            Case case4 = new Case();
            case4.setTitle("Pregled strehe");
            case4.setDescription("Redni letni pregled stanja strehe.");
            case4.setStatus("PREDLOG");
            case4.setAuthor(upravnik);
            case4.setAssignedTo(upravnik);
            case4.setCreatedDate(LocalDateTime.now().minusDays(1));
            case4.setLastModifiedDate(LocalDateTime.now().minusDays(1));
            case4.setBuildings(Set.of(building1)); // SPREMENJENO NA SET
            case4.setStartDate(LocalDateTime.now().plusWeeks(2));
            caseRepository.save(case4);

            System.out.println("Vzorčni podatki so bili uspešno ustvarjeni.");
        }
    }
}