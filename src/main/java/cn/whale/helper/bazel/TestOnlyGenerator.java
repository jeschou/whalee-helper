package cn.whale.helper.bazel;

import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.List;

public class TestOnlyGenerator extends BasicGenerator {


    public TestOnlyGenerator(Project project, File dir, List<File> goTestFiles) {
        super(project, dir, goTestFiles);
    }

    @Override
    protected String getTemplateName() {
        return "test_only";
    }
}
