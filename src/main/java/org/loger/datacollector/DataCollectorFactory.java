package org.loger.datacollector;

import org.loger.Main;
import org.loger.session.ISessionManager;
import org.loger.session.SessionManager;

public class DataCollectorFactory {
    public static ISessionManager createSessionManager(Main plugin) {
        return new SessionManager(plugin);
    }
}
