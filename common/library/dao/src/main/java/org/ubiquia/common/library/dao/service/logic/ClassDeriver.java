package org.ubiquia.common.library.dao.service.logic;

import static org.reflections.scanners.Scanners.SubTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dao.QueryFilter;

/**
 * A service that will derive a class towards building predicates.
 */
@Service
public class ClassDeriver {

    private static final Logger logger = LoggerFactory.getLogger(ClassDeriver.class);
    private final Reflections reflections;

    public ClassDeriver() {
        this.reflections = new Reflections("org.ubiquia");
    }

    /**
     * Attempt to build our predicate class from a base class.
     *
     * @param clazz       The base class.
     * @param queryFilter The filter provided by the client.
     * @return Either the base class or a subclass.
     * @throws NoSuchFieldException Exception from invalid fields.
     */
    public Class<?> tryGetPredicateClass(Class<?> clazz, QueryFilter queryFilter)
        throws NoSuchFieldException {

        Class predicateClass = clazz;

        var candidateClasses = new ArrayList<Class>();
        var fields = FieldUtils.getAllFieldsList(clazz);

        for (var param : queryFilter.getParameters()) {
            var split = Arrays.asList(param.getKey().split("\\."));
            var fieldName = split.get(0);

            var match = fields.stream().filter(x ->
                x.getName().equalsIgnoreCase(fieldName)).findFirst();
            if (match.isEmpty()) {
                logger.debug("Could not match field name {} of class {} to any field;"
                        + " checking subclasses...",
                    fieldName,
                    clazz.getSimpleName());

                var subclasses = this.reflections.get(SubTypes.of(clazz).asClass());
                for (var sub : subclasses) {
                    fields = FieldUtils.getAllFieldsList(sub);
                    var subField = fields.stream().filter(x -> x.getName().equalsIgnoreCase(fieldName))
                        .findFirst();
                    if (subField.isPresent()) {
                        candidateClasses.add(sub);
                    }
                }

                if (candidateClasses.size() > 1) {
                    throw new IllegalArgumentException("ERROR: Found multiple subclasses of "
                        + " parent class "
                        + clazz.getSimpleName()
                        + " with field named "
                        + fieldName
                        + "; no way of knowing which class was intended."
                        + " Candidate classes: "
                        + candidateClasses
                    );
                } else if (candidateClasses.size() == 1) {
                    logger.debug("Found field name {} in subclass {} of parent class {}; "
                            + "assuming this class as predicate...",
                        fieldName,
                        clazz,
                        candidateClasses.get(0).getSimpleName());
                    predicateClass = candidateClasses.get(0);
                } else {
                    throw new NoSuchFieldException("ERROR: Could not match key word "
                        + param.getKey()
                        + " to any field in "
                        + clazz.getSimpleName()
                        + " or sub classes.");
                }
            }
        }

        return predicateClass;
    }

    /**
     * Attempt to build our predicate class from a base class.
     *
     * @param clazz     The base class.
     * @param keychains The keychains provided by the client.
     * @return Either the base class or a subclass.
     * @throws NoSuchFieldException Exception from invalid fields.
     */
    public Class<?> tryGetPredicateClass(Class<?> clazz, List<String> keychains)
        throws NoSuchFieldException {
        Class predicateClass = clazz;

        var candidateClasses = new ArrayList<Class>();

        var fields = FieldUtils.getAllFieldsList(clazz);

        for (var param : keychains) {
            var split = Arrays.asList(param.split("\\."));
            var fieldName = this.getStringWithoutOperatorSymbols(split.get(0));

            var match = fields.stream().filter(x ->
                x.getName().equalsIgnoreCase(fieldName)).findFirst();
            if (match.isEmpty()) {
                logger.debug("Could not match field name {} of class {} to any field;"
                        + " checking subclasses...",
                    fieldName,
                    clazz.getSimpleName());

                var subclasses = this.reflections.get(SubTypes.of(clazz).asClass());
                for (var sub : subclasses) {
                    fields = FieldUtils.getAllFieldsList(sub);
                    var subField = fields.stream().filter(x ->
                            x.getName().equalsIgnoreCase(fieldName))
                        .findFirst();
                    if (subField.isPresent()) {
                        candidateClasses.add(sub);
                    }
                }

                if (candidateClasses.size() > 1) {
                    throw new IllegalArgumentException("ERROR: Found multiple subclasses of "
                        + " parent class "
                        + clazz.getSimpleName()
                        + " with field named "
                        + fieldName
                        + "; no way of knowing which class was intended."
                        + " Candidate classes: "
                        + candidateClasses
                    );
                } else if (candidateClasses.size() == 1) {
                    logger.debug("Found field name {} in subclass {} of parent class {}; assuming this class"
                            + "as predicate...",
                        fieldName,
                        clazz,
                        candidateClasses.get(0).getSimpleName());
                    predicateClass = candidateClasses.get(0);
                } else {
                    throw new NoSuchFieldException("ERROR: Could not match key word "
                        + param
                        + " to any field in "
                        + clazz.getSimpleName()
                        + " or sub classes.");
                }
            }
        }

        return predicateClass;
    }

    /**
     * Attempt to build our predicate class from a base class.
     *
     * @param clazz    The base class.
     * @param keychain The keychain provided by the client.
     * @return Either the base class or a subclass.
     * @throws NoSuchFieldException Exception from invalid fields.
     */
    public Class<?> tryGetPredicateClass(Class<?> clazz, String keychain)
        throws NoSuchFieldException {
        Class predicateClass = clazz;

        var candidateClasses = new ArrayList<Class>();

        var fields = FieldUtils.getAllFieldsList(clazz);

        var split = Arrays.asList(keychain.split("\\."));
        var fieldName = this.getStringWithoutOperatorSymbols(split.get(0));

        var match = fields.stream().filter(x ->
            x.getName().equalsIgnoreCase(fieldName)).findFirst();
        if (match.isEmpty()) {
            logger.debug("Could not match field name {} of class {} to any field;"
                    + " checking subclasses...",
                fieldName,
                clazz.getSimpleName());

            var subclasses = this.reflections.get(SubTypes.of(clazz).asClass());
            for (var sub : subclasses) {
                fields = FieldUtils.getAllFieldsList(sub);
                var subField = fields.stream().filter(x ->
                        x.getName().equalsIgnoreCase(fieldName))
                    .findFirst();
                if (subField.isPresent()) {
                    candidateClasses.add(sub);
                }
            }

            if (candidateClasses.size() > 1) {
                throw new IllegalArgumentException("ERROR: Found multiple subclasses of "
                    + " parent class "
                    + clazz.getSimpleName()
                    + " with field named "
                    + fieldName
                    + "; no way of knowing which class was intended."
                    + " Candidate classes: "
                    + candidateClasses
                );
            } else if (candidateClasses.size() == 1) {
                logger.debug("Found field name {} in subclass {} of parent class {}; assuming this class"
                        + "as predicate...",
                    fieldName,
                    clazz,
                    candidateClasses.get(0).getSimpleName());
                predicateClass = candidateClasses.get(0);
            } else {
                throw new NoSuchFieldException("ERROR: Could not match key word "
                    + keychain
                    + " to any field in "
                    + clazz.getSimpleName()
                    + " or sub classes.");
            }
        }

        return predicateClass;
    }

    /**
     * Given a string, build and return the key without any operator symbols.
     *
     * @param key The key we're replacing strings from.
     * @return A string without the symbols.
     */
    private String getStringWithoutOperatorSymbols(final String key) {
        var keyWord = key.replaceAll("[<>*]", "");
        return keyWord;
    }
}
