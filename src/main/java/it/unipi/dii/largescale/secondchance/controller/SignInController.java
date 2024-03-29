package main.java.it.unipi.dii.largescale.secondchance.connection.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import main.java.it.unipi.dii.largescale.secondchance.connection.*;
import main.java.it.unipi.dii.largescale.secondchance.connection.utils.*;
import org.bson.Document;
import java.io.IOException;

public class SignInController {

    public AnchorPane anchorRoot;

    public Button SignUp;
    public Button SignIn;
    public Pane welcomePane;

    @FXML private TextField us;
    @FXML private PasswordField pw;

    public void ShowSignUp() throws IOException {

        Stage stage = (Stage) SignUp.getScene().getWindow();
        Utility.changePage(stage, "SignUp");
    }

    public void ShowAdminPanel() throws IOException {

        Stage stage = (Stage) SignIn.getScene().getWindow();
        Utility.changePage(stage, "AdminPanel");

    }

    public void ShowHome() throws IOException {

        Stage stage = (Stage) SignIn.getScene().getWindow();
        Utility.changePage(stage, "MainPage");

    }

    public void login() throws IOException {

        String username = us.getText();
        String password = pw.getText();
        String encrypted = CryptWithMD5.cryptWithMD5(password);
        Utility.printTerminal("PASSWORD: " + password + "\nENCRYPTED: " + encrypted);
        Session session;

        if(!us.getText().isEmpty() && !pw.getText().isEmpty()) {
            Utility.printTerminal("Value: " + us.getText() + "\nValue: " + pw.getText());

            if (us.getText().equals("admin")) {
                session = Session.getInstance();
                Document user  = ConnectionMongoDB.connMongo.findUserByUsername(us.getText());
                System.out.println("USER: " + user);
                //session.setLogUser(user, true);
                if(ConnectionMongoDB.connMongo.logInUser(username, encrypted))
                    ShowAdminPanel();
            }else {
                boolean logged = ConnectionMongoDB.connMongo.logInUser(username, encrypted);
                //boolean logged = conn.logInUser(username, password);
                if (logged) {
                    session = Session.getInstance();
                    Document user  = ConnectionMongoDB.connMongo.findUserByUsername(us.getText());
                    session.setLogUser(user);
                    ShowHome();
                }
            }
            //clear TextField
            us.setText("");
            pw.setText("");

        }else {
            Utility.infoBox("Please, insert username and password.", "Error", "Empty fields!");
        }
    }
}