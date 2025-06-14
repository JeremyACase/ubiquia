package org.ubiquia.core.belief.state.generator.service.generator.relationship;

import static org.ubiquia.core.belief.state.generator.service.generator.relationship.JavaJpaRelationCodegen.RelationPair.Cardinality.ONE;
import static org.ubiquia.core.belief.state.generator.service.generator.relationship.JavaJpaRelationCodegen.RelationPair.extractRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.languages.JavaClientCodegen;
import org.openapitools.codegen.model.ModelsMap;

/**
 * Custom generator that decides JPA-relationship ownership
 * and injects vendor extensions every template can read.
 */
public class JavaJpaRelationCodegen extends JavaClientCodegen {

    /* ------------------------------------------------------------------
     * Tiny helper that gives the same key for (A,B) and (B,A)
     * ---------------------------------------------------------------- */
    private static String pairKey(String a, String b) {
        return (a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a);
    }

    @Override
    public String getName() {
        return "java-jpa-relation";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        additionalProperties.put("modelInheritanceSupport", true);
    }

    // ----------------------------------------------------------
    // 1. Hook that runs AFTER all models are parsed
    // ----------------------------------------------------------
    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {

        // 1. Index all models by classname
        Map<String, CodegenModel> modelIndex = new HashMap<>();
        for (ModelsMap modelsMap : objs.values()) {
            for (var mm : modelsMap.getModels()) {
                CodegenModel model = mm.getModel();
                modelIndex.put(model.classname, model);
            }
        }

        // 2. Patch parent from allOf and wire up parentModel for Mustache
        for (ModelsMap modelsMap : objs.values()) {
            for (var mm : modelsMap.getModels()) {
                CodegenModel model = mm.getModel();

                if ((model.parent == null || model.parent.isEmpty())
                    && model.allOf != null && !model.allOf.isEmpty()) {

                    for (Object item : model.allOf) {
                        String ref = extractRef(item); // helper shown below
                        if (ref != null) {
                            String parentName = ref.substring(ref.lastIndexOf('/') + 1);
                            if (!parentName.equals(model.classname)) {
                                model.parent = parentName;
                                CodegenModel parentModel = modelIndex.get(parentName);

                                if (parentModel != null) {
                                    model.parentModel = parentModel;  // ★ REQUIRED for Mustache
                                    model.imports.add(parentName);    // Optional, ensures import in Java
                                }

                                break; // stop after first match
                            }
                        }
                    }
                } else if (model.parent != null) {
                    // already set by OpenAPI Generator — wire up model object too
                    model.parentModel = modelIndex.get(model.parent);
                    model.imports.add(model.parent);
                }
            }
        }

        // 3. Identify which models are extended by others
        Map<String, Boolean> hasChildren = new HashMap<>();
        for (CodegenModel model : modelIndex.values()) {
            if (model.parent != null && !model.parent.isEmpty()) {
                hasChildren.put(model.parent, Boolean.TRUE);
            }
        }

        // 4. Stamp x-is-base-class on root or polymorphic parents only
        for (CodegenModel model : modelIndex.values()) {
            boolean isBase =
                (model.parent == null || model.parent.isEmpty()) &&
                    (hasChildren.containsKey(model.classname) || model.discriminator != null);

            if (isBase) {
                model.vendorExtensions.put("x-is-base-class", Boolean.TRUE);
            }
        }

        // 5. Relationship discovery (unchanged)
        Map<String, RelationPair> pairs = new HashMap<>();

        for (CodegenModel source : modelIndex.values()) {
            for (CodegenProperty prop : source.vars) {
                boolean isModelRef = prop.isModel
                    || (prop.isContainer && prop.items != null && prop.items.isModel);

                if (!isModelRef) continue;

                String targetName = prop.isContainer
                    ? prop.items.baseType
                    : prop.baseType;

                CodegenModel target = modelIndex.get(targetName);
                if (target == null || target.isEnum || target.isAlias || !target.getIsModel()) continue;

                RelationPair pair = pairs.computeIfAbsent(
                    pairKey(source.classname, target.classname),
                    k -> new RelationPair());

                pair.addSide(source, prop);
            }
        }

        // 6. Assign ownership for bidirectional relationships
        for (RelationPair p : pairs.values()) {
            if (!p.isBidirectional()) continue;
            switch (p.jointType()) {
                case ONE_TO_ONE, MANY_TO_MANY -> p.chooseOwnerLexicographically();
                default -> {}
            }
        }

        // 7. Add JPA annotations to properties via vendor extensions
        pairs.values().forEach(RelationPair::stampExtensions);

        // 8. Debug — optional
        modelIndex.values().forEach(m -> System.out.printf(
            "MODEL %-20s parent=%-15s base=%s%n", m.classname, m.parent, m.vendorExtensions.get("x-is-base-class")));

        // 9. Return all processed models
        return super.postProcessAllModels(objs);
    }


    private enum RelationType {
        ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
    }

    /* ******************************************************************
     * RelationPair  – holds the two “sides” of a relationship
     * **************************************************************** */
    public static final class RelationPair {
        Side left;      // we do not know owner yet
        Side right;

        void addSide(CodegenModel mdl, CodegenProperty prop) {
            if (left == null) {
                left = new Side(mdl, prop);
            } else if (right == null) {
                right = new Side(mdl, prop);
            }
        }

        boolean isBidirectional() {
            return left != null && right != null;
        }

        Cardinality cardOf(Side s) {
            return s.prop.isContainer ? Cardinality.MANY : ONE;
        }

        RelationType jointType() {
            if (left == null || right == null) {
                // Default type when we can't determine both sides
                return RelationType.ONE_TO_ONE; // or use a special `UNIDIRECTIONAL` enum if you prefer
            }

            Cardinality l = cardOf(left);
            Cardinality r = cardOf(right);

            if (l == ONE && r == ONE) return RelationType.ONE_TO_ONE;
            if (l == ONE && r == Cardinality.MANY) return RelationType.ONE_TO_MANY;
            if (l == Cardinality.MANY && r == ONE) return RelationType.MANY_TO_ONE;

            return RelationType.MANY_TO_MANY;
        }

        /**
         * For 1-1 or N-N choose owner by class-name order
         */
        void chooseOwnerLexicographically() {
            if (left.model.classname.compareTo(right.model.classname) < 0) {
                left.owningSide = true;
            } else {
                right.owningSide = true;
            }
        }

        void stampExtensions() {
            if (left  != null) stamp(left , right);   // always present
            if (right != null) stamp(right, left );   // only if right side exists
        }

        public static String extractRef(Object item) {
            if (item == null) return null;

            if (item instanceof String s && s.startsWith("#/")) return s;

            if (item instanceof Map<?,?> map) {
                Object val = map.get("$ref");
                if (val instanceof String str && str.startsWith("#/")) return str;
            }

            try {
                var method = item.getClass().getMethod("get$ref");
                Object val = method.invoke(item);
                if (val instanceof String str && str.startsWith("#/")) return str;
            } catch (Exception ignored) {}

            return null;
        }


        private void stamp(Side me, Side other) {
            Map<String, Object> v = me.prop.vendorExtensions;

            // Who owns the FK?
            v.put("x-is-owning-side", me.owningSide || other == null);
            if (other != null) {
                v.put("x-mapped-by", other.prop.name);
                v.put("x-related-classname", other.model.classname);
            }

            /* ---------- decide per-side annotation --------- */
            boolean meMany    = me.prop.isContainer;
            boolean otherMany = other != null && other.prop.isContainer;

            boolean isO2O = !meMany && !otherMany;
            boolean isM2M =  meMany &&  otherMany;
            boolean isO2M =  meMany && !otherMany;   // One-to-Many (this side owns the collection)
            boolean isM2O = !meMany &&  otherMany;   // Many-to-One (this side is singular)

            v.put("x-is-one-to-one",   isO2O);
            v.put("x-is-many-to-many", isM2M);
            v.put("x-is-one-to-many",  isO2M);
            v.put("x-is-many-to-one",  isM2O);

            // keep a generic tag if you wish
            if     (isO2O) v.put("x-relation-type", "ONE_TO_ONE");
            else if(isO2M) v.put("x-relation-type", "ONE_TO_MANY");
            else if(isM2O) v.put("x-relation-type", "MANY_TO_ONE");
            else           v.put("x-relation-type", "MANY_TO_MANY");
        }

        enum Cardinality { ONE, MANY }

        /* ---------------- Side ---------------- */
        private static final class Side {
            final CodegenModel model;
            final CodegenProperty prop;
            boolean owningSide = false;

            Side(CodegenModel m, CodegenProperty p) {
                model = m;
                prop = p;
            }
        }
    }
}
