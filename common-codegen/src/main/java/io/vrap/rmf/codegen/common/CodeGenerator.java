package io.vrap.rmf.codegen.common;

import com.google.common.collect.Lists;
import io.vrap.rmf.raml.model.modules.Api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class CodeGenerator {
    private final GeneratorConfig config;
    private final List<CodeGenerationTask> tasks;

    CodeGenerator(final GeneratorConfig config, final List<CodeGenerationTask> tasks) {
        this.config = config;
        this.tasks = tasks;
    }

    public List<GenerationResult> generate(final Api api) {
        return tasks.stream()
                .map(task -> run(task, api))
                .collect(Collectors.toList());
    }

    private GenerationResult run(final CodeGenerationTask task, final Api api) {
        try {
            return task.generate(api);
        } catch (final IOException e) {
            return GenerationResult.of(task.getName(), Collections.emptyList(), e);
        }
    }

    public static CodeGenerator withAllTasks(final GeneratorConfig config) {
        final List<CodeGenerationTask> allTasks = Lists.newArrayList(ServiceLoader.load(CodeGenerationTask.class));
        return new CodeGenerator(config, allTasks);
    }
}
