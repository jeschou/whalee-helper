package cn.whale.helper.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.whale.helper.utils.Utils.safeClose;

public class CloserHolder implements Closeable {

    List<Closeable> list = new ArrayList<>();

    public void add(Closeable... c) {
        list.addAll(Arrays.asList(c));
    }

    @Override
    public void close() throws IOException {
        for (Closeable c : list) {
            safeClose(c);
        }
    }
}