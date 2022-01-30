package main.java.it.unipi.dii.largescale.secondchance.connection.controller;

import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import main.java.it.unipi.dii.largescale.secondchance.connection.ConnectionMongoDB;
import main.java.it.unipi.dii.largescale.secondchance.connection.ConnectionNeo4jDB;
import main.java.it.unipi.dii.largescale.secondchance.connection.entity.Insertion;
import main.java.it.unipi.dii.largescale.secondchance.connection.utils.Utility;

import java.util.ArrayList;

public class InsertionListLikedController {

    private ArrayList<Insertion> insertions;
    public BorderPane bp;
    public VBox box;
    public Pane prev, next;
    private final int k = 3;
    private int index;

    public void initialize(String username) {

        System.out.println("username: " + username);
        ArrayList<String> followed_ins = ConnectionNeo4jDB.connNeo.retrieveFollowedInsertionByUser(username);
        System.out.println("followed_ins: " + followed_ins);
        insertions = ConnectionMongoDB.connMongo.findInsertionDetailsNeo4J(followed_ins);
        System.out.println("Insertions: " + insertions.get(0));

        box = new VBox(20);
        index = 0;

        prev.setDisable(true);
        next.setDisable(true);
        prev.setVisible(false);
        next.setVisible(false);

        showInsertionList();
    }

    public void showInsertionList() {

        box.getChildren().clear();

        //if there are more than k insertions enable next button
        if (insertions.size() - index > k) {
            next.setDisable(false);
            next.setVisible(true);
        }
        System.out.println("(show) INDEX: " + index);

        for (int i = 0; i < k && index < insertions.size(); i++) {
            addInsertions();
        }
        bp.setCenter(box);

    }

    private void addInsertions() {

        String uniq_id = insertions.get(index).getId();

        HBox hb = new HBox();
        VBox det = new VBox();

        ImageView image = Utility.getGoodImage(insertions.get(index).getImage_url(), 150);
        Label category = new Label("Category: " + insertions.get(index).getCategory());
        Label price = new Label(insertions.get(index).getPrice() + "€");
        Label views = new Label("Views: " + insertions.get(index).getViews());

        det.getChildren().add(category);
        det.getChildren().add(price);
        det.getChildren().add(views);
        hb.getChildren().add(image);
        hb.getChildren().add(det);
        box.getChildren().add(hb);

        image.setOnMouseClicked(event->{
                    try {
                        SearchInsertionController sic = new SearchInsertionController();
                        System.out.println(uniq_id);
                        sic.showInsertionPage(uniq_id);
                        HomeController.updateInsertionview(uniq_id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );


        GridPane.setHalignment(image, HPos.LEFT);
        GridPane.setHalignment(category, HPos.LEFT);
        GridPane.setHalignment(price, HPos.LEFT);
        GridPane.setHalignment(views, HPos.LEFT);

        det.setStyle("-fx-padding: 0 0 0 50;");
        hb.setStyle(
                "-fx-padding: 20;" +
                        " -fx-background-color: rgb(230, 230, 255);");
        box.setStyle(
                " -fx-hgap: 10;" +
                        " -fx-vgap: 10;" +
                        " -fx-max-height: 180;" +
                        " -fx-min-width: 530;" +
                        " -fx-max-width: 600;");

        index++;
        System.out.println("(add) INDEX: " + index);
    }

    public void prevPage() {
        System.out.println("(prev) INDEX: " + index);

        box.getChildren().clear();

        if((index%k) == 0)
            index -= k;
        else
            index -= (index%k);
        index -= k;

        System.out.println("(prev) INDEX: " + index);

        if (index == 0) {
            prev.setDisable(true);
            prev.setVisible(false);
        }

        showInsertionList();

    }

    public void nextPage() {

        box.getChildren().clear();

        System.out.println("(next) INDEX: " + index);

        showInsertionList();

        if (index == insertions.size()) {
            next.setDisable(true);
            next.setVisible(false);
        }

        prev.setVisible(true);
        prev.setDisable(false);
    }
}
