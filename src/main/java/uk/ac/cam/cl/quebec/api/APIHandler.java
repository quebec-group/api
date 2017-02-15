package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
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

        JSONObject responseJson = new JSONObject();
        responseJson.put("statusCode", "200");
        responseJson.put("headers", new JSONObject());

        JSONObject bodyJSON = new JSONObject();

        try {
            bodyJSON = getResultForQuery(input);
        } catch (ParseException e) {
            if (context != null) {
                context.getLogger().log(e.toString());
            }

            bodyJSON = errorBody("JSON parsing error" + e.getMessage());
        } catch (ParamNotSpecifiedException e) {
            if (context != null) {
                context.getLogger().log(e.toString());
            }
            bodyJSON = errorBody(e.getMessage());
        } finally {
            responseJson.put("body", bodyJSON.toString());
        }


        return responseJson;
    }

    private String getUserID(JSONObject input) {
        LinkedHashMap requestContext = (LinkedHashMap) input.get("requestContext");
        LinkedHashMap identity = (LinkedHashMap) requestContext.get("identity");
        return (String) identity.get("cognitoIdentityId");
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
                        getParam(params, "location"),
                        getParam(params, "time"),
                        getUserID(input));
            case "addUserToEvent":
                return db.addUserToEvent(getParam(params, "eventID"),
                        getParam(params, "userID"));
            case "removeUserFromEvent":
                return db.removeUserFromEvent(getParam(params, "eventID"),
                        getUserID(input));
            case "getEvents":
                return db.getEvents(getUserID(input));
            default:
                return errorBody("API '" + request + "' not supported");
        }

    }

    private JSONObject errorBody(String message) {
        JSONObject error = new JSONObject();
        error.put("status", "failure");
        error.put("error", message);
        return error;
    }


}