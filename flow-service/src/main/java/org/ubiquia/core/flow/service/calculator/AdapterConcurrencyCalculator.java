package org.ubiquia.core.flow.service.calculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.service.logic.adapter.AdapterTypeLogic;

@Service
public class AdapterConcurrencyCalculator {

    @Autowired
    private AdapterTypeLogic adapterTypeLogic;

    /**
     * Provided an adapter, calculate the page size to use to query the inbox.
     * @param adapter The adapter to calculate a page size for.
     * @return A page size.
     */
    public Integer getInboxQueryPageSizeFor(final AbstractAdapter adapter) {

        Integer pageSize = null;
        var adapterContext = adapter.getAdapterContext();

        if (this
            .adapterTypeLogic
            .adapterTypeRequiresEgressSettings(adapterContext.getAdapterType())) {

            // I know what you're thinking: "but what if the page size is 0?"
            // This scenario is guarded against during the inboxPolling process
            // via the adapterInboxPollingLogic.
            pageSize = adapterContext.getEgressSettings().getEgressConcurrency()
                - adapterContext.getOpenMessages();
        } else {
            pageSize = 1;
        }

        return pageSize;
    }
}
