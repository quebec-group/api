package uk.ac.cam.cl.quebec.api.json;

import org.json.simple.JSONObject;
import uk.ac.cam.cl.quebec.api.APIException;

public class JSON extends JSONObject {

    public JSON() {
        super();
    }

    public JSON(JSONObject toWrap) {
        super();
        toWrap.forEach(this::putIfAbsent);
    }

    public String getString(String key) throws APIException {
        if (containsKey(key) && get(key) instanceof String) {
            return (String) get(key);
        }

        throw new APIException("Parameter '" + key + "' not found");
    }

    public int getInteger(String key) throws APIException {
        if (containsKey(key) && get(key) instanceof Number) {
            return getInt(get(key));
        }

        throw new APIException("Parameter '" + key + "' not found");
    }

    private int getInt(Object object) {
        return ((Number) object).intValue();
    }
}
