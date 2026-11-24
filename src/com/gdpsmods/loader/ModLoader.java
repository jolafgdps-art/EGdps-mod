package com.gdpsmods.loader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import org.json.JSONObject;

public class ModLoader {
    public static final String VERSION = "1.0.0";
    public static final String REPO = "jolafgdps-art/EGdps-mod";
    static boolean started = false;

    public static void init(final Activity a) {
        if (started) return;
        started = true;
        final SharedPreferences p = a.getSharedPreferences("modloader", 0);
        ModManager.init(a, p);

        boolean crashed = p.getBoolean("booting", false);
        String culprit = p.getString("current", null);
        boolean safeMode = false;

        if (crashed) {
            if (culprit != null) {
                p.edit().putBoolean("en_" + culprit, false).commit();
                ModManager.log("CUARENTENA: " + culprit + " crasheo el juego y fue desactivado");
                toast(a, "Mod en cuarentena: " + culprit);
            } else {
                safeMode = true;
                ModManager.log("MODO SEGURO: el juego se cerro antes y no se sabe que mod fue");
                toast(a, "Modo seguro: mods no cargados esta vez");
            }
            p.edit().remove("booting").remove("current").commit();
        }

        ModManager.scan();

        if (!safeMode) {
            p.edit().putBoolean("booting", true).commit();
            for (ModInfo m : ModManager.MODS) if (m.enabled) ModManager.load(m);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                public void run() { p.edit().remove("booting").commit(); }
            }, 15000); // si el juego sobrevive 15s, se considera arranque sano
        }

        ModsUI.install(a);
        checkUpdate(a);
    }

    static void toast(final Activity a, final String s) {
        a.runOnUiThread(new Runnable() {
            public void run() { Toast.makeText(a, s, Toast.LENGTH_LONG).show(); }
        });
    }

    static void checkUpdate(final Activity a) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String tag = new JSONObject(ModManager.http(
                        "https://api.github.com/repos/" + REPO + "/releases/latest")).getString("tag_name");
                    if (!tag.replace("v", "").equals(VERSION))
                        toast(a, "Nueva version del loader: " + tag);
                } catch (Exception e) { /* sin red o sin releases */ }
            }
        }).start();
    }
          }
