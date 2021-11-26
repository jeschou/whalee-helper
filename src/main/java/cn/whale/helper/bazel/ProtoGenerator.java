package cn.whale.helper.bazel;

import cn.whale.helper.utils.ProtoUtil;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ProtoGenerator extends AbstractBazelGenerator {


    private final List<File> protoFiles;
    private final List<File> goGenFiles;

    public ProtoGenerator(Project project, File dir, List<File> protoFiles, List<File> goGenFiles) {
        super(project, dir);
        this.protoFiles = protoFiles;
        this.goGenFiles = goGenFiles;
    }

    @Override
    protected String getTemplateName() {
        return "proto";
    }

    @Override
    protected Map<String, String> prepareTemplateParam() throws IOException {
        Map<String, String> param = new HashMap<>();
        param.put("proto_files", getQuotedFileNames(protoFiles));

        List<String> deps = collect_proto_library_deps(projectRoot, protoFiles);
        param.put("proto_library_deps", joinAsMultiLine(quote(deps)));

        deps = collect_go_proto_library_deps(projectRoot, protoFiles);
        param.put("go_proto_library_deps", joinAsMultiLine(quote(deps)));

        List<File> goFiles = goGenFiles;
        param.put("go_files", getQuotedFileNames(goFiles));

        deps = collectGoDep(goFiles);
        param.put("go_library_deps", joinAsMultiLine(quote(deps)));

        return param;
    }

    List<String> collect_proto_library_deps(File projectRoot, List<File> protoFiles) throws IOException {
        Set<String> curDirProtoFiles = protoFiles.stream().map(File::getAbsolutePath).collect(Collectors.toSet());
        List<String> protoDeps = new ArrayList<>();
        Set<String> uniqueSet = new HashSet<>();
        for (String ppath : ProtoUtil.readImports(protoFiles)) {
            File pf = new File(projectRoot, ppath);
            if (curDirProtoFiles.contains(pf.getAbsolutePath())) {
                continue;
            }
            String[] go_package = ProtoUtil.readGoPackage(pf);
            if (go_package == null) {
                System.out.println(pf + " has no go_package declare");
                continue;
            }
            if (go_package.length == 1) {
                // 兼容逻辑
                go_package = new String[]{go_package[0], Utils.splitAndGet(go_package[0], "/", -1)};
            }
            String dep = go_package[0].replace(projectModuleName, "/") + ":" + go_package[1] + "_proto";
            if (uniqueSet.add(dep)) {
                protoDeps.add(dep);
            }
        }
        return protoDeps;
    }

    List<String> collect_go_proto_library_deps(File projectRoot, List<File> protoFiles) throws IOException {
        Set<String> curDirProtoFiles = protoFiles.stream().map(File::getAbsolutePath).collect(Collectors.toSet());
        List<String> protoImports = ProtoUtil.readImports(protoFiles);
        List<String> libDep = new ArrayList<>();
        Set<String> uniqueSet = new HashSet<>();
        for (String ppath : protoImports) {
            File pf = new File(projectRoot, ppath);
            if (curDirProtoFiles.contains(pf.getAbsolutePath())) {
                continue;
            }
            String[] go_package = ProtoUtil.readGoPackage(pf);
            if (go_package == null) {
                System.out.println(ppath + " has no go_package declare");
                continue;
            }
            String dep = go_package[0].replace(projectModuleName, "/") + ":go_default_library";
            if (uniqueSet.add(dep)) {
                libDep.add(dep);
            }
        }
        return libDep;
    }
}
