package com.aciderix.obbbootstrap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Injected into every game APK patched by the OBB Installer hub.
 *
 * Runs in the GAME's process (not the hub's), so it owns the game's UID and
 * can write freely to {@code Android/obb/<game.package>/} - bypassing every
 * scoped-storage / shared-UID / OEM restriction we hit at the hub level.
 *
 * Pure Java, no Kotlin stdlib, no AndroidX - keeps the injected dex tiny.
 */
public class ObbBootstrapProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        if (ctx == null) return true;
        try {
            File obbDir = ctx.getObbDir();
            if (obbDir == null) return true;
            //noinspection ResultOfMethodCallIgnored
            obbDir.mkdirs();
            AssetManager assets = ctx.getAssets();
            String[] files = assets.list("");
            if (files == null) return true;
            for (String name : files) {
                if (name == null) continue;
                String lower = name.toLowerCase();
                if (!lower.endsWith(".obb")) continue;
                File target = new File(obbDir, name);
                if (target.exists() && target.length() > 0L) continue;
                copy(assets, name, target);
            }
        } catch (Throwable ignored) {
            // Best effort: fail silently and let the game report missing OBB
            // exactly as it would on a botched manual sideload.
        }
        return true;
    }

    private static void copy(AssetManager assets, String assetName, File target) throws Exception {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = assets.open(assetName);
            out = new FileOutputStream(target);
            byte[] buf = new byte[256 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
        } finally {
            if (out != null) try { out.close(); } catch (Exception ignored) {}
            if (in != null) try { in.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
}
