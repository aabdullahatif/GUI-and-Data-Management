package viewmodel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Person;
import service.MyLogger;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;

import dao.DbConnectivityClass;
import dao.StorageUploader;

public class DB_GUI_Controller implements Initializable {

    StorageUploader store = new StorageUploader();
    @FXML
    TextField first_name, last_name, department, email, imageURL;
    @FXML
    MenuItem newItem, editItem, deleteItem;
    @FXML
    ComboBox<String> major_drop;
    @FXML
    ImageView img_view;
    @FXML
    MenuBar menuBar;
    @FXML
    private TableView<Person> tv;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();
    @FXML
    Button editButton;
    @FXML
    Button deleteButton;
    @FXML
    Button addButton;
    @FXML
    ProgressBar progressBar;
    @FXML
    TextField statusText;

    boolean canModify = false;
    boolean canAdd = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
        tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
        tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
        tv.setItems(data);

        for (majors m : majors.values()) {
            major_drop.getItems().add(m.toString().replace('_', ' '));
        }
        textBoxCheck();
    }

    @FXML
    protected void textBoxCheck() {
        canModify =
                !(first_name.getText().isEmpty() ||
                        last_name.getText().isEmpty() ||
                        department.getText().isEmpty() ||
                        major_drop.getValue().isBlank() ||
                        email.getText().isEmpty());
        canAdd =
                (!first_name.getText().isEmpty() &&
                        !last_name.getText().isEmpty() &&
                        !department.getText().isEmpty() &&
                        !major_drop.getValue().isBlank() &&
                        !email.getText().isEmpty());

        editButton.setDisable(!canModify);
        deleteButton.setDisable(!canModify);
        editItem.setDisable(!canModify);
        deleteItem.setDisable(!canModify);
        newItem.setDisable(!canAdd);
        addButton.setDisable(!canAdd);
        newItem.setDisable(!canAdd);
        editItem.setDisable(!canModify);
        deleteItem.setDisable(!canModify);
    }

    @FXML
    protected void addNewRecord() {
        Person p = new Person(first_name.getText(), last_name.getText(), department.getText(),
                major_drop.getValue(), email.getText(), imageURL.getText());
        cnUtil.insertUser(p);
        p.setId(cnUtil.retrieveId(p));
        data.add(p);
        clearForm();
        statusText.setText(DbConnectivityClass.status);
    }

    @FXML
    protected void clearForm() {
        first_name.clear();
        last_name.clear();
        department.clear();
        major_drop.setValue(null);
        email.clear();
        imageURL.clear();
        textBoxCheck();
        statusText.setText("Cleared details.");
    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root);
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void editRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        int index = data.indexOf(p);
        Person updatedPerson = new Person(first_name.getText(), last_name.getText(), department.getText(),
                major_drop.getValue(), email.getText(), imageURL.getText());
        updatedPerson.setId(p.getId());
        cnUtil.editUser(updatedPerson);
        data.set(index, updatedPerson);
        tv.getSelectionModel().select(index);
    }

    @FXML
    protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        cnUtil.deleteRecord(p);
        data.remove(p);
    }

    @FXML
    protected void showImage() {
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));
            Task<Void> uploadTask = createUploadTask(file);
            progressBar.progressProperty().bind(uploadTask.progressProperty());
            new Thread(uploadTask).start();
        }
    }

    @FXML
    protected void addRecord() {
        showSomeone();
    }

    @FXML
    protected to importCsv() {
        File file = new FileChooser().showOpenDialog(img_view.getScene().getWindow());
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                Person newPerson = new Person(parts[0], parts[1], parts[2], parts[3], parts[4], "");
                cnUtil.insertUser(newPerson);
            }
            statusText.setText("Import successful.");
        } catch (IOException | SQLException e) {
            statusText.setText("Import failed: " + e.getMessage());
        }
    }

    @FXML
    protected void exportCsv() throws IOException {
        File file = new File("export.csv");
        try (PrintWriter out = new PrintWriter(file)) {
            out.println("firstname,lastname,department,major,email");
            for (Person p : data) {
                out.printf("%s,%s,%s,%s,%s\n", p.getFirstName(), p.getLastName(), p.getDepartment(), p.getMajor(), p.getEmail());
            }
            statusText.setText("Exported to " + file.getAbsolutePath());
        }
    }

    private Task<Void> createUploadTask(File file) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                BlobClient blobClient = store.getContainerClient().getBlobClient(file.getName());
                try (InputStream is = new FileInputStream(file);
                     OutputStream os = blobClient.getBlockBlobClient().getBlobOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    long fileSize = Files.size(file.toPath());
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        updateProgress(totalBytesRead, fileSize);
                    }
                }
                return null;
            }
        };
    }

    private enum Major {Business, CSC, CPIS}

    private static class Results {
        String fname;
        String lname;
        Major major;

        public Results(String fname, String lname, Major major) {
            this.fname = fname;
            this.lname = lname;
            this.major = major;
        }
    }
}
