package org.ubiquia.core.belief.state.generator.service.generator.acl;

import static org.ubiquia.core.belief.state.generator.service.generator.acl.UbiquiaAclEntityGenerator.RelationPair.Cardinality.ONE;

import java.util.HashMap;
import java.util.Map;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.languages.JavaClientCodegen;
import org.openapitools.codegen.model.ModelsMap;

/**
 * Post-processor that figures out JPA relationship metadata and
 * stores it in vendor-extensions every template can use.
 */
public class UbiquiaAclEntityGenerator extends JavaClientCodegen {

    /* ------------------------------------------------------------
     * helper that gives the same key for (A,B) and (B,A)
     * ---------------------------------------------------------- */
    private static String pairKey(String a, String b) {
        return (a.compareTo(b) < 0 ? a + '_' + b : b + '_' + a);
    }

    private final Map<String, CodegenModel> modelIndex = new HashMap<>();

    @Override
    public String getName() {
        return "ubiquia-acl-entity-generator";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        this.setEnablePostProcessFile(true);

        modelTemplateFiles.put("modelRepository.mustache", "Repository.java");
        modelTemplateFiles.put("modelRelationshipBuilder.mustache", "RelationshipBuilder.java");
    }

    /* ------------------------------------------------------------
     * after all models are parsed, enrich fields with JPA hints
     * ---------------------------------------------------------- */
    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {

        /* 1️⃣ index every model by class-name for fast look-ups */
        modelIndex.clear(); // ← ensures no stale state
        for (ModelsMap mm : objs.values()) {
            mm.getModels().forEach(m -> modelIndex.put(m.getModel().classname, m.getModel()));
        }

        /* 2️⃣ collect potential relationship pairs */
        Map<String, RelationPair> pairs = new HashMap<>();

        for (CodegenModel source : modelIndex.values()) {
            for (CodegenProperty prop : source.vars) {

                boolean isModelRef = prop.isModel ||
                    (prop.isContainer && prop.items != null && prop.items.isModel);
                if (!isModelRef) {
                    continue;
                }

                String targetName = prop.isContainer ? prop.items.baseType : prop.baseType;
                CodegenModel target = modelIndex.get(targetName);
                if (target == null || target.isEnum || target.isAlias || !target.getIsModel()) {
                    continue;
                }

                RelationPair pair = pairs.computeIfAbsent(
                    pairKey(source.classname, target.classname), k -> new RelationPair());
                pair.addSide(source, prop);
            }
        }

        /* 3️⃣ pick owning side when both directions are present */
        for (RelationPair p : pairs.values()) {
            if (!p.isBidirectional()) {
                continue;
            }

            switch (p.jointType()) {
                case ONE_TO_ONE, MANY_TO_MANY -> p.chooseOwnerLexicographically();
                default -> { /* nothing – JPA rules handle 1-N / N-1 */ }
            }
        }

        /* 4️⃣ write vendor-extensions on every property */
        pairs.values().forEach(RelationPair::stampExtensions);

        /* optional debug
        modelIndex.values().forEach(m ->
            System.out.printf("MODEL %-20s vars=%d%n", m.classname, m.vars.size())
        );
        */

        return super.postProcessAllModels(objs);
    }

    @Override
    public void postProcessFile(java.io.File file, String fileType) {

        if ("model".equals(fileType)) {
            String filename = file.getName();

            // Check for Repository.java or ModelRelationshipBuilder.java
            if (filename.endsWith("Repository.java") || filename.endsWith("RelationshipBuilder.java")) {
                String modelName = filename
                    .replace("Repository.java", "")
                    .replace("RelationshipBuilder.java", "");

                CodegenModel model = modelIndex.get(modelName);
                if (model != null && model.isEnum) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.printf("Deleted enum-generated file: %s%n", file.getAbsolutePath());
                    } else {
                        System.out.printf("Failed to delete file: %s%n", file.getAbsolutePath());
                    }
                }
            }
        }

        super.postProcessFile(file, fileType);
    }

    /* ============================================================ */

    private enum RelationType { ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY }

    /**
     * Holds the two “sides” of a relationship
     */
    public static final class RelationPair {
        Side left;
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
                return RelationType.ONE_TO_ONE; // uni-dir
            }
            Cardinality l = cardOf(left), r = cardOf(right);
            if (l == ONE && r == ONE) {
                return RelationType.ONE_TO_ONE;
            }
            if (l == ONE && r == Cardinality.MANY) {
                return RelationType.ONE_TO_MANY;
            }
            if (l == Cardinality.MANY && r == ONE) {
                return RelationType.MANY_TO_ONE;
            }
            return RelationType.MANY_TO_MANY;
        }

        /**
         * For 1-1 and N-N pick the owner deterministically
         */
        void chooseOwnerLexicographically() {
            if (left.model.classname.compareTo(right.model.classname) < 0) {
                left.owningSide = true;
            } else {
                right.owningSide = true;
            }
        }

        void stampExtensions() {
            if (left != null) {
                stamp(left, right);
            }
            if (right != null) {
                stamp(right, left);
            }
        }

        private void stamp(Side me, Side other) {
            Map<String, Object> v = me.prop.vendorExtensions;

            /* ownership + helper data */
            v.put("x-is-owning-side", me.owningSide || other == null);
            if (other != null) {
                v.put("x-mapped-by", other.prop.name);
                v.put("x-related-classname", other.model.classname);
            }

            /* cardinalities */
            boolean meMany = me.prop.isContainer;
            boolean otherMany = other != null && other.prop.isContainer;

            boolean o2o = !meMany && !otherMany;
            boolean m2m = meMany && otherMany;
            boolean o2m = meMany && !otherMany;  // this side owns collection
            boolean m2o = !meMany && otherMany;  // this side is single fk

            v.put("x-is-one-to-one", o2o);
            v.put("x-is-many-to-many", m2m);
            v.put("x-is-one-to-many", o2m);
            v.put("x-is-many-to-one", m2o);
            v.put("x-relation-type",
                o2o ? "ONE_TO_ONE" :
                    o2m ? "ONE_TO_MANY" :
                        m2o ? "MANY_TO_ONE" :
                            "MANY_TO_MANY");
        }

        enum Cardinality { ONE, MANY }

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
