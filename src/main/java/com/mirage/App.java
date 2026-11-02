package com.mirage;

import com.mirage.icon.AppIcon;
import com.mirage.mcp.MirageMcpServer;
import com.mirage.ui.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class App extends Application {

    private MirageMcpServer mcpServer;

    @Override
    public void start(Stage stage) throws IOException {
        MainWindow window = new MainWindow();
        Scene scene = new Scene(window.getRoot(), 1200, 800);

        stage.setTitle("Mirage");
        List<Image> icons = new ArrayList<>();
        for (int size : AppIcon.SIZES) {
            icons.add(new Image(new ByteArrayInputStream(AppIcon.png(AppIcon.defaultIcon(size)))));
        }
        stage.getIcons().setAll(icons);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.show();

        List<String> args = getParameters().getRaw();
        if (args.contains("--mcp")) {
            mcpServer = MirageMcpServer.start(
                    window,
                    outputDirectory(args)
            );
        }
    }

    @Override
    public void stop() {
        if (mcpServer != null) {
            mcpServer.close();
        }
    }

    private static Path outputDirectory(List<String> args) {
        int optionIndex = args.indexOf("--output-dir");
        if (optionIndex < 0) {
            return Path.of("output");
        }
        if (optionIndex + 1 >= args.size()
                || args.get(optionIndex + 1).startsWith("--")
                || optionIndex + 2 != args.size()) {
            throw new IllegalArgumentException(
                    "Usage: --mcp [--output-dir <directory>]"
            );
        }
        return Path.of(args.get(optionIndex + 1));
    }

    public static void main(String[] args) {
        boolean mcpMode = List.of(args).contains("--mcp");
        launch(args);
        if (mcpMode) {
            System.exit(0);
        }
    }
}
