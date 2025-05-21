package org.ubiquia.core.flow.service.decorator.override;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.interfaces.InterfaceLogger;
import org.ubiquia.core.flow.model.embeddable.GraphDeployment;
import org.ubiquia.core.flow.model.embeddable.OverrideSettingsStringified;

@Service
public abstract class GenericOverrideDecorator<T> implements InterfaceLogger {

    protected HashMap<String, Field> cachedFields;

    protected HashMap<String, Class> cachedFieldClasses;

    @Autowired
    protected ObjectMapper objectMapper;

    protected Class<T> persistedClass;

    /**
     * Ye Old Constructor.
     */
    @SuppressWarnings("unchecked")
    public GenericOverrideDecorator() {

        this.getLogger().info("Initializing...");

        // Cache our persistent class in derived classes.
        this.persistedClass = (Class<T>) ((ParameterizedType) this.getClass()
            .getGenericSuperclass()).getActualTypeArguments()[0];

        this.cachedFields = new HashMap<>();
        this.cachedFieldClasses = new HashMap<>();

        this.tryCacheFields();

        this.getLogger().info("...finished initializing.");
    }

    /**
     * Override a model's value provided the graph deployment settings.
     *
     * @param model            The model whose baseline values we're overriding.
     * @param overrideSettings The list of settings we can use to override from.
     * @param graphDeployment  The graph and its settings we're deploying.
     * @throws JsonProcessingException Exception from processing JSON.
     * @throws IllegalAccessException  Reflection exceptions.
     */
    @Transactional
    public void tryOverrideBaselineValues(
        T model,
        final List<OverrideSettingsStringified> overrideSettings,
        final GraphDeployment graphDeployment)
        throws JsonProcessingException,
        IllegalAccessException {

        this.getLogger().info("Attempting to override baseline values with settings: {}...",
            this.objectMapper.writeValueAsString(graphDeployment));

        if (Objects.nonNull(graphDeployment.getGraphSettings())
            && Objects.nonNull(graphDeployment.getGraphSettings().getFlag())) {

            this.getLogger().info("...found a deployment flag: {}; processing...",
                graphDeployment.getGraphSettings().getFlag());

            var matchingFlagSettings = overrideSettings
                .stream()
                .filter(x -> x
                    .getFlag()
                    .equals(graphDeployment.getGraphSettings().getFlag()))
                .toList();

            for (var match : matchingFlagSettings) {
                this.getLogger().info("...overriding value for key {}...", match.getKey());

                if (this.cachedFields.containsKey(match.getKey())) {
                    this.setOverrideValueForField(model, match);
                } else {
                    throw new IllegalArgumentException("ERROR: Cannot find a value to override for "
                        + "key: "
                        + match.getKey());
                }
            }
        }

        this.getLogger().info("...completed.");

    }

    /**
     * A helper method to set the value for a model's field.
     *
     * @param model The model we're overriding a baseline value for.
     * @param match The matching override.
     * @throws JsonProcessingException Exception from parsing values.
     * @throws IllegalAccessException  Reflection exceptions.
     */
    private void setOverrideValueForField(
        T model,
        final OverrideSettingsStringified match)
        throws JsonProcessingException,
        IllegalAccessException {

        this.getLogger().info("...found cached field for key: {}; overriding "
            + "baseline...", match.getKey());
        var field = this.cachedFields.get(match.getKey());
        var clazz = this.cachedFieldClasses.get(match.getKey());

        this.getLogger().debug("...converting value: {}", match.getValue());
        var converted = this.objectMapper.readValue(match.getValue(), clazz);
        field.set(model, converted);
        this.getLogger().info("...setting value to: {}",
            this.objectMapper.writeValueAsString(converted));
    }

    /**
     * Attempt to cache the fields for our classes for future use.
     */
    private void tryCacheFields() {
        var fields = FieldUtils.getAllFields(this.persistedClass);
        for (var field : fields) {
            this.getLogger().debug("...caching data for field: {}...", field.getName());
            this.cachedFields.put(field.getName(), field);
            if (List.class.isAssignableFrom(field.getType())) {
                this.getLogger().debug("...field is a list class; skipping...");
            } else {
                field.setAccessible(true);
                this.cachedFieldClasses.put(field.getName(), field.getType());
            }
            this.getLogger().debug("...completed caching data...");
        }
    }
}
