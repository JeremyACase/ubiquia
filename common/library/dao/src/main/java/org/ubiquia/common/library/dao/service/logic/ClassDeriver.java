package org.ubiquia.common.library.dao.service.logic;

import static org.reflections.scanners.Scanners.SubTypes;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dao.QueryFilter;
import org.ubiquia.common.model.ubiquia.dao.QueryFilterParameter;

/**
 * Service to derive the most specific class matching a given query filter or keychain.
 */
@Service
public class ClassDeriver {

    private static final Logger logger = LoggerFactory.getLogger(ClassDeriver.class);
    private final Reflections reflections = new Reflections("org.ubiquia");

    /**
     * Derive the most specific class for the given QueryFilter.
     *
     * @param clazz       the root class
     * @param queryFilter the query filter containing parameters with keychains
     * @return the derived class
     * @throws NoSuchFieldException if no matching field is found
     */
    public Class<?> tryGetPredicateClass(Class<?> clazz, QueryFilter queryFilter)
        throws NoSuchFieldException {
        var keychains = queryFilter.getParameters().stream()
            .map(QueryFilterParameter::getKey)
            .collect(Collectors.toList());
        return tryGetPredicateClass(clazz, keychains);
    }

    /**
     * Derive the most specific class for a list of keychains.
     *
     * @param clazz     the root class
     * @param keychains the list of keychains
     * @return the derived class
     * @throws NoSuchFieldException if no matching field is found
     */
    public Class<?> tryGetPredicateClass(Class<?> clazz, List<String> keychains)
        throws NoSuchFieldException {

        var currentClass = clazz;
        for (String keychain : keychains) {
            currentClass = findBestMatchingClass(currentClass, keychain);
        }
        return currentClass;
    }

    /**
     * Derive the most specific class for a single keychain.
     *
     * @param clazz    the root class
     * @param keychain the keychain
     * @return the derived class
     * @throws NoSuchFieldException if no matching field is found
     */
    public Class<?> tryGetPredicateClass(Class<?> clazz, String keychain)
        throws NoSuchFieldException {
        return this.findBestMatchingClass(clazz, keychain);
    }

    /**
     * Given a keychain, find the class (root or subclass) containing the first segment.
     */
    private Class<?> findBestMatchingClass(Class<?> clazz, String keychain)
        throws NoSuchFieldException {

        var firstSegment = extractFirstSegment(keychain);

        if (hasFieldIgnoreCase(clazz, firstSegment)) {
            return clazz;
        }

        return findSubclassWithFieldOrThrow(clazz, firstSegment, keychain);
    }

    /**
     * Extract the first segment of a keychain and remove operator symbols.
     */
    private String extractFirstSegment(String keychain) {
        var firstSegment = keychain.split("\\.")[0];
        return removeOperatorSymbols(firstSegment);
    }

    /**
     * Remove operator symbols from a key (e.g., "<", ">", "*").
     */
    private String removeOperatorSymbols(String key) {
        return key.replaceAll("[<>*]", "");
    }

    /**
     * Check if a field exists in the class (case-insensitive).
     */
    private boolean hasFieldIgnoreCase(Class<?> clazz, String fieldName) {
        return FieldUtils.getAllFieldsList(clazz).stream()
            .anyMatch(field -> field.getName().equalsIgnoreCase(fieldName));
    }

    /**
     * Search for a subclass of the given class that contains the specified field.
     * Throws an exception if multiple or no subclasses match.
     */
    private Class<?> findSubclassWithFieldOrThrow(Class<?> clazz, String fieldName, String keychain)
        throws NoSuchFieldException {

        logger.debug("Could not match field '{}' in class '{}'. Checking subclasses...",
            fieldName, clazz.getSimpleName());

        var subclasses = reflections.get(SubTypes.of(clazz).asClass());

        var matchingSubclasses = subclasses.stream()
            .filter(sub -> hasFieldIgnoreCase(sub, fieldName))
            .toList();

        if (matchingSubclasses.isEmpty()) {
            throw new NoSuchFieldException("No field matching keychain '"
                + keychain + "' in " + clazz.getSimpleName() + " or its subclasses.");
        }

        if (matchingSubclasses.size() > 1) {
            throw new IllegalArgumentException("Multiple subclasses of "
                + clazz.getSimpleName()
                + " found with field '" + fieldName + "'. Candidates: "
                + matchingSubclasses);
        }

        var subclass = matchingSubclasses.get(0);
        logger.debug("Field '{}' found in subclass '{}'; using subclass as predicate.",
            fieldName, subclass.getSimpleName());
        return subclass;
    }
}
