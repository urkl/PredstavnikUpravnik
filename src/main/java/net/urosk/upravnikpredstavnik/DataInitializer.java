package net.urosk.upravnikpredstavnik;

import net.urosk.upravnikpredstavnik.data.entity.Building; // NOV UVOZ
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.BuildingRepository; // NOV UVOZ
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.ArrayList; // NOV UVOZ

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final BuildingRepository buildingRepository; // NOV DODATEK

    public DataInitializer(UserRepository userRepository, CaseRepository caseRepository, BuildingRepository buildingRepository) {
        this.userRepository = userRepository;
        this.caseRepository = caseRepository;
        this.buildingRepository = buildingRepository; // NOV DODATEK
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // Počisti obstoječe podatke (samo za razvoj)
            userRepository.deleteAll();
            caseRepository.deleteAll();
            buildingRepository.deleteAll(); // NOV DODATEK

            // --- SPREMEMBE SPODAJ ---

            // Ustvari uporabnike z listo vlog
            User stanovalec = new User();
            stanovalec.setName("Janez Novak");
            stanovalec.setEmail("janez.novak@example.com");
            stanovalec.setRoles(Set.of("STANOVALEC")); // Vloga kot seznam nizov
            stanovalec.setActivated(true);
            userRepository.save(stanovalec);

            User upravnik = new User();
            upravnik.setName("Ana Kos");
            upravnik.setEmail("ana.kos@example.com");
            upravnik.setRoles(Set.of("UPRAVNIK")); // Vloga kot seznam nizov
            upravnik.setActivated(true);
            userRepository.save(upravnik);

            User predstavnik = new User();
            predstavnik.setName("Marko Horvat");
            predstavnik.setEmail("marko.horvat@example.com");
            predstavnik.setRoles(Set.of("PREDSTAVNIK", "UPRAVNIK")); // Lahko ima več vlog
            predstavnik.setActivated(true);
            userRepository.save(predstavnik);

            // NOV DODATEK: Ustvari objekte (Building)
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

            // NOV DODATEK: Poveži uporabnike z objekti, ki jih upravljajo
            upravnik.setManagedBuildings(Set.of(building1, building2, building3));
            userRepository.save(upravnik);

            predstavnik.setManagedBuildings(Set.of(building1));
            userRepository.save(predstavnik);

            stanovalec.setManagedBuildings(Set.of(building2)); // Stanovalec pripada eni zgradbi
            userRepository.save(stanovalec);


            // Ustvari zadeve s statusi kot nizi in dodaj objekte
            Case case1 = new Case();
            case1.setTitle("Menjava žarnice v 2. nadstropju");
            case1.setDescription("Žarnica na hodniku pred stanovanjem št. 12 ne deluje.");
            case1.setStatus("PREDLOG"); // Status kot niz
            case1.setAuthor(stanovalec);
            case1.setAssignedTo(upravnik);
            case1.setCreatedDate(LocalDateTime.now().minusDays(2));
            case1.setLastModifiedDate(LocalDateTime.now().minusDays(2));
            case1.setBuildings(Set.of(building2)); // Poveži z zgradbo 2
            caseRepository.save(case1);

            Case case2 = new Case();
            case2.setTitle("Čiščenje kolesarnice");
            case2.setDescription("Kolesarnica potrebuje temeljito čiščenje in ureditev.");
            case2.setStatus("V_DELU"); // Status kot niz
            case2.setAuthor(predstavnik);
            case2.setAssignedTo(upravnik);
            case2.setCreatedDate(LocalDateTime.now().minusDays(10));
            case2.setLastModifiedDate(LocalDateTime.now().minusDays(1));
            case2.setBuildings(Set.of(building1)); // Poveži z zgradbo 1
            caseRepository.save(case2);

            Case case3 = new Case();
            case3.setTitle("Popravilo vhodnih vrat");
            case3.setDescription("Vhodna vrata se ne zapirajo pravilno in ostajajo priprta.");
            case3.setStatus("ZAKLJUCENO"); // Status kot niz
            case3.setAuthor(stanovalec);
            case3.setAssignedTo(upravnik);
            case3.setCreatedDate(LocalDateTime.now().minusMonths(1));
            case3.setLastModifiedDate(LocalDateTime.now().minusWeeks(2));
            case3.setBuildings(Set.of(building2, building3)); // Poveži z zgradbo 2 in 3
            caseRepository.save(case3);

            System.out.println("Vzorčni podatki so bili uspešno ustvarjeni.");
        }
    }
}