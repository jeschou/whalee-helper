package cn.whale.helper.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * for test only
 */
public class SimpleFileLogger {
    File file;

    public SimpleFileLogger(String fp) {
        file = new File(fp);
    }

    public void info(Object... args) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(file.length());
            raf.writeChars(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
            raf.writeChars(" INFO");
            for (Object o : args) {
                raf.writeChars(" ");
                raf.writeChars(String.valueOf(o));
            }
            raf.writeChars("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
