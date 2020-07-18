package cn.whale.helper.bazel;

import cn.whale.helper.bazel.model.BazelBuild;
import cn.whale.helper.template.LineBasedTemplateRender;
import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.GoUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractBazelGenerator {

    protected static Notifier notifier = new Notifier("whgo_helper bazel");

    static Map<String, String> stdlibs = new HashMap<>();

    static String indent = "        ";

    protected Project project;

    protected String projectModuleName;

    protected File projectRoot;

    protected File dir;

    public AbstractBazelGenerator(Project project, File dir) {
        this.project = project;
        this.dir = dir;
        this.projectRoot = Utils.getWhgoProjectRoot(project);

        projectModuleName = Utils.getProjectGoModuleName(projectRoot);
    }

    static List<String> quote(List<String> strings) {
        return strings.stream().map(Utils::quote).collect(Collectors.toList());
    }

    public static void collectStdLib(Project project) {
        Map<String, String> map = new HashMap<>();
        File projectRoot = Utils.getWhgoProjectRoot(project);
        File f = new File(projectRoot, "WORKSPACE");
        if (!f.exists()) {
            System.out.println(f + " not exists");
            return;
        }
        String name = "";
        try {
            for (String line : Utils.readLines(f)) {
                line = line.trim();
                if (line.startsWith("name =") || line.startsWith("name=")) {
                    name = Utils.substrBetweenQuote(line);
                } else if (line.startsWith("importpath =") || line.startsWith("importpath=")) {
                    String importpath = Utils.substrBetweenQuote(line);
                    if (Utils.isNotEmpty(name)) {
                        map.put(importpath, name);
                    }
                } else if (line.equals(")")) {
                    name = "";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            notifier.error(project, Utils.getStackTrace(e));
        }
        stdlibs = map;
    }

    protected abstract String getTemplateName();

    protected LineBasedTemplateRender loadTemplate() throws IOException {
        InputStream is = AbstractBazelGenerator.class.getResourceAsStream("./templates/" + getTemplateName() + ".bazel");
        LineBasedTemplateRender templ = new LineBasedTemplateRender();
        templ.loadTemplate(is);
        return templ;
    }

    private Map<String, String> prepareCommonParam() {
        Map<String, String> param = new HashMap<>();

        String dirname = dir.getName();
        param.put("dirname", dirname);

        String dir_name = dirname.replace('-', '_');
        param.put("dir_name", dir_name);

        String whale_dir_name = dir_name;
        if (!whale_dir_name.startsWith("whale_")) {
            whale_dir_name = "whale_" + dir_name;
        }
        param.put("whale_dir_name", whale_dir_name);

        String relativepath = Utils.relativePath(projectRoot.getParentFile(), dir);
        param.put("relativepath", relativepath);
        return param;
    }

    protected abstract Map<String, String> prepareTemplateParam() throws IOException;

    protected void update(BazelBuild bb) throws IOException {

    }

    public void generate() throws IOException {
        File bazelFile = new File(dir, "BUILD.bazel");

//        if (bazelFile.isFile()) {
//            BazelBuild bb = BazelBuildParser.parse(Utils.reader(bazelFile));
//            update(bb);
//            Utils.writeFile(bazelFile, bb.toString());
//        } else {
//            LineBasedTemplateParser template = loadTemplate();
//
//            template.parse(prepareCommonParam());
//            template.parse(prepareTemplateParam());
//            Utils.writeFile(bazelFile, template.getResult());
//        }
        LineBasedTemplateRender template = loadTemplate();

        template.render(prepareCommonParam());
        template.render(prepareTemplateParam());
        Utils.writeFile(bazelFile, template.getResult());


        notifier.info(project, "gen " + Utils.relativePath(projectRoot, bazelFile) + " success");
    }

    protected String getQuotedFileNames(List<File> fs) {
        List<String> quotedStrs = fs.stream().map(File::getName).map(Utils::quote).collect(Collectors.toList());

        return joinAsMultiLine(quotedStrs);
    }

    protected String joinAsMultiLine(List<String> list) {
        if (list.isEmpty()) {
            return "";
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return "\n" + indent + Utils.join(list, ",\n" + indent) + ",\n    ";
    }

    List<String> collectGoDep(List<File> goFiles) throws IOException {
        List<String> imports = GoUtils.readGoImports(goFiles, projectModuleName);
        List<String> deps = new ArrayList<>();
        ArrayList<String> convertedDep = new ArrayList<>();
        for (String imp : imports) {
            if (imp.startsWith(projectModuleName)) {
                deps.add(imp.replace(projectModuleName, "/") + ":go_default_library");
            } else {
                // github.com/ etc
                convertedDep.add(convertDep(stdlibs, imp));
                //deps.add(convertDep(stdlibs, imp) + ":go_default_library");
            }
        }
        Collections.sort(convertedDep);
        for (String s : convertedDep) {
            deps.add(s + ":go_default_library");
        }

        return deps;
    }


    String convertDep(Map<String, String> stdmap, String stdimport) {
        String[] segs = stdimport.split("/");
        for (int i = segs.length; i > 0; i--) {
            String lib0 = Utils.join(segs, 0, i, "/");
            String std0 = stdmap.get(lib0);
            if (std0 != null) {
                return "@" + std0 + "//" + Utils.join(segs, i, segs.length, "/");
            }
        }
        System.out.println("user //vendor for " + stdimport);
        return "//vendor/" + stdimport;
    }
}
