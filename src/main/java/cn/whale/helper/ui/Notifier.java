package cn.whale.helper.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationGroupEP;
import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.Map;

public class Notifier {

    private final NotificationGroup NOTIFICATION_GROUP;
    static Map<String,Notifier> instanceMap=new HashMap<>();

    public Notifier(String displayId) {
        NOTIFICATION_GROUP = new NotificationGroup(displayId, NotificationDisplayType.BALLOON, true);
    }

    public static Notifier getInstance(String displayId) {
        Notifier ins = instanceMap.get(displayId);
        if (ins==null){
            ins=new Notifier(displayId);
            instanceMap.put(displayId, ins);
        }
        return ins;
    }


    public void info(Project project, String content) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.INFORMATION);
        notification.notify(project);
    }

    public void warn(Project project, String content) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.WARNING);
        notification.notify(project);
    }

    public void error(Project project, String content) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(content, NotificationType.ERROR);
        notification.notify(project);
    }
}
