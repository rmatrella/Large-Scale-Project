package main.java.connection;

import com.mongodb.client.*;
import main.java.controller.SignInController;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;

import com.mongodb.ConnectionString;

import java.io.IOException;

public class ConnectionMongoDB{

    private MongoClient mongoClient;
    private MongoDatabase db;

    public void openConnection(){
        ConnectionString uri = new ConnectionString("mongodb://localhost:27017");
        mongoClient = MongoClients.create(uri);
        db = mongoClient.getDatabase("project");
    }

    public void closeConnection() {
        mongoClient.close();
    }

    public boolean logInUser(String username, String password) throws IOException {

        this.openConnection();
        MongoCollection<Document> myColl = db.getCollection("user");
        MongoCursor<Document> cursor  = myColl.find(and(eq("username", username),
                eq("password", password))).iterator();
        if(!cursor.hasNext()) {
            System.out.println("Username or Password wrong, try again");
            this.closeConnection();
            return false;
        } else{
            System.out.println("FOUND!!!!");
        }
        this.closeConnection();
        return true;
    }

}