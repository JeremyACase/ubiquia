package org.ubiquia.core.flow.component.adapter;


import org.ubiquia.core.flow.model.adapter.AdapterContext;

/**
 * An abstract class that can be used to adapt to Agents in Ubiquia. There are different
 * types of adapters; each inherits from this abstract base class.
 * Adapters are built at runtime using data from the database. They can be torn down or deployed
 * dynamically when a graph is similarly torn down or deployed.
 * All adapters do very-different things (i.e., poll versus POST.) As such, some adapters
 * will constantly monitor their "backpressure", while others will not.
 */
public abstract class AAdapter {

    private AdapterContext adapterContext;

    public AdapterContext getAdapterContext() {
        return adapterContext;
    }

    public void setAdapterContext(AdapterContext adapterContext) {
        this.adapterContext = adapterContext;
    }
}