package org.ubiquia.core.flow;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.library.api.repository.UbiquiaAgentRepository;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;
import org.ubiquia.core.flow.repository.*;
import org.ubiquia.core.flow.service.logic.ubiquia.UbiquiaAgentLogic;
import org.ubiquia.core.flow.service.manager.AdapterManager;

@Service
public class TestHelper {


    @Autowired
    private AgentCommunicationLanguageRepository aclRepository;

    @Autowired
    private AdapterManager adapterManager;

    @Autowired
    private AdapterRepository adapterRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private FlowEventRepository flowEventRepository;

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private UbiquiaAgentLogic ubiquiaAgentLogic;

    @Autowired
    private UbiquiaAgentRepository ubiquiaAgentRepository;

    public void setupAgentState() {
        this.adapterManager.teardownAllAdapters();
        this.ubiquiaAgentLogic.tryInitializeAgentInDatabase();
    }

    /**
     * A helper method that can "find" a deployed adapter from the context.
     *
     * @param adapterName The adapter name to find.
     * @param graphName   The graph to find the adapter from.
     * @return The deployed adapter.
     */
    @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
    public AbstractAdapter findAdapter(final String adapterName, final String graphName) {

        var adapterMap = (HashMap<String, HashMap<String, AbstractAdapter>>) ReflectionTestUtils
            .getField(this.adapterManager, "adapterMap");

        var adapterEntity = this
            .adapterRepository
            .findByGraphNameAndName(
                graphName,
                adapterName)
            .get();

        var adapter = adapterMap
            .get(graphName)
            .get(adapterEntity.getId());

        return adapter;
    }
}