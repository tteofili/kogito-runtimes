/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.codegen.prediction;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.kie.kogito.codegen.AbstractApplicationSection;
import org.kie.kogito.codegen.AddonsConfig;
import org.kie.kogito.codegen.InvalidTemplateException;
import org.kie.kogito.codegen.TemplatedGenerator;
import org.kie.kogito.codegen.context.KogitoBuildContext;

import java.util.List;

public class PredictionModelsGenerator extends AbstractApplicationSection {

    private static final String RESOURCE = "/class-templates/PredictionModelsTemplate.java";
    private static final String RESOURCE_CDI = "/class-templates/CdiPredictionModelsTemplate.java";
    private static final String RESOURCE_SPRING = "/class-templates/spring/SpringPredictionModelsTemplate.java";
    private static final String SECTION_CLASS_NAME = "PredictionModels";

    protected final List<PMMLResource> resources;
    protected final String applicationCanonicalName;
    protected AddonsConfig addonsConfig = AddonsConfig.DEFAULT;
    protected final TemplatedGenerator templatedGenerator;

    public PredictionModelsGenerator(KogitoBuildContext buildContext, String packageName, String applicationCanonicalName, List<PMMLResource> resources) {
        super(buildContext, SECTION_CLASS_NAME);
        this.applicationCanonicalName = applicationCanonicalName;
        this.resources = resources;

        this.templatedGenerator = new TemplatedGenerator(
                buildContext,
                packageName,
                SECTION_CLASS_NAME,
                RESOURCE_CDI,
                RESOURCE_SPRING,
                RESOURCE);
    }

    public PredictionModelsGenerator withAddons(AddonsConfig addonsConfig) {
        this.addonsConfig = addonsConfig;
        return this;
    }

    @Override
    public CompilationUnit compilationUnit() {
        CompilationUnit compilationUnit = templatedGenerator.compilationUnitOrThrow("Invalid Template: No CompilationUnit");
        populateStaticKieRuntimeFactoryFunctionInit(compilationUnit);
        return compilationUnit;
    }

    private void populateStaticKieRuntimeFactoryFunctionInit(CompilationUnit compilationUnit) {
        final InitializerDeclaration staticDeclaration = compilationUnit
                .findFirst(InitializerDeclaration.class)
                .orElseThrow(() -> new InvalidTemplateException(
                        SECTION_CLASS_NAME,
                        templatedGenerator.templatePath(),
                        "Missing static block"));
        final MethodCallExpr initMethod = staticDeclaration
                .findFirst(MethodCallExpr.class, mtd -> "init".equals(mtd.getNameAsString()))
                .orElseThrow(() -> new InvalidTemplateException(
                        SECTION_CLASS_NAME,
                        templatedGenerator.templatePath(),
                        "Missing init() method"));

        for (PMMLResource resource : resources) {
            StringLiteralExpr getResAsStream = getReadResourceMethod(resource);
            initMethod.addArgument(getResAsStream);
        }
    }

    private StringLiteralExpr getReadResourceMethod(PMMLResource resource) {
        String source = resource.getModelPath();
        return new StringLiteralExpr(source);
    }
}
