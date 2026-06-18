package org.miea04.foundation.api.capability;

/**
 * CapabilityKey
 *
 * @author MieMie
 */
public record CapabilityKey(String namespace, String path) {

    public static CapabilityKey of(String namespace, String path){
        return new CapabilityKey(namespace, path);
    }

}
