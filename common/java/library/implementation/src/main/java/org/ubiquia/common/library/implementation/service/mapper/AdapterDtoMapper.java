package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Adapter;
import org.ubiquia.common.model.ubiquia.entity.AdapterEntity;


@Service
public class AdapterDtoMapper extends GenericDtoMapper<
    AdapterEntity,
    Adapter> {

    @Autowired
    private ComponentDtoMapper componentDtoMapper;

    @Autowired
    private OverrideSettingsMapper overrideSettingsMapper;

    @Override
    public Adapter map(final AdapterEntity from) throws JsonProcessingException {

        Adapter to = null;
        if (Objects.nonNull(from)) {
            to = new Adapter();
            super.setAbstractEntityFields(from, to);

            to.setName(from.getName());
            to.setAdapterSettings(from.getAdapterSettings());
            to.setAdapterType(from.getAdapterType());
            to.setFlowEvents(new ArrayList<>());
            to.setBrokerSettings(from.getBrokerSettings());
            to.setComponent(this.componentDtoMapper.map(from.getComponent()));
            to.setDownstreamAdapters(this.mapUpstreamOrDownstreamAdapters(
                from.getDownstreamAdapters()));
            to.setDescription(from.getDescription());
            to.setEgressSettings(from.getEgressSettings());
            to.setEndpoint(from.getEndpoint());
            to.setPollSettings(from.getPollSettings());
            to.setUpstreamAdapters(this.mapUpstreamOrDownstreamAdapters(
                from.getUpstreamAdapters()));
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
    private List<Adapter> mapUpstreamOrDownstreamAdapters(final List<AdapterEntity> froms)
        throws JsonProcessingException {
        var tos = new ArrayList<Adapter>();

        for (var from : froms) {
            var to = new Adapter();
            super.setAbstractEntityFields(from, to);
            to.setName(from.getName());
            to.setAdapterType(from.getAdapterType());
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

            to.setUpstreamAdapters(new ArrayList<>());
            to.setDownstreamAdapters(new ArrayList<>());
            tos.add(to);
        }

        return tos;
    }
}
