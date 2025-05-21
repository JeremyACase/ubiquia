package org.ubiquia.core.flow.exception;

import java.util.List;

/**
 * Class that maintains error messages to send back to erroneous REST requests.
 */
public class ErrorResponse {

    private List<String> details;

    private String message;

    /**
     * Get the details associated with this reponse.
     *
     * @return The details.
     */
    public List<String> getDetails() {
        return this.details;
    }

    /**
     * Set the details of this response.
     *
     * @param details The details to set.
     */
    public void setDetails(final List<String> details) {
        this.details = details;
    }

    /**
     * Get the message of this response.
     *
     * @return The message.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Set the message of this response.
     *
     * @param message The message to set.
     */
    public void setMessage(final String message) {
        this.message = message;
    }
}
