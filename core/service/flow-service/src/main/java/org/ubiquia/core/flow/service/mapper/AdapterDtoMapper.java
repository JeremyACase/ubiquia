package org.ubiquia.core.flow.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.AdapterDto;
import org.ubiquia.core.flow.model.entity.Adapter;


@Service
public class AdapterDtoMapper extends GenericDtoMapper<
    Adapter,
    AdapterDto> {

    @Autowired
    private AgentDtoMapper agentDtoMapper;

    @Autowired
    private OverrideSettingsMapper overrideSettingsMapper;

    @Override
    public AdapterDto map(final Adapter from) throws JsonProcessingException {

        AdapterDto to = null;
        if (Objects.nonNull(from)) {
            to = new AdapterDto();
            super.setAbstractEntityFields(from, to);

            to.setAdapterName(from.getAdapterName());
            to.setAdapterSettings(from.getAdapterSettings());
            to.setAdapterType(from.getAdapterType());
            to.setFlowEvents(new ArrayList<>());
            to.setBrokerSettings(from.getBrokerSettings());
            to.setAgent(this.agentDtoMapper.map(from.getAgent()));
            to.setDownstreamAdapters(this.mapUpstreamOrDownstreamAdapters(
                from.getDownstreamAdapters()));
            to.setDescription(from.getDescription());
            to.setEgressSettings(from.getEgressSettings());
            to.setEndpoint(from.getEndpoint());
            to.setPollSettings(from.getPollSettings());
            to.setUpstreamAdapters(this.mapUpstreamOrDownstreamAdapters(
                from.getUpstreamAdapters()));

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
    private List<AdapterDto> mapUpstreamOrDownstreamAdapters(final List<Adapter> froms)
        throws JsonProcessingException {
        var tos = new ArrayList<AdapterDto>();

        for (var from : froms) {
            var to = new AdapterDto();
            super.setAbstractEntityFields(from, to);
            to.setAdapterName(from.getAdapterName());
            to.setAdapterType(from.getAdapterType());
            to.setAgent(this.agentDtoMapper.map(from.getAgent()));
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
