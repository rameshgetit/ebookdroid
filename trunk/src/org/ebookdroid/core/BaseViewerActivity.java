package org.ebookdroid.core;

import org.ebookdroid.R;
import org.ebookdroid.core.actions.ActionController;
import org.ebookdroid.core.actions.ActionEx;
import org.ebookdroid.core.actions.ActionMethod;
import org.ebookdroid.core.actions.ActionMethodDef;
import org.ebookdroid.core.actions.ActionTarget;
import org.ebookdroid.core.actions.IActionController;
import org.ebookdroid.core.actions.params.Constant;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.events.DecodingProgressListener;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.core.settings.AppSettings;
import org.ebookdroid.core.settings.BookSettingsActivity;
import org.ebookdroid.core.settings.ISettingsChangeListener;
import org.ebookdroid.core.settings.SettingsActivity;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.settings.books.Bookmark;
import org.ebookdroid.core.touch.TouchManager;
import org.ebookdroid.core.utils.PathFromUri;
import org.ebookdroid.core.views.PageViewZoomControls;
import org.ebookdroid.utils.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ActionTarget(
// action list
actions = {
        // start
        @ActionMethodDef(id = R.id.actions_addBookmark, method = "addBookmark"),
        @ActionMethodDef(id = R.id.mainmenu_close, method = "closeActivity"),
        @ActionMethodDef(id = R.id.actions_gotoOutlineItem, method="gotoOutlineItem"),
        @ActionMethodDef(id = R.id.actions_redecodingWithPassord, method="redecodingWithPassord"),
        @ActionMethodDef(id = R.id.mainmenu_settings, method="showAppSettings"),
        @ActionMethodDef(id = R.id.mainmenu_bookmark, method="showBookmarkDialog"),
        @ActionMethodDef(id = R.id.mainmenu_booksettings, method="showBookSettings"),
        @ActionMethodDef(id = R.id.mainmenu_goto_page, method="showDialog"),
        @ActionMethodDef(id = R.id.mainmenu_outline, method="showOutline"),
        @ActionMethodDef(id = R.id.mainmenu_nightmode, method="toggleNightMode"),
        @ActionMethodDef(id = R.id.mainmenu_zoom, method="toggleZoomControls"),
// finish
})
public abstract class BaseViewerActivity extends Activity implements IViewerActivity, DecodingProgressListener,
        CurrentPageListener, ISettingsChangeListener {

    public static final LogContext LCTX = LogContext.ROOT.lctx("Core");

    private static final int DIALOG_GOTO = 0;

    public static final DisplayMetrics DM = new DisplayMetrics();

    private BaseDocumentView view;

    private final AtomicReference<IDocumentViewController> ctrl = new AtomicReference<IDocumentViewController>(
            new EmptyContoller());

    private Toast pageNumberToast;

    private ZoomModel zoomModel;

    private PageViewZoomControls zoomControls;

    private FrameLayout frameLayout;

    private DecodingProgressModel progressModel;

    private DocumentModel documentModel;

    private String currentFilename;

    private ActionController<BaseViewerActivity> actions;

    /**
     * Instantiates a new base viewer activity.
     */
    public BaseViewerActivity() {
        super();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindowManager().getDefaultDisplay().getMetrics(DM);

        SettingsManager.addListener(this);

        actions = new ActionController<BaseViewerActivity>(this, this);
        actions.createAction(R.id.mainmenu_goto_page, new Constant("dialogId", DIALOG_GOTO));

        frameLayout = createMainContainer();
        view = new BaseDocumentView(this);

        initActivity();

        initView();
    }

    @Override
    protected void onDestroy() {
        if (documentModel != null) {
            documentModel.recycle();
            documentModel = null;
        }
        SettingsManager.removeListener(this);
        super.onDestroy();
    }

    private void initActivity() {
        SettingsManager.applyAppSettingsChanges(null, SettingsManager.getAppSettings());
    }

    private void initView() {

        getView().setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        frameLayout.addView(getView());
        frameLayout.addView(getZoomControls());
        setContentView(frameLayout);

        final DecodeService decodeService = createDecodeService();
        documentModel = new DocumentModel(decodeService);
        documentModel.addListener(BaseViewerActivity.this);
        progressModel = new DecodingProgressModel();
        progressModel.addListener(BaseViewerActivity.this);

        final Uri uri = getIntent().getData();
        final String fileName = PathFromUri.retrieve(getContentResolver(), uri);
        SettingsManager.init(fileName);
        SettingsManager.applyBookSettingsChanges(null, SettingsManager.getBookSettings(), null);

        startDecoding(decodeService, fileName, "");
    }

    private void startDecoding(final DecodeService decodeService, final String fileName, final String password) {
        new BookLoadTask(decodeService, fileName, password).execute();
    }

    private void askPassword(final DecodeService decodeService, final String fileName) {
        setContentView(R.layout.password);
        final Button ok = (Button) findViewById(R.id.pass_ok);

        ok.setOnClickListener(actions.getOrCreateAction(R.id.actions_redecodingWithPassord)
                .putValue("decodeService", decodeService).putValue("fileName", fileName));

        final Button cancel = (Button) findViewById(R.id.pass_cancel);
        cancel.setOnClickListener(actions.getOrCreateAction(R.id.mainmenu_close));
    }

    @ActionMethod(ids = R.id.actions_redecodingWithPassord)
    public void redecodingWithPassord(final ActionEx action) {
        final EditText te = (EditText) findViewById(R.id.pass_req);
        final DecodeService decodeService = action.getParameter("decodeService");
        final String fileName = action.getParameter("fileName");

        startDecoding(decodeService, fileName, te.getText().toString());
    }

    private void showErrorDlg(final String msg) {
        setContentView(R.layout.error);
        final TextView errortext = (TextView) findViewById(R.id.error_text);
        if (msg != null && msg.length() > 0) {
            errortext.setText(msg);
        } else {
            errortext.setText("Unexpected error occured!");
        }
        final Button cancel = (Button) findViewById(R.id.error_close);
        cancel.setOnClickListener(actions.getOrCreateAction(R.id.mainmenu_close));
    }

    @Override
    public IDocumentViewController switchDocumentController() {
        final BookSettings bs = SettingsManager.getBookSettings();

        final IDocumentViewController newDc = bs.singlePage ? new SinglePageDocumentView(this)
                : new ContiniousDocumentView(this);
        final IDocumentViewController oldDc = ctrl.getAndSet(newDc);

        getZoomModel().removeListener(oldDc);
        getZoomModel().addListener(newDc);

        return newDc;
    }

    @Override
    public void decodingProgressChanged(final int currentlyDecoding) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    setProgressBarIndeterminateVisibility(true);
                    getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                            currentlyDecoding == 0 ? 10000 : currentlyDecoding);
                } catch (final Throwable e) {
                }
            }
        });
    }

    @Override
    public void currentPageChanged(final PageIndex oldIndex, final PageIndex newIndex) {
        final int pageCount = documentModel.getPageCount();
        String prefix = "";

        if (pageCount > 0) {
            final String pageText = (newIndex.viewIndex + 1) + "/" + pageCount;
            if (SettingsManager.getAppSettings().getPageInTitle()) {
                prefix = "(" + pageText + ") ";
            } else {
                if (pageNumberToast != null) {
                    pageNumberToast.setText(pageText);
                } else {
                    pageNumberToast = Toast.makeText(this, pageText, 300);
                }
                pageNumberToast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
                pageNumberToast.show();
            }
        }

        getWindow().setTitle(prefix + currentFilename);
        SettingsManager.currentPageChanged(oldIndex, newIndex);
    }

    private void setWindowTitle() {
        currentFilename = getIntent().getData().getLastPathSegment();

        currentFilename = StringUtils.cleanupTitle(currentFilename);

        getWindow().setTitle(currentFilename);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setWindowTitle();
    }

    private PageViewZoomControls getZoomControls() {
        if (zoomControls == null) {
            zoomControls = new PageViewZoomControls(this, getZoomModel());
            zoomControls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        }
        return zoomControls;
    }

    private FrameLayout createMainContainer() {
        return new FrameLayout(this);
    }

    protected abstract DecodeService createDecodeService();

    /**
     * Called on creation options menu
     * 
     * @param menu
     *            the main menu
     * @return true, if successful
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onMenuOpened(final int featureId, final Menu menu) {
        getView().changeLayoutLock(true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onOptionsMenuClosed(final Menu menu) {
        final Window w = getWindow();
        if (SettingsManager.getAppSettings().getFullScreen()) {
            w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        getView().post(new Runnable() {

            @Override
            public void run() {
                getView().changeLayoutLock(false);
            }
        });
    }

    @ActionMethod(ids = R.id.actions_gotoOutlineItem)
    public void gotoOutlineItem(final ActionEx action) {
        final int item = action.getParameter(IActionController.DIALOG_ITEM_PROPERTY);
        final List<OutlineLink> outline = action.getParameter("outline");

        final String link = outline.get(item).getLink();
        if (link.startsWith("#")) {
            int pageNumber = 0;
            try {
                pageNumber = Integer.parseInt(link.substring(1).replace(" ", ""));
            } catch (final Exception e) {
                pageNumber = 0;
            }
            if (pageNumber < 1 || pageNumber > documentModel.getPageCount()) {
                Toast.makeText(getApplicationContext(),
                        "Page number out of range. Valid range: 1-" + documentModel.getDecodeService().getPageCount(),
                        2000).show();
                return;
            }
            getDocumentController().goToPage(pageNumber - 1);
        } else if (link.startsWith("http:")) {
            final Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link));
            startActivity(i);
        }
    }

    @ActionMethod(ids = R.id.mainmenu_outline)
    public void showOutline(final ActionEx action) {
        final List<OutlineLink> outline = documentModel.getDecodeService().getOutline();
        if ((outline != null) && (outline.size() > 0)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final CharSequence[] items = outline.toArray(new CharSequence[outline.size()]);
            builder.setTitle("Outline");
            builder.setItems(items, actions.getOrCreateAction(R.id.actions_gotoOutlineItem)
                    .putValue("outline", outline));
            final AlertDialog alert = builder.create();
            alert.show();
        } else {
            Toast.makeText(getApplicationContext(), "Document without Outline", Toast.LENGTH_SHORT).show();
        }

    }

    @ActionMethod(ids = R.id.mainmenu_goto_page)
    public void showDialog(final ActionEx action) {
        final int dialogId = action.getParameter("dialogId");
        showDialog(dialogId);
    }

    @ActionMethod(ids = R.id.mainmenu_booksettings)
    public void showBookSettings(final ActionEx action) {
        final Intent bsa = new Intent(BaseViewerActivity.this, BookSettingsActivity.class);
        bsa.setData(Uri.fromFile(new File(SettingsManager.getBookSettings().fileName)));
        startActivity(bsa);
    }

    @ActionMethod(ids = R.id.mainmenu_settings)
    public void showAppSettings(final ActionEx action) {
        final Intent i = new Intent(BaseViewerActivity.this, SettingsActivity.class);
        startActivity(i);
    }

    @ActionMethod(ids = R.id.mainmenu_nightmode)
    public void toggleNightMode(final ActionEx action) {
        SettingsManager.getAppSettings().switchNightMode();
        view.redrawView();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int actionId = item.getItemId();
        final ActionEx action = actions.getOrCreateAction(actionId);
        if (action.getMethod().isValid()) {
            action.run();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @ActionMethod(ids = R.id.mainmenu_bookmark)
    public void showBookmarkDialog(final ActionEx action) {
        final int page = getDocumentModel().getCurrentViewPageIndex();

        final String message = getString(R.string.add_bookmark_name);

        final EditText input = new EditText(this);
        input.setText(getString(R.string.text_page) + " " + (page + 1));
        input.selectAll();

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_add_bookmark)
                .setMessage(message)
                .setView(input)
                .setPositiveButton(R.string.password_ok,
                        actions.getOrCreateAction(R.id.actions_addBookmark).putValue("input", input))
                .setNegativeButton(R.string.password_cancel, actions.getOrCreateAction(R.id.actions_no_action)).show();
    }

    @ActionMethod(ids = R.id.actions_addBookmark)
    public void addBookmark(ActionEx action) {
        final EditText input = action.getParameter("input");
        final Editable value = input.getText();
        final BookSettings bs = SettingsManager.getBookSettings();
        bs.bookmarks.add(new Bookmark(getDocumentModel().getCurrentIndex(), value.toString()));
        SettingsManager.edit(bs).commit();
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        switch (id) {
            case DIALOG_GOTO:
                return new GoToPageDialog(this);
        }
        return null;
    }

    /**
     * Gets the zoom model.
     * 
     * @return the zoom model
     */
    @Override
    public ZoomModel getZoomModel() {
        if (zoomModel == null) {
            zoomModel = new ZoomModel();
        }
        return zoomModel;
    }

    @Override
    public DecodeService getDecodeService() {
        return documentModel.getDecodeService();
    }

    /**
     * Gets the decoding progress model.
     * 
     * @return the decoding progress model
     */
    @Override
    public DecodingProgressModel getDecodingProgressModel() {
        return progressModel;
    }

    @Override
    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    @Override
    public IDocumentViewController getDocumentController() {
        return ctrl.get();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public BaseDocumentView getView() {
        return view;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public IActionController<?> getActionController() {
        return actions;
    }

    @ActionMethod(ids = R.id.mainmenu_zoom)
    public void toggleZoomControls(final ActionEx action) {
        zoomControls.toggleZoomControls();
    }

    @Override
    public final boolean dispatchKeyEvent(final KeyEvent event) {
        if (getDocumentController().dispatchKeyEvent(event)) {
            return true;
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getRepeatCount() == 0) {
                    closeActivity(null);
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @ActionMethod(ids = R.id.mainmenu_close)
    public void closeActivity(final ActionEx action) {
        SettingsManager.clearCurrentBookSettings();
        finish();
    }

    @Override
    public void onAppSettingsChanged(final AppSettings oldSettings, final AppSettings newSettings,
            final AppSettings.Diff diff) {
        if (diff.isRotationChanged()) {
            setRequestedOrientation(newSettings.getRotation().getOrientation());
        }

        if (diff.isFullScreenChanged()) {
            final Window window = getWindow();
            if (newSettings.getFullScreen()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }

        if (diff.isShowTitleChanged() && diff.isFirstTime()) {
            final Window window = getWindow();
            try {
                if (!newSettings.getShowTitle()) {
                    window.requestFeature(Window.FEATURE_NO_TITLE);
                } else {
                    // Android 3.0+ you need both progress!!!
                    window.requestFeature(Window.FEATURE_PROGRESS);
                    window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                    setProgressBarIndeterminate(true);
                }
            } catch (final Throwable th) {
                LCTX.e("Error on requestFeature call: " + th.getMessage());
            }
        }
        if (diff.isKeepScreenOnChanged()) {
            getView().setKeepScreenOn(newSettings.isKeepScreenOn());
        }

        TouchManager.applyOldStyleSettings(newSettings);
    }

    @Override
    public void onBookSettingsChanged(final BookSettings oldSettings, final BookSettings newSettings,
            final BookSettings.Diff diff, final AppSettings.Diff appDiff) {

        boolean redrawn = false;
        if (diff.isSinglePageChanged() || diff.isSplitPagesChanged()) {
            redrawn = true;
            IDocumentViewController newDc = switchDocumentController();
            if (!diff.isFirstTime()) {
                newDc.init();
                newDc.show();
            }
        }

        if (diff.isZoomChanged() && diff.isFirstTime()) {
            redrawn = true;
            getZoomModel().setZoom(newSettings.getZoom());
        }

        final IDocumentViewController dc = getDocumentController();
        if (diff.isPageAlignChanged()) {
            dc.setAlign(newSettings.pageAlign);
        }

        if (diff.isAnimationTypeChanged()) {
            dc.updateAnimationType();
        }

        if (!redrawn && appDiff != null) {
            if (appDiff.isMaxImageSizeChanged() || appDiff.isPagesInMemoryChanged() || appDiff.isDecodeModeChanged()) {
                dc.updateMemorySettings();
            }
        }

        final DocumentModel dm = getDocumentModel();
        currentPageChanged(PageIndex.NULL, dm.getCurrentIndex());
    }

    private final class BookLoadTask extends AsyncTask<String, Void, Exception> {

        private final DecodeService m_decodeService;
        private final String m_fileName;
        private final String m_password;
        private ProgressDialog progressDialog;

        private BookLoadTask(DecodeService decodeService, String fileName, String password) {
            m_decodeService = decodeService;
            m_fileName = fileName;
            m_password = password;
        }

        @Override
        protected void onPreExecute() {
            LCTX.d("onPreExecute(): start");
            try {
                progressDialog = ProgressDialog.show(BaseViewerActivity.this, "", "Loading... Please wait", true);
            } catch (final Throwable th) {
                LCTX.e("Unexpected error", th);
            } finally {
                LCTX.d("onPreExecute(): finish");
            }
        }

        @Override
        protected Exception doInBackground(final String... params) {
            LCTX.d("doInBackground(): start");
            try {
                m_decodeService.open(m_fileName, m_password);
                getDocumentController().init();
                return null;
            } catch (final Exception e) {
                LCTX.e(e.getMessage(), e);
                return e;
            } catch (final Throwable th) {
                LCTX.e("Unexpected error", th);
                return new Exception(th.getMessage());
            } finally {
                LCTX.d("doInBackground(): finish");
            }
        }

        @Override
        protected void onPostExecute(final Exception result) {
            LCTX.d("onPostExecute(): start");
            try {
                if (result == null) {
                    getDocumentController().show();

                    final DocumentModel dm = getDocumentModel();
                    currentPageChanged(PageIndex.NULL, dm.getCurrentIndex());

                    setProgressBarIndeterminateVisibility(false);

                    progressDialog.dismiss();
                } else {
                    progressDialog.dismiss();

                    final String msg = result.getMessage();
                    if ("PDF needs a password!".equals(msg)) {
                        askPassword(m_decodeService, m_fileName);
                    } else {
                        showErrorDlg(msg);
                    }
                }
            } catch (final Throwable th) {
                LCTX.e("Unexpected error", th);
            } finally {
                LCTX.d("onPostExecute(): finish");
            }
        }
    }

    private class EmptyContoller implements IDocumentViewController {

        @Override
        public void zoomChanged(final float newZoom, final float oldZoom) {
        }

        @Override
        public void commitZoom() {
        }

        @Override
        public void goToPage(final int page) {
        }

        @Override
        public void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        }

        @Override
        public int getFirstVisiblePage() {
            return 0;
        }

        @Override
        public int calculateCurrentPage(final ViewState viewState) {
            return 0;
        }

        @Override
        public int getLastVisiblePage() {
            return 0;
        }

        @Override
        public void verticalDpadScroll(final int i) {
        }

        @Override
        public void verticalConfigScroll(final int i) {
        }

        @Override
        public void redrawView() {
        }

        @Override
        public void redrawView(final ViewState viewState) {
        }

        @Override
        public void setAlign(final PageAlign byResValue) {
        }

        @Override
        public IViewerActivity getBase() {
            return BaseViewerActivity.this;
        }

        @Override
        public BaseDocumentView getView() {
            return view;
        }

        @Override
        public void updateAnimationType() {
        }

        @Override
        public void updateMemorySettings() {
        }

        @Override
        public void drawView(final Canvas canvas, final ViewState viewState) {
        }

        @Override
        public boolean onLayoutChanged(final boolean layoutChanged, final boolean layoutLocked, final int left,
                final int top, final int right, final int bottom) {
            return false;
        }

        @Override
        public Rect getScrollLimits() {
            return new Rect(0, 0, 0, 0);
        }

        @Override
        public boolean onTouchEvent(final MotionEvent ev) {
            return false;
        }

        @Override
        public void onScrollChanged(final int newPage, final int direction) {
        }

        @Override
        public boolean dispatchKeyEvent(final KeyEvent event) {
            return false;
        }

        @Override
        public ViewState updatePageVisibility(final int newPage, final int direction, final float zoom) {
            return new ViewState(this);
        }

        public void show() {
        }

        public final void init() {
        }
    }
}
