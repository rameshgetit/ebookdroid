package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.definitions.LibPreferences;
import org.ebookdroid.common.settings.listeners.ILibSettingsChangeListener;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.HashSet;
import java.util.Set;

import org.emdev.utils.android.AndroidVersion;
import org.emdev.utils.filesystem.FileExtensionFilter;

public class LibSettings implements LibPreferences {

    private static LibSettings current;

    /* =============== Browser settings =============== */

    public final boolean useBookcase;

    public final Set<String> autoScanDirs;

    public final String searchBookQuery;

    public final FileExtensionFilter allowedFileTypes;

    private LibSettings() {
        SharedPreferences prefs = SettingsManager.prefs;
        /* =============== Browser settings =============== */
        useBookcase = USE_BOOK_CASE.getPreferenceValue(prefs);
        autoScanDirs = AUTO_SCAN_DIRS.getPreferenceValue(prefs);
        searchBookQuery = SEARCH_BOOK_QUERY.getPreferenceValue(prefs);
        allowedFileTypes = FILE_TYPE_FILTER.getPreferenceValue(prefs);
    }

    /* =============== Browser settings =============== */

    public boolean getUseBookcase() {
        return !AndroidVersion.is1x && useBookcase;
    }

    /* =============== */

    public static void init() {
        current = new LibSettings();
    }

    public static LibSettings current() {
        SettingsManager.lock.readLock().lock();
        try {
            return current;
        } finally {
            SettingsManager.lock.readLock().unlock();
        }
    }
    
    public static void changeAutoScanDirs(final String dir, final boolean add) {
        SettingsManager.lock.writeLock().lock();
        try {
            final Set<String> dirs = new HashSet<String>(current.autoScanDirs);
            if (add && dirs.add(dir) || dirs.remove(dir)) {
                final Editor edit = SettingsManager.prefs.edit();
                LibPreferences.AUTO_SCAN_DIRS.setPreferenceValue(edit, dirs);
                edit.commit();
                final LibSettings oldSettings = current;
                current = new LibSettings();
                applyLibSettingsChanges(oldSettings, current);
            }
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    public static void updateSearchBookQuery(final String searchQuery) {
        SettingsManager.lock.writeLock().lock();
        try {
            final Editor edit = SettingsManager.prefs.edit();
            LibPreferences.SEARCH_BOOK_QUERY.setPreferenceValue(edit, searchQuery);
            edit.commit();
            final LibSettings oldSettings = current;
            current = new LibSettings();
            applyLibSettingsChanges(oldSettings, current);
        } finally {
            SettingsManager.lock.writeLock().unlock();
        }
    }

    static Diff onSettingsChanged() {
        final LibSettings oldLibSettings = current;
        current = new LibSettings();
        return applyLibSettingsChanges(oldLibSettings, current);
    }

    public static LibSettings.Diff applyLibSettingsChanges(final LibSettings oldSettings, final LibSettings newSettings) {
        final LibSettings.Diff diff = new LibSettings.Diff(oldSettings, newSettings);
        final ILibSettingsChangeListener l = SettingsManager.listeners.getListener();
        l.onLibSettingsChanged(oldSettings, newSettings, diff);
        return diff;
    }

    public static class Diff {

        private static final int D_UseBookcase = 0x0001 << 12;
        private static final int D_AutoScanDirs = 0x0001 << 14;
        private static final int D_AllowedFileTypes = 0x0001 << 15;

        private int mask;
        private final boolean firstTime;

        public Diff(final LibSettings olds, final LibSettings news) {
            firstTime = olds == null;
            if (firstTime) {
                mask = 0xFFFFFFFF;
            } else if (news != null) {
                if (olds.getUseBookcase() != news.getUseBookcase()) {
                    mask |= D_UseBookcase;
                }
                if (!olds.autoScanDirs.equals(news.autoScanDirs)) {
                    mask |= D_AutoScanDirs;
                }
                if (!olds.allowedFileTypes.equals(news.allowedFileTypes)) {
                    mask |= D_AllowedFileTypes;
                }
            }
        }

        public boolean isFirstTime() {
            return firstTime;
        }

        public boolean isUseBookcaseChanged() {
            return 0 != (mask & D_UseBookcase);
        }

        public boolean isAutoScanDirsChanged() {
            return 0 != (mask & D_AutoScanDirs);
        }

        public boolean isAllowedFileTypesChanged() {
            return 0 != (mask & D_AllowedFileTypes);
        }
    }
}
