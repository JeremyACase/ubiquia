package org.ubiquia.core.flow.service.decorator.override;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.common.model.ubiquia.dto.AgentDto;
import org.ubiquia.common.model.ubiquia.dto.ConfigDto;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.embeddable.GraphSettings;
import org.ubiquia.common.model.ubiquia.embeddable.OverrideSettingsStringified;
import org.ubiquia.core.flow.TestHelper;


@SpringBootTest
@AutoConfigureMockMvc
public class AgentOverrideDecoratorTest {

    @Autowired
    private AgentOverrideDecorator agentOverrideDecorator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.clearAllState();
    }

    @Test
    public void testOverridesBaselineValues_isValid()
        throws JsonProcessingException,
        IllegalAccessException {

        var baselineConfigmap = "application.yml: |\n"
            + "        config_0:\n"
            + "          value: true\n"
            + "        config_1:\n"
            + "          value: true";
        var baselineConfig = new ConfigDto();
        baselineConfig.setConfigMountPath("test");
        baselineConfig.setConfigMap(baselineConfigmap);

        var overriddenConfigmap = "application.yml: |\n"
            + "        config_0:\n"
            + "          value: false\n"
            + "        config_1:\n"
            + "          value: false";
        var overrideConfig = new ConfigDto();
        overrideConfig.setConfigMountPath("test");
        overrideConfig.setConfigMap(overriddenConfigmap);

        var overrideSettings = new OverrideSettingsStringified();
        overrideSettings.setFlag("test");
        overrideSettings.setKey("config");
        overrideSettings.setValue(this.objectMapper.writeValueAsString(overrideConfig));

        var overrideSettingsList = new ArrayList<OverrideSettingsStringified>();
        overrideSettingsList.add(overrideSettings);

        var deployment = new GraphDeployment();
        deployment.setGraphSettings(new GraphSettings());
        deployment.getGraphSettings().setFlag("test");

        var agent = new AgentDto();
        agent.setConfig(baselineConfig);
        this.agentOverrideDecorator.tryOverrideBaselineValues(
            agent,
            overrideSettingsList,
            deployment);

        Assertions.assertEquals(
            overrideConfig.getConfigMap(),
            agent.getConfig().getConfigMap());
    }
}