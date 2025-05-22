package org.ubiquia.core.flow.service.calculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.model.adapter.AdapterContext;
import org.ubiquia.common.models.adapter.backpressure.BackPressure;
import org.ubiquia.common.models.adapter.backpressure.Egress;
import org.ubiquia.common.models.adapter.backpressure.Ingress;
import org.ubiquia.core.flow.service.logic.adapter.AdapterTypeLogic;

/**
 * This is a service dedicated to calculating back pressure for adapters.
 */
@Service
public class BackPressureCalculator {

    @Autowired
    private AdapterTypeLogic adapterTypeLogic;


    /**
     * Calculate back pressure for a given adapter.
     *
     * @param adapter The adapter to calculate back pressure for.
     * @return An object containing back pressure data.
     */
    public BackPressure calculateBackPressureFor(final AbstractAdapter adapter) {
        var ingress = new Ingress();

        var backpressure = new BackPressure();
        backpressure.setIngress(ingress);

        var adapterContext = adapter.getAdapterContext();
        if (this
            .adapterTypeLogic
            .adapterTypeRequiresEgressSettings(adapterContext.getAdapterType())) {

            var egress = new Egress();
            backpressure.setEgress(egress);
            egress.setCurrentOpenMessages(adapterContext.getOpenMessages());
            egress.setMaxOpenMessages(adapterContext.getEgressSettings().getEgressConcurrency());
        }

        if (!adapterContext.getBackPressureSamplings().isEmpty()) {
            ingress.setQueuedRecords(adapterContext.getBackPressureSamplings().get(0));
            if (adapterContext.getBackPressureSamplings().size() > 1) {
                var rate = this.getRate(adapterContext);
                ingress.setQueueRatePerMinute(rate);
            } else {
                ingress.setQueueRatePerMinute(
                    Float.parseFloat(adapterContext
                        .getBackPressureSamplings()
                        .get(0)
                        .toString()));
            }
        } else {
            ingress.setQueuedRecords(0L);
            ingress.setQueueRatePerMinute(0F);
        }
        return backpressure;
    }

    /**
     * A method to return the back pressure "queue rate."
     *
     * @param adapterContext The adapter to calculate back pressure queue rate for.
     * @return The back pressure queue rate.
     */
    private float getRate(final AdapterContext adapterContext) {

        // Gather variables for code readability.
        var x = adapterContext.getBackPressureSamplings().get(1);
        var y = adapterContext.getBackPressureSamplings().get(0);
        var z = adapterContext.getBackpressurePollRatePerMinute();
        Long r = null;

        // Get our rate of change over our two data points and multiply times the
        // rate per minute we query for back pressure. This determines the
        // rate at which our backpressure queue is growing or shrinking.
        r = (x - y) * z;

        var rate = Float.parseFloat(r.toString());
        return rate;
    }
}
