package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.neo4j.driver.v1.exceptions.ClientException;

// uk.ac.cam.cl.quebec.api.APIHandler::handleRequest
public class APIHandler implements RequestHandler<JsonObject, JsonObject> {

    private DBManager db = new DBManager();
    private SQSWrapper sqs = new SQSWrapper();

    @Override
    public JsonObject handleRequest(JsonObject input, Context context) {

        JsonObject responseJson = null;

        try {
            responseJson = new JsonObject();
            responseJson.addProperty("statusCode", "200");
            responseJson.add("headers", new JsonObject());
            responseJson.addProperty("body", getResultForQuery(input).toString());

            if (context != null) {
                context.getLogger().log(responseJson.toString());
            }
        } catch (APIException|ClientException e) {
            if (context != null) {
                context.getLogger().log(e.toString());
            }

            responseJson = new JsonObject();
            responseJson.addProperty("statusCode", "400");
            responseJson.add("headers", new JsonObject());
            responseJson.addProperty("body", errorBody(e.getMessage()).toString());
        }


        return responseJson;
    }

    private String getUserID(JsonObject input) {
        JsonObject requestContext = input.getAsJsonObject("requestContext");
        JsonObject identity = requestContext.getAsJsonObject("identity");
        return identity.get("cognitoIdentityId").getAsString();
    }

    private JsonObject getParams(JsonObject input) {
        return new JsonPrimitive(input.get("body").getAsString()).getAsJsonObject();
    }

    private String getRequest(JsonObject input) {
        String path = input.get("path").getAsString();
        return path.replace("/api/", "");
    }

    private String getString(JsonObject params, String key) throws APIException {
        String param = params.get(key).getAsString();

        if (param == null) {
            throw new APIException("Parameter '" + key + "' not found");
        }

        return param;
    }

    private int getInteger(JsonObject params, String key) throws APIException {
        if (params.has(key)) {
            return params.get(key).getAsInt();
        }

        throw new APIException("Parameter '" + key + "' not found");
    }


    private JsonObject getResultForQuery(JsonObject input) throws APIException, ClientException {
        JsonObject params = getParams(input);
        String request = getRequest(input);

        switch (request) {
            case "createUser":
                return db.createUser(getUserID(input),
                        getString(params, "name"),
                        getString(params, "email"),
                        getString(params, "arn"));
            case "follow":
                return db.follow(getUserID(input),
                        getString(params, "userID"));
            case "unfollow":
                return db.unfollow(getUserID(input),
                        getString(params, "userID"));
            case "following":
                return db.getFollowing(getString(params, "userID"));
            case "followers":
                return db.getFollowers(getString(params, "userID"));
            case "setProfileVideo": {
                String S3ID = getString(params, "S3ID");
                String userID = getUserID(input);

                JsonObject response = db.setProfileVideo(userID, S3ID);

                sqs.sendTrainingVideo(S3ID, userID, response.get("videoID").getAsInt());

                return response;
            }
            case "addVideoToEvent":
                return addVideoToEvent(getUserID(input),
                        getInteger(params, "eventID"),
                        getString(params, "S3ID"));
            case "createEvent": {
                String userID = getUserID(input);
                JsonObject eventResponse = db.createEvent(getString(params, "title"),
                        getString(params, "location"),
                        getString(params, "time"),
                        userID);

                String S3ID = getString(params, "videoPath");
                int eventID = eventResponse.get("eventID").getAsInt();

                return addVideoToEvent(userID, eventID, S3ID);
            }
            case "addUserToEvent":
                return db.addUserToEvent(getInteger(params, "eventID"),
                        getString(params, "userID"));
            case "removeFromEvent":
                return db.removeUserFromEvent(getInteger(params, "eventID"),
                        getUserID(input));
            case "getEvents":
                return db.getEvents(getUserID(input));
            case "likeEvent":
                return db.likeEvent(getUserID(input), getInteger(params, "eventID"));
            case "unlikeEvent":
                return db.unlikeEvent(getUserID(input), getInteger(params, "eventID"));
            case "getInfo":
                return db.getInfo(getUserID(input));
            default:
                throw new APIException("API '" + request + "' not supported");
        }
    }

    private JsonObject errorBody(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("errorMessage", message);
        return error;
    }

    private JsonObject addVideoToEvent(String userID, int eventID, String S3ID) {
        JsonObject response = db.addVideoToEvent(eventID, S3ID);

        sqs.sendEventVideo(S3ID, eventID, response.get("videoID").getAsInt(),
                db.getRelatedUsers(userID));

        return response;
    }
}
