package org.ebookdroid.core.settings;

import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.curl.PageAnimationType;
import org.ebookdroid.core.events.CurrentPageListener;

public class BookSettings implements CurrentPageListener {

    final String fileName;

    long lastUpdated;

    int currentDocPage;

    int currentViewPage;

    boolean splitPages;

    boolean singlePage;

    PageAlign pageAlign = PageAlign.AUTO;

    PageAnimationType animationType = PageAnimationType.NONE;

    BookSettings(final String fileName) {
        this(fileName, null);
    }

    BookSettings(final String fileName, AppSettings appSettings) {
        this.fileName = fileName;
        this.lastUpdated = System.currentTimeMillis();
        if (appSettings != null) {
            appSettings.fillBookSettings(this);
        }
    }

    @Override
    public void currentPageChanged(int docPageIndex, int viewPageIndex) {
        this.currentDocPage = docPageIndex;
        this.currentViewPage = viewPageIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public int getCurrentDocPage() {
        return currentDocPage;
    }

    public int getCurrentViewPage() {
        return currentViewPage;
    }

    public boolean getSinglePage() {
        return singlePage;
    }

    public PageAlign getPageAlign() {
        return pageAlign;
    }

    public boolean getSplitPages() {
        return splitPages;
    }

    public PageAnimationType getAnimationType() {
        return animationType;
    }
}
