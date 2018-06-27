package io.vrap.rmf.codegen.common;

import java.nio.file.Path;
import java.util.List;

public class GenerationResult {
    private final String task;
    private final List<Path> generatedFiles;
    private final Exception exception;

    private GenerationResult(final String task, final List<Path> generatedFiles, final Exception exception) {
        this.task = task;
        this.generatedFiles = generatedFiles;
        this.exception = exception;
    }

    public List<Path> getGeneratedFiles() {
        return generatedFiles;
    }

    public String getTask() {
        return task;
    }

    public static GenerationResult of(final String task, final List<Path> generatedFiles) {
        return new GenerationResult(task, generatedFiles, null);
    }


    public static GenerationResult of(final String task, final List<Path> generatedFiles, final Exception exception) {
        return new GenerationResult(task, generatedFiles, exception);
    }
}
