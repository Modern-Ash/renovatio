package org.shark.renovatio.provider.cobol.translation;

import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class CobolIntermediateModelService {

    private final SimpleCobolIrParser parser;
    public CobolIntermediateModelService() {
        this(new SimpleCobolIrParser());
    }

    public CobolIntermediateModelService(SimpleCobolIrParser parser) {
        this.parser = parser;
    }

    public CobolIntermediateModel parse(Path cobolFile) {
        try {
            return parser.parse(cobolFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse COBOL file " + cobolFile, ex);
        }
    }

    public CobolIntermediateModel parse(String cobolSource) {
        return parser.parse(cobolSource);
    }
}
