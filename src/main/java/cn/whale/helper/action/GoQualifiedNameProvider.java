package cn.whale.helper.action;

import cn.whale.helper.utils.IDEUtils;
import cn.whale.helper.utils.Utils;
import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedList;

public class GoQualifiedNameProvider implements QualifiedNameProvider {

    @Override
    public @Nullable PsiElement adjustElementToCopy(@NotNull PsiElement psiElement) {
        return psiElement;
    }

    @Override
    public @Nullable String getQualifiedName(@NotNull PsiElement psiElement) {
        if (psiElement.getClass().getName().equals("com.goide.psi.GoFile")) {
            VirtualFile f = psiElement.getContainingFile().getVirtualFile();
            try {
                return getPackageName(f);
            } catch (IOException e) {
            }
            return null;
        }
        return null;
    }

    @Override
    public @Nullable PsiElement qualifiedNameToElement(@NotNull String s, @NotNull Project project) {
        return null;
    }

    private String getPackageName(VirtualFile f) throws IOException {
        LinkedList<String> list = new LinkedList<>();
        while (f != null) {
            if (f.isDirectory()) {
                @Nullable VirtualFile goModFile = f.findChild("go.mod");
                if (goModFile != null) {
                    String moduleDeclare = Utils.readFirstLine(IDEUtils.toFile(goModFile), line -> line.startsWith("module "));
                    if (moduleDeclare != null) {
                        list.add(0, moduleDeclare.substring("module ".length()));
                    }
                    break;
                } else {
                    list.add(0, f.getName());
                }
            }
            f = f.getParent();
        }
        return Utils.join(list, "/");
    }
}
