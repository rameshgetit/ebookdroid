package org.ebookdroid.ui.library;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.cache.ThumbnailFile;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.LibSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.listeners.ILibSettingsChangeListener;
import org.ebookdroid.ui.library.adapters.BookNode;
import org.ebookdroid.ui.library.adapters.BooksAdapter;
import org.ebookdroid.ui.library.adapters.FileListAdapter;
import org.ebookdroid.ui.library.adapters.RecentAdapter;
import org.ebookdroid.ui.library.dialogs.FolderDlg;
import org.ebookdroid.ui.opds.OPDSActivity;
import org.ebookdroid.ui.settings.SettingsUI;
import org.ebookdroid.ui.viewer.ViewerActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionController;
import org.emdev.ui.actions.ActionDialogBuilder;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.ActionMethodDef;
import org.emdev.ui.actions.ActionTarget;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.actions.params.EditableValue;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.progress.UIFileCopying;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.filesystem.FileExtensionFilter;
import org.emdev.utils.filesystem.PathFromUri;

@ActionTarget(
// actions
actions = {
        // start
        @ActionMethodDef(id = R.id.recentmenu_cleanrecent, method = "showClearRecentDialog"),
        @ActionMethodDef(id = R.id.recent_showbrowser, method = "goFileBrowser"),
        @ActionMethodDef(id = R.id.recent_showlibrary, method = "goLibrary"),
        @ActionMethodDef(id = R.id.mainmenu_settings, method = "showSettings"),
        @ActionMethodDef(id = R.id.actions_clearRecent, method = "doClearRecent"),
        @ActionMethodDef(id = R.id.ShelfCaption, method = "showSelectShelfDlg"),
        @ActionMethodDef(id = R.id.actions_selectShelf, method = "selectShelf"),
        @ActionMethodDef(id = R.id.ShelfLeftButton, method = "selectPrevShelf"),
        @ActionMethodDef(id = R.id.ShelfRightButton, method = "selectNextShelf"),
        @ActionMethodDef(id = R.id.recentmenu_searchBook, method = "showSearchDlg"),
        @ActionMethodDef(id = R.id.actions_searchBook, method = "searchBook"),
        @ActionMethodDef(id = R.id.mainmenu_opds, method = "goOPDSBrowser")
// finish
})
public class RecentActivityController extends ActionController<RecentActivity> implements IBrowserActivity,
        ILibSettingsChangeListener {

    public final LogContext LCTX;

    private static final AtomicLong SEQ = new AtomicLong();

    private static final int CLEAR_RECENT_LIST = 0;
    private static final int DELETE_BOOKMARKS = 1;
    private static final int DELETE_BOOK_SETTINGS = 2;
    private static final int ERASE_DISK_CACHE = 3;

    private RecentAdapter recentAdapter;
    private FileListAdapter libraryAdapter;
    private BooksAdapter bookshelfAdapter;

    private boolean firstResume = true;

    public RecentActivityController(final RecentActivity activity) {
        super(activity);
        LCTX = LogContext.ROOT.lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement());
    }

    public void onCreate() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onCreate(): " + getManagedComponent());
        }

        recentAdapter = new RecentAdapter(this);
        bookshelfAdapter = new BooksAdapter(this, recentAdapter);
        libraryAdapter = new FileListAdapter(bookshelfAdapter);

        SettingsManager.addListener(this);
        SettingsManager.applyLibSettingsChanges(null, SettingsManager.getLibSettings());

        final BookSettings recent = SettingsManager.getRecentBook();

        if (firstResume) {
            final boolean shouldLoad = SettingsManager.getAppSettings().loadRecent;
            final File file = (recent != null && recent.fileName != null) ? new File(recent.fileName) : null;
            final boolean found = file != null ? file.exists()
                    && SettingsManager.getLibSettings().allowedFileTypes.accept(file) : false;

            if (LCTX.isDebugEnabled()) {
                LCTX.d("Last book: " + (file != null ? file.getAbsolutePath() : "") + ", found: " + found
                        + ", should load: " + shouldLoad);
            }

            if (shouldLoad && found) {
                changeLibraryView(RecentActivity.VIEW_RECENT);
                showDocument(Uri.fromFile(file));
                return;
            }
        }

        changeLibraryView(recent != null ? RecentActivity.VIEW_RECENT : RecentActivity.VIEW_LIBRARY);
    }

    public void onRestore(final RecentActivity activity) {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onRestore(): " + activity);
        }
        setManagedComponent(activity);

        SettingsManager.applyLibSettingsChanges(null, SettingsManager.getLibSettings());
        final BookSettings recent = SettingsManager.getRecentBook();
        changeLibraryView(recent != null ? RecentActivity.VIEW_RECENT : RecentActivity.VIEW_LIBRARY);
    }

    protected void onResume() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onResume()");
        }

        final LibSettings libSettings = SettingsManager.getLibSettings();
        if (libSettings.getUseBookcase()) {
            if (firstResume) {
                bookshelfAdapter.startScan();
            }
            recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), libSettings.allowedFileTypes);
        } else {
            if (getManagedComponent().getViewMode() == RecentActivity.VIEW_RECENT) {
                if (SettingsManager.getRecentBook() == null) {
                    changeLibraryView(RecentActivity.VIEW_LIBRARY);
                } else {
                    recentAdapter
                            .setBooks(SettingsManager.getAllBooksSettings().values(), libSettings.allowedFileTypes);
                }
            }
        }

        firstResume = false;
    }

    protected void onPause() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onPause()");
        }
    }

    protected void onDestroy() {
        if (LCTX.isDebugEnabled()) {
            LCTX.d("onDestroy()");
        }
    }

    @ActionMethod(ids = R.id.recentmenu_cleanrecent)
    public void showClearRecentDialog(final ActionEx action) {
        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), this);

        builder.setTitle(R.string.clear_recent_title);
        builder.setMultiChoiceItems(R.array.list_clear_recent_mode, R.id.actions_clearRecent);
        builder.setPositiveButton(R.id.actions_clearRecent);
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_clearRecent)
    public void doClearRecent(final ActionEx action) {
        if (action.isDialogItemSelected(ERASE_DISK_CACHE)) {
            CacheManager.clear();
            recentAdapter.notifyDataSetInvalidated();
            libraryAdapter.notifyDataSetInvalidated();
        }

        if (action.isDialogItemSelected(DELETE_BOOK_SETTINGS)) {
            SettingsManager.deleteAllBookSettings();
            recentAdapter.clearBooks();
            libraryAdapter.notifyDataSetInvalidated();
        } else {
            if (action.isDialogItemSelected(CLEAR_RECENT_LIST)) {
                SettingsManager.clearAllRecentBookSettings();
                recentAdapter.clearBooks();
                libraryAdapter.notifyDataSetInvalidated();
            }
            if (action.isDialogItemSelected(DELETE_BOOKMARKS)) {
                SettingsManager.deleteAllBookmarks();
            }
        }

    }

    @ActionMethod(ids = R.id.recentmenu_searchBook)
    public void showSearchDlg(final ActionEx action) {
        final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), this);

        final EditText input = new EditText(getManagedComponent());
        input.setSingleLine();
        input.setText(LengthUtils.safeString(bookshelfAdapter.getSearchQuery()));
        input.selectAll();

        builder.setTitle(R.string.search_book_dlg_title);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.search_go, R.id.actions_searchBook,
                new EditableValue("input", input));
        builder.setNegativeButton();
        builder.show();
    }

    @ActionMethod(ids = R.id.actions_searchBook)
    public void searchBook(final ActionEx action) {
        final Editable value = action.getParameter("input");
        final String searchQuery = value.toString();
        if (bookshelfAdapter.startSearch(searchQuery)) {
            if (SettingsManager.getLibSettings().getUseBookcase()) {
                getManagedComponent().showBookshelf(BooksAdapter.SEARCH_INDEX);
            }
        }
    }

    @ActionMethod(ids = R.id.mainmenu_settings)
    public void showSettings(final ActionEx action) {
        libraryAdapter.stopScan();
        bookshelfAdapter.stopScan();
        SettingsUI.showAppSettings(getManagedComponent());
    }

    @ActionMethod(ids = { R.id.bookmenu_copy, R.id.bookmenu_move })
    public void copyBook(final ActionEx action) {
        final BookNode book = action.getParameter("source");
        if (book == null) {
            return;
        }
        int id = action.id == R.id.bookmenu_copy ? R.id.actions_doCopyBook : R.id.actions_doMoveBook;
        getOrCreateAction(id).putValue("source", book);

        final Intent intent = new Intent("copyBook", Uri.fromFile(new File(book.path)), getManagedComponent(),
                FolderDlg.class);
        intent.putExtra(AbstractActionActivity.ACTIVITY_RESULT_ACTION_ID, id);

        getManagedComponent().startActivityForResult(intent, 1);
    }

    @ActionMethod(ids = { R.id.actions_doCopyBook, R.id.actions_doMoveBook })
    public void doCopyBook(final ActionEx action) {
        Intent data = action.getParameter("activityResultData");
        File targetFolder = new File(PathFromUri.retrieve(getManagedComponent().getContentResolver(), data.getData()));
        BookNode book = action.getParameter("source");

        new CopyBookTask(targetFolder, action.id == R.id.actions_doMoveBook).execute(book);
    }

    private class CopyBookTask extends AsyncTask<BookNode, String, File> implements IProgressIndicator {

        private final File targetFolder;
        private final boolean removeOrigin;

        private ProgressDialog progressDialog;
        private BookNode book;
        private File origin;

        public CopyBookTask(final File targetFolder, final boolean removeOrigin) {
            this.targetFolder = targetFolder;
            this.removeOrigin = removeOrigin;
        }

        @Override
        protected void onPreExecute() {
            onProgressUpdate(getManagedComponent().getResources().getString(R.string.opds_connecting));
        }

        @Override
        protected File doInBackground(final BookNode... params) {
            book = params[0];
            origin = new File(book.path);
            final File target = new File(targetFolder, origin.getName());

            try {
                final UIFileCopying worker = new UIFileCopying(R.string.opds_loading_book, 256 * 1024, this);
                final BufferedInputStream in = new BufferedInputStream(new FileInputStream(origin), 256 * 1024);
                final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target), 256 * 1024);

                worker.copy(origin.length(), in, out);

                return target;
            } catch (final Throwable th) {
                th.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final File result) {
            if (progressDialog != null) {
                try {
                    progressDialog.dismiss();
                } catch (final Throwable th) {
                }
            }
            if (result != null && book != null && origin != null) {
                try {
                    BookSettings bs = null;
                    if (book.settings != null) {
                        bs = SettingsManager.copyBookSettings(result, book.settings);
                    }
                    if (removeOrigin) {
                        if (bs != null) {
                            recentAdapter.replaceBook(book, bs);
                            SettingsManager.deleteBookSettings(book.settings);
                            book.settings = null;
                        }
                        origin.delete();
                    } else {
                        if (bs != null) {
                            recentAdapter.replaceBook(null, bs);
                        }
                    }
                } catch (Throwable th) {
                    // TODO Auto-generated catch block
                    th.printStackTrace();
                }

                Toast.makeText(EBookDroidApp.context, "Book download complete: " + result.getAbsolutePath(), 0).show();
            } else {
                Toast.makeText(EBookDroidApp.context, "Book download failed", 0).show();
            }
        }

        @Override
        public void setProgressDialogMessage(final int resourceID, final Object... args) {
            publishProgress(getManagedComponent().getResources().getString(resourceID, args));
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            final int length = LengthUtils.length(values);
            if (length == 0) {
                return;
            }
            final String last = values[length - 1];
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = ProgressDialog.show(getManagedComponent(), "", last, true);
                // progressDialog.setCancelable(true);
                // progressDialog.setCanceledOnTouchOutside(true);
                // progressDialog.setOnCancelListener(this);
            } else {
                progressDialog.setMessage(last);
            }
        }

    }

    @Override
    public Context getContext() {
        return getManagedComponent();
    }

    @Override
    public Activity getActivity() {
        return getManagedComponent();
    }

    @Override
    public void setCurrentDir(final File newDir) {
    }

    @Override
    public void showDocument(final Uri uri) {
        libraryAdapter.stopScan();
        bookshelfAdapter.stopScan();
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(getManagedComponent(), ViewerActivity.class);
        getManagedComponent().startActivity(intent);
    }

    @ActionMethod(ids = R.id.ShelfCaption)
    public void showSelectShelfDlg(final ActionEx action) {
        final List<String> names = bookshelfAdapter.getListNames();

        if (LengthUtils.isNotEmpty(names)) {
            final ActionDialogBuilder builder = new ActionDialogBuilder(getContext(), this);
            builder.setTitle(R.string.bookcase_shelves);
            builder.setItems(names.toArray(new String[names.size()]), this.getOrCreateAction(R.id.actions_selectShelf));
            builder.show();
        }
    }

    @ActionMethod(ids = R.id.actions_selectShelf)
    public void selectShelf(final ActionEx action) {
        final Integer item = action.getParameter(IActionController.DIALOG_ITEM_PROPERTY);
        getManagedComponent().showBookshelf(item);
    }

    @ActionMethod(ids = R.id.ShelfLeftButton)
    public void selectPrevShelf(final ActionEx action) {
        getManagedComponent().showPrevBookshelf();
    }

    @ActionMethod(ids = R.id.ShelfRightButton)
    public void selectNextShelf(final ActionEx action) {
        getManagedComponent().showNextBookshelf();
    }

    @ActionMethod(ids = R.id.recent_showlibrary)
    public void goLibrary(final ActionEx action) {
        if (!SettingsManager.getLibSettings().getUseBookcase()) {
            final int viewMode = getManagedComponent().getViewMode();
            if (viewMode == RecentActivity.VIEW_RECENT) {
                changeLibraryView(RecentActivity.VIEW_LIBRARY);
            } else if (viewMode == RecentActivity.VIEW_LIBRARY) {
                changeLibraryView(RecentActivity.VIEW_RECENT);
            }
        }
    }

    @ActionMethod(ids = R.id.recent_showbrowser)
    public void goFileBrowser(final ActionEx action) {
        final Intent myIntent = new Intent(getManagedComponent(), BrowserActivity.class);
        getManagedComponent().startActivity(myIntent);
    }

    @ActionMethod(ids = R.id.mainmenu_opds)
    public void goOPDSBrowser(final ActionEx action) {
        final Intent myIntent = new Intent(getManagedComponent(), OPDSActivity.class);
        getManagedComponent().startActivity(myIntent);
    }

    @Override
    public void showProgress(final boolean show) {
        final ProgressBar progress = (ProgressBar) getManagedComponent().findViewById(R.id.recentprogress);
        if (show) {
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }
    }

    @Override
    public void loadThumbnail(final String path, final ImageView imageView, final int defaultResID) {
        final ThumbnailFile tf = CacheManager.getThumbnailFile(path);
        final Bitmap bmp = tf.getImage();
        if (bmp != null) {
            imageView.setImageBitmap(bmp);
        }
    }

    @Override
    public void onLibSettingsChanged(final LibSettings oldSettings, final LibSettings newSettings,
            final LibSettings.Diff diff) {
        final FileExtensionFilter filter = newSettings.allowedFileTypes;
        if (diff.isUseBookcaseChanged()) {

            if (newSettings.getUseBookcase()) {
                recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), filter);
                getManagedComponent().showBookcase(bookshelfAdapter, recentAdapter);
            } else {
                getManagedComponent().showLibrary(libraryAdapter, recentAdapter);
            }
            return;
        }

        if (diff.isAutoScanDirsChanged()) {
            if (newSettings.getUseBookcase()) {
                bookshelfAdapter.startScan();
            } else {
                libraryAdapter.startScan();
            }
            return;
        }
        if (diff.isAllowedFileTypesChanged()) {
            recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), filter);
            if (newSettings.getUseBookcase()) {
                bookshelfAdapter.startScan();
            } else {
                libraryAdapter.startScan();
            }
        }
    }

    public void changeLibraryView(final int view) {
        if (!SettingsManager.getLibSettings().getUseBookcase()) {
            getManagedComponent().changeLibraryView(view);
            if (view == RecentActivity.VIEW_LIBRARY) {
                libraryAdapter.startScan();
            } else {
                final FileExtensionFilter filter = SettingsManager.getLibSettings().allowedFileTypes;
                recentAdapter.setBooks(SettingsManager.getAllBooksSettings().values(), filter);
            }
        }
    }
}
