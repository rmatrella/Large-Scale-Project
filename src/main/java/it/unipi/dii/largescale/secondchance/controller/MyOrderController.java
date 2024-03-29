package main.java.it.unipi.dii.largescale.secondchance.connection.controller;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import main.java.it.unipi.dii.largescale.secondchance.connection.ConnectionMongoDB;
import main.java.it.unipi.dii.largescale.secondchance.connection.entity.Review;
import main.java.it.unipi.dii.largescale.secondchance.connection.entity.User;
import main.java.it.unipi.dii.largescale.secondchance.connection.utils.Session;
import main.java.it.unipi.dii.largescale.secondchance.connection.utils.Utility;
import org.bson.Document;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MyOrderController{


    private static final int MAX_LENGTH = 50;
    public BorderPane ordersContainer;
    public ComboBox<String> comboBox;
    public Pane prev;
    public Pane next;

    public ArrayList<Document> ordersList;
    public VBox panel;
    User user;

    int indexPage;
    int k = 6;
    boolean kind;

    public void initialize(){

        //set buttons
        prev.setDisable(true);
        next.setDisable(true);
        prev.setVisible(false);
        next.setVisible(false);

        k = 4;
        indexPage = 1;

        ordersList = new ArrayList<>();
        panel = new VBox();
        panel.getChildren().clear();
        ordersContainer.setCenter(panel);

    }


    public void showOrders() {

        String type = comboBox.getValue();

        Session session = Session.getInstance();
        user = session.getLoggedUser();

        if(type.equals("Items purchased"))
        {
            ordersList.clear();
            panel.getChildren().clear();
            indexPage = 1;
            ordersList = ConnectionMongoDB.connMongo.findAllOrders(true, user.getUsername());
            System.out.println("ORDER: " + ordersList);
            kind = true;
            showAllOrders(true);
        }
        else if(type.equals("Items sold"))
        {
            ordersList.clear();
            panel.getChildren().clear();
            indexPage = 1;
            ordersList = ConnectionMongoDB.connMongo.findAllOrders(false, user.getUsername());
            kind = false;
            showAllOrders(false);
        }

        if(ordersList.size() > k)
        {
            next.setVisible(true);
            next.setDisable(false);
        }
    }

    private void showAllOrders(boolean choice) {

        for(int i = 0; i < k && indexPage <= ordersList.size(); i++)
        {
            addOrder(choice);
            indexPage++;
        }

        if(ordersList.size() < indexPage)
        {
            next.setDisable(true);
            next.setVisible(false);
        }

    }

    private void addOrder(boolean choice) {

        HBox hbox;
        HBox revbox;
        VBox vbox;
        Button review = new Button();
        Document ins = (Document) ordersList.get(indexPage-1).get("insertion");

        String sellerUser = ins.getString("seller");
        String orderTimestamp = ordersList.get(indexPage-1).getString("timestamp");
        Label buyer = new Label("Buyer: " + ordersList.get(indexPage-1).getString("buyer"));
        Label timestamp = new Label("Date Order: " + ordersList.get(indexPage-1).getString("timestamp"));
        Label seller = new Label("Seller: " + ins.getString("seller"));
        Label price = new Label("Price: " + ins.getDouble("price"));
        Label size = new Label("Size: " + ins.getString("size"));
        Label status = new Label("Status: " + ins.getString("status"));
        Label category = new Label("Category: " + ins.getString("category"));
        ImageView image = Utility.getGoodImage(ins.getString("image"), 110);

        if(ordersList.get(indexPage-1).getBoolean("reviewed") && choice) {
            review.setText("Already reviewed!");
            review.setDisable(true);
        }
        else
            review.setText("Review now!");

        if(choice) {
            vbox = new VBox(seller, timestamp, price, category, size, status);
            revbox = new HBox(vbox, review);
            hbox = new HBox(image, vbox, revbox);
            revbox.setSpacing(1000);
        }
        else {
            vbox = new VBox(buyer, timestamp, price, category, size, status);
            hbox = new HBox(image, vbox);
        }

        panel.setSpacing(12);
        hbox.setSpacing(80);
        hbox.setStyle("-fx-padding: 5px; -fx-background-color: white;");
        vbox.setStyle("-fx-padding: 5px; -fx-font-size: 14px");

        panel.getChildren().add(hbox);

        review.setOnMouseClicked(event-> {
                    addReview(sellerUser, orderTimestamp, review);
                }
        );
    }

    public void PrevOrder() {

        panel.getChildren().clear();
        int ind = (indexPage%(k+1)) == 0? 1 : (indexPage%(k+1));
        indexPage-= (k + ind);

        next.setDisable(false);
        next.setVisible(true);

        if(indexPage < k)
        {
            prev.setDisable(true);
            prev.setVisible(false);
        }

        showAllOrders(kind);

    }

    public void NextOrder() {

        panel.getChildren().clear();

        showAllOrders(kind);

        prev.setDisable(false);
        prev.setVisible(true);
    }

    private void addReview(String seller, String timestampOrder, Button revButton) {

        StackPane secondaryLayout = new StackPane();
        TextArea txtArea = new TextArea();
        TextField txtTitle = new TextField();
        ComboBox<String> rating = new ComboBox();

        rating.getSelectionModel().select("rating");
        rating.getItems().add("1");
        rating.getItems().add("2");
        rating.getItems().add("3");
        rating.getItems().add("4");
        rating.getItems().add("5");

        txtArea.setPrefHeight(150);
        txtArea.setPrefWidth(250);

        txtTitle.setPrefHeight(10);
        txtTitle.setPrefWidth(100);

        Button sendReview = new Button();
        sendReview.setText("Send review");
        sendReview.setStyle("-fx-border-width: 50");

        txtTitle.setPromptText("Title review");
        txtArea.setPromptText("Max 150 characters");

        HBox hbox = new HBox(txtTitle, rating);
        VBox vbox = new VBox(hbox, txtArea, sendReview);
        hbox.setSpacing(10);
        vbox.setSpacing(10);

        secondaryLayout.getChildren().add(vbox);
        Scene secondScene = new Scene(secondaryLayout, 400, 200);

        // New window
        Stage newWindow = new Stage();
        newWindow.setTitle("Review user");
        newWindow.setScene(secondScene);
        newWindow.show();

        sendReview.setOnMouseClicked(event-> {

            if (txtArea.getText().trim().equals("") || txtTitle.getText().trim().isEmpty() || rating.getValue().equals("rating")) {
                Utility.infoBox("Insert all the data", "User Alert", "Empty fields");
                return;
            }

            if(txtArea.getText().length() > MAX_LENGTH)
            {
                Utility.infoBox("Review with too many characters", "User Alert", "Incorrect field");
                return;
            }

            sendReview(txtArea, txtTitle, sendReview, revButton, Integer.parseInt(rating.getValue()), seller, timestampOrder);
        });

    }

    public void sendReview(TextArea txtArea, TextField txtTitle, Button sendReview, Button revButton, int rating, String seller, String timestampOrder){

        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        String timestamp = date.format(new Date());
        System.out.println("timestamp: " + timestamp);

        Review rev = new Review( user.getUsername(), seller, txtArea.getText(), timestamp, txtTitle.getText(), rating);

        ConnectionMongoDB.connMongo.addReview(rev);
        ConnectionMongoDB.connMongo.updateSellerRating(rev.getSeller());
        ConnectionMongoDB.connMongo.updateOrder(timestampOrder);
        sendReview.setDisable(true);
        revButton.setText("Already reviewed!");
        revButton.setDisable(true);

        Stage stage = (Stage) sendReview.getScene().getWindow();
        stage.close();
    }
}
