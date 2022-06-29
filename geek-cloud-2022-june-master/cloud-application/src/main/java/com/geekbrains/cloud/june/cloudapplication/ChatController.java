package com.geekbrains.cloud.june.cloudapplication;

import com.geekbrains.cloud.CloudMessage;
import com.geekbrains.cloud.FileMessage;
import com.geekbrains.cloud.FileRequest;
import com.geekbrains.cloud.ListFiles;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class ChatController implements Initializable {

    private String homeDir;

    @FXML
    public ListView<String> clientView;

    @FXML
    public ListView<String> serverView;

    private Network network;
    private Path pathToDir;

    private void readLoop() {
        try {
            while (true) {
                CloudMessage message = network.read();
                if (message instanceof ListFiles listFiles) {
                    Platform.runLater(() -> {
                        serverView.getItems().clear();
                        serverView.getItems().addAll(listFiles.getFiles());
                    });
                } else if (message instanceof FileMessage fileMessage) {
                    Path current = pathToDir.resolve(fileMessage.getName());
                    Files.write(current, fileMessage.getData());
                    Platform.runLater(() -> {
                        clientView.getItems().clear();
                        if(!(Path.of(homeDir).endsWith("client_files"))){
                            clientView.getItems().add(0,"[ ... ]");
                        }
                        clientView.getItems().addAll(pathToDir.toFile().list());
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Connection lost");
        }
    }

    // post init fx fields
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            homeDir = "client_files";
            clientView.getItems().clear();
            clientView.getItems().addAll(getFiles(homeDir));
            network = new Network(8189);
            pathToDir = Path.of(homeDir);
            Thread readThread = new Thread(this::readLoop);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private List<String> getFiles(String dir) {
        String[] list = new File(dir).list();
        assert list != null;
        return Arrays.asList(list);
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String file = clientView.getSelectionModel().getSelectedItem();
        network.write(new FileMessage(pathToDir.resolve(file)));
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String file = serverView.getSelectionModel().getSelectedItem();
        network.write(new FileRequest(file));
    }


    public void goToDir(MouseEvent mouseEvent) {
        Path path;
        File curDir;
        String nameDir = clientView.getSelectionModel().getSelectedItem(); // Выбранный файл
        if(nameDir.startsWith("[ ... ]")) {  // Проверяем не был ли нажат переход на папку выше
            pathToDir = pathToDir.getParent(); // Если да, то поднимаемся на папку выше
            curDir = new File(pathToDir.toString());
            if (pathToDir.endsWith("client_files")) { //Если это "верхняя папка, просто отображаем объекты
                clientView.getItems().clear();
                clientView.getItems().addAll(curDir.list());
            } else {    //Если у папки еще есть куда подниматься, добавляем в листвью 0 индексом ...
                clientView.getItems().clear();
                clientView.getItems().add(0, "[ ... ]");
                clientView.getItems().addAll(curDir.list());
            }
        }else { // Если выделен объект не [ ... ]
            path = pathToDir.resolve(nameDir).toAbsolutePath(); // делаем путь к объекту
            curDir = new File(path.toString());     // объект файла с таким путем
            if(!(curDir.isFile())){             // проверяем не директория ли это?
                clientView.getItems().clear(); // если да, выстраиваем список файлов + переход наверх
                clientView.getItems().add(0,"[ ... ]");
                clientView.getItems().addAll(curDir.list());
                pathToDir = path;
            }
        }
    }

    public void goToServerDir(MouseEvent mouseEvent) throws IOException {
        String dir = serverView.getSelectionModel().getSelectedItem();
        network.write(new GoToDirServer(dir)); // передаём имя директории в которую хотим войти
    }
}