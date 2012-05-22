package org.ebookdroid.opds;

public abstract class Entry {

    public final Feed parent;

    public final String id;
    public final String title;
    public final Content content;

    public Entry(final Feed parent, final String id, final String title, final Content content) {
        this.parent = parent;
        this.id = id;
        this.title = title;
        this.content = content;
    }

}
