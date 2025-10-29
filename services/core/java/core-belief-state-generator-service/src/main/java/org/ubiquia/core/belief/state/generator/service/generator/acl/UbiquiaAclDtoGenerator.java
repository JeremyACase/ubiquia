package org.ubiquia.core.belief.state.generator.service.generator.acl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.languages.JavaClientCodegen;
import org.openapitools.codegen.model.ModelsMap;

public class UbiquiaAclDtoGenerator extends JavaClientCodegen {

    private final Map<String, CodegenModel> modelIndex = new HashMap<>();

    @Override
    public void processOpts() {
        super.processOpts();
        setEnablePostProcessFile(true);

        modelTemplateFiles.put("modelIngressDtoMapper.mustache", "IngressDtoMapper.java");
        modelTemplateFiles.put("modelEgressDtoMapper.mustache", "EgressDtoMapper.java");
        modelTemplateFiles.put("modelController.mustache", "Controller.java");
    }

    @Override
    public String getName() {
        return "ubiquia-acl-dto-generator";
    }

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        modelIndex.clear();
        for (var mm : objs.values()) {
            for (var m : mm.getModels()) {
                var cg = m.getModel();
                modelIndex.put(cg.classname, cg);
            }
        }
        return super.postProcessAllModels(objs);
    }

    @Override
    public void postProcessFile(java.io.File file, String fileType) {
        if (!"model".equals(fileType)) {
            super.postProcessFile(file, fileType);
            return;
        }

        var filename = file.getName();
        var isTargeted =
            filename.endsWith("IngressDtoMapper.java")
                || filename.endsWith("EgressDtoMapper.java")
                || filename.endsWith("Controller.java");

        if (!isTargeted) {
            super.postProcessFile(file, fileType);
            return;
        }

        var modelName = filename
            .replace("IngressDtoMapper.java", "")
            .replace("EgressDtoMapper.java", "")
            .replace("Controller.java", "");

        var model = modelIndex.get(modelName);

        // Skip (delete) generated files when the model is embeddable.
        if (Objects.nonNull(model) && this.isEmbeddable(model)) {
            var deleted = file.delete();
            System.out.printf("%s skipped file for %s (embeddable)%n",
                deleted ? "Deleted" : "Failed to delete",
                modelName);
            return; // don't pass to super; we intentionally skipped this file
        }

        if (Objects.nonNull(model) && model.isEnum) {
            var deleted = file.delete();
            System.out.printf("%s skipped file for %s (enum)%n",
                deleted ? "Deleted" : "Failed to delete",
                modelName);
            return;
        }

        super.postProcessFile(file, fileType);
    }

    /* ----------------------- helpers ----------------------- */

    private boolean isEmbeddable(CodegenModel m) {
        return m != null && Boolean.TRUE.equals(m.vendorExtensions.get("x-embeddable"));
    }
}
