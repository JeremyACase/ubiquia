package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import reactor.core.publisher.Mono;

/**
 * Proxy controller that routes communication-service requests to the Flow Service
 * <em>Agent Communication Language (ACL)</em> endpoints.
 *
 * <p>
 * Mounted at {@code /ubiquia/communication-service/flow-service/agent-communication-language},
 * this façade forwards requests to the downstream Flow Service base
 * {@code /ubiquia/flow-service/agent-communication-language}. All proxy mechanics
 * (propagating method, headers, query params, body; reactive I/O; response handling)
 * are inherited from {@link AbstractUbiquiaDaoControllerProxy}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Provide the downstream base URL for ACL endpoints via {@link #getUrlHelper()}.</li>
 *   <li>Forward ACL registration requests (e.g., {@code /register/post}) to the Flow Service.</li>
 * </ul>
 *
 * <p>Downstream host/port are sourced from {@link FlowServiceConfig}.</p>
 */
@RestController
@RequestMapping("/ubiquia/communication-service/flow-service/agent-communication-language")
public class AgentCommunicationLanguageControllerProxy
    extends AbstractUbiquiaDaoControllerProxy<AgentCommunicationLanguage> {

    /** Flow Service host/port configuration used to build the downstream base URL. */
    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /**
     * Proxies an ACL registration request to the Flow Service.
     *
     * <p>Forwards the incoming POST (including headers and query parameters) to the
     * downstream {@code /register/post} endpoint and returns the downstream response as-is.</p>
     *
     * @param body    the {@link AgentCommunicationLanguage} payload to register
     * @param request the original reactive request whose method/headers/query are mirrored
     * @return a {@link Mono} emitting the downstream {@link ResponseEntity} containing an
     *         {@link IngressResponse}, or an error if the call fails
     */
    @PostMapping("/register/post")
    public Mono<ResponseEntity<IngressResponse>> proxyGraphPost(
        @RequestBody AgentCommunicationLanguage body,
        ServerHttpRequest request) {

        var proxied = super.proxyToPostEndpoint("/register/post", request, body);
        return proxied;
    }

    /**
     * Builds the base URL for the Flow Service <em>agent-communication-language</em> endpoints.
     *
     * <p>Example format:
     * {@code http://<host>:<port>/ubiquia/flow-service/agent-communication-language}</p>
     *
     * @return fully qualified base URL used by inherited proxy methods
     */
    @Override
    public String getUrlHelper() {
        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia/flow-service/agent-communication-language";
        return url;
    }
}
