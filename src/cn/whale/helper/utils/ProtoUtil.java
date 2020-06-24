package cn.whale.helper.utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ProtoUtil {
    public static List<String> readImports(File f) throws IOException {
        List<String> lines = Utils.readLines(f, "import ");
        List<String> imports = new ArrayList<>(lines.size());
        for (String l : lines) {
            imports.add(Utils.substrBetweenQuote(l));
        }
        Collections.sort(imports);
        return imports;
    }

    public static List<String> readImports(List<File> fs) throws IOException {
        List<String> imports = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (File f : fs) {
            for (String imp : readImports(f)) {
                if (!set.contains(imp)) {
                    imports.add(imp);
                    set.add(imp);
                }
            }
        }
        Collections.sort(imports);
        return imports;
    }

    /**
     * @param f
     * @return [package, alias]
     * @throws IOException
     */
    public static String[] readGoPackage(File f) throws IOException {
        String go_packageLine = Utils.readFirstLine(f, s -> s.contains("go_package"));
        if (go_packageLine == null) {
            return null;
        }
        return Utils.substrBetweenQuote(go_packageLine).split(";");
    }

}
