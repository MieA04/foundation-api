package org.miea04.foundation;

import org.miea04.foundation.api.capability.CapabilityRegistry;
import org.miea04.foundation.api.container.FoundationContainer;

/**
 * Foundation
 *
 * @author MieMie
 */
public class Foundation {

    private final FoundationContainer foundationContainer;
    private final CapabilityRegistry capabilityRegistry;

    public Foundation(FoundationContainer foundationContainer, CapabilityRegistry capabilityRegistry){
        this.foundationContainer = foundationContainer;
        this.capabilityRegistry = capabilityRegistry;
    }

    public FoundationContainer getContainer() {
        return this.foundationContainer;
    }

    public CapabilityRegistry getCapabilityRegistry(){
        return this.capabilityRegistry;
    }

}
