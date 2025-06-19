package org.ubiquia.common.library.belief.state.libraries.service.logic;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AclControllerLogic {

    private static final Logger logger = LoggerFactory.getLogger(AclControllerLogic.class);

    private final Pattern camelcaseRegex;

    /**
     * Abandon all hope, ye who enter here.
     */
    public AclControllerLogic() {
        logger.debug("Initializing...");
        this.camelcaseRegex = Pattern.compile("-([a-z])");
        logger.debug("...initialized...");
    }

    /**
     * Helper method to parse a servlet request into predicates.
     *
     * @param request The servlet request to parse.
     * @return A map of parameters to build predicates from.
     */
    public HashMap<String, String[]> getParameterMapFrom(final HttpServletRequest request) {
        var map = new HashMap<String, String[]>();
        for (var key : request.getParameterMap().keySet()) {
            var camelCase = this.camelcaseRegex.matcher(key)
                .replaceAll(x -> x.group(1).toUpperCase());
            map.put(camelCase, request.getParameterMap().get(key));
        }
        this.removePageKeysHelper(map);
        return map;
    }

    /**
     * Remove any superfluous keys from our map of parameters to query for.
     *
     * @param map The map to remove keys from.
     */
    private void removePageKeysHelper(HashMap<String, String[]> map) {
        map.remove("page");
        map.remove("size");
        map.remove("sortDescending");
        map.remove("sortByFields");
        map.remove("multiselectFields");

        logger.debug("Map now contains the following: {}", map.keySet());
    }
}