package org.ubiquia.core.belief.state.generator.service.k8s;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.kubernetes.client.util.generic.options.CreateOptions;
import io.kubernetes.client.util.generic.options.ListOptions;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type-safe wrapper around {@link GenericKubernetesApi} that binds a namespace, eliminating
 * the repetitive namespace argument at every call site.
 *
 * <p>Use {@link #deleteBySelector} to list-and-delete all matching resources in one call.
 *
 * @param <T> the Kubernetes resource type (e.g. {@code V1Deployment})
 * @param <L> the corresponding list type (e.g. {@code V1DeploymentList})
 */
public final class K8sResourceClient<T extends KubernetesObject, L extends KubernetesListObject> {

    private static final Logger logger = LoggerFactory.getLogger(K8sResourceClient.class);

    private final GenericKubernetesApi<T, L> api;
    private final String namespace;
    private final String resourceKind;

    /**
     * Constructs a client bound to the given namespace.
     *
     * @param api          the underlying Kubernetes API handle
     * @param namespace    the namespace all operations will target
     * @param resourceKind human-readable kind label used in log messages
     *                     (e.g. {@code "deployment"})
     */
    public K8sResourceClient(
        final GenericKubernetesApi<T, L> api,
        final String namespace,
        final String resourceKind) {

        this.api = api;
        this.namespace = namespace;
        this.resourceKind = resourceKind;
    }

    /**
     * Returns the underlying {@link GenericKubernetesApi}, needed for informer registration.
     *
     * @return the raw API client
     */
    public GenericKubernetesApi<T, L> getApi() {
        return this.api;
    }

    /**
     * Fetches the named resource from the bound namespace.
     *
     * @param name the resource name
     * @return the API response
     */
    public KubernetesApiResponse<T> get(final String name) {
        return this.api.get(this.namespace, name);
    }

    /**
     * Creates the given resource in the bound namespace.
     *
     * @param resource the resource to create
     * @param options  create options
     * @return the API response
     */
    public KubernetesApiResponse<T> create(final T resource, final CreateOptions options) {
        return this.api.create(this.namespace, resource, options);
    }

    /**
     * Deletes the named resource from the bound namespace.
     *
     * @param name the resource name
     * @return the API response
     */
    public KubernetesApiResponse<T> delete(final String name) {
        return this.api.delete(this.namespace, name);
    }

    /**
     * Lists resources in the bound namespace using the given options.
     *
     * @param options list options (e.g. label selectors, field selectors)
     * @return the API response containing the list object
     */
    public KubernetesApiResponse<L> list(final ListOptions options) {
        return this.api.list(this.namespace, options);
    }

    /**
     * Returns all resources in the bound namespace whose labels match {@code labelSelector}.
     *
     * @param labelSelector a Kubernetes label selector expression
     * @return the matching resources; empty list if none
     */
    public List<? extends KubernetesObject> listBySelector(final String labelSelector) {
        var opts = new ListOptions();
        opts.setLabelSelector(labelSelector);
        return this.api.list(this.namespace, opts).getObject().getItems();
    }

    /**
     * Deletes every resource in the bound namespace whose labels match {@code labelSelector},
     * logging each deletion.
     *
     * @param labelSelector a Kubernetes label selector expression
     */
    public void deleteBySelector(final String labelSelector) {
        this.listBySelector(labelSelector).forEach(item -> {
            var name = item.getMetadata().getName();
            logger.info("...deleting {} with name: {}...", this.resourceKind, name);
            this.api.delete(this.namespace, name);
        });
    }
}
