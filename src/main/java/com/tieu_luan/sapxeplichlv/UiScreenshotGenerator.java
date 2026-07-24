package com.tieu_luan.sapxeplichlv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

/**
 * Renders documentation screenshots from FXML without initializing controllers
 * or connecting to the database.
 */
public class UiScreenshotGenerator extends Application {

    private static final Path OUTPUT_DIRECTORY = Path.of("docs", "images");

    private static final List<Screen> SCREENS = List.of(
            new Screen("intro_page", "intro", 800, 600),
            new Screen("login", "login", 800, 600),
            new Screen("manager_home", "manager-home", 1366, 768),
            new Screen("manager_employee", "employee-management", 1366, 768),
            new Screen("manager_schedule", "schedule-management", 1366, 768),
            new Screen("employee_home", "employee-home", 1366, 768),
            new Screen("employee_request", "shift-request", 1366, 768),
            new Screen("employee_profile", "employee-profile", 1366, 768));

    @Override
    public void start(Stage stage) throws Exception {
        Files.createDirectories(OUTPUT_DIRECTORY);

        for (Screen screen : SCREENS) {
            Parent root = loadPreview(screen.fxmlName());
            Scene scene = new Scene(root, screen.width(), screen.height());

            stage.setScene(scene);
            stage.setWidth(screen.width());
            stage.setHeight(screen.height());
            stage.show();

            root.resize(screen.width(), screen.height());
            root.applyCss();
            root.layout();

            WritableImage image = new WritableImage(screen.width(), screen.height());
            root.snapshot(null, image);
            Path output = OUTPUT_DIRECTORY.resolve(screen.outputName() + ".png");
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", output.toFile());
            System.out.println("Created " + output);
        }

        stage.close();
    }

    private Parent loadPreview(String fxmlName) throws IOException {
        URL resource = App.class.getResource("views/" + fxmlName + ".fxml");
        if (resource == null) {
            throw new IOException("FXML not found: " + fxmlName);
        }

        String fxml;
        try (var input = resource.openStream()) {
            fxml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        String previewFxml = fxml
                .replaceAll("\\s+fx:controller=\"[^\"]+\"", "")
                .replaceAll("\\s+on[A-Z][A-Za-z]+=\"#[^\"]+\"", "");

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(resource);
        return loader.load(new ByteArrayInputStream(
                previewFxml.getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record Screen(
            String fxmlName,
            String outputName,
            int width,
            int height) {
    }
}
