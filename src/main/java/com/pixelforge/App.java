package com.pixelforge;

import com.pixelforge.mcp.PixelForgeMcpServer;
import com.pixelforge.ui.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

public final class App extends Application {

    private PixelForgeMcpServer mcpServer;

    @Override
    public void start(Stage stage) {
        MainWindow window = new MainWindow();
        Scene scene = new Scene(window.getRoot(), 1200, 800);

        stage.setTitle("Mirage");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.show();

        List<String> args = getParameters().getRaw();
        if (args.contains("--mcp")) {
            mcpServer = PixelForgeMcpServer.start(
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
