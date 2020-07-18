package cn.whale.helper.dao;

import cn.whale.helper.template.SimpleTemplateRender;
import cn.whale.helper.ui.Notifier;
import cn.whale.helper.utils.GoUtils;
import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GoDaoGenerator {

    protected static Notifier notifier = new Notifier("whgo_helper gorm-dao-gen");

    private Project project;
    private VirtualFile selectedFile;

    private String fileName;
    private String tableName;
    private String structName;
    private List<Object[]> fields;

    public GoDaoGenerator(Project project, VirtualFile selectedFile, String fileName, String tableName, String structName, List<Object[]> fields) {
        this.project = project;
        this.selectedFile = selectedFile;
        this.fileName = fileName;
        this.tableName = tableName;
        this.structName = structName;
        this.fields = fields;
        if (!this.fileName.endsWith(".go")) {
            this.fileName = fileName + ".go";
        }
    }

    public void generate() {
        VirtualFile targetDir = selectedFile;
        if (!targetDir.isDirectory()) {
            targetDir = targetDir.getParent();
        }

        SimpleTemplateRender templateRender = new SimpleTemplateRender();
        try {
            templateRender.loadTemplate(GoDaoGenerator.class.getResourceAsStream("dao_template.txt"));
        } catch (IOException e) {
            e.printStackTrace();
            notifier.error(project, Utils.getStackTrace(e));
            return;
        }

        Map<String, String> params = new HashMap<>();
        String packageName = targetDir.getName();
        List<VirtualFile> fs = IDEUtils.collectChild(targetDir, IDEUtils::isGoFile);
        if (!fs.isEmpty()) {
            try {
                String packageName0 = GoUtils.getPackage(new File(fs.get(0).getPath()), true);
                if (packageName0 != null) {
                    packageName = packageName0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        params.put("package", packageName);
        params.put("structName", Utils.unTitle(structName));
        params.put("StructName", Utils.toTitle(structName));
        params.put("tableName", tableName);

        StringBuilder impsSb = new StringBuilder();
        List<String> imps = collectImport();
        impsSb.append("\n");
        for (String s : imps) {
            impsSb.append("\t").append(Utils.quote(s)).append("\n");
        }
        params.put("imports", impsSb.toString());


        int maxFieldWidth = 0;
        int maxTypeWidth = 0;
        for (Object[] segs : fields) {
            maxFieldWidth = Math.max(maxFieldWidth, ((String) segs[3]).length());
            maxTypeWidth = Math.max(maxTypeWidth, ((String) segs[4]).length());
        }

        StringBuilder fieldsSb = new StringBuilder();
        for (Object[] segs : fields) {
            fieldsSb.append("\t").append(String.format("%-" + maxFieldWidth + "s %-" + maxTypeWidth + "s %s", segs[3], segs[4], segs[5])).append("\n");
        }
        if (fieldsSb.length() > 0) {
            fieldsSb.deleteCharAt(fieldsSb.length() - 1);
        }
        params.put("structFields", fieldsSb.toString());

        templateRender.render(params);

        File f = new File(targetDir.getPath(), fileName);
        try {
            Utils.writeFile(f, templateRender.getResult());
            RefreshQueue.getInstance().refresh(true, true, null, targetDir);
        } catch (IOException e) {
            e.printStackTrace();
            notifier.error(project, Utils.getStackTrace(e));
        }

    }

    private List<String> collectImport() {
        Set<String> sets = new HashSet<>();
        sets.add("github.com/jinzhu/gorm");
        for (Object[] segs : fields) {
            String goType = (String) segs[4];
            if (goType.contains("time.")) {
                sets.add("time");
            }
            if (goType.contains("pg.StringArray")) {
                sets.add("github.com/lib/pq");
            }
            if (goType.contains("postgres.")) {
                sets.add("github.com/jinzhu/gorm/dialects/postgres");
            }
        }
        ArrayList<String> imps = new ArrayList<>(sets);
        Collections.sort(imps);
        return imps;
    }
}
