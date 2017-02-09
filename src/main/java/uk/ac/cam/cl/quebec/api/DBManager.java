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
        StatementResult result = runQuery("CREATE (u:User {name:'" + name +
                "', email:'" + email +
                "', userID:'" + ID + "'}) " +
                "CREATE (p:Picture)" +
                "CREATE (u)-[:PROFILE_PIC]->(p)");
        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject setPictureID(String userID, String S3ID) {
        StatementResult result = runQuery("MATCH (u:User {userID:'" + userID + "'})" +
                "MATCH (u)-[:PROFILE_PIC]->(p:Picture)" +
                "SET p.S3ID = '" + S3ID + "'");


        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject setVideoID(String eventID, String S3ID) {
        StatementResult result = runQuery("MATCH (u:Event {eventID:'" + eventID + "'})" +
                "MATCH (u)-[:VIDEO]->(v:Video)" +
                "SET v.S3ID = '" + S3ID + "'");

        JSONObject response = new JSONObject();
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
            friend.put("profileID", "TODO");
            friends.add(friend);
        }

        response.put("friends", friends);

        return response;
    }

    public JSONObject getFriends(String userID) {
        StatementResult result = runQuery("MATCH (u:User {userID:'" + userID + "'})-[:FRIENDS]->(friends) RETURN friends");

        JSONObject response = getFriendsFromResult(result);
        response.put("userID", userID);
        return response;
    }

    public JSONObject addFriend(String userAID, String userBID) {
        StatementResult result = runQuery("MATCH (a:User {userID:'"+ userAID + "'}) " +
                "MATCH (b:User {userID:'" + userBID + "'}) " +
                "CREATE (a)-[:FRIENDS]->(b)");


        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject removeFriend(String userAID, String userBID) {
        StatementResult result = runQuery("MATCH (a:User {userID:'"+ userAID + "'}) " +
                "MATCH (b:User {userID:'" + userBID + "'}) " +
                "MATCH (a)-[r:FRIENDS]->(b)" +
                "DELETE r");


        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject addFriendRequest(String userAID, String userBID) {
        StatementResult result = runQuery("MATCH (a:User {userID:'"+ userAID + "'}) " +
                "MATCH (b:User {userID:'" + userBID + "'}) " +
                "CREATE (a)-[:REQUEST]->(b)");

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject getPendingFriendRequests(String userID) {
        StatementResult result = runQuery("MATCH (friends) -[:REQUEST]->(u:User {userID:'" + userID + "'})RETURN friends");

        JSONObject response = getFriendsFromResult(result);
        response.put("userID", userID);
        return response;
    }

    public JSONObject getSentFriendRequests(String userID) {
        StatementResult result = runQuery("MATCH (u:User {userID:'" + userID + "'})-[:REQUEST]->(friends) RETURN friends");

        JSONObject response = getFriendsFromResult(result);
        response.put("userID", userID);
        return response;
    }

    public JSONObject removeFriendRequest(String userAID, String userBID) {
        StatementResult result = runQuery("MATCH (a:User {userID:'"+ userAID + "'}) " +
                "MATCH (b:User {userID:'" + userBID + "'}) " +
                "MATCH (a)-[r:REQUEST]->(b)" +
                "DELETE r");

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;

    }

    public JSONObject createEvent(String title, String creatorID) {
        String getUniqueID = "MERGE (id:UniqueId{name:'Event'})\n" +
                "ON CREATE SET id.count = 1\n" +
                "ON MATCH SET id.count = id.count + 1\n" +
                "WITH id.count AS uid ";

        StatementResult result = runQuery(getUniqueID + "MATCH (u:User {userID:'" + creatorID + "'})" +
                "CREATE (e:Event {title:'" + title + "', eventID:uid})" +
                "CREATE (v:Video)" +
                "CREATE (u)-[:CREATED]->(e)," +
                "(u)-[:ATTENDED]->(e)," +
                "(e)-[:VIDEO]->(v)");

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject addUserToEvent(String eventID, String userID) {
        StatementResult result = runQuery("MATCH (u:User {userID:'" + userID + "'})" +
                "MATCH (e:Event {eventID:'" + eventID + "'})" +
                "CREATE (u)-[:ATTENDED]->(e)");

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }

    public JSONObject removeUserFromEvent(String eventID, String userID) {
        StatementResult result = runQuery("MATCH (u:User {userID:'" + userID +"'})" +
                "MATCH (e:Event {eventID:'" + eventID +"'})" +
                "MATCH (u)-[r:ATTENDED]->(e)" +
                "DELETE r");

        JSONObject response = new JSONObject();
        response.put("status", "success");
        return response;
    }


    public StatementResult runQuery(String query) {
        Session session = getSession();
        StatementResult result = session.run(query);
        session.close();
        return result;
    }
}
