package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.presentation.BooksAdapter;
import org.ebookdroid.core.presentation.FileListAdapter;
import org.ebookdroid.core.presentation.RecentAdapter;
import org.ebookdroid.core.settings.AppSettings;
import org.ebookdroid.core.settings.AppSettings.Diff;
import org.ebookdroid.core.settings.ISettingsChangeListener;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.utils.FileExtensionFilter;
import org.ebookdroid.core.views.BookcaseView;
import org.ebookdroid.core.views.LibraryView;
import org.ebookdroid.core.views.RecentBooksView;
import org.ebookdroid.utils.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class RecentActivity extends Activity implements IBrowserActivity, ISettingsChangeListener {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Core");

    private static final int VIEW_RECENT = 0;
    private static final int VIEW_LIBRARY = 1;

    private RecentAdapter recentAdapter;
    private FileListAdapter libraryAdapter;
    private BooksAdapter bookshelfAdapter;

    private ViewFlipper viewflipper;
    private ImageView libraryButton;

    private final Map<String, SoftReference<Bitmap>> thumbnails = new HashMap<String, SoftReference<Bitmap>>();

    private Bitmap cornerThmbBitmap;

    private Bitmap leftThmbBitmap;

    private Bitmap topThmbBitmap;

    private View.OnClickListener handler;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recent);

        cornerThmbBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bt_corner);
        leftThmbBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bt_left);
        topThmbBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bt_top);

        recentAdapter = new RecentAdapter(this);
        libraryAdapter = new FileListAdapter(this);
        bookshelfAdapter = new BooksAdapter(this, recentAdapter);

        libraryButton = (ImageView) findViewById(R.id.recentlibrary);

        viewflipper = (ViewFlipper) findViewById(R.id.recentflip);

        handler = new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                switch (v.getId()) {
                    case R.id.recentlibrary:
                        goLibrary(v);
                        break;
                    case R.id.recentbrowser:
                        goFileBrowser(v);
                        break;
                }
            }
        };

        final View recentBrowser = findViewById(R.id.recentbrowser);
        if (recentBrowser != null) {
            recentBrowser.setOnClickListener(handler);
        }

        SettingsManager.addListener(this);
        SettingsManager.applyAppSettingsChanges(null, SettingsManager.getAppSettings());

        final boolean shouldLoad = SettingsManager.getAppSettings().isLoadRecentBook();
        final BookSettings recent = SettingsManager.getRecentBook();
        final File file = recent != null ? new File(recent.fileName) : null;
        final boolean found = file != null ? file.exists()
                && SettingsManager.getAppSettings().getAllowedFileTypes().accept(file) : false;

        if (LCTX.isDebugEnabled()) {
            LCTX.d("Last book: " + (file != null ? file.getAbsolutePath() : "") + ", found: " + found
                    + ", should load: " + shouldLoad);
        }

        if (shouldLoad && found) {
            changeLibraryView(VIEW_RECENT);
            showDocument(Uri.fromFile(file));
        } else {
            changeLibraryView(recent != null ? VIEW_RECENT : VIEW_LIBRARY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SettingsManager.getAppSettings().getUseBookcase()) {
            bookshelfAdapter.startScan(SettingsManager.getAppSettings().getAllowedFileTypes());
            recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), SettingsManager.getAppSettings()
                    .getAllowedFileTypes());
        } else {
            if (viewflipper.getDisplayedChild() == VIEW_RECENT) {
                if (SettingsManager.getRecentBook() == null) {
                    changeLibraryView(VIEW_LIBRARY);
                } else {
                    recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), SettingsManager
                            .getAppSettings().getAllowedFileTypes());
                }

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.recentmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.recentmenu_cleanrecent:
                clearRecent(null);
                return true;
            case R.id.recentmenu_settings:
                showSettings(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clearRecent(final View view) {
        final class EmptyDialogButtonListener implements DialogInterface.OnClickListener {

            @Override
            public void onClick(final DialogInterface dialog, final int whichButton) {
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.clear_recent_title);
        builder.setMessage(R.string.clear_recent_text);
        builder.setPositiveButton(R.string.password_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int whichButton) {
                SettingsManager.deleteAllBookSettings();
                recentAdapter.clearBooks();

                libraryAdapter.notifyDataSetInvalidated();
            }
        });
        builder.setNegativeButton(R.string.password_cancel, new EmptyDialogButtonListener()).show();

    }

    public void showSettings(final View view) {
        libraryAdapter.stopScan();
        bookshelfAdapter.stopScan();
        final Intent i = new Intent(RecentActivity.this, SettingsActivity.class);
        startActivity(i);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void setCurrentDir(final File newDir) {
    }

    @Override
    public void showDocument(final Uri uri) {
        libraryAdapter.stopScan();
        bookshelfAdapter.stopScan();
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        final Class<? extends Activity> activity = Activities.getByUri(uri);
        if (activity != null) {
            intent.setClass(this, activity);
            startActivity(intent);
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void changeLibraryView(final int view) {
        if (!SettingsManager.getAppSettings().getUseBookcase()) {
            final FileExtensionFilter filter = SettingsManager.getAppSettings().getAllowedFileTypes();

            if (view == VIEW_LIBRARY) {
                viewflipper.setDisplayedChild(VIEW_LIBRARY);
                libraryButton.setImageResource(R.drawable.actionbar_recent);
                libraryAdapter.startScan(filter);
            } else {
                viewflipper.setDisplayedChild(VIEW_RECENT);
                libraryButton.setImageResource(R.drawable.actionbar_library);
                recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), filter);
            }
        }
    }

    public void goLibrary(final View view) {
        if (!SettingsManager.getAppSettings().getUseBookcase()) {
            if (viewflipper.getDisplayedChild() == VIEW_RECENT) {
                changeLibraryView(VIEW_LIBRARY);
            } else if (viewflipper.getDisplayedChild() == VIEW_LIBRARY) {
                changeLibraryView(VIEW_RECENT);
            }
        }
    }

    public void goFileBrowser(final View view) {
        final Intent myIntent = new Intent(RecentActivity.this, BrowserActivity.class);
        startActivity(myIntent);
    }

    @Override
    public void showProgress(final boolean show) {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.recentprogress);
        if (show) {
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }
    }

    @Override
    public void loadThumbnail(final String path, final ImageView imageView, final int defaultResID) {
        final String md5 = StringUtils.md5(path);
        final SoftReference<Bitmap> ref = thumbnails.get(md5);
        Bitmap bmp = ref != null ? ref.get() : null;
        if (bmp == null) {
            final File cacheDir = getContext().getFilesDir();
            final File thumbnailFile = new File(cacheDir, md5 + ".thumbnail");
            if (thumbnailFile.exists()) {

                final Bitmap tmpbmp = BitmapFactory.decodeFile(thumbnailFile.getPath());
                if (tmpbmp == null) {
                    thumbnailFile.delete();
                } else {
                    final int width = tmpbmp.getWidth() + 33;
                    final int height = tmpbmp.getHeight() + 23;
                    bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                    bmp.eraseColor(Color.TRANSPARENT);

                    final Canvas c = new Canvas(bmp);

                    c.drawBitmap(cornerThmbBitmap, null, new Rect(0, 0, 33, 23), null);
                    c.drawBitmap(topThmbBitmap, null, new Rect(33, 0, width, 23), null);
                    c.drawBitmap(leftThmbBitmap, null, new Rect(0, 23, 33, height), null);
                    c.drawBitmap(tmpbmp, null, new Rect(33, 23, width, height), null);

                    thumbnails.put(md5, new SoftReference<Bitmap>(bmp));
                }
            }
        }

        if (bmp != null) {
            imageView.setImageBitmap(bmp);
        } else {
            imageView.setImageResource(defaultResID);
        }
    }

    @Override
    public void onAppSettingsChanged(final AppSettings oldSettings, final AppSettings newSettings, final Diff diff) {
        if (diff.isUseBookcaseChanged()) {
            viewflipper.removeAllViews();

            if (SettingsManager.getAppSettings().getUseBookcase()) {
                libraryButton.setImageResource(R.drawable.actionbar_shelf);
                libraryButton.setOnClickListener(null);

                viewflipper.addView(new BookcaseView(this, bookshelfAdapter), 0);

                recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), SettingsManager.getAppSettings()
                        .getAllowedFileTypes());
            } else {
                libraryButton.setImageResource(R.drawable.actionbar_library);
                libraryButton.setOnClickListener(handler);

                viewflipper.addView(new RecentBooksView(this, recentAdapter), VIEW_RECENT);
                viewflipper.addView(new LibraryView(this, libraryAdapter), VIEW_LIBRARY);
            }
        }
    }

    @Override
    public void onBookSettingsChanged(final BookSettings oldSettings, final BookSettings newSettings,
            final org.ebookdroid.core.settings.books.BookSettings.Diff diff, final Diff appDiff) {

    }
}
