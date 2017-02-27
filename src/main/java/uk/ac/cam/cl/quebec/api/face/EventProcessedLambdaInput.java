package uk.ac.cam.cl.quebec.api.face;

import java.util.List;

public class EventProcessedLambdaInput {
    private int eventID;
    private int videoID;
    private String thumbnailS3Path;
    private List<String> members;

    public Integer getVideoID() {
        return videoID;
    }

    public void setVideoID(int videoID) {
        this.videoID = videoID;
    }

    public String getThumbnailS3Path() {
        return thumbnailS3Path;
    }

    public void setThumbnailS3Path(String thumbnailS3Path) {
        this.thumbnailS3Path = thumbnailS3Path;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
