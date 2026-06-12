package org.ubiquia.core.belief.state.generator.service.builder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder for Kubernetes resource label maps.
 *
 * <p>Typical usage:
 * <pre>
 *   K8sLabelBuilder.from(baseLabels)
 *       .remove("component", "app.kubernetes.io/managed-by")
 *       .put("domain", domainName)
 *       .withBeliefState(beliefStateName)
 *       .build();
 * </pre>
 *
 * <p>The source map is never mutated; each builder works on its own copy.
 */
public final class K8sLabelBuilder {

    private static final String LABEL_BELIEF_STATE = "belief-state";
    private static final String LABEL_MANAGED_BY = "app.kubernetes.io/managed-by";
    private static final String MANAGED_BY_UBIQUIA = "ubiquia";

    private final Map<String, String> labels;

    private K8sLabelBuilder(final Map<String, String> baseLabels) {
        this.labels = new HashMap<>(baseLabels);
    }

    /**
     * Starts a new builder pre-populated with a copy of {@code baseLabels}.
     *
     * @param baseLabels the label map to copy from (not mutated)
     * @return a new builder instance
     */
    public static K8sLabelBuilder from(final Map<String, String> baseLabels) {
        return new K8sLabelBuilder(baseLabels);
    }

    /**
     * Removes the given keys from the working label set.
     *
     * @param keys keys to remove
     * @return this builder
     */
    public K8sLabelBuilder remove(final String... keys) {
        Arrays.stream(keys).forEach(this.labels::remove);
        return this;
    }

    /**
     * Adds or replaces a single label entry.
     *
     * @param key   the label key
     * @param value the label value
     * @return this builder
     */
    public K8sLabelBuilder put(final String key, final String value) {
        this.labels.put(key, value);
        return this;
    }

    /**
     * Stamps the standard Ubiquia-managed labels onto the working set:
     * {@code belief-state = beliefStateName} and
     * {@code app.kubernetes.io/managed-by = ubiquia}.
     *
     * @param beliefStateName the belief-state resource name
     * @return this builder
     */
    public K8sLabelBuilder withBeliefState(final String beliefStateName) {
        this.labels.put(LABEL_BELIEF_STATE, beliefStateName);
        this.labels.put(LABEL_MANAGED_BY, MANAGED_BY_UBIQUIA);
        return this;
    }

    /**
     * Returns the assembled label map.
     *
     * @return the mutable label map produced by this builder
     */
    public Map<String, String> build() {
        return this.labels;
    }
}
