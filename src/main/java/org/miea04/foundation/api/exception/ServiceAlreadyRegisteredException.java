package org.miea04.foundation.api.exception;

/**
 * ServiceAlreadyRegisteredException
 *
 * @author MieMie
 */
public class ServiceAlreadyRegisteredException extends FoundationException{

    public ServiceAlreadyRegisteredException(String serviceName){
        super("Service already registered: " + serviceName);
    }

    public ServiceAlreadyRegisteredException(String serviceName, Throwable cause) {
        super(serviceName, cause);
    }

}
