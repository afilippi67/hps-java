package org.hps.conditions.api;

/**
 * Thrown by methods of {@link ConditionsObject} or other associated classes such as converters and collections.
 */
@SuppressWarnings("serial")
public final class ConditionsObjectException extends Exception {

    /**
     * The associated conditions object to the error.
     */
    private ConditionsObject object;

    /**
     * Error with a message.
     *
     * @param message the error message
     */
    public ConditionsObjectException(final String message) {
        super(message);
    }

    /**
     * Error with an associated throwable.
     *
     * @param message the error message
     * @param cause the error's cause
     */
    public ConditionsObjectException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Error with a message and object.
     *
     * @param message the error message
     * @param object the associated conditions object
     */
    public ConditionsObjectException(final String message, final ConditionsObject object) {
        super(message);
        this.object = object;
    }

    /**
     * Get the associated conditions object to the error.
     * 
     * @return the object associated with the error
     */
    public ConditionsObject getConditionsObject() {
        return object;
    }
}
