package cn.whale.helper.listener;

import cn.whale.helper.utils.CloserHolder;
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
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UpdateCheckService implements AppLifecycleListener {

    // the directory where this plugin installed at plugins
    static final String DIR = "whalee-helper";
    static final String releaseBase = "https://whale-alivia.oss-cn-hangzhou.aliyuncs.com/whalee-helper/";

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

    public static synchronized void checkUpdate() {
        try (CloserHolder ch = new CloserHolder()) {

            @Nullable IdeaPluginDescriptor plugin = getPlugin();
            if (plugin == null) {
                return;
            }
            InputStream is = new URL(releaseBase + "latest.txt").openStream();
            ch.add(is);
            String text = Utils.readText(is).trim();
            @Nullable SemVer newVersion = SemVer.parseFromText(text);
            @Nullable SemVer curVersion = SemVer.parseFromText(plugin.getVersion());
            if (newVersion == null) {
                return;
            }
            if (curVersion != null && newVersion.compareTo(curVersion) <= 0) {
                return;
            }
            showPopup(text, plugin.getVersion());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void showPopup(String newVersion, String oldVersion) {
        Notification notify = new Notification("Update Notification", "New version found :", NotificationType.INFORMATION);
        notify.setTitle(DIR + " update!");
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

                try (CloserHolder ch = new CloserHolder()) {
                    progressIndicator.setText("connecting");
                    progressIndicator.setIndeterminate(false);
                    progressIndicator.setFraction(0);
                    File f = File.createTempFile(DIR, "");
                    URLConnection uc = new URL(releaseBase + DIR + "-" + version + ".zip").openConnection();
                    int length = uc.getContentLength();
                    int downloadedBytes = 0;
                    byte[] buff = new byte[16 * 1024];
                    InputStream is = uc.getInputStream();
                    OutputStream fos = new FileOutputStream(f);
                    ch.add(is, fos);
                    progressIndicator.setText("Downloading");
                    while (true) {
                        if (progressIndicator.isCanceled()) {
                            ch.close();
                            Utils.deleteFile(f, false);
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
                    ch.close();
                    onComplete.accept(f);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    static void installUpdate(File f) throws IOException {

        String classLocation = UpdateCheckService.class.getResource(UpdateCheckService.class.getSimpleName() + ".class").getFile();
        String jarPath = Utils.substringAfter(Utils.substringBefore(classLocation, "!"), "file:");
        jarPath = jarPath.replace("%20", " ");
        File pluginsDir = new File(Utils.substringBeforeLast(jarPath, "/" + DIR + "/"));

        // delete old dir
        Utils.deleteFile(new File(pluginsDir, DIR), true);

        ZipFile zip = new ZipFile(f);
        Enumeration<? extends ZipEntry> enu = zip.entries();
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
        Utils.deleteFile(f, false);
        showRestartPopup();
    }

    static void showRestartPopup() {
        Notification notify = new Notification("Update Notification", "New version found :", NotificationType.INFORMATION);
        notify.setTitle(DIR + " update success!");
        notify.setContent("Restart now or later");

        notify.addAction(new RestartIDEAction("Restart"));
        Notifications.Bus.notify(notify);
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
