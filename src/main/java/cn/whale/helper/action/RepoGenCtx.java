package cn.whale.helper.action;

import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.List;

public class RepoGenCtx {
    public int gormVersion = 1;
    public String defaultDatabase;

    public RepoGenCtx(VirtualFile virtualFile) {
        guessUsedDatabase(virtualFile);
    }

    /**
     * try detect existing -repo files, get database in using. <br>
     * and use it in dialog as default database option
     * @param virtualFile
     */
    private void guessUsedDatabase(VirtualFile virtualFile) {
        if (virtualFile == null) {
            return;
        }
        if (!virtualFile.isDirectory()) {
            virtualFile = virtualFile.getParent();
        }
        List<VirtualFile> repos = IDEUtils.collectChildDeep(virtualFile, f -> !f.isDirectory() && f.getName().contains("-repo"));
        if (repos.isEmpty()) {
            return;
        }
        try {
            String line = Utils.readFirstLine(IDEUtils.toFile(repos.get(0)), s -> s.contains(".NewTransactionManager("));
            if (!Utils.isEmpty(line)) {
                String[] segs = line.trim().split("[\",)]");
                defaultDatabase = segs[segs.length - 1].trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isGormV2() {
        return gormVersion == 2;
    }
}
