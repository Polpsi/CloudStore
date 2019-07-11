package com.flamexander.netty.example.client;

import com.flamexander.netty.example.common.AbstractMessage;
import com.flamexander.netty.example.common.FileMessage;
import com.flamexander.netty.example.common.FileRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    TextField tfFileName;

    @FXML
    TextField sfFileName;

    @FXML
    ListView<String> filesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }

                    if (am instanceof FileRequest) {
                        FileRequest fr = (FileRequest) am;
                        if (Files.exists(Paths.get("client_storage/" + fr.getFilename()))) {
                            FileMessage fm = new FileMessage(Paths.get("client_storage/" + fr.getFilename()));
                            Network.sendMsg(am);
                        }
                    }

                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new FileRequest(tfFileName.getText()));
            tfFileName.clear();
        }
    }

    public void pressOnSendBtn(ActionEvent actionEvent) {
        if (sfFileName.getLength() > 0) {
            if (Files.exists(Paths.get("client_storage/" + sfFileName.getText()))) {
                FileMessage fm = null;
                try {
                    fm = new FileMessage(Paths.get("client_storage/" + sfFileName.getText()));
                    Network.sendMsg(fm);
                    sfFileName.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
