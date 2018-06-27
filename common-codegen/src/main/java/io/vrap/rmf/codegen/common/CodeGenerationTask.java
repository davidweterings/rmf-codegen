package io.vrap.rmf.codegen.common;

import io.vrap.rmf.raml.model.modules.Api;
import org.eclipse.emf.ecore.EObject;

import java.io.IOException;

public interface CodeGenerationTask {
    /**
     * Generate code from the given api.
     *
     * @param api the api
     * @return the result of the generation
     *
     * @throws IOException
     */
    GenerationResult generate(Api api) throws IOException;

    String getName();
}
