package org.ubiquia.core.belief.state.generator.service.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GenerationSupportProcessor {

    public void postProcess() throws IOException {
        this.copyFile(
            "src/main/resources/template/java/support/Application.Java",
            "generated/src/main/java/org/ubiquia/acl/generated/Application.java");
    }

    private void copyFile(
        final String sourcePathStr,
        final String destinationPathStr) throws IOException {

        var sourcePath = Paths.get(sourcePathStr);
        var destinationPath = Paths.get(destinationPathStr);

        // Ensure destination directory exists
        Files.createDirectories(destinationPath.getParent());

        // Copy the file (REPLACE_EXISTING if destination file exists)
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }
}