package org.ubiquia.core.belief.state.generator.service.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.models.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.library.implementation.service.builder.BeliefStateNameBuilder;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.core.belief.state.generator.service.builder.BeliefStateDeploymentBuilder;

/**
 * Unit tests for {@link BeliefStateOperator}.
 *
 * <p>Each test injects a real {@link K8sResourceClient} wrapping a mocked
 * {@link GenericKubernetesApi}, which lets us verify the full operator → client → raw-API chain
 * without booting a Spring context.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BeliefStateOperatorTest {

    private static final String UBIQUIA_DEPLOYMENT_NAME =
        "ubiquia-core-belief-state-generator-service";

    private BeliefStateOperator operator;

    @Mock
    private BeliefStateDeploymentBuilder beliefStateDeploymentBuilder;

    @Mock
    private BeliefStateNameBuilder beliefStateNameBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GenericKubernetesApi<V1Deployment, V1DeploymentList> rawDeploymentApi;

    @Mock
    private GenericKubernetesApi<V1Service, V1ServiceList> rawServiceApi;

    @Mock
    private DomainOntology domainOntology;

    @BeforeEach
    void setUp() {
        operator = new BeliefStateOperator();

        var deploymentClient =
            new K8sResourceClient<>(rawDeploymentApi, "test-ns", "deployment");
        var serviceClient =
            new K8sResourceClient<>(rawServiceApi, "test-ns", "service");

        ReflectionTestUtils.setField(operator, "beliefStateDeploymentBuilder",
            beliefStateDeploymentBuilder);
        ReflectionTestUtils.setField(operator, "beliefStateNameBuilder", beliefStateNameBuilder);
        ReflectionTestUtils.setField(operator, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(operator, "deploymentClient", deploymentClient);
        ReflectionTestUtils.setField(operator, "serviceClient", serviceClient);
        ReflectionTestUtils.setField(operator, "namespace", "test-ns");
    }

    // -------------------------------------------------------------------------
    // tryDeployBeliefState
    // -------------------------------------------------------------------------

    @Test
    void tryDeployBeliefState_whenNoDeployment_createsDeploymentAndService() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");
        when(domainOntology.getName()).thenReturn("MyDomain");
        when(domainOntology.getVersion()).thenReturn(version(1, 0, 0));
        when(beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology))
            .thenReturn("mydomain-belief-state-1-0-0");

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getObject()).thenReturn(null);
        when(rawDeploymentApi.get("test-ns", "mydomain-belief-state-1-0-0"))
            .thenReturn(getResponse);

        var deployment = new V1Deployment();
        var service = new V1Service();
        when(beliefStateDeploymentBuilder.buildDeploymentFrom(domainOntology))
            .thenReturn(deployment);
        when(beliefStateDeploymentBuilder.buildServiceFrom(domainOntology))
            .thenReturn(service);

        var createDepResponse = mock(KubernetesApiResponse.class);
        when(createDepResponse.isSuccess()).thenReturn(true);
        when(rawDeploymentApi.create(eq("test-ns"), eq(deployment), any(CreateOptions.class)))
            .thenReturn(createDepResponse);

        var createSvcResponse = mock(KubernetesApiResponse.class);
        when(createSvcResponse.isSuccess()).thenReturn(true);
        when(rawServiceApi.create(eq("test-ns"), eq(service), any(CreateOptions.class)))
            .thenReturn(createSvcResponse);

        operator.tryDeployBeliefState(domainOntology);

        verify(rawDeploymentApi).create(eq("test-ns"), eq(deployment), any(CreateOptions.class));
        verify(rawServiceApi).create(eq("test-ns"), eq(service), any(CreateOptions.class));
    }

    @Test
    void tryDeployBeliefState_whenDeploymentExists_doesNothing() throws Exception {
        when(domainOntology.getName()).thenReturn("MyDomain");
        when(domainOntology.getVersion()).thenReturn(version(1, 2, 3));
        when(beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology))
            .thenReturn("mydomain-belief-state-1-2-3");

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getObject()).thenReturn(new V1Deployment());
        when(rawDeploymentApi.get("test-ns", "mydomain-belief-state-1-2-3"))
            .thenReturn(getResponse);

        operator.tryDeployBeliefState(domainOntology);

        verify(rawDeploymentApi, never()).create(any(), any(), any());
        verify(rawServiceApi, never()).create(any(), any(), any());
        verify(beliefStateDeploymentBuilder, never()).buildDeploymentFrom(any());
        verify(beliefStateDeploymentBuilder, never()).buildServiceFrom(any());
    }

    @Test
    void tryDeployBeliefState_whenDeploymentCreateFails_throws() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");
        when(domainOntology.getName()).thenReturn("MyDomain");
        when(domainOntology.getVersion()).thenReturn(version(2, 0, 0));
        when(beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology))
            .thenReturn("mydomain-belief-state-2-0-0");

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getObject()).thenReturn(null);
        when(rawDeploymentApi.get("test-ns", "mydomain-belief-state-2-0-0"))
            .thenReturn(getResponse);

        var deployment = new V1Deployment();
        when(beliefStateDeploymentBuilder.buildDeploymentFrom(domainOntology))
            .thenReturn(deployment);

        var createResponse = mock(KubernetesApiResponse.class);
        when(createResponse.isSuccess()).thenReturn(false);
        when(rawDeploymentApi.create(eq("test-ns"), eq(deployment), any(CreateOptions.class)))
            .thenReturn(createResponse);

        assertThatThrownBy(() -> operator.tryDeployBeliefState(domainOntology))
            .isInstanceOf(IllegalArgumentException.class);

        verify(rawServiceApi, never()).create(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // tryDeleteAllDeployedBeliefStateResources
    // -------------------------------------------------------------------------

    @Test
    void tryDeleteAllDeployedBeliefStateResources_success_deletesEachDeploymentAndService()
        throws Exception {

        var d1 = deploymentNamed("bs-a");
        var d2 = deploymentNamed("bs-b");
        var list = new V1DeploymentList();
        list.setItems(List.of(d1, d2));

        var listResponse = mock(KubernetesApiResponse.class);
        when(listResponse.isSuccess()).thenReturn(true);
        when(listResponse.getObject()).thenReturn(list);
        when(rawDeploymentApi.list(eq("test-ns"), any(ListOptions.class)))
            .thenReturn(listResponse);

        operator.tryDeleteAllDeployedBeliefStateResources();

        verify(rawDeploymentApi).delete("test-ns", "bs-a");
        verify(rawDeploymentApi).delete("test-ns", "bs-b");
        verify(rawServiceApi).delete("test-ns", "bs-a");
        verify(rawServiceApi).delete("test-ns", "bs-b");
    }

    @Test
    void tryDeleteAllDeployedBeliefStateResources_usesBeliefStateLabelSelector() throws Exception {
        var list = new V1DeploymentList();
        list.setItems(List.of());

        var listResponse = mock(KubernetesApiResponse.class);
        when(listResponse.isSuccess()).thenReturn(true);
        when(listResponse.getObject()).thenReturn(list);
        when(rawDeploymentApi.list(eq("test-ns"), any(ListOptions.class)))
            .thenReturn(listResponse);

        operator.tryDeleteAllDeployedBeliefStateResources();

        var captor = ArgumentCaptor.forClass(ListOptions.class);
        verify(rawDeploymentApi).list(eq("test-ns"), captor.capture());
        assertThat(captor.getValue().getLabelSelector()).isEqualTo("belief-state");
    }

    @Test
    void tryDeleteAllDeployedBeliefStateResources_whenListFails_throws() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        var listResponse = mock(KubernetesApiResponse.class);
        when(listResponse.isSuccess()).thenReturn(false);
        when(rawDeploymentApi.list(eq("test-ns"), any(ListOptions.class)))
            .thenReturn(listResponse);

        assertThatThrownBy(() -> operator.tryDeleteAllDeployedBeliefStateResources())
            .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // deleteBeliefStateResources
    // -------------------------------------------------------------------------

    @Test
    void deleteBeliefStateResources_deletesDeploymentViaDeploymentClientAndServiceViaServiceClient() {
        var dList = new V1DeploymentList();
        dList.setItems(List.of(deploymentNamed("dep-1")));
        var depListResponse = mock(KubernetesApiResponse.class);
        when(depListResponse.getObject()).thenReturn(dList);
        when(rawDeploymentApi.list(eq("test-ns"), any(ListOptions.class)))
            .thenReturn(depListResponse);

        var sList = new V1ServiceList();
        sList.setItems(List.of(serviceNamed("svc-1")));
        var svcListResponse = mock(KubernetesApiResponse.class);
        when(svcListResponse.getObject()).thenReturn(sList);
        when(rawServiceApi.list(eq("test-ns"), any(ListOptions.class)))
            .thenReturn(svcListResponse);

        operator.deleteBeliefStateResources("bs-x");

        // deployment deleted via deploymentClient
        verify(rawDeploymentApi).delete("test-ns", "dep-1");
        // service deleted via serviceClient (not deploymentClient — the pre-refactor bug)
        verify(rawServiceApi).delete("test-ns", "svc-1");
        verify(rawDeploymentApi, never()).delete("test-ns", "svc-1");
    }

    @Test
    void deleteBeliefStateResources_usesBeliedStateEqualsSelector() {
        var dList = new V1DeploymentList();
        dList.setItems(List.of());
        var depListResponse = mock(KubernetesApiResponse.class);
        when(depListResponse.getObject()).thenReturn(dList);
        when(rawDeploymentApi.list(eq("test-ns"), any(ListOptions.class)))
            .thenReturn(depListResponse);

        var sList = new V1ServiceList();
        sList.setItems(List.of());
        var svcListResponse = mock(KubernetesApiResponse.class);
        when(svcListResponse.getObject()).thenReturn(sList);
        when(rawServiceApi.list(eq("test-ns"), any(ListOptions.class)))
            .thenReturn(svcListResponse);

        operator.deleteBeliefStateResources("my-domain-bs");

        var depCaptor = ArgumentCaptor.forClass(ListOptions.class);
        verify(rawDeploymentApi).list(eq("test-ns"), depCaptor.capture());
        assertThat(depCaptor.getValue().getLabelSelector()).isEqualTo("belief-state=my-domain-bs");

        var svcCaptor = ArgumentCaptor.forClass(ListOptions.class);
        verify(rawServiceApi).list(eq("test-ns"), svcCaptor.capture());
        assertThat(svcCaptor.getValue().getLabelSelector()).isEqualTo("belief-state=my-domain-bs");
    }

    // -------------------------------------------------------------------------
    // teardown
    // -------------------------------------------------------------------------

    @Test
    void teardown_whenUbiquiaDeploymentMissing_triggersCleanup() throws Exception {
        var spy = spy(operator);
        doNothing().when(spy).tryDeleteAllDeployedBeliefStateResources();

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getHttpStatusCode()).thenReturn(HttpStatus.NO_CONTENT.value());
        when(rawDeploymentApi.get("test-ns", UBIQUIA_DEPLOYMENT_NAME)).thenReturn(getResponse);

        spy.teardown();

        verify(spy).tryDeleteAllDeployedBeliefStateResources();
    }

    @Test
    void teardown_whenUbiquiaDeploymentRequestFails_triggersCleanup() throws Exception {
        var spy = spy(operator);
        doNothing().when(spy).tryDeleteAllDeployedBeliefStateResources();

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getHttpStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(getResponse.isSuccess()).thenReturn(false);
        when(rawDeploymentApi.get("test-ns", UBIQUIA_DEPLOYMENT_NAME)).thenReturn(getResponse);

        spy.teardown();

        verify(spy).tryDeleteAllDeployedBeliefStateResources();
    }

    @Test
    void teardown_whenUbiquiaDeploymentPresent_doesNotTriggerCleanup() throws Exception {
        var spy = spy(operator);

        var getResponse = mock(KubernetesApiResponse.class);
        when(getResponse.getHttpStatusCode()).thenReturn(HttpStatus.OK.value());
        when(getResponse.isSuccess()).thenReturn(true);
        when(rawDeploymentApi.get("test-ns", UBIQUIA_DEPLOYMENT_NAME)).thenReturn(getResponse);

        spy.teardown();

        verify(spy, never()).tryDeleteAllDeployedBeliefStateResources();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static SemanticVersion version(int major, int minor, int patch) {
        var v = new SemanticVersion();
        v.setMajor(major);
        v.setMinor(minor);
        v.setPatch(patch);
        return v;
    }

    private static V1Deployment deploymentNamed(String name) {
        var d = new V1Deployment();
        var meta = new V1ObjectMeta();
        meta.setName(name);
        d.setMetadata(meta);
        return d;
    }

    private static V1Service serviceNamed(String name) {
        var s = new V1Service();
        var meta = new V1ObjectMeta();
        meta.setName(name);
        s.setMetadata(meta);
        return s;
    }
}
