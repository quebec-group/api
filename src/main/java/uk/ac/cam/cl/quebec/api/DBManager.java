package uk.ac.cam.cl.quebec.api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.neo4j.driver.v1.*;

public class DBManager {
    private static String DB_URL = System.getenv("dbUrl");
    private static String DB_USER = System.getenv("dbUser");
    private static String DB_PASS = System.getenv("dbPassword");

    private static Driver driver;

    private Session getSession() {
        if (driver == null) {
            driver = GraphDatabase.driver(DB_URL, AuthTokens.basic(DB_USER, DB_PASS));
        }

        return driver.session();
    }

    public JSONObject createUser(String ID, String name, String email) {
        Statement statement = new Statement(
                "CREATE (u:User {name: {name}, email: {email}, userID: {ID}}) ",
                Values.parameters("name", name, "email", email, "ID", ID));
        StatementResult result = runQuery(statement);
        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject setPictureID(String userID, String S3ID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "SET u.profilePicID = {S3ID} ",
                Values.parameters("userID", userID, "S3ID", S3ID));
        StatementResult result = runQuery(statement);


        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject setVideoID(String eventID, String S3ID) {
        Statement statement = new Statement(
                "MATCH (u:Event {eventID: {eventID}}) " +
                "SET u.videoID = {S3ID} ",
                Values.parameters("eventID", Integer.parseInt(eventID), "S3ID", S3ID));

        StatementResult result = runQuery(statement);

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject getFriends(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FRIENDS]-(friends:User) " +
                "RETURN friends",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getFriendsFromResult(result);
    }

    public JSONObject addFriend(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "CREATE (a)-[:FRIENDS]->(b)",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);


        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject removeFriend(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "MATCH (a)-[r:FRIENDS]-(b) " +
                "DELETE r",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);


        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject addFriendRequest(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "CREATE (a)-[:REQUEST]->(b)",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject getPendingFriendRequests(String userID) {
        Statement statement = new Statement(
                "MATCH (friends) -[:REQUEST]->(u:User {userID: {userID}}) " +
                "RETURN friends",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getFriendsFromResult(result);
    }

    public JSONObject getSentFriendRequests(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:REQUEST]->(friends) " +
                "RETURN friends",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getFriendsFromResult(result);
    }

    public JSONObject removeFriendRequest(String userAID, String userBID) {
        Statement statement = new Statement("MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "MATCH (a)-[r:REQUEST]->(b)" +
                "DELETE r",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;

    }

    public JSONObject createEvent(String title, String location, String time, String creatorID) {
        Statement statement = new Statement(
                // Get UUID
                "MERGE (id:UniqueId{name:'Event'}) " +
                "ON CREATE SET id.count = 1 " +
                "ON MATCH SET id.count = id.count + 1 " +
                "WITH id.count AS uid " +

                // Create event
                "MATCH (u:User {userID: {creatorID}}) " +
                "CREATE (e:Event {title: {title}, eventID: uid, location: {location}, time: {time}}) " +
                "CREATE (u)-[:CREATED]->(e), " +
                "(u)-[:ATTENDED]->(e)",
                Values.parameters("title", title,
                        "creatorID", creatorID,
                        "location", location,
                        "time", time));
        StatementResult result = runQuery(statement);

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject addUserToEvent(String eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE (u)-[:ATTENDED]->(e)",
                Values.parameters("eventID", Integer.parseInt(eventID), "userID", userID));
        StatementResult result = runQuery(statement);

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject removeUserFromEvent(String eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u)-[r:ATTENDED]->(e) " +
                "DELETE r",
                Values.parameters("eventID", Integer.parseInt(eventID), "userID", userID));
        StatementResult result = runQuery(statement);

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject getEvents(String userID) {
        Statement statement = new Statement(
                "MATCH (caller:User {userID: {userID}})-[*1..2]-(events:Event) " +
                "RETURN events",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getEventsFromResults(result);
    }


    public StatementResult runQuery(Statement query) {
        Session session = getSession();
        StatementResult result = session.run(query);
        session.close();
        return result;
    }

    private JSONObject getEventsFromResults(StatementResult result) {
        JSONObject response = new JSONObject();
        JSONArray events = new JSONArray();

        while (result.hasNext()) {
            Record record = result.next();
            JSONObject event = new JSONObject();
            event.put("eventID", record.get("events").get("eventID"));
            event.put("title", record.get("events").get("title"));
            event.put("location", record.get("events").get("location"));
            event.put("videoID", record.get("events").get("videoID"));
            events.add(event);
        }

        response.put("events", events);
        response.put("status", "success");

        return response;
    }

    private JSONObject getFriendsFromResult(StatementResult result) {
        JSONObject response = new JSONObject();
        JSONArray friends = new JSONArray();

        while (result.hasNext()) {
            Record record = result.next();
            JSONObject friend = new JSONObject();
            friend.put("userID", record.get("friends").get("userID"));
            friend.put("name", record.get("friends").get("name"));
            friend.put("email", record.get("friends").get("email"));
            friend.put("profileID", record.get("friends").get("profilePicID"));
            friends.add(friend);
        }

        response.put("friends", friends);
        response.put("status", "success");

        return response;
    }
}
