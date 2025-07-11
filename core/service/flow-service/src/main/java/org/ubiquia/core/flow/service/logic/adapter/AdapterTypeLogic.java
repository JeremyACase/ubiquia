package org.ubiquia.core.flow.service.logic.adapter;

import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;

/**
 * A service that exposes adapter type logic.
 */
@Service
public class AdapterTypeLogic {

    /**
     * Return whether or not this type of adapter requires egress settings.
     *
     * @param adapterType The type of adapter to verify.
     * @return Whether or not the adapter requires egress settings.
     */
    public Boolean adapterTypeRequiresEgressSettings(final AdapterType adapterType) {

        return adapterType.equals(AdapterType.PUSH)
            || adapterType.equals(AdapterType.EGRESS)
            || adapterType.equals(AdapterType.HIDDEN)
            || adapterType.equals(AdapterType.SUBSCRIBE)
            || adapterType.equals(AdapterType.MERGE);
    }

    /**
     * Return whether or not this type of adapter requires a component.
     *
     * @param adapterType The type of adapter to verify.
     * @return Whether or not the adapter requires an component.
     */
    public Boolean adapterTypeRequiresComponent(final AdapterType adapterType) {

        return adapterType.equals(AdapterType.PUSH)
            || adapterType.equals(AdapterType.HIDDEN)
            || adapterType.equals(AdapterType.SUBSCRIBE)
            || adapterType.equals(AdapterType.MERGE);
    }

    /**
     * Return whether or not this type of adapter requires an endpoint.
     *
     * @param adapterType The type of adapter to verify.
     * @return Whether or not the adapter requires an endpoint.
     */
    public Boolean adapterTypeRequiresEndpoint(final AdapterType adapterType) {

        return !adapterType.equals(AdapterType.QUEUE)
            && !adapterType.equals(AdapterType.PUBLISH);
    }
}
