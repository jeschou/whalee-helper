package cn.whale.helper.utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class GoUtils {

    static final Function<String, Boolean> IMPORT_BREAK = (line) -> line.startsWith("var") || line.startsWith("const") || line.startsWith("func");

    public static List<String> readGoImports(File f, String projectModuleName) throws IOException {

        List<String> imports = new ArrayList<>();

        for (String line : Utils.readLinesBefore(f, IMPORT_BREAK)) {
            if (line.endsWith("\"")) {
                String import_ = Utils.substrBetweenQuote(line);
                if (import_.contains(".") || import_.contains(projectModuleName)) {
                    imports.add(import_);
                }
            }
        }
        Collections.sort(imports);
        return imports;
    }

    public static List<String> readGoImports(List<File> fs, String projectModuleName) throws IOException {

        List<String> imports = new ArrayList<>();
        Set<String> set = new HashSet<>();

        for (File f : fs) {
            for (String imp : readGoImports(f, projectModuleName)) {
                if (!set.contains(imp)) {
                    imports.add(imp);
                    set.add(imp);
                }
            }
        }

        imports.sort((a, b) -> {
            if (a.contains(".") && !b.contains(".")) {
                return 1;
            }
            if (!a.contains(".") && b.contains(".")) {
                return -1;
            }
            return a.compareTo(b);
        });
        return imports;
    }
}
