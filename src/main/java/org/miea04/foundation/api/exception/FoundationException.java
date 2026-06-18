package org.miea04.foundation.api.exception;

/**
 * FoundationException
 *
 * @author MieMie
 */
public class FoundationException extends RuntimeException{

    public FoundationException(String msg) {
        super(msg);
    }

    public FoundationException(String msg, Throwable cause){
        super(msg, cause);
    }

}
