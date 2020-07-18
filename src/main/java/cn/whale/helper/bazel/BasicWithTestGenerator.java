package cn.whale.helper.bazel;

import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class BasicWithTestGenerator extends BasicGenerator {


    private final List<File> goTestFiles;

    public BasicWithTestGenerator(Project project, File dir, List<File> goSrcFiles, List<File> goTestFiles) {
        super(project, dir, goSrcFiles);
        this.goTestFiles = goTestFiles;
    }

    @Override
    protected String getTemplateName() {
        return "basic_with_test";
    }

    @Override
    protected Map<String, String> prepareTemplateParam() throws IOException {
        Map<String, String> param = super.prepareTemplateParam();
        param.put("test_files", getQuotedFileNames(goTestFiles));
        List<String> testDeps = collectGoDep(goTestFiles);
        testDeps.removeAll(srcDeps);
        param.put("go_test_deps", joinAsMultiLine(quote(testDeps)));
        return param;
    }
}
