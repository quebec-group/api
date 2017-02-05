package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.LinkedHashMap;

// uk.ac.cam.cl.quebec.api.TestDB::handleRequest
public class TestDB implements RequestHandler<JSONObject, JSONObject> {

    private JSONParser parser = new JSONParser();
    private DBManager db = new DBManager();

    @Override
    public JSONObject handleRequest(JSONObject input, Context context) {
        if (context != null) {
            LambdaLogger logger = context.getLogger();
            logger.log("Loading Java Lambda handler simple\n");
        }

        JSONObject responseJson = new JSONObject();

        try {
            responseJson.put("statusCode", "200");
            responseJson.put("headers", new JSONObject());
            responseJson.put("body", getResultForQuery(input).toString());
        } catch (ParseException e) {
            if (context != null) {
                context.getLogger().log(e.toString());
            }

            responseJson.put("statusCode", "500");
            responseJson.put("body", "JSON parsing error");
        }


        return responseJson;
    }

    private String getUserID(JSONObject input) {
        LinkedHashMap requestContext = (LinkedHashMap) input.get("requestContext");
        LinkedHashMap identity = (LinkedHashMap) requestContext.get("identity");
        return (String) identity.get("user");
    }

    private JSONObject getParams(JSONObject input) throws ParseException {
        return (JSONObject) parser.parse((String) input.get("body"));
    }

    private String getRequest(JSONObject input) {
        String path = (String) input.get("path");
        return path.replace("/items/", "");
    }


    private JSONObject getResultForQuery(JSONObject input) throws ParseException {
        JSONObject params = getParams(input);
        String request = getRequest(input);

        switch (request) {
            case "createUser":
                return db.createUser(getUserID(input),
                        (String) params.get("name"),
                        (String) params.get("email"));
            case "getFriends":
                return db.getFriends(getUserID(input));
            case "setPictureID":
                return db.setPictureID(getUserID(input),
                        (String) params.get("S3ID"));
            case "setVideoID":
                return db.setVideoID(getUserID(input),
                        (String) params.get("S3ID"));
            case "addFriend":
                return db.addFriend(getUserID(input),
                        (String) params.get("friendID"));
            case "removeFriend":
                return db.removeFriend(getUserID(input),
                        (String) params.get("friendID"));
            case "addFriendRequest":
                return db.addFriendRequest(getUserID(input),
                        (String) params.get("friendID"));
            case "getPendingFriendRequests":
                return db.getPendingFriendRequests(getUserID(input));
            case "getSentFriendRequests":
                return db.getSentFriendRequests(getUserID(input));
            case "createEvent":
                return db.createEvent((String) params.get("eventID"),
                        (String) params.get("title"),
                        getUserID(input));
            case "addUserToEvent":
                return db.addUserToEvent((String) params.get("eventID"),
                        (String) params.get("userID"));
            case "removeUserFromEvent":
                return db.removeUserFromEvent((String) params.get("eventID"),
                        getUserID(input));
            default:
                JSONObject error = new JSONObject();
                error.put("status", "API not supported");
                return error;
        }

    }
}