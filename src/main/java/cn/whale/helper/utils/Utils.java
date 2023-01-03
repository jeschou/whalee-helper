package cn.whale.helper.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.project.Project;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class Utils {
    public final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * @param str null safe
     * @param sep null safe, regexp
     * @param idx like python, can be negative, -1 means the last. no IndexOutOfBoundsException
     * @return
     */
    public static String splitAndGet(String str, String sep, int idx) {
        if (str == null || sep == null) return null;
        String[] segs = str.split(sep);
        if (idx < 0) idx += segs.length;
        if (idx >= 0 && idx < segs.length) {
            return segs[idx];
        }
        return null;
    }

    public static String trimToEmpty(String str) {
        if (str == null) {
            return "";
        }
        return str.trim();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNoneEmpty(String... strs) {
        for (String s : strs) {
            if (isEmpty(s)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAnyEmpty(String... strs) {
        for (String s : strs) {
            if (isEmpty(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param str if empty, return false
     * @param arr empty element will be skip
     * @return
     */
    public static boolean containsAny(String str, String... arr) {
        if (isEmpty(str)) return false;
        for (String s : arr) {
            if (isNotEmpty(s) && str.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public static String toTitle(String str) {
        if (isEmpty(str)) return str;
        return Character.toTitleCase(str.charAt(0)) + str.substring(1);
    }

    public static String unTitle(String str) {
        if (isEmpty(str)) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    public static String toTitleCamelCase(String cn) {
        String[] segs = cn.split("_+");
        for (int i = 0; i < segs.length; i++) {
            segs[i] = toTitle(segs[i]);
        }
        return join(segs, 0, segs.length, "");
    }

    public static Object[] pluck(Object[][] arr2, int col) {
        Object[] arr = new Object[arr2.length];
        for (int i = 0; i < arr2.length; i++) {
            arr[i] = arr2[i][col];
        }
        return arr;
    }


    public static String readText(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            copy(is, baos);
        } finally {
            safeClose(is);
        }

        return baos.toString("UTF-8");
    }

    public static String readText(InputStream is, String charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            copy(is, baos);
        } finally {
            safeClose(is);
        }

        return baos.toString(charset);
    }

    public static String readText(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            copy(fis, baos);
        }

        return baos.toString(StandardCharsets.UTF_8);
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buff = new byte[8 * 1024];
        int len;
        while ((len = is.read(buff)) != -1) {
            os.write(buff, 0, len);
        }
    }

    public static void safeClose(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException ignored) {

        }
    }

    public static BufferedReader reader(File f) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(f)));
    }

    public static List<String> readLines(File f) throws IOException {
        return readLines(new FileInputStream(f));
    }

    public static List<String> readLines(InputStream inputStream) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        }
        return list;
    }

    public static List<String> readLines(File f, String filter) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = reader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (filter == null || line.contains(filter)) list.add(line);
            }
        }
        return list;
    }

    public static List<String> readLinesBefore(File f, Function<String, Boolean> breakTest) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = reader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (breakTest.apply(line)) return list;
                list.add(line);
            }
        }
        return list;
    }

    public static String readFirstLine(File f, Predicate<String> predicate) throws IOException {
        try (BufferedReader br = reader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (predicate.test(line)) return line;
            }
        }
        return null;
    }

    public static void writeFile(File f, String text) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(text.getBytes());
            fos.flush();
        }
    }

    public static String substrBetweenQuote(String str) {
        int idx0 = str.indexOf('"');
        if (idx0 == -1) return null;
        int idx1 = str.lastIndexOf('"');
        if (idx1 == -1) return null;
        return str.substring(idx0 + 1, idx1);
    }

    public static String quote(String str) {
        return '"' + str + '"';
    }

    public static List<String> quote(List<String> strs) {
        List<String> list = new ArrayList<>(strs.size());

        for (String s : strs) {
            list.add(quote(s));
        }
        return list;
    }

    /**
     * @param str
     * @param sep if empty or not found, return str
     * @return
     */
    public static String substringBefore(String str, String sep) {
        if (str == null) return null;
        if (isEmpty(sep)) return str;
        int idx = str.indexOf(sep);
        if (idx == -1) {
            return str;
        }
        return str.substring(0, idx);
    }

    /**
     * @param str
     * @param sep if empty or not found, return str
     * @return
     */
    public static String substringBeforeLast(String str, String sep) {
        if (str == null) return null;
        if (isEmpty(sep)) return str;
        int idx = str.lastIndexOf(sep);
        if (idx == -1) {
            return str;
        }
        return str.substring(0, idx);
    }

    /**
     * @param str
     * @param sep if empty or not found, return str
     * @return
     */
    public static String substringAfter(String str, String sep) {
        if (str == null) return null;
        if (isEmpty(sep)) return str;
        int idx = str.indexOf(sep);
        if (idx == -1) {
            return str;
        }
        return str.substring(idx + sep.length());
    }

    /**
     * @param str
     * @param sep if empty or not found, return str
     * @return
     */
    public static String substringAfterLast(String str, String sep) {
        if (str == null) return null;
        if (isEmpty(sep)) return str;
        int idx = str.lastIndexOf(sep);
        if (idx == -1) {
            return str;
        }
        return str.substring(idx + sep.length());
    }

    public static String relativePath(File root, File f) {
        Path rootPath = root.toPath();
        Path fPath = f.toPath();
        Path rPath = fPath.subpath(rootPath.getNameCount(), fPath.getNameCount());
        return rPath.toString().replace('\\', '/');
    }

    public static String join(String[] arr, int begin, int end, String sep) {
        StringBuilder sb = new StringBuilder();
        int last = Math.min(end, arr.length) - 1;
        for (int i = begin; i <= last; i++) {
            sb.append(arr[i]);
            if (i != last) {
                sb.append(sep);
            }
        }
        return sb.toString();
    }

    public static String join(Collection<?> collection, String sep) {
        StringBuilder sb = new StringBuilder();
        int last = collection.size() - 1;
        int i = 0;
        for (Object obj : collection) {
            sb.append(obj);
            if (i < last) {
                sb.append(sep);
            }
            i++;
        }
        return sb.toString();
    }

    public static String getStackTrace(Throwable thr) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter pw = new PrintWriter(stringWriter)) {
            thr.printStackTrace(pw);
        }
        return stringWriter.toString();
    }

    public static void dump(Object... args) {
        File targetFile = new File(System.getProperty("user.home"), ".whgo_dump.log");
        if (!targetFile.exists()) {
            try {
                writeFile(targetFile, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
            targetFile.deleteOnExit();
        }
        StringBuilder sb = new StringBuilder();
        try {

            for (Object obj : args) {
                sb.append(new Date().toString()).append("\t");
                sb.append(obj.toString()).append("\n\n");
            }
            appendFile(targetFile, sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendFile(File f, String text) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            raf.seek(f.length());
            raf.writeChars(text);
        }
    }

    public static File getWhgoProjectRoot(Project project) {
        File dir = new File(project.getBasePath());
        if (dir.isFile()) {
            dir = dir.getParentFile();
        }
        while (dir != null && !new File(dir, "go.mod").exists()) {
            dir = dir.getParentFile();
        }
        return dir;
    }

    public static String getProjectGoModuleName(File projectRoot) {
        String moduleDeclareLine = null;
        try {
            moduleDeclareLine = readFirstLine(new File(projectRoot, "go.mod"), (s) -> s.startsWith("module "));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (moduleDeclareLine == null) {
            return "whgo";
        }
        return moduleDeclareLine.trim().split(" ")[1];
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    public static String getLoginShell() {
        try {
            String home = System.getProperty("user.home");
            Process proc = Runtime.getRuntime().exec("dscl . -read " + home + " UserShell");
            String text = Utils.trimToEmpty(Utils.readText(proc.getInputStream()));
            String sh = Utils.trimToEmpty(Utils.substringAfter(text, ":"));
            if (Utils.isNotEmpty(sh)) {
                return sh;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "/bin/bash";
    }

    /**
     * only work with public field
     */
    public static <T> T getFieldValue(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getField(fieldName);
            return (T) f.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * only work with public field
     */
    public static void setFieldValue(Object obj, String fieldName, Object val) {
        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, val);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * only work with public field
     */
    public static void copyByField(Object toObj, Object fromObj) {
        try {
            Field[] fs = fromObj.getClass().getFields();
            for (Field f : fs) {
                int md = f.getModifiers();
                if (!Modifier.isPublic(md) || Modifier.isStatic(md)) continue;
                setFieldValue(toObj, f.getName(), f.get(fromObj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * for windows, shellCmd is CMD.<br>
     * for *unix , shellCmd is shell
     *
     * @return [stdout, stderror]
     */
    public static String[] executeShell(File cmdDir, String shellCmd) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.directory(cmdDir);
        if (Utils.isWindows()) {
            processBuilder.command("cmd.exe", "/C", shellCmd);
        } else {
            File f = File.createTempFile("whalee_helper", ".sh");
            Utils.writeFile(f, shellCmd);
            String sh = Utils.getLoginShell();
            processBuilder.command(sh, "-i", f.getAbsolutePath());
        }
        Process process = processBuilder.start();
        String normalContent = Utils.readText(process.getInputStream());
        String errorContent = Utils.readText(process.getErrorStream());
        process.destroy();
        return new String[]{normalContent, errorContent};
    }

    public static void copyFileTo(File file, OutputStream os) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            copy(fis, os);
        }
    }

    public static void deleteFile(File f, boolean force) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory() && force) {
            File[] fs = f.listFiles();
            if (fs != null) {
                for (File f0 : fs) {
                    deleteFile(f0, force);
                }
            }
        }
        f.delete();
    }

    public static Properties loadProperties() {
        Properties properties = new Properties();
        File f = new File(System.getProperty("user.home"), ".whalee-helper.properties");
        if (!f.isFile()) {
            return properties;
        }
        try {
            properties.load(new FileInputStream(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
