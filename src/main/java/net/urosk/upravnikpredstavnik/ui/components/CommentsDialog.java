package net.urosk.upravnikpredstavnik.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import net.urosk.upravnikpredstavnik.data.entity.Case;
import net.urosk.upravnikpredstavnik.data.entity.Comment;
import net.urosk.upravnikpredstavnik.data.entity.User;
import net.urosk.upravnikpredstavnik.data.repository.CaseRepository;
import net.urosk.upravnikpredstavnik.security.AuthenticatedUser;
import net.urosk.upravnikpredstavnik.service.AuditService; // NOV IMPORT

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class CommentsDialog extends Dialog {

    private final Case aCase;
    private final CaseRepository caseRepository;
    private final User currentUser;
    private final VerticalLayout commentsContainer;
    private final Runnable onCloseCallback;
    private final AuditService auditService; // NOVO POLJE
    private final AuthenticatedUser authenticatedUser;

    // === SPREMENJEN KONSTRUKTOR ===
    public CommentsDialog(Case aCase, CaseRepository caseRepository, AuthenticatedUser authenticatedUser, AuditService auditService, Runnable onCloseCallback) {
        this.aCase = aCase;
        this.caseRepository = caseRepository;
        this.currentUser = authenticatedUser.get().orElse(null);
        this.auditService = auditService; // Shrani servis
        this.onCloseCallback = onCloseCallback;
        this.authenticatedUser = authenticatedUser;

        setHeaderTitle("Pogovor: " + aCase.getTitle());
        setDraggable(true);
        setResizable(true);
        setWidth("min(90vw, 700px)");
        setHeight("80vh");

        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);

        commentsContainer = new VerticalLayout();
        commentsContainer.getStyle().set("overflow-y", "auto");
        commentsContainer.setSizeFull();
        commentsContainer.setSpacing(true);

        mainLayout.add(commentsContainer);
        mainLayout.expand(commentsContainer);

        if (currentUser != null) {
            mainLayout.add(createCommentForm());
        } else {
            mainLayout.add(new Span("Za komentiranje se morate prijaviti."));
        }

        add(mainLayout);

        Button closeButton = new Button("Zapri", e -> close());
        getFooter().add(closeButton);

        addDialogCloseActionListener(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });

        refreshComments();
    }

    private Component createCommentForm() {
        TextArea commentInput = new TextArea();
        commentInput.setPlaceholder("Napišite sporočilo...");
        commentInput.setWidthFull();

        Button sendButton = new Button("Pošlji", new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        sendButton.addClickListener(e -> {
            String content = commentInput.getValue();
            if (content != null && !content.trim().isEmpty()) {
                addComment(content.trim());
                commentInput.clear();
                commentInput.focus();
            }
        });

        HorizontalLayout form = new HorizontalLayout(commentInput, sendButton);
        form.setAlignItems(FlexComponent.Alignment.CENTER);
        form.setWidthFull();
        form.expand(commentInput);
        return form;
    }

    private void addComment(String content) {
        Comment newComment = new Comment();
        newComment.setAuthor(currentUser);
        newComment.setContent(content);
        newComment.setTimestamp(LocalDateTime.now());
        aCase.getComments().add(newComment);
        caseRepository.save(aCase);
        String userEmail = authenticatedUser.get().map(User::getEmail).orElse("Neznan uporabnik");
        // === NOVO: Beleženje dogodka ===
        auditService.log("NOV KOMENTAR", Case.class, aCase.getId(), "Dodan nov komentar.",userEmail);

        refreshComments();
    }

    private void refreshComments() {
        commentsContainer.removeAll();
        if (aCase.getComments() != null) {
            aCase.getComments().stream()
                    .sorted(Comparator.comparing(Comment::getTimestamp))
                    .forEach(comment -> commentsContainer.add(createCommentBubble(comment)));
        }
        commentsContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    private Component createCommentBubble(Comment comment) {
        String authorName = (comment.getAuthor() != null) ? comment.getAuthor().getName() : "Neznan avtor";
        Span authorSpan = new Span(authorName);
        authorSpan.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");

        Span timestampSpan = new Span(comment.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm, dd.MM.yy")));
        timestampSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout header = new HorizontalLayout(authorSpan, timestampSpan);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Paragraph content = new Paragraph(comment.getContent());
        content.getStyle().set("margin-top", "0").set("white-space", "pre-wrap");

        VerticalLayout bubble = new VerticalLayout(header, content);
        bubble.setSpacing(false);
        bubble.setSizeUndefined();
        bubble.getStyle()
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("max-width", "80%");

        boolean isMyComment = currentUser != null && comment.getAuthor() != null &&
                currentUser.getId().equals(comment.getAuthor().getId());

        HorizontalLayout wrapper = new HorizontalLayout(bubble);
        wrapper.setWidthFull();

        if (isMyComment) {
            bubble.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
            wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        } else {
            bubble.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
            wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        }

        return wrapper;
    }
}