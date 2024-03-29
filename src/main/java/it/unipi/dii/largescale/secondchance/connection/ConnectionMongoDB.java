package main.java.it.unipi.dii.largescale.secondchance.connection;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import main.java.it.unipi.dii.largescale.secondchance.connection.entity.*;
import main.java.it.unipi.dii.largescale.secondchance.connection.utils.*;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class ConnectionMongoDB{

    public static ConnectionMongoDB connMongo;
    private MongoClient mongoClient;
    private MongoDatabase db;
    MongoCursor<Document> cursor;

    MongoCollection<Document> userColl;
    MongoCollection<Document> orderColl;
    MongoCollection<Document> insertionColl;
    MongoCollection<Document> adminColl;

    /* ********* CONNECTION SECTION ********* */

    public void openConnection() {


        // LOCAL DATABASE WITHOUT REPLICAS
       /* ConnectionString uri = new ConnectionString("mongodb://localhost:27017");
        mongoClient = MongoClients.create(uri);
        db = mongoClient.getDatabase("local");

        userColl = db.getCollection("user");
        adminColl = db.getCollection("admin");
        insertionColl = db.getCollection("insertion");
        orderColl = db.getCollection("order");
*/

        mongoClient = MongoClients.create(
                "mongodb://172.16.4.114:27020,172.16.4.115:27020,172.16.4.116:27020/" +
                        "?retryWrites=true&w=majority&wtimeout=10000");

        // Read Preferences at DB level
        db = mongoClient.getDatabase("lsmdb")
                .withReadPreference(ReadPreference.secondary());

        // Read Preferences at collection level
        userColl = db.getCollection("user")
                .withReadPreference(ReadPreference.secondary());

        insertionColl = db.getCollection("insertion")
                .withReadPreference(ReadPreference.secondary());

        orderColl = db.getCollection("order")
                .withReadPreference(ReadPreference.secondary());

        adminColl = db.getCollection("admin")
                .withReadPreference(ReadPreference.secondary());

        // Write concern at DB level
        db = mongoClient.getDatabase("lsmdb")
                .withWriteConcern(WriteConcern.W1);

        System.out.println("**************** USER ******************");
        System.out.println(userColl.countDocuments());
        System.out.println("**************** ORDER ******************");
        System.out.println(orderColl.countDocuments());
        System.out.println("**************** INSERTION ******************");
        System.out.println(insertionColl.countDocuments());
        System.out.println("**************** ADMIN ******************");
        System.out.println(adminColl.countDocuments());

        // 2 - Find the first document
        userColl.find().limit(1).forEach(printDocuments());

    }

    public void closeConnection() {
        mongoClient.close();
    }

    /* ********* USER SECTION ********* */

    public boolean logInUser(String username, String password) throws IOException {

        if(!userAlreadyPresent(username, password)) {
            Utility.infoBox("Username or Password wrong, try again", "Error", "Try again");
            return false;
        } else{
            Utility.printTerminal("User found in db");
        }
        return true;
    }

    public boolean registerUser(User u) {

        if (userAlreadyPresent(u.getUsername(), u.getPassword())) {
            Utility.infoBox("Please, choose another username and try again.", "Error", "Username already used!");
            return false;
        }

        Document user = new Document("address", u.getAddress())
                .append("balance", u.getBalance())
                .append("city", u.getCity())
                .append("country", u.getCountry())
                .append("email", u.getEmail())
                .append("img_profile", u.getImage())
                .append("name", u.getName())
                .append("password", u.getPassword())
                .append("suspended", u.getSuspended())
                .append("username", u.getUsername());

        userColl.insertOne(user);

        return true;
    }

    private static Consumer<Document> printDocuments() {
        return doc -> System.out.println(doc.toJson());
    }

    public Document findUserByUsername(String username) {

        cursor = userColl.find(eq("username", username)).iterator();

        if (cursor.hasNext())
            return cursor.next();

        Utility.infoBox("There is no user with this username.", "Error", "Username not found!");
        return null;

    }

    private boolean userAlreadyPresent(String username, String password) {

        cursor = userColl.find(and(eq("username", username),
                eq("password", password))).iterator();

        return cursor.hasNext();
    }

    public User findUserDetails(String username) {

        User logUser = new User();
        Document user = userColl.find(eq("username", username)).first();
        logUser.setUsername(user.getString("username"));
        logUser.setName(user.getString("name"));
        logUser.setEmail(user.getString("email"));
        logUser.setAddress(user.getString("address"));
        logUser.setCity(user.getString("city"));
        logUser.setCountry(user.getString("country"));
        logUser.setSuspended(user.getString("suspended"));
        logUser.setBalance(user.getDouble("balance"));
        logUser.setImage(user.getString("img_profile"));
        logUser.setRating(user.getDouble("rating"));

        return logUser;
    }

    public ArrayList<Document> followedUserInsertions(ArrayList<String> usList) {

        ArrayList<Document> insertions = new ArrayList<>();

        //System.out.println("insertion id: " + usList);

        for (int i = 0; i < usList.size(); i++) {
            Document d = insertionColl.find(eq("uniq_id", usList.get(i))).first();
            insertions.add(d);
        }

        return insertions;
    }

    public ArrayList<Document> findViralInsertions(int k) {

        ArrayList<Document> insertions = new ArrayList<>();
        Bson sort = sort(descending("interested"));
        Bson limit = limit(k);

        AggregateIterable<Document> r = insertionColl.aggregate(Arrays.asList(sort ,limit));

        for (Document document : r)
        {
            insertions.add(document);
        }
        return insertions;
    }

    public ArrayList<Document> findUserByFilters(String country, String rating) {

        ArrayList<Document> users = new ArrayList<>();
        double r ;
        double lowerBound = 0;
        double upperBound = 0;

        if(!rating.equals("rating")){
            r = Double.parseDouble(rating);
            lowerBound = r-0.5;
            upperBound = r+0.5;
        }

        if(country.equals("country") && !rating.equals("rating"))
        {
            cursor  = userColl.find(and(lte("rating", upperBound), gt("rating", lowerBound))).iterator();
        }
        else if(!country.equals("country") && rating.equals("rating"))
        {
             cursor  = userColl.find(eq("country", country)).iterator();
        }
        else{
             cursor  = userColl.find(and(eq("country", country), lte("rating", upperBound), gt("rating", lowerBound))).iterator();
        }

        while(cursor.hasNext())
        {
            users.add(cursor.next());
        }

        return users;

    }

    private ArrayList<Document> partialSearch(int index, ArrayList<Document> insertions, String size, String price, String gender, String status, String category, String color) {

        switch (index) {
            case 0:
                cursor  = insertionColl.find(eq("size", size)).iterator();
                break;
            case 1:
                String[] split = price.split("-");
                Utility.printTerminal("Selected range: " + split[0]);
                if(split.length == 1) {
                    cursor  = insertionColl.find(gte("price", Double.parseDouble(split[0]))).iterator();
                } else {
                    Utility.printTerminal("Selected range: " + split[1]);
                    cursor  = insertionColl.find(and(gte("price", Double.parseDouble(split[0])),
                            lte("price", Double.parseDouble(split[1])))).iterator();
                }
                break;
            case 2:
                cursor  = insertionColl.find(eq("gender", gender)).iterator();
                break;
            case 3:
                cursor  = insertionColl.find(eq("status", status)).iterator();
                break;
            case 4:
                cursor  = insertionColl.find(eq("category", category)).iterator();
                break;
            case 5:
                cursor  = insertionColl.find(eq("color", color)).iterator();
                break;
            default:
                break;
        }
        while(cursor.hasNext())
            insertions.add(cursor.next());

        return insertions;
    }

    /* ********* INSERTION SECTION ********* */

    public ArrayList<Document> findInsertionByFilters(String size, String price, String gender, String status, String category, String color) {

        ArrayList<Document> insertions = new ArrayList<>();

        //the following variables are 1 if the relative filter is applied
        int sizeFilterOn, priceFilterOn, genderFilterOn, statusFilterOn, categoryFilterOn, colorFilterOn;
        sizeFilterOn = (size.equals("size")) ? 0 : 1;
        priceFilterOn = (price.equals("price")) ? 0 : 1;
        genderFilterOn = (gender.equals("gender")) ? 0 : 1;
        statusFilterOn = (status.equals("status")) ? 0 : 1;
        categoryFilterOn = (category.equals("category")) ? 0 : 1;
        colorFilterOn = (color.equals("color")) ? 0 : 1;

        int[] filter = {sizeFilterOn, priceFilterOn, genderFilterOn,
                statusFilterOn,categoryFilterOn, colorFilterOn};

        for (int i = 0; i < 6; i++) {
            if (filter[i] == 1)
                partialSearch(i, insertions, size, price, gender, status, category, color);
        }
        return insertions;
    }

    public ArrayList<Document> findInsertionBySeller(String seller) {

        ArrayList<Document> insertions = new ArrayList<>();

        cursor = insertionColl.find(eq("seller", seller)).iterator();
        while (cursor.hasNext())
            insertions.add(cursor.next());

        return insertions;
    }

    public ArrayList<Document> findInsertionByBrand(String brand) {

        ArrayList<Document> insertions = new ArrayList<>();

        cursor = insertionColl.find(eq("brand", brand)).iterator();
        while (cursor.hasNext())
            insertions.add(cursor.next());

        return insertions;
    }

    public Insertion findInsertion(String insertion_id) {

        Insertion insertion = new Insertion();
        Document insertion_found = insertionColl.find(eq("uniq_id", insertion_id)).first();

        insertion.setId(insertion_found.getString("uniq_id"));
        insertion.setBrand(insertion_found.getString("brand"));
        insertion.setCountry(insertion_found.getString("country"));
        insertion.setCategory(insertion_found.getString("category"));
        insertion.setColor(insertion_found.getString("color"));
        insertion.setDescription(insertion_found.getString("description"));
        insertion.setGender(insertion_found.getString("gender"));
        insertion.setImage_url(insertion_found.getString("image_url"));
        insertion.setInterested(insertion_found.getInteger("interested"));
        insertion.setPrice(insertion_found.getDouble("price"));
        insertion.setViews(insertion_found.getInteger("views"));
        insertion.setSeller(insertion_found.getString("seller"));
        insertion.setSize(insertion_found.getString("size"));
        insertion.setStatus(insertion_found.getString("status"));
        insertion.setTimestamp(insertion_found.getString("timestamp"));

        return insertion;

    }

    public boolean buyCurrentInsertion(String username, Insertion insertion){

        ClientSession clientSession = mongoClient.startSession();

        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        String timestamp = date.format(new Date());

        TransactionBody<String> txnFunc = () -> {

            Document balance = db.getCollection("user").find(eq("username", username)).first();
            double balanceBuyer = balance.getDouble("balance") - insertion.getPrice();

            Bson filter = and(eq("username", username), gte("balance", insertion.getPrice()));
            Bson update = set("balance", balanceBuyer);

            //update buyer balance
            Document ret = db.getCollection("user").findOneAndUpdate(filter, update);

            if (ret == null) {
                Utility.infoBox("Cannot purchase, not enough balance", "Error", "Error purchase");
                return "Buyer has not enough balance";
            }

            //update seller balance
            Bson filter2 = eq("username", insertion.getSeller());
            Bson update2 = inc("balance", insertion.getPrice());

            Document ret3 = db.getCollection("user").findOneAndUpdate(filter2, update2);

            if (ret3 == null) {
                Utility.infoBox("Cannot buy product", "Error", "Error purchase");

                return "Cannot increment seller balance";
            }

            Document order = new Document()
                    .append("_id", new ObjectId())
                    .append("timestamp", timestamp)
                    .append("buyer", username)
                    .append("reviewed", false)
                    .append("insertion", new Document("image", insertion.getImage_url()).
                            append("seller", insertion.getSeller()).
                            append("price", insertion.getPrice()).
                            append("size", insertion.getSize()).
                            append("status", insertion.getStatus()).
                            append("category", insertion.getCategory()));

            //insert new document into order collection
            try {
                orderColl.insertOne(order);

            } catch (MongoException me) {
                System.err.println("Unable to insert due to an error: " + me);
            }

            db.getCollection("insertion").deleteOne(new Document("image_url", insertion.getImage_url()).append("seller", insertion.getSeller()));
            return "OK";
        };
        return executeTransaction(clientSession, txnFunc);
    }

    public boolean deleteBuyInsertion(String username, Insertion insertion) {

        ClientSession clientSession = mongoClient.startSession();

        TransactionBody<String> txnFunc = () -> {

            Bson filter = and(eq("username", username), gte("balance", insertion.getPrice()));
            Bson update = inc("balance", insertion.getPrice());

            //update buyer balance
            Document ret = db.getCollection("user").findOneAndUpdate(filter, update);


            //update seller balance
            Bson filter2 = eq("username", insertion.getSeller());
            Bson update2 = inc("balance", -(insertion.getPrice()));

            Document ret3 = db.getCollection("user").findOneAndUpdate(filter2, update2);

            insertionColl.insertOne(Insertion.toDocument(insertion));

            return "OK";

        };
        return executeTransaction(clientSession, txnFunc);

    }

    private boolean executeTransaction(ClientSession clientSession, TransactionBody<String> txnFunc) {

        String message = "";

        message = clientSession.withTransaction(txnFunc);

        System.out.println(message);

        return message.equals("OK");
    }

    public boolean updateNumInterested(String insertion_id, int i) {

        Bson filter = eq("uniq_id", insertion_id);
        Bson update = inc("interested", i);
        try {
            db.getCollection("insertion").findOneAndUpdate(filter, update);
            return true;
        }catch(MongoException me){
            return false;
        }
    }

    public void updateNumView(String uniq_id) {

        Bson filter = eq("uniq_id", uniq_id);
        Bson update = inc("views", 1);

        db.getCollection("insertion").findOneAndUpdate(filter, update);

    }

    /* ********* ADMIN SECTION ********* */

    public Document verifyUserInDB(String username, boolean choice) {

        Document user;

        if (choice)
            user = userColl.find(eq("username", username)).first();
        else
            user = userColl.find(eq("name", username)).first();

        return user;
    }

    public Document verifyInsertionInDB(String id, boolean choice) {

        Document insertion;

        if (choice)
            insertion = insertionColl.find(eq("uniq_id", id)).first();
         else
            insertion = insertionColl.find(eq("seller", id)).first();

        return insertion;
    }

    public ArrayList<Document> findMostActiveUsersSellers(int k, boolean choice) {
        // true = select the top k most active users
        // false = select the top k most active sellers

        ArrayList<Document> array = new ArrayList<>();
        MongoCollection<Document> myColl;

        if (choice)
            myColl = userColl;
        else
            myColl = orderColl;

        Bson limit = limit(k);

        if (!choice) { //most sold orders
            System.out.println("Sezione orders");
            Bson project = project(fields(excludeId(), include("count"), computed("seller", "$_id")));
            AggregateIterable<Document> aggr = myColl.aggregate(
                    Arrays.asList(
                            Aggregates.group("$insertion.seller",
                                    Accumulators.sum("count", 1)),
                            project,
                            Aggregates.sort(descending("count")),
                            limit
                    )
            );

            for (Document document : aggr) {
                System.out.println("DOCUMENT:" + document);
                array.add(document);
            }
        } else { //most insertions published
            System.out.println("Sezione insertions");
            Bson project = project(fields(excludeId(), include("count"), computed("seller", "$_id")));
            AggregateIterable<Document> aggr = myColl.aggregate(
                    Arrays.asList(
                            Aggregates.group("$seller",
                                    Accumulators.sum("count", 1)),
                            project,
                            Aggregates.sort(descending("count")),
                            limit
                    )
            );

            for (Document document : aggr) {
                System.out.println(document);
                array.add(document);
            }
        }
        return array;
    }

    public ArrayList<Document> findTopKRatedUser(int k, String country) {

        ArrayList<Document> array = new ArrayList<>();

        Bson limit = limit(k);
        Bson project = project(fields(excludeId(), include("username"), include("rating")));
        AggregateIterable<Document> aggr  = userColl.aggregate(
                Arrays.asList(
                        Aggregates.match(Filters.eq("country", country)),
                        Aggregates.sort(descending("rating")),
                        project,
                        limit
                )
        );

        for (Document document : aggr) {

            //document.append("name", document.getString("name"));
            //document.append("rating", document.getDouble("rating"));
            array.add(document);
        }
        return array;
    }

    public ArrayList<Document> findTopKInterestingInsertion(int k, String category) {

        ArrayList<Document> array = new ArrayList<>();

        Bson limit = limit(k);
        AggregateIterable<Document> aggr  = insertionColl.aggregate(
                Arrays.asList(
                        Aggregates.match(Filters.eq("category", category)),
                        Aggregates.sort(descending("interested")),
                        limit
                )
        );

        for (Document document : aggr)
            array.add(document);

        return array;
    }

    public ArrayList<Document> findTopKViewedInsertion(int k, String category) {

        ArrayList<Document> array = new ArrayList<>();

        Bson limit = limit(k);
        AggregateIterable<Document> aggr  = insertionColl.aggregate(
                Arrays.asList(
                        Aggregates.match(Filters.eq("category", category)),
                        Aggregates.sort(descending("viewed")),
                        limit
                )
        );

        for (Document document : aggr)
            array.add(document);

        return array;
    }

    public void suspendUser(String username) {

        Document query = new Document().append("username",  username);
        Bson updates = Updates.combine(
                Updates.set("suspended", "Y"));
                UpdateOptions options = new UpdateOptions().upsert(true);

        try {
            UpdateResult result = userColl.updateOne(query, updates, options);
            System.out.println("Modified document count: " + result.getModifiedCount());
            System.out.println("Upserted id: " + result.getUpsertedId()); // only contains a value when an upsert is performed
        }catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
        }



    }

    public void unsuspendUser(String username) {

        Document query = new Document().append("username",  username);

        Bson updates = Updates.combine(
                Updates.set("suspended", "N"));
                UpdateOptions options = new UpdateOptions().upsert(true);

        try {
            UpdateResult result = userColl.updateOne(query, updates, options);
            System.out.println("Modified document count: " + result.getModifiedCount());
            System.out.println("Upserted id: " + result.getUpsertedId()); // only contains a value when an upsert is performed
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
        }
    }

    public Insertion findInsertionDetails(String id) {

        Insertion ins = new Insertion();
        Document insertion = insertionColl.find(eq("uniq_id", id)).first();

        ins.setCategory(insertion.getString("category"));
        ins.setPrice(insertion.getDouble("price"));
        ins.setViews(insertion.getInteger("views"));

        return ins;
    }

    public ArrayList<Insertion> findMultipleInsertionDetails(String seller) {

        ArrayList<Insertion> array = new ArrayList<Insertion>();

        AggregateIterable<Document> aggr  = insertionColl.aggregate(
                Arrays.asList(
                        Aggregates.match(Filters.eq("seller", seller))
                )
        );

        for (Document document : aggr) {
            Insertion ins = new Insertion();
            ins.setCategory(document.getString("category"));
            ins.setPrice(document.getDouble("price"));
            ins.setViews(document.getInteger("views"));
            ins.setId(document.getString("uniq_id"));
            ins.setImage_url(document.getString("image_url"));
            array.add(ins);
        }
        return array;
    }

    public ArrayList<Insertion> findInsertionDetailsNeo4J(ArrayList<String> followed_ins)  {

        Insertion ins;
        ArrayList<Insertion> insertions = new ArrayList<Insertion>();

        for(int i=0; i < followed_ins.size(); i++) {
            Document insertion = insertionColl.find(eq("uniq_id", followed_ins.get(i).toString())).first();

            ins = new Insertion();
            ins.setCategory(insertion.getString("category"));
            ins.setPrice(insertion.getDouble("price"));
            ins.setImage_url(insertion.getString("image_url"));
            ins.setViews(insertion.getInteger("views"));
            ins.setSeller(insertion.getString("seller"));
            ins.setId(insertion.getString("uniq_id"));

            insertions.add(ins);
        }
        return insertions;
    }

    public boolean findByInsertionId (String id) {

        cursor = insertionColl.find(eq("uniq_id", id)).iterator();

        if (cursor.hasNext())
            return true;

        return false;
    }

    public boolean addInsertion(Insertion i) {

        Document ins = Insertion.toDocument(i);
        insertionColl.insertOne(ins);
        return true;

    }

    public ArrayList<Document> findAllOrders(Boolean choice, String username) {

        if(choice)
            cursor = orderColl.find(eq("buyer", username)).iterator();
        else
            cursor = orderColl.find(eq("insertion.seller", username)).iterator();

        ArrayList<Document> find_orders = new ArrayList<>();

        while (cursor.hasNext())
        {
            find_orders.add(cursor.next());
        }
        return find_orders;
    }

    public void addReview(Review rev) {

        Document review = new Document()
                .append("timestamp", rev.getTimestamp())
                .append("reviewer", rev.getReviewer())
                .append("title", rev.getTitle())
                .append("text", rev.getText())
                .append("rating", rev.getRating());

        System.out.println("REVIEW: " + review);
        BasicDBObject query = new BasicDBObject();
        query.put("username",rev.getSeller());

        BasicDBObject push_data = new BasicDBObject("$push", new BasicDBObject("reviews", review));

        userColl.findOneAndUpdate(query, push_data);
    }

    public void updateSellerRating(String seller) {

        Document d = userColl.find(eq("username", seller)).first();
        List<Document> list = null;
        list = d.getList("reviews", Document.class);

        Double avg = 0.0;
        int sum = 0;

        for (Document document : list) {
            sum += document.getInteger("rating");
        }

        avg = (double) sum / (double) list.size();

        BasicDBObject newDocument = new BasicDBObject();
        newDocument.put("rating", avg);
        // {$set: {"rating": avg}}
        BasicDBObject updateObject = new BasicDBObject();
        updateObject.put("$set", newDocument);
        // condition (where field "reviews" exists)
        BasicDBObject query = new BasicDBObject();
        query.put("username", d.getString("username"));

        userColl.updateOne(query, updateObject);
    }

    public void updateOrder(String timestamp) {

        BasicDBObject query = new BasicDBObject();
        query.put("timestamp", timestamp);

        BasicDBObject set = new BasicDBObject("$set", new BasicDBObject("reviewed", true));
        orderColl.findOneAndUpdate(query, set);
    }

    /* ********** BALANCE SECTION ********** */

    public void addFundsToWallet(String username, String id_code) {

        Document code = adminColl.find(eq("code", id_code)).first();

        if (code == null || Objects.equals(code.getString("assigned"), "T")) {
            Utility.infoBox("The code that you have inserted is not valid.", "Error", "Code doesn't exist!");
            return;
        }

        double credit = code.getInteger("credit");

        Document queryUser = new Document().append("username",  username);
        Document queryAdmin = new Document().append("code",  id_code);

        Document user = userColl.find(eq("username", username)).first();

        double new_balance = user.getDouble("balance") + credit;

        Bson updatesAdmin = Updates.combine(
                Updates.set("assigned", "T")
        );

        Bson updatesUser = Updates.combine(
                Updates.set("balance", new_balance)
        );

        UpdateOptions options = new UpdateOptions().upsert(true);

        try {
            UpdateResult resultUser = userColl.updateOne(queryUser, updatesUser, options);
            UpdateResult resultAdmin = adminColl.updateOne(queryAdmin, updatesAdmin, options);
            System.out.println("Modified document count: " + resultUser.getModifiedCount());
            System.out.println("Upserted id: " + resultUser.getUpsertedId()); // only contains a value when an upsert is performed
            System.out.println("Modified document count: " + resultAdmin.getModifiedCount());
            System.out.println("Upserted id: " + resultAdmin.getUpsertedId()); // only contains a value when an upsert is performed
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return;
        }

        Utility.infoBox("Deposit of " + code.getInteger("credit") + "€ euros successfully executed", "Success", "Deposit done!");
        deleteCode(code.getString("_id"));
    }

    public double updateBalance(String username) {

        Document user = userColl.find(eq("username", username)).first();

        double balance = user.getDouble("balance");

        System.out.println("NEW BALANCE: " + String.format("%.2f", balance));

        return balance;
    }

    public List<Document> getReviewsByUser(String username) {

        List<Document> list = null;

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("username", username);

        Document user = userColl.find(eq("username", username)).first();

        MongoCursor<Document> cursor = userColl.find(whereQuery).iterator();

        try {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                if(doc.get("reviews") == null)
                    return new ArrayList<>();
                list = (List<Document>)doc.get("reviews");

                Document d = list.get(0);
                System.out.println(d.getString("reviewer")); // display specific field
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public boolean deleteInsertionMongo(String id) {

        Bson query = eq("uniq_id", id);

        try {
            DeleteResult result = insertionColl.deleteOne(query);
            return true;
        } catch (MongoException me) {
            System.err.println("Unable to delete due to an error: " + me);
            return false;
        }
    }

    private void deleteCode(String id) {

        Bson query = eq("_id", id);

        try {
            DeleteResult result = adminColl.deleteOne(query);
            System.out.println("Deleted document count: " + result.getDeletedCount());
        } catch (MongoException me) {
            System.err.println("Unable to delete due to an error: " + me);
        }

    }

    public void deleteUserMongo(String username) {

        Bson query = eq("username", username);

        try {
             userColl.deleteOne(query);
        } catch (MongoException me) {
            System.err.println("Unable to delete due to an error: " + me);
        }
    }

}
