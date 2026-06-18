package org.miea04.foundation.api.exception;

/**
 * CapabilityRegisterException
 *
 * @author MieMie
 */
public class CapabilityRegisterRepeatException extends FoundationException{
    public CapabilityRegisterRepeatException(String key) {
        super("Capability register repeat" + ": " + key);
    }
}
