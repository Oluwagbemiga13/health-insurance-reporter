package cz.oluwagbemiga.eutax.ui;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides the shared application icon for every window, ensuring future frames reuse the same image.
 */
@Slf4j
public final class AppIconProvider {

    private static final String ICON_RESOURCE = "/assets/at_logo.jpg";
    private static Image cachedIcon;

    private AppIconProvider() {
        throw new IllegalStateException("Utility class");
    }

    public static void apply(Window window) {
        if (window == null) {
            return;
        }
        Image icon = getIcon();
        if (icon != null) {
            window.setIconImage(icon);
        }
    }

    private static Image getIcon() {
        Image icon = cachedIcon;
        if (icon != null) {
            return icon;
        }
        synchronized (AppIconProvider.class) {
            if (cachedIcon == null) {
                cachedIcon = loadIcon();
            }
            return cachedIcon;
        }
    }

    private static Image loadIcon() {
        try (InputStream stream = AppIconProvider.class.getResourceAsStream(ICON_RESOURCE)) {
            if (stream == null) {
                log.warn("Application icon {} not found; falling back to default.", ICON_RESOURCE);
                return fallbackIcon();
            }
            Image image = ImageIO.read(stream);
            if (image == null) {
                log.warn("Unable to decode application icon {}; falling back to default.", ICON_RESOURCE);
                return fallbackIcon();
            }
            return image;
        } catch (IOException ex) {
            log.warn("Failed to load application icon {}; falling back to default.", ICON_RESOURCE, ex);
            return fallbackIcon();
        }
    }

    private static Image fallbackIcon() {
        Icon i = UIManager.getIcon("OptionPane.informationIcon");
        if (i instanceof ImageIcon icon) {
            return icon.getImage();
        }
        return null;
    }
}
