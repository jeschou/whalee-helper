package cn.whale.helper.bazel;

import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicGenerator extends AbstractBazelGenerator {

    protected final List<File> goSrcFiles;

    // for sub class usage
    protected List<String> srcDeps;


    public BasicGenerator(Project project, File dir, List<File> goSrcFiles) {
        super(project, dir);
        this.goSrcFiles = goSrcFiles;
    }

    @Override
    protected String getTemplateName() {
        try {
            if (containsMainFunc(goSrcFiles)) {
                return "basic_with_main";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "basic";
    }

    @Override
    protected Map<String, String> prepareTemplateParam() throws IOException {
        Map<String, String> param = new HashMap<>();
        param.put("src_files", getQuotedFileNames(goSrcFiles));
        srcDeps = collectGoDep(goSrcFiles);
        param.put("go_library_deps", joinAsMultiLine(quote(srcDeps)));
        return param;
    }

    protected boolean containsMainFunc(List<File> fs) throws IOException {
        for (File f : fs) {
            String mainDef = Utils.readFirstLine(f, s -> s.trim().startsWith("func main()"));
            if (mainDef != null) {
                return true;
            }
        }
        return false;
    }


}
