package uk.ac.cam.cl.quebec.api;

public class LambdaOutput {
    private boolean succeeded;
    private String errorMessage = "";

    public LambdaOutput(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public LambdaOutput(boolean succeeded, String errorMessage) {
        this.succeeded = succeeded;
        this.errorMessage = errorMessage;
    }

    public boolean didSucceed() {
        return succeeded;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return super.toString() + "\n\tSucceeded? " + succeeded + "\n\tError message: " + errorMessage;
    }
}
