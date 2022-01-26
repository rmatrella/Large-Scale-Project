package main.java.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Utility {

    public static void infoBox(String infoMessage, String titleBar, String headerMessage)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titleBar);
        alert.setHeaderText(headerMessage);
        alert.setContentText(infoMessage);
        alert.showAndWait();
    }


    public static void showUsers(GridPane list, ArrayList<Document> filter, int item) throws IOException {

        list.getChildren().clear();

        try(FileInputStream imageStream = new FileInputStream("target/classes/img/user.png"))
        {
            Image image = new Image(imageStream);

            Label username = new Label(filter.get(item).getString("username"));
            Label country = new Label(filter.get(item).getString("country"));
            Label city = new Label(filter.get(item).getString("city"));

            list.add(new ImageView(image), 0, 0);
            list.add(username, 0, 1);
            list.add(country, 0, 2);
            list.add(city, 0, 3);

            GridPane.setHalignment(username, HPos.CENTER);
            GridPane.setHalignment(country, HPos.CENTER);
            GridPane.setHalignment(city, HPos.CENTER);
        }
        list.setStyle(
                "    -fx-padding: 20;\n" +
                        "    -fx-hgap: 10;\n" +
                        "    -fx-vgap: 10;");

    }

    public static String generateRandomString() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 24;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        printTerminal(generatedString);
        return generatedString;
    }

    public static ImageView getGoodImage(String url_image, int dimension){

        ImageView image;

        try {
            URL url = new URL(url_image);
            URLConnection uc = url.openConnection();
            uc.setRequestProperty("Cookie", "foo=bar");
            uc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            //uc.setReadTimeout(5000);
            //uc.setConnectTimeout(5000);
            uc.getInputStream();
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            BufferedImage img = ImageIO.read(url);
            Image images = SwingFXUtils.toFXImage(img, null);
            image = new ImageView();
            image.setFitHeight(dimension);
            image.setFitWidth(dimension);
            image.setImage(images);

        } catch (IOException e) { //case image not valid any more (link with 404 page)
            //e.printStackTrace();
            Image img = new Image("./img/empty.jpg");
            image = new ImageView(img);
            image.setFitHeight(dimension);
            image.setFitWidth(dimension);
            //image.setPreserveRatio(true);
        }

        return image;
    }

    public static void printTerminal(String msg){
        System.out.println("************************************************");
        System.out.println(msg);
        System.out.println("************************************************");
    }

    public static void changePage(Stage stage, String page) throws IOException {
        URL url = new File("src/main/resources/FXML/" + page + ".fxml").toURI().toURL();
        Parent root = FXMLLoader.load(url);

        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }

    public static int prevPage(int index, int k, Pane prev) {

        int new_index = 0;

        if((index%k) == 0)
            new_index = index - 3;
        else
            new_index = index - (index%k);
        new_index -= 3;

        if (new_index == 0) {
            prev.setDisable(true);
            prev.setVisible(false);
        }

        return new_index;
    }


    public static void nextPage(int index, ArrayList<Document> list, Pane next, Pane prev) {

        System.out.println("INDEX: " + index + "list: " + list.size());

        if (index == list.size()) {
            next.setDisable(true);
            next.setVisible(false);
        }

        prev.setVisible(true);
        prev.setDisable(false);
    }

    public static int prevPageReviews(int index, int k, Pane prev) {

        int new_index = 0;

        if((index%k) == 0)
            new_index = index - 2;
        else
            new_index = index - (index%k);
        new_index -= 2;

        if (new_index == 0) {
            prev.setDisable(true);
            prev.setVisible(false);
        }

        return new_index;
    }
}
