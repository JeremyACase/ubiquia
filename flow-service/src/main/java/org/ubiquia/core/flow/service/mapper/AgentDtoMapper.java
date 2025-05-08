package org.ubiquia.core.flow.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.AgentDto;
import org.ubiquia.core.flow.model.dto.ConfigDTO;
import org.ubiquia.core.flow.model.embeddable.OverrideSettings;
import org.ubiquia.core.flow.model.entity.Agent;


@Service
public class AgentDtoMapper extends GenericDtoMapper<
    Agent,
    AgentDto> {

    @Override
    public AgentDto map(final Agent from) throws JsonProcessingException {

        AgentDto to = null;
        if (Objects.nonNull(from)) {
            to = new AgentDto();
            super.setAEntityFields(from, to);

            if (Objects.nonNull(from.getConfig())) {
                var config = new ConfigDTO();
                config.setConfigMountPath(from.getConfig().getConfigMountPath());
                var configMap = super.objectMapper.readValue(
                    from.getConfig().getConfigMap(),
                    Object.class);
                config.setConfigMap(configMap);
                to.setConfig(config);
            }

            to.setAgentName(from.getDataTransformName());
            to.setDescription(from.getDescription());
            to.setImage(from.getImage());
            to.setLivenessProbe(from.getLivenessProbe());
            to.setInitContainer(from.getInitContainer());
            to.setVolumes(from.getVolumes().stream().toList());
            to.setEnvironmentVariables(from.getEnvironmentVariables().stream().toList());
            to.setPort(from.getPort());
            to.setScaleSettings(from.getScaleSettings());
            to.setDataTransformType(from.getType());

            if (Objects.nonNull(from.getOverrideSettings())) {
                var overrideSettings = new ArrayList<OverrideSettings>();
                for (var fromSetting : from.getOverrideSettings()) {
                    var toSetting = new OverrideSettings();
                    toSetting.setFlag(fromSetting.getFlag());
                    toSetting.setValue(this.objectMapper.readValue(
                        fromSetting.getValue(),
                        Object.class));
                    toSetting.setKey(fromSetting.getKey());
                    overrideSettings.add(toSetting);
                }
                to.setOverrideSettings(overrideSettings);
            }
        }
        return to;
    }
}
