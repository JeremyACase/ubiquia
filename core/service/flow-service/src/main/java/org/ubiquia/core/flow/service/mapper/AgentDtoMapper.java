package org.ubiquia.core.flow.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Agent;
import org.ubiquia.common.model.ubiquia.dto.Config;
import org.ubiquia.common.model.ubiquia.embeddable.OverrideSettings;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;


@Service
public class AgentDtoMapper extends GenericDtoMapper<
    AgentEntity,
    Agent> {

    @Override
    public Agent map(final AgentEntity from) throws JsonProcessingException {

        Agent to = null;
        if (Objects.nonNull(from)) {
            to = new Agent();
            super.setAbstractEntityFields(from, to);

            if (Objects.nonNull(from.getConfig())) {
                var config = new Config();
                config.setConfigMountPath(from.getConfig().getConfigMountPath());
                var configMap = super.objectMapper.readValue(
                    from.getConfig().getConfigMap(),
                    Object.class);
                config.setConfigMap(configMap);
                to.setConfig(config);
            }

            to.setAgentName(from.getAgentName());
            to.setDescription(from.getDescription());
            to.setImage(from.getImage());
            to.setLivenessProbe(from.getLivenessProbe());
            to.setInitContainer(from.getInitContainer());
            to.setVolumes(from.getVolumes().stream().toList());
            to.setEnvironmentVariables(from.getEnvironmentVariables().stream().toList());
            to.setPort(from.getPort());
            to.setScaleSettings(from.getScaleSettings());
            to.setAgentType(from.getType());
            to.setCommunicationServiceSettings(from.getCommunicationServiceSettings());

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
