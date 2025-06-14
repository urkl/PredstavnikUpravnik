package net.urosk.upravnikpredstavnik.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.Comment;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;
    private final UserRepository userRepository;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.info.name:BlokApp}")
    private String appName;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Async
    public void notifyStatusChange(Case aCase, String oldStatus, String newStatus) {
        if (!mailEnabled) return;
        Set<User> recipients = getCaseStakeholders(aCase);
        String subject = String.format("[%s] Sprememba statusa: %s", appName, aCase.getTitle());
        String text = String.format(
                "Pozdravljeni,\n\nStatus zadeve '%s' se je spremenil iz '%s' v '%s'.",
                aCase.getTitle(), oldStatus, newStatus
        );
        recipients.forEach(user -> sendSimpleMessage(user.getEmail(), subject, text));
    }

    @Async
    public void notifyNewComment(Case aCase, Comment newComment) {
        if (!mailEnabled) return;
        Set<User> recipients = getCaseStakeholders(aCase);
        recipients.remove(newComment.getAuthor());
        if (recipients.isEmpty()) return;
        String subject = String.format("[%s] Nov komentar: %s", appName, aCase.getTitle());
        String text = String.format(
                "Pozdravljeni,\n\nUporabnik %s je dodal nov komentar na zadevi '%s':\n\n\"%s\"",
                newComment.getAuthor().getName(), aCase.getTitle(), newComment.getContent()
        );
        recipients.forEach(user -> sendSimpleMessage(user.getEmail(), subject, text));
    }

    private Set<User> getCaseStakeholders(Case aCase) {
        Set<User> recipients = new HashSet<>();
        if (aCase.getAuthor() != null) recipients.add(aCase.getAuthor());
        if (aCase.getAssignedTo() != null) recipients.add(aCase.getAssignedTo());
        if (aCase
                .getCoordinators() != null) recipients.addAll(aCase.getCoordinators());
        recipients.addAll(userRepository.findByRolesIn(Set.of("ROLE_UPRAVNIK", "ROLE_PREDSTAVNIK", "ROLE_ADMINISTRATOR")));
        return recipients;
    }

    private void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            log.info("E-pošta poslana na {}", to);
        } catch (Exception e) {
            log.error("Napaka pri pošiljanju e-pošte na {}: {}", to, e.getMessage());
        }
    }
}