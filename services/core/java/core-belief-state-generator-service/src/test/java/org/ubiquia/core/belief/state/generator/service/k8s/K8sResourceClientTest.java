package org.ubiquia.core.belief.state.generator.service.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.kubernetes.client.util.generic.options.CreateOptions;
import io.kubernetes.client.util.generic.options.ListOptions;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link K8sResourceClient}. */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class K8sResourceClientTest {

    @Mock
    private GenericKubernetesApi<V1Deployment, V1DeploymentList> api;

    private K8sResourceClient<V1Deployment, V1DeploymentList> client;

    @BeforeEach
    void setUp() {
        client = new K8sResourceClient<>(api, "test-ns", "deployment");
    }

    @Test
    void get_delegatesToApiWithBoundNamespace() {
        var response = mock(KubernetesApiResponse.class);
        when(api.get("test-ns", "my-dep")).thenReturn(response);

        var result = client.get("my-dep");

        assertThat(result).isSameAs(response);
        verify(api).get("test-ns", "my-dep");
    }

    @Test
    void create_delegatesToApiWithBoundNamespace() {
        var deployment = new V1Deployment();
        var options = new CreateOptions();
        var response = mock(KubernetesApiResponse.class);
        when(api.create("test-ns", deployment, options)).thenReturn(response);

        var result = client.create(deployment, options);

        assertThat(result).isSameAs(response);
        verify(api).create("test-ns", deployment, options);
    }

    @Test
    void delete_delegatesToApiWithBoundNamespace() {
        var response = mock(KubernetesApiResponse.class);
        when(api.delete("test-ns", "my-dep")).thenReturn(response);

        var result = client.delete("my-dep");

        assertThat(result).isSameAs(response);
        verify(api).delete("test-ns", "my-dep");
    }

    @Test
    void list_delegatesToApiWithBoundNamespace() {
        var opts = new ListOptions();
        var response = mock(KubernetesApiResponse.class);
        when(api.list("test-ns", opts)).thenReturn(response);

        var result = client.list(opts);

        assertThat(result).isSameAs(response);
        verify(api).list("test-ns", opts);
    }

    @Test
    void listBySelector_setsLabelSelectorAndReturnsItems() {
        var dep = deployment("dep-1");
        var list = new V1DeploymentList();
        list.setItems(List.of(dep));

        var response = mock(KubernetesApiResponse.class);
        when(response.getObject()).thenReturn(list);
        when(api.list(eq("test-ns"), any(ListOptions.class))).thenReturn(response);

        var items = client.listBySelector("app=ubiquia");

        assertThat(items).hasSize(1);
        var captor = ArgumentCaptor.forClass(ListOptions.class);
        verify(api).list(eq("test-ns"), captor.capture());
        assertThat(captor.getValue().getLabelSelector()).isEqualTo("app=ubiquia");
    }

    @Test
    void deleteBySelector_deletesEachMatchingItem() {
        var list = new V1DeploymentList();
        list.setItems(List.of(deployment("dep-1"), deployment("dep-2")));

        var response = mock(KubernetesApiResponse.class);
        when(response.getObject()).thenReturn(list);
        when(api.list(eq("test-ns"), any(ListOptions.class))).thenReturn(response);

        client.deleteBySelector("belief-state=bs-x");

        verify(api).delete("test-ns", "dep-1");
        verify(api).delete("test-ns", "dep-2");
    }

    @Test
    void deleteBySelector_withNoMatches_deletesNothing() {
        var emptyList = new V1DeploymentList();
        emptyList.setItems(List.of());

        var response = mock(KubernetesApiResponse.class);
        when(response.getObject()).thenReturn(emptyList);
        when(api.list(eq("test-ns"), any(ListOptions.class))).thenReturn(response);

        client.deleteBySelector("belief-state=nonexistent");

        verify(api, never()).delete(anyString(), anyString());
    }

    @Test
    void deleteBySelector_usesBoundNamespaceForBothListAndDelete() {
        var list = new V1DeploymentList();
        list.setItems(List.of(deployment("dep-1")));

        var response = mock(KubernetesApiResponse.class);
        when(response.getObject()).thenReturn(list);
        when(api.list(eq("test-ns"), any(ListOptions.class))).thenReturn(response);

        client.deleteBySelector("some-selector");

        verify(api).list(eq("test-ns"), any(ListOptions.class));
        verify(api).delete(eq("test-ns"), anyString());
    }

    @Test
    void getApi_returnsUnderlyingApi() {
        assertThat(client.getApi()).isSameAs(api);
    }

    private static V1Deployment deployment(String name) {
        var d = new V1Deployment();
        var meta = new V1ObjectMeta();
        meta.setName(name);
        d.setMetadata(meta);
        return d;
    }
}
