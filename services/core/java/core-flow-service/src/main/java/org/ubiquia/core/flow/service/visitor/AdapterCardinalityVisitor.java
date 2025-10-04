package org.ubiquia.core.flow.service.visitor;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;

/**
 * A service that can verify cardinalities of components.
 */
@Service
public class AdapterCardinalityVisitor {

    /**
     * Determine whether the adapter has a matching cardinality setting in the graph deployment.
     *
     * @param adapterName     The adapter to look for.
     * @param graphDeployment The deployment we're looking from.
     * @return Whether there's a matching cardinality or not.
     */
    public Boolean hasMatchingCardinality(
        final String adapterName,
        final GraphDeployment graphDeployment) {

        var matchingCardinality = false;

        var cardinality = graphDeployment.getCardinality();
        if (Objects.nonNull(cardinality)
            && Objects.nonNull(cardinality.getComponentlessAdapterSettings())) {

            var match = cardinality
                .getComponentlessAdapterSettings()
                .stream()
                .filter(x -> x.getName().equals(adapterName))
                .findFirst();

            if (match.isPresent()) {
                matchingCardinality = true;
            }

        }
        return matchingCardinality;
    }

    /**
     * Determine whether the adapter's cardinality is set or not.
     *
     * @param adapterName     The adapter to determine.
     * @param graphDeployment The graph deployment settings we're looking for.
     * @return Whether the cardinality is set or not.
     */
    public Boolean isCardinalityEnabled(
        final String adapterName,
        final GraphDeployment graphDeployment) {

        var cardinalityEnabled = false;

        var cardinality = graphDeployment.getCardinality();

        var match = cardinality
            .getComponentlessAdapterSettings()
            .stream()
            .filter(x -> x.getName().equals(adapterName))
            .findFirst();

        cardinalityEnabled = match.get().getEnabled();

        return cardinalityEnabled;
    }
}
