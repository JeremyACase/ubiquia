package org.ubiquia.core.belief.state.generator.service.generator.preprocessor;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Preprocessor {

    @Autowired
    private CleanupPreprocessor cleanupPreprocessor;

    public void preprocess() throws IOException {
        this.cleanupPreprocessor.preprocess();
    }

}
