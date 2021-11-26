package cn.whale.helper.bazel;

import cn.whale.helper.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BazelUtils {

    static Comparator<File> SORT_WITHOUT_EXT = Comparator.comparing(f -> Utils.substringBefore(f.getName(), "."));

    public static void updateBUILDbazel(Project project, VirtualFile vdir) throws Exception {
        File dir = new File(vdir.getPath());

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        boolean hasMain = false;
        boolean hasDockerfile = false;
        List<File> goFiles = new ArrayList<>();
        List<File> goSrcFiles = new ArrayList<>();
        List<File> goTestFiles = new ArrayList<>();

        List<File> protoFiles = new ArrayList<>();
        List<File> protoGenFiles = new ArrayList<>();
        for (File f : files) {
            String fn = f.getName();
            if ("main.go".equals(fn)) {
                hasMain = true;
            }
            if ("Dockerfile".equals(fn)) {
                hasDockerfile = true;
            }
            if (fn.endsWith(".proto")) {
                protoFiles.add(f);
            }
            if (fn.endsWith(".go")) {
                goFiles.add(f);
                if (fn.endsWith("_test.go")) {
                    goTestFiles.add(f);
                } else if (fn.endsWith(".pb.gw.go")) {
                    protoGenFiles.add(f);
                } else {
                    goSrcFiles.add(f);
                }
            }
        }
        if (goFiles.isEmpty()) {
            return;
        }

        goSrcFiles.sort(SORT_WITHOUT_EXT);
        goTestFiles.sort(SORT_WITHOUT_EXT);
        protoFiles.sort(SORT_WITHOUT_EXT);
        protoGenFiles.sort(SORT_WITHOUT_EXT);

        if (hasMain) {
            new MainGenerator(project, dir, goSrcFiles, goTestFiles).generate();
        } else if (protoFiles.size() > 0) {
            new ProtoGenerator(project, dir, protoFiles, protoGenFiles).generate();
        } else if (goTestFiles.size() > 0) {
            if (goSrcFiles.isEmpty()) {
                new TestOnlyGenerator(project, dir, goTestFiles).generate();
            } else {
                new BasicWithTestGenerator(project, dir, goSrcFiles, goTestFiles).generate();
            }
        } else {
            new BasicGenerator(project, dir, goSrcFiles).generate();
        }
        RefreshQueue.getInstance().refresh(true, true, null, vdir);
    }
}
