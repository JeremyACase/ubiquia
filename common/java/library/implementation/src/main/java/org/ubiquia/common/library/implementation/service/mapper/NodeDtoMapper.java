package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;


@Service
public class NodeDtoMapper extends GenericDtoMapper<
    NodeEntity,
    Node> {

    @Autowired
    private ComponentDtoMapper componentDtoMapper;

    @Autowired
    private OverrideSettingsMapper overrideSettingsMapper;

    @Override
    public Node map(final NodeEntity from) throws JsonProcessingException {

        Node to = null;
        if (Objects.nonNull(from)) {
            to = new Node();
            super.setAbstractEntityFields(from, to);

            to.setName(from.getName());
            to.setNodeSettings(from.getNodeSettings());
            to.setNodeType(from.getNodeType());
            to.setFlowEvents(new ArrayList<>());
            to.setBrokerSettings(from.getBrokerSettings());
            to.setComponent(this.componentDtoMapper.map(from.getComponent()));
            to.setDownstreamNodes(this.mapUpstreamOrDownstreamAdapters(
                from.getDownstreamNodes()));
            to.setDescription(from.getDescription());
            to.setEgressSettings(from.getEgressSettings());
            to.setEndpoint(from.getEndpoint());
            to.setPollSettings(from.getPollSettings());
            to.setUpstreamNodes(this.mapUpstreamOrDownstreamAdapters(
                from.getUpstreamNodes()));
            to.setCommunicationServiceSettings(from.getCommunicationServiceSettings());

            to.setOverrideSettings(new ArrayList<>());
            if (Objects.nonNull(from.getOverrideSettings())) {
                var converted = this.overrideSettingsMapper.mapToObjectRepresentation(
                    from.getOverrideSettings());
                to.getOverrideSettings().addAll(converted);
            }

            to.setInputSubSchemas(new ArrayList<>());
            to.getInputSubSchemas().addAll(from.getInputSubSchemas());

            to.setOutputSubSchema(from.getOutputSubSchema());

        }
        return to;
    }

    /**
     * Helper method to map an adapter's upstream/downstream adapters without creating a
     * cycle.
     *
     * @param froms The adapters to map.
     * @return The mapped adapters.
     * @throws JsonProcessingException Exception from mapping.
     */
    private List<Node> mapUpstreamOrDownstreamAdapters(final List<NodeEntity> froms)
        throws JsonProcessingException {
        var tos = new ArrayList<Node>();

        for (var from : froms) {
            var to = new Node();
            super.setAbstractEntityFields(from, to);
            to.setName(from.getName());
            to.setNodeType(from.getNodeType());
            to.setComponent(this.componentDtoMapper.map(from.getComponent()));
            to.setDescription(from.getDescription());
            to.setEgressSettings(from.getEgressSettings());
            to.setEndpoint(from.getEndpoint());
            to.setFlowEvents(new ArrayList<>());
            to.setPollSettings(from.getPollSettings());
            to.setOverrideSettings(new ArrayList<>());
            if (Objects.nonNull(from.getOverrideSettings())) {
                var converted = this.overrideSettingsMapper.mapToObjectRepresentation(
                    from.getOverrideSettings());
                to.getOverrideSettings().addAll(converted);
            }

            to.setUpstreamNodes(new ArrayList<>());
            to.setDownstreamNodes(new ArrayList<>());
            tos.add(to);
        }

        return tos;
    }
}
