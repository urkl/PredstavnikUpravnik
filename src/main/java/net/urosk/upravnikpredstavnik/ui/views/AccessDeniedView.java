package net.urosk.upravnikpredstavnik.ui.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "access-denied")
@PageTitle("Dostop Zavrnjen")
@AnonymousAllowed // Dovolimo dostop vsem, da lahko vidijo sporočilo
public class AccessDeniedView extends VerticalLayout {
    public AccessDeniedView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Dostop zavrnjen");
        Paragraph explanation = new Paragraph("Žal nimate ustreznih pravic za dostop do te strani.");
        add(title, explanation);
    }
}