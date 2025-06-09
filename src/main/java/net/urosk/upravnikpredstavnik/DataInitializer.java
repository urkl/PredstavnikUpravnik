// FILE: src/main/java/net/urosk/upravnikpredstavnik/DataInitializer.java
package net.urosk.upravnikpredstavnik;

import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.Role;
import net.urosk.upravnikpredstavnik.data.Status;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CaseRepository caseRepository;

    public DataInitializer(UserRepository userRepository, CaseRepository caseRepository) {
        this.userRepository = userRepository;
        this.caseRepository = caseRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // Počisti obstoječe podatke (samo za razvoj)
            userRepository.deleteAll();
            caseRepository.deleteAll();

            // Ustvari uporabnike
            User stanovalec = new User();
            stanovalec.setName("Janez Novak");
            stanovalec.setEmail("janez.novak@example.com");
            stanovalec.setRole(Role.STANOVALEC);
            stanovalec.setActivated(true);
            userRepository.save(stanovalec);

            User upravnik = new User();
            upravnik.setName("Ana Kos");
            upravnik.setEmail("ana.kos@example.com");
            upravnik.setRole(Role.UPRAVNIK);
            upravnik.setActivated(true);
            userRepository.save(upravnik);

            User predstavnik = new User();
            predstavnik.setName("Marko Horvat");
            predstavnik.setEmail("marko.horvat@example.com");
            predstavnik.setRole(Role.PREDSTAVNIK);
            predstavnik.setActivated(true);
            userRepository.save(predstavnik);

            // Ustvari zadeve
            Case case1 = new Case();
            case1.setTitle("Menjava žarnice v 2. nadstropju");
            case1.setDescription("Žarnica na hodniku pred stanovanjem št. 12 ne deluje.");
            case1.setStatus(Status.PREDLOG);
            case1.setAuthor(stanovalec);
            case1.setAssignedTo(upravnik);
            case1.setCreatedDate(LocalDateTime.now().minusDays(2));
            case1.setLastModifiedDate(LocalDateTime.now().minusDays(2));
            caseRepository.save(case1);

            Case case2 = new Case();
            case2.setTitle("Čiščenje kolesarnice");
            case2.setDescription("Kolesarnica potrebuje temeljito čiščenje in ureditev.");
            case2.setStatus(Status.V_DELU);
            case2.setAuthor(predstavnik);
            case2.setAssignedTo(upravnik);
            case2.setCreatedDate(LocalDateTime.now().minusDays(10));
            case2.setLastModifiedDate(LocalDateTime.now().minusDays(1));
            caseRepository.save(case2);

            Case case3 = new Case();
            case3.setTitle("Popravilo vhodnih vrat");
            case3.setDescription("Vhodna vrata se ne zapirajo pravilno in ostajajo priprta.");
            case3.setStatus(Status.ZAKLJUCENO);
            case3.setAuthor(stanovalec);
            case3.setAssignedTo(upravnik);
            case3.setCreatedDate(LocalDateTime.now().minusMonths(1));
            case3.setLastModifiedDate(LocalDateTime.now().minusWeeks(2));
            caseRepository.save(case3);

            System.out.println("Vzorčni podatki so bili uspešno ustvarjeni.");
        }
    }
}