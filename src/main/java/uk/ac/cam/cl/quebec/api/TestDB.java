package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.simple.JSONObject;
import org.neo4j.driver.v1.*;

// uk.ac.cam.cl.quebec.api.TestDB::handleRequest
public class TestDB implements RequestHandler<JSONObject, JSONObject> {

    private static String DB_PASS = System.getenv("grapheneDBPass");

    @Override
    public JSONObject handleRequest(JSONObject input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Loading Java Lambda handler simple\n");

        JSONObject responseBody = new JSONObject();
        responseBody.put("message", getDatabaseQueryResult());

        JSONObject responseJson = new JSONObject();
        responseJson.put("statusCode", "200");
        responseJson.put("headers", new JSONObject());
        responseJson.put("body", responseBody.toString());

        return responseJson;
    }

    public String getDatabaseQueryResult() {
        StringBuilder builder = new StringBuilder();

        //Test database connection
        Driver driver = GraphDatabase.driver("bolt://hobby-dckpkbcijildgbkelginfool.dbs.graphenedb.com:24786",
                AuthTokens.basic("quebec", DB_PASS));

        Session session = driver.session();

        StatementResult result = session.run("MATCH (you {name:\"Callum\"})-[:FRIENDS_WITH]->(yourFriends) RETURN yourFriends");

        while (result.hasNext()) {
            Record record = result.next();
            builder.append("Friend: ")
                    .append(record.get("yourFriends").get("name").asString())
                    .append(", ")
                    .append(record.get("yourFriends").get("crsid").asString())
                    .append(" \n");
        }

        session.close();
        driver.close();

        return builder.toString();

    }
}