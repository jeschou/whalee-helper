package cn.whale.helper.bazel;

import cn.whale.helper.bazel.model.BazelBuild;
import cn.whale.helper.bazel.model.Def;
import cn.whale.helper.bazel.model.GoTest;
import cn.whale.helper.bazel.model.ListParam;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainGenerator extends BasicGenerator {


    private final List<File> goTestFiles;

    public MainGenerator(Project project, File dir, List<File> goSrcFiles, List<File> goTestFiles) {
        super(project, dir, goSrcFiles);
        this.goTestFiles = goTestFiles;
    }

    @Override
    protected String getTemplateName() {
        return "main";
    }


    @Override
    protected Map<String, String> prepareTemplateParam() throws IOException {
        Map<String, String> param = super.prepareTemplateParam();
        if (dir.getName().startsWith("whale")) {
            param.put("server_address", "--server_address=0.0.0.0:50051");
            param.put("registry", "--registry=mdns");
        } else {
            param.put("server_address", "");
            param.put("registry", "");
        }
        return param;
    }

    @Override
    protected void update(BazelBuild bb) throws IOException {
        Def go_library = bb.getDefByName("go_library");
        if (go_library == null) {
            return;
        }
        ListParam srcs = (ListParam) go_library.getParamByName("srcs");
        if (srcs != null) {
            srcs.values = goSrcFiles.stream().map(File::getName).collect(Collectors.toList());
        }
        ListParam deps = (ListParam) go_library.getParamByName("deps");
        if (srcDeps.size() > 0) {
            if (deps == null) {
                deps = new ListParam();
                deps.name = "deps";
                go_library.params.add(deps);
            }
            deps.values = srcDeps;
        }

        if (goTestFiles.size() > 0) {
            List<String> test_srcs = goTestFiles.stream().map(File::getName).collect(Collectors.toList());
            List<String> testDeps = collectGoDep(goTestFiles);
            testDeps.removeAll(srcDeps);

            if (bb.getDefByName("go_test") == null) {
                Def go_test = new GoTest(test_srcs, go_library.name, testDeps);
                bb.addGoTest(go_test);
            }
        }

    }
}
