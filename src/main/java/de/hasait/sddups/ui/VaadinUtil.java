package de.hasait.sddups.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableRunnable;

/**
 *
 */
public final class VaadinUtil {

    public static void accessUiOfComponent(Component component, SerializableRunnable accessTask, SerializableRunnable detachHandler) {
        component.getUI().ifPresent(ui -> ui.accessLater(accessTask, detachHandler).run());
    }

    private VaadinUtil() {
        super();
    }

}
