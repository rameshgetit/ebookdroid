package org.ebookdroid.ui.opds;

import org.ebookdroid.R;
import org.ebookdroid.opds.model.Book;
import org.ebookdroid.opds.model.Entry;
import org.ebookdroid.opds.model.Feed;
import org.ebookdroid.opds.model.Link;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import java.util.concurrent.atomic.AtomicLong;

import org.emdev.common.android.AndroidVersion;
import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.uimanager.IUIManager;

public class OPDSActivity extends AbstractActionActivity<OPDSActivity, OPDSActivityController> {

    public final LogContext LCTX;

    private static final AtomicLong SEQ = new AtomicLong();

    ExpandableListView list;

    public OPDSActivity() {
        LCTX = LogManager.root().lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement(), true);
    }

    @Override
    protected OPDSActivityController createController() {
        return new OPDSActivityController(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onCreate()");
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.opds);
        setActionForView(R.id.opdsaddfeed);

        list = (ExpandableListView) findViewById(R.id.opdslist);
        list.setGroupIndicator(null);
        list.setChildIndicator(null);
        this.registerForContextMenu(list);

        final OPDSActivityController c = restoreController();
        if (c != null) {
            c.onRestore(this);
        } else {
            getController().onCreate();
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPostCreate()");
        }
        super.onPostCreate(savedInstanceState);
        getController().onPostCreate();
    }

    @Override
    protected void onDestroy() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy()");
        }
        super.onDestroy();
        getController().onDestroy(isFinishing());
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            final Feed current = getController().adapter.getCurrentFeed();
            if (current == null) {
                finish();
            } else {
                getController().setCurrentFeed(current.parent);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opdsmenu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        if (menuInfo instanceof ExpandableListContextMenuInfo) {
            final ExpandableListContextMenuInfo cmi = (ExpandableListContextMenuInfo) menuInfo;
            final int type = ExpandableListView.getPackedPositionType(cmi.packedPosition);
            final int groupPosition = ExpandableListView.getPackedPositionGroup(cmi.packedPosition);
            final int childPosition = ExpandableListView.getPackedPositionChild(cmi.packedPosition);
            // System.out.println("OPDSActivity.onCreateContextMenu(): " + type + ", " + groupPosition + ", "
            // + childPosition);
            switch (type) {
                case ExpandableListView.PACKED_POSITION_TYPE_NULL:
                    onCreateContextMenu(menu);
                    return;
                case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                    final Entry entry = getController().adapter.getGroup(groupPosition);
                    if (entry instanceof Feed) {
                        onCreateFeedContextMenu(menu, (Feed) entry);
                    } else if (entry instanceof Book) {
                        onCreateBookContextMenu(menu, (Book) entry);
                    }
                    return;
                case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                    final Entry group = getController().adapter.getGroup(groupPosition);
                    final Object child = getController().adapter.getChild(groupPosition, childPosition);
                    if (child instanceof Link) {
                        onCreateLinkContextMenu(menu, (Book) group, (Link) child);
                    } else if (child instanceof Feed) {
                        onCreateFacetContextMenu(menu, (Feed) group, (Feed) child);
                    }
                    return;
            }
        }
        onCreateContextMenu(menu);
    }

    protected void onCreateContextMenu(final ContextMenu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_defmenu, menu);

        final Feed feed = getController().adapter.getCurrentFeed();
        menu.setHeaderTitle(getFeedTitle(feed));
        updateMenuItems(menu, feed);
    }

    protected void onCreateFeedContextMenu(final ContextMenu menu, final Feed feed) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_feedmenu, menu);

        menu.setHeaderTitle(getFeedTitle(feed));
        updateMenuItems(menu, feed.parent);

        setMenuParameters(menu, new Constant("feed", feed));
    }

    protected void onCreateFacetContextMenu(final ContextMenu menu, final Feed feed, final Feed facet) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_facetmenu, menu);

        menu.setHeaderTitle(getFeedTitle(facet));
        updateMenuItems(menu, feed.parent);

        setMenuParameters(menu, new Constant("feed", facet));
    }

    protected void onCreateBookContextMenu(final ContextMenu menu, final Book book) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_bookmenu, menu);

        menu.setHeaderTitle(book.title);
        setMenuParameters(menu, new Constant("book", book));
    }

    protected void onCreateLinkContextMenu(final ContextMenu menu, final Book book, final Link link) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opds_bookmenu, menu);

        menu.setHeaderTitle(book.title);
        setMenuParameters(menu, new Constant("book", book), new Constant("link", link));
    }

    protected void updateMenuItems(final Menu menu) {
        updateMenuItems(menu, getController().adapter.getCurrentFeed());
    }

    protected void updateMenuItems(final Menu menu, final Feed feed) {
        final boolean canUp = feed != null;
        final boolean canNext = feed != null && feed.next != null;
        final boolean canPrev = feed != null && feed.prev != null;

        if (menu != null) {
            if (AndroidVersion.lessThan3x) {
                setMenuItemEnabled(menu, canUp, R.id.opdsupfolder, R.drawable.opds_menu_nav_up_enabled,
                        R.drawable.opds_menu_nav_up_disabled);
                setMenuItemEnabled(menu, canNext, R.id.opdsnextfolder, R.drawable.opds_menu_nav_next_enabled,
                        R.drawable.opds_menu_nav_next_disabled);
                setMenuItemEnabled(menu, canPrev, R.id.opdsprevfolder, R.drawable.opds_menu_nav_prev_enabled,
                        R.drawable.opds_menu_nav_prev_disabled);
            } else {
                setMenuItemEnabled(menu, canUp, R.id.opdsupfolder, R.drawable.opds_actionbar_nav_up_enabled,
                        R.drawable.opds_actionbar_nav_up_disabled);
                setMenuItemEnabled(menu, canNext, R.id.opdsnextfolder, R.drawable.opds_actionbar_nav_next_enabled,
                        R.drawable.opds_actionbar_nav_next_disabled);
                setMenuItemEnabled(menu, canPrev, R.id.opdsprevfolder, R.drawable.opds_actionbar_nav_prev_enabled,
                        R.drawable.opds_actionbar_nav_prev_disabled);
            }
        }
    }

    protected String getFeedTitle(final Feed feed) {
        return feed != null ? feed.title : getResources().getString(R.string.opds);
    }

    protected void onCurrrentFeedChanged(final Feed feed) {
        IUIManager.instance.invalidateOptionsMenu(this);
        setTitle(getFeedTitle(feed));
        findViewById(R.id.opdsaddfeed).setVisibility(feed != null ? View.GONE : View.VISIBLE);
    }

    protected void onFeedLoaded(final Feed feed) {
        IUIManager.instance.invalidateOptionsMenu(this);
    }
}
