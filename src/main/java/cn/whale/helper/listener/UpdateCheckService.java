package cn.whale.helper.listener;

import cn.whale.helper.utils.Utils;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.NlsActions;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.function.Consumer;

public class UpdateCheckService implements AppLifecycleListener {

    static {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    // check update every 4 hour after application started
                    Thread.sleep(4L * 3600 * 1000);
                    checkUpdate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    static PluginId pluginId() {
        return PluginId.findId("com.meetwhale.whgo.project.helper");
    }

    static IdeaPluginDescriptor getPlugin() {
        return PluginManager.getInstance().findEnabledPlugin(pluginId());
    }

    @Override
    public void appStarted() {
        new Thread(() -> {
            try {
                // after start up , sleep 30s
                Thread.sleep(30L * 1000);
                checkUpdate();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    static final String releaseBase = "https://whale-alivia.oss-cn-hangzhou.aliyuncs.com/whalee-helper/";

    public static synchronized void checkUpdate() {
        try {

            @Nullable IdeaPluginDescriptor plugin = getPlugin();
            if (plugin == null) {
                return;
            }
            InputStream is = new URL(releaseBase + "latest.txt").openStream();
            String text = Utils.readText(is).trim();
            if (text.equals(plugin.getVersion())) {
                return;
            }
            showPopup(text, plugin.getVersion());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void showPopup(String newVersion, String oldVersion) {
        Notification notify = new Notification("Update Notification", "New version found :", NotificationType.INFORMATION);
        notify.setTitle("whalee-helper update!");
        notify.setContent(String.format("new version: %s, current version: %s", newVersion, oldVersion));

        notify.addAction(new DownloadUpdateAction("Install Update", newVersion, notify));
        Notifications.Bus.notify(notify);

    }


    /**
     * download with porgress, cancellable
     *
     * @param version
     * @param onComplete
     * @throws IOException
     */
    static void downloadFile(String version, Consumer<File> onComplete) throws IOException {

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Downloading Update") {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {

                try {
                    progressIndicator.setText("connecting");
                    progressIndicator.setIndeterminate(false);
                    progressIndicator.setFraction(0);
                    File f = File.createTempFile("whalee-helper", "");
                    URLConnection uc = new URL(releaseBase + "whalee-helper-" + version + ".zip").openConnection();
                    int length = uc.getContentLength();
                    int downloadedBytes = 0;
                    byte[] buff = new byte[16 * 1024];
                    InputStream is = new URL(releaseBase + "whalee-helper-" + version + ".zip").openStream();
                    OutputStream fos = new FileOutputStream(f);
                    progressIndicator.setText("Downloading");
                    while (true) {
                        if (progressIndicator.isCanceled()) {
                            is.close();
                            fos.close();
                            f.delete();
                            return;
                        }
                        int len = is.read(buff);
                        if (len == -1) {
                            break;
                        }
                        downloadedBytes += len;
                        fos.write(buff, 0, len);
                        progressIndicator.setFraction(downloadedBytes * 1d / length);
                    }
                    is.close();
                    fos.close();
                    onComplete.accept(f);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    static void installUpdate(File f) throws IOException {

        String DIR = "whalee-helper";

        String classLocation = UpdateCheckService.class.getResource(UpdateCheckService.class.getSimpleName() + ".class").getFile();
        String jarPath = Utils.substringAfter(Utils.substringBefore(classLocation, "!"), "file:");
        jarPath = jarPath.replace("%20", " ");
        File pluginsDir = new File(Utils.substringBeforeLast(jarPath, "/" + DIR + "/"));

        // delete old dir
        deleteFile(new File(pluginsDir, DIR));

        ZipFile zip = new ZipFile(f);
        Enumeration<ZipEntry> enu = zip.getEntries();
        while (enu.hasMoreElements()) {
            ZipEntry entry = enu.nextElement();
            if (entry.isDirectory()) {
                new File(pluginsDir, entry.getName()).mkdirs();
            } else {
                File f0 = new File(pluginsDir, entry.getName());
                try (FileOutputStream fos = new FileOutputStream(f0)) {
                    Utils.copy(zip.getInputStream(entry), fos);
                }
            }
        }
        zip.close();
        showRestartPopup();
    }

    static void showRestartPopup() {
        Notification notify = new Notification("Update Notification", "New version found :", NotificationType.INFORMATION);
        notify.setTitle("whalee-helper update success!");
        notify.setContent("Restart now or later");

        notify.addAction(new RestartIDEAction("Restart"));
        Notifications.Bus.notify(notify);
    }


    static void deleteFile(File file) {
        if (file == null) return;

        if (file.isDirectory()) {
            File[] fs = file.listFiles();
            if (fs != null) {
                for (File f : fs) {
                    deleteFile(f);
                }
            }
        }
        file.delete();
    }

    static class DownloadUpdateAction extends AnAction {
        private final Notification notify;
        String version;

        public DownloadUpdateAction(@Nullable @NlsActions.ActionText String text, String version, Notification notify) {
            super(text);
            this.version = version;
            this.notify = notify;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            notify.hideBalloon();
            try {
                downloadFile(version, f -> {
                    try {
                        installUpdate(f);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class RestartIDEAction extends AnAction {

        public RestartIDEAction(String text) {
            super(text);
        }


        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            ApplicationManager.getApplication().restart();
        }
    }
}
