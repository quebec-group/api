package uk.ac.cam.cl.quebec.api;

import java.util.List;

public class EventProcessedLambdaInput {
    private String eventID;
    private List<String> members;

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
