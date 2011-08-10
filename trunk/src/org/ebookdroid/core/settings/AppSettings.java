package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.RotationType;
import org.ebookdroid.core.curl.PageAnimationType;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class AppSettings {

    private final SharedPreferences prefs;

    private Boolean tapScroll;

    private Integer tapSize;

    private Boolean singlePage;

    private Integer pagesInMemory;

    private Boolean nightMode;

    private PageAlign pageAlign;

    private Boolean fullScreen;

    private RotationType rotation;

    private Boolean showTitle;

    private Integer scrollHeight;

    private Boolean sliceLimit;

    private PageAnimationType animationType;

    private Boolean splitPages;

    private Boolean pageInTitle;

    AppSettings(final Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean getPageInTitle() {
        if (pageInTitle == null) {
            pageInTitle = prefs.getBoolean("pageintitle", true);
        }
        return pageInTitle;
    }

    public boolean getTapScroll() {
        if (tapScroll == null) {
            tapScroll = prefs.getBoolean("tapscroll", false);
        }
        return tapScroll;
    }

    public int getTapSize() {
        if (tapSize == null) {
            tapSize = getIntValue("tapsize", 10);
        }
        return tapSize.intValue();
    }

    boolean getSinglePage() {
        if (singlePage == null) {
            singlePage = prefs.getBoolean("singlepage", false);
        }
        return singlePage;
    }

    public int getPagesInMemory() {
        if (pagesInMemory == null) {
            pagesInMemory = getIntValue("pagesinmemory", 2);
        }
        return pagesInMemory.intValue();
    }

    public boolean getNightMode() {
        if (nightMode == null) {
            nightMode = prefs.getBoolean("nightmode", false);
        }
        return nightMode;
    }

    public void switchNightMode() {
        nightMode = !nightMode;
        final Editor edit = prefs.edit();
        edit.putBoolean("nightmode", nightMode);
        edit.commit();
    }

    PageAlign getPageAlign() {
        if (pageAlign == null) {
            final String align = prefs.getString("align", PageAlign.AUTO.getResValue());
            pageAlign = PageAlign.getByResValue(align);
            if (pageAlign == null) {
                pageAlign = PageAlign.AUTO;
            }
        }
        return pageAlign;
    }

    public boolean getFullScreen() {
        if (fullScreen == null) {
            fullScreen = prefs.getBoolean("fullscreen", false);
        }
        return fullScreen;
    }

    public RotationType getRotation() {
        if (rotation == null) {
            final String rotationStr = prefs.getString("rotation", RotationType.AUTOMATIC.getResValue());
            rotation = RotationType.getByResValue(rotationStr);
            if (rotation == null) {
                rotation = RotationType.AUTOMATIC;
            }
        }
        return rotation;
    }

    public boolean getShowTitle() {
        if (showTitle == null) {
            showTitle = prefs.getBoolean("title", true);
        }
        return showTitle;
    }

    public int getScrollHeight() {
        if (scrollHeight == null) {
            scrollHeight = getIntValue("scrollheight", 50);
        }
        return scrollHeight.intValue();
    }

    public Boolean getSliceLimit() {
        if (sliceLimit == null) {
            sliceLimit = prefs.getBoolean("slicelimit", true);
        }
        return sliceLimit;
    }

    PageAnimationType getAnimationType() {
        if (animationType == null) {
            animationType = PageAnimationType.get(prefs.getString("animationType", null));
        }
        return animationType;
    }

    boolean getSplitPages() {
        if (splitPages == null) {
            splitPages = prefs.getBoolean("splitpages", false);
        }
        return splitPages;
    }

    void clearPseudoBookSettings() {
        final Editor editor = prefs.edit();
        editor.remove("book");
        editor.remove("book_splitpages");
        editor.remove("book_singlepage");
        editor.remove("book_align");
        editor.remove("book_animationType");
        editor.commit();
    }

    void updatePseudoBookSettings(final BookSettings bs) {
        final Editor editor = prefs.edit();
        editor.putString("book", bs.getFileName());
        editor.putBoolean("book_splitpages", bs.getSplitPages());
        editor.putBoolean("book_singlepage", bs.getSinglePage());
        editor.putString("book_align", bs.getPageAlign().getResValue());
        editor.putString("book_animationType", bs.getAnimationType().getResValue());
        editor.commit();
    }

    void fillBookSettings(final BookSettings bs) {
        bs.splitPages = prefs.getBoolean("book_splitpages", getSplitPages());
        bs.singlePage = prefs.getBoolean("book_singlepage", getSinglePage());

        bs.pageAlign = PageAlign.getByResValue(prefs.getString("book_align", getPageAlign().getResValue()));
        if (bs.pageAlign == null) {
            bs.pageAlign = PageAlign.AUTO;
        }
        bs.animationType = PageAnimationType.get(prefs.getString("book_animationType", getAnimationType().getResValue()));
        if (bs.animationType == null) {
            bs.animationType = PageAnimationType.NONE;
        }

    }

    private int getIntValue(final String key, final int defaultValue) {
        final String str = prefs.getString(key, "" + defaultValue);
        int value = defaultValue;
        try {
            value = Integer.parseInt(str);
        } catch (final NumberFormatException e) {
        }
        return value;
    }

}
