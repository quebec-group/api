package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.LinkedHashMap;

// uk.ac.cam.cl.quebec.api.APIHandler::handleRequest
public class APIHandler implements RequestHandler<JSONObject, JSONObject> {

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
        } catch (ParamNotSpecifiedException e) {
            if (context != null) {
                context.getLogger().log(e.toString());
            }

            responseJson.put("statusCode", "500");
            responseJson.put("body", "Incorrect paramters, requires: " + e.getMessage());
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
        return path.replace("/api/", "");
    }

    private String getParam(JSONObject params, String key) throws ParamNotSpecifiedException {
        String param = (String) params.get(key);

        if (param == null) {
            throw new ParamNotSpecifiedException("Parameter " + key + " not found");
        }

        return param;
    }


    private JSONObject getResultForQuery(JSONObject input) throws ParseException, ParamNotSpecifiedException {
        JSONObject params = getParams(input);
        String request = getRequest(input);

        switch (request) {
            case "createUser":
                return db.createUser(getUserID(input),
                        getParam(params, "name"),
                        getParam(params, "email"));
            case "getFriends":
                return db.getFriends(getUserID(input));
            case "setPictureID":
                return db.setPictureID(getUserID(input),
                        getParam(params, "S3ID"));
            case "setVideoID":
                return db.setVideoID(getUserID(input),
                        getParam(params, "S3ID"));
            case "addFriend":
                return db.addFriend(getUserID(input),
                        getParam(params, "friendID"));
            case "removeFriend":
                return db.removeFriend(getUserID(input),
                        getParam(params, "friendID"));
            case "addFriendRequest":
                return db.addFriendRequest(getUserID(input),
                        getParam(params, "friendID"));
            case "getPendingFriendRequests":
                return db.getPendingFriendRequests(getUserID(input));
            case "getSentFriendRequests":
                return db.getSentFriendRequests(getUserID(input));
            case "createEvent":
                return db.createEvent(getParam(params, "title"),
                        getUserID(input));
            case "addUserToEvent":
                return db.addUserToEvent(getParam(params, "eventID"),
                        getParam(params, "userID"));
            case "removeUserFromEvent":
                return db.removeUserFromEvent(getParam(params, "eventID"),
                        getUserID(input));
            default:
                JSONObject error = new JSONObject();
                error.put("status", "API '" + request + "' not supported");
                return error;
        }

    }

    public static void main(String[] args) {
        APIHandler test = new APIHandler();
//        test.db.createUser("7", "Miteyan", "mmm@cam.ac.uk");
//        test.db.createUser("8", "George", "ggg@cam.ac.uk");
//        test.db.addFriend("7", "8");
        test.db.createEvent("Event title", "7");
//        test.db.addUserToEvent("17", "8");
    }
}