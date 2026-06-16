package org.ubiquia.core.belief.state.generator.service.generator.domain;

import static org.ubiquia.core.belief.state.generator.service.generator.domain.UbiquiaDomainEntityGenerator.RelationPair.Cardinality.ONE;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.languages.JavaClientCodegen;
import org.openapitools.codegen.model.ModelsMap;

/**
 * Post-processor that figures out JPA relationship metadata and
 * stores it in vendor-extensions every template can use.
 */
public class UbiquiaDomainEntityGenerator extends JavaClientCodegen {

    private final Map<String, CodegenModel> modelIndex = new HashMap<>();

    @Override
    public String getName() {
        return "ubiquia-domain-entity-generator";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        setEnablePostProcessFile(true);
        supportingFiles.clear();

        modelTemplateFiles.put("modelRepository.mustache", "Repository.java");
        modelTemplateFiles.put("modelRelationshipBuilder.mustache", "RelationshipBuilder.java");
    }

    // H2 reserved words that are unsafe as bare column names
    private static final Set<String> SQL_RESERVED_WORDS = Set.of(
        "key", "value", "index", "order", "group", "select", "from",
        "where", "table", "column", "type", "name", "level", "position",
        "row", "rows", "set", "system", "user", "current", "check"
    );

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        indexModels(objs);
        attachBaseNames();

        var pairs = collectRelationPairs();
        chooseOwners(pairs);
        pairs.values().forEach(RelationPair::stampExtensions);

        attachPerPropertyExtensions();
        attachColumnAnnotationsToEmbeddables();

        return super.postProcessAllModels(objs);
    }

    @Override
    public void postProcessFile(java.io.File file, String fileType) {
        if (!"model".equals(fileType)) {
            super.postProcessFile(file, fileType);
            return;
        }

        var filename = file.getName();
        if (!isSupportFile(filename)) {
            super.postProcessFile(file, fileType);
            return;
        }

        var modelName = stripSupportSuffix(filename);
        var model = modelIndex.get(modelName);
        if (Objects.isNull(model)) {
            super.postProcessFile(file, fileType);
            return;
        }

        if (model.isEnum || isEmbeddable(model)) {
            var deleted = file.delete();
            System.out.printf("%s %s (%s)%n",
                deleted ? "Deleted" : "Failed to delete",
                file.getAbsolutePath(),
                model.isEnum ? "enum" : "embeddable");
        }

        super.postProcessFile(file, fileType);
    }

    /* ------------------------------------------------------------ */
    /* Helper methods                                               */
    /* ------------------------------------------------------------ */

    private String pairKey(String a, String b) {
        return (a.compareTo(b) < 0 ? a + '_' + b : b + '_' + a);
    }

    private boolean isEmbeddable(CodegenModel m) {
        return Boolean.TRUE.equals(m.vendorExtensions.get("x-embeddable"));
    }

    private boolean isSkippable(CodegenModel m) {
        return Objects.isNull(m) || m.isEnum || m.isAlias || !m.getIsModel() || isEmbeddable(m);
    }

    private boolean isModelRef(CodegenProperty p) {
        return p.isModel || (p.isContainer && Objects.nonNull(p.items) && p.items.isModel);
    }

    private CodegenModel getTargetModel(CodegenProperty p) {
        var targetName = p.isContainer ? p.items.baseType : p.baseType;
        return modelIndex.get(targetName);
    }

    private void setBaseClassname(CodegenModel model) {
        var base = model.classname.endsWith("Entity")
            ? model.classname.replaceFirst("Entity$", "")
            : model.classname;
        model.vendorExtensions.put("x-base-classname", base);
    }

    private void indexModels(Map<String, ModelsMap> objs) {
        modelIndex.clear();
        for (var mm : objs.values()) {
            for (var m : mm.getModels()) {
                var cg = m.getModel();
                modelIndex.put(cg.classname, cg);
            }
        }
    }

    private void attachBaseNames() {
        for (var model : modelIndex.values()) {
            setBaseClassname(model);
        }
    }

    private Map<String, RelationPair> collectRelationPairs() {
        var pairs = new HashMap<String, RelationPair>();

        for (var source : modelIndex.values()) {
            if (isSkippable(source)) {
                continue;
            }

            for (var prop : source.vars) {
                if (!isModelRef(prop)) {
                    continue;
                }

                var target = getTargetModel(prop);
                if (isSkippable(target)) {
                    continue;
                }

                var key = pairKey(source.classname, target.classname);
                var pair = pairs.computeIfAbsent(key, k -> new RelationPair());
                pair.addSide(source, prop);
            }
        }
        return pairs;
    }

    private void chooseOwners(Map<String, RelationPair> pairs) {
        for (var p : pairs.values()) {
            if (!p.isBidirectional()) {
                continue;
            }
            switch (p.jointType()) {
                case ONE_TO_ONE, MANY_TO_MANY -> p.chooseOwnerLexicographically();
                default -> { /* 1-N / N-1 handled implicitly */ }
            }
        }
    }

    private boolean isSupportFile(String filename) {
        return filename.endsWith("Repository.java")
            || filename.endsWith("RelationshipBuilder.java");
    }

    private String stripSupportSuffix(String filename) {
        return filename.replace("Repository.java", "").replace("RelationshipBuilder.java", "");
    }

    /* ============================================================ */
    /* Column annotations for embeddable fields                    */
    /* ============================================================ */

    private void attachColumnAnnotationsToEmbeddables() {
        for (var model : modelIndex.values()) {
            if (!isEmbeddable(model)) {
                continue;
            }
            for (var p : model.vars) {
                if (SQL_RESERVED_WORDS.contains(p.name.toLowerCase())) {
                    p.vendorExtensions.put(
                        "x-field-extra-annotation",
                        "@Column(name = \"pair_" + p.name + "\")");
                }
            }
        }
    }

    /* ============================================================ */
    /* NEW: Per-property vendor extensions                          */
    /* ============================================================ */

    private void attachPerPropertyExtensions() {
        for (var model : modelIndex.values()) {
            if (isSkippable(model)) {
                continue;
            }

            boolean hasElementCollections = false; // class-level flag for conditional imports

            final String ownerLower = toOwnerLower(model.classname);

            for (var p : model.vars) {
                final var v = p.vendorExtensions;

                // Always provide stable names so templates never rely on outer scope
                v.put("x-owner-classname-lower", ownerLower);
                v.put("x-field-name", p.name);
                v.put("x-join-column", ownerLower + "_id");
                v.put("x-collection-table-name", ownerLower + "_" + p.name);

                // ElementCollection: only for collections whose element type is an embeddable
                if (p.isContainer && Objects.nonNull(p.items) && p.items.isModel) {
                    var elem = modelIndex.get(p.items.baseType);
                    if (Objects.nonNull(elem) && isEmbeddable(elem)) {
                        v.put("x-element-collection", Boolean.TRUE);
                        hasElementCollections = true;
                    }
                }
            }

            if (hasElementCollections) {
                model.vendorExtensions.put("hasElementCollections", Boolean.TRUE);
            }
        }
    }

    private String toOwnerLower(String classname) {
        if (Objects.isNull(classname) || classname.isEmpty()) {
            return "";
        }
        // lowerCamel "Entity" stays part of the name here (matches your current codegen names)
        return classname.substring(0, 1).toLowerCase() + classname.substring(1);
    }

    /* ============================================================ */

    private enum RelationType { ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY }

    /** Holds the two "sides" of a relationship. */
    public final class RelationPair {
        Side left;
        Side right;

        void addSide(CodegenModel mdl, CodegenProperty prop) {
            if (Objects.isNull(left)) {
                left = new Side(mdl, prop);
            } else if (Objects.isNull(right)) {
                right = new Side(mdl, prop);
            }
        }

        boolean isBidirectional() {
            return Objects.nonNull(left) && Objects.nonNull(right);
        }

        Cardinality cardOf(Side s) {
            return s.prop.isContainer ? Cardinality.MANY : ONE;
        }

        RelationType jointType() {
            if (Objects.isNull(left) || Objects.isNull(right)) {
                return RelationType.ONE_TO_ONE;
            }
            var l = cardOf(left);
            var r = cardOf(right);
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

        void chooseOwnerLexicographically() {
            if (left.model.classname.compareTo(right.model.classname) < 0) {
                left.owningSide = true;
            } else {
                right.owningSide = true;
            }
        }

        void stampExtensions() {
            if (Objects.nonNull(left)) {
                stamp(left, right);
            }
            if (Objects.nonNull(right)) {
                stamp(right, left);
            }
        }

        private void stamp(Side me, Side other) {
            if (isEmbeddable(me.model)) {
                return;
            }

            var v = me.prop.vendorExtensions;
            v.put("x-is-owning-side", me.owningSide || Objects.isNull(other));

            if (Objects.nonNull(other)) {
                v.put("x-mapped-by", other.prop.name);
                v.put("x-related-classname", other.model.classname);
            }

            var meMany = me.prop.isContainer;
            var otherMany = Objects.nonNull(other) && other.prop.isContainer;

            var o2o = !meMany && !otherMany;
            var m2m = meMany && otherMany;
            var o2m = meMany && !otherMany;
            var m2o = !meMany && otherMany;

            v.put("x-is-one-to-one", o2o);
            v.put("x-is-many-to-many", m2m);
            v.put("x-is-one-to-many", o2m);
            v.put("x-is-many-to-one", m2o);
            String relationType = o2o ? "ONE_TO_ONE"
                : o2m ? "ONE_TO_MANY"
                : m2o ? "MANY_TO_ONE"
                : "MANY_TO_MANY";
            v.put("x-relation-type", relationType);
        }

        enum Cardinality { ONE, MANY }

        private final class Side {
            final CodegenModel model;
            final CodegenProperty prop;
            boolean owningSide = false;

            Side(CodegenModel m, CodegenProperty p) {
                this.model = m;
                this.prop = p;
            }
        }
    }
}
