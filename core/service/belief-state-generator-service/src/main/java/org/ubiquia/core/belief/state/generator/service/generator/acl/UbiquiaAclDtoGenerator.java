package org.ubiquia.core.belief.state.generator.service.generator.acl;

import java.util.HashMap;
import java.util.Map;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.languages.JavaClientCodegen;
import org.openapitools.codegen.model.ModelsMap;

public class UbiquiaAclDtoGenerator extends JavaClientCodegen {

    private final Map<String, CodegenModel> modelIndex = new HashMap<>();

    @Override
    public void processOpts() {
        super.processOpts();

        super.setEnablePostProcessFile(true);

        super.modelTemplateFiles.put("modelIngressDtoMapper.mustache", "IngressDtoMapper.java");
        super.modelTemplateFiles.put("modelEgressDtoMapper.mustache", "EgressDtoMapper.java");
        super.modelTemplateFiles.put("modelController.mustache", "Controller.java");
    }

    @Override
    public String getName() {
        return "ubiquia-acl-dto-generator";
    }

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {

        /* 1️⃣ index every model by class-name for fast look-ups */
        modelIndex.clear(); // ← ensures no stale state
        for (ModelsMap mm : objs.values()) {
            mm.getModels().forEach(m -> modelIndex.put(m.getModel().classname, m.getModel()));
        }

        return super.postProcessAllModels(objs);
    }

    @Override
    public void postProcessFile(java.io.File file, String fileType) {
        if ("model".equals(fileType)) {
            String filename = file.getName();

            if (filename.endsWith("IngressDtoMapper.java") ||
                filename.endsWith("EgressDtoMapper.java") ||
                filename.endsWith("Controller.java")) {

                String modelName = filename
                    .replace("IngressDtoMapper.java", "")
                    .replace("EgressDtoMapper.java", "")
                    .replace("Controller.java", "");

                CodegenModel model = modelIndex.get(modelName);
                if (model != null && model.isEnum) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.printf("Deleted enum-derived file: %s%n", file.getAbsolutePath());
                    } else {
                        System.out.printf("Failed to delete file: %s%n", file.getAbsolutePath());
                    }
                }
            }
        }

        super.postProcessFile(file, fileType);
    }

}
