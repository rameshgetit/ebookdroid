package org.ebookdroid.ui.viewer;

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;

import android.app.Activity;
import android.content.Context;

import org.emdev.ui.actions.IActionController;

public interface IActivityController extends IActionController<ViewerActivity> {

    Context getContext();

    Activity getActivity();

    DecodeService getDecodeService();

    DocumentModel getDocumentModel();

    IView getView();

    IViewController getDocumentController();

    IActionController<?> getActionController();

    ZoomModel getZoomModel();

    DecodingProgressModel getDecodingProgressModel();

    IViewController switchDocumentController();

    void jumpToPage(int viewIndex, float offsetX, float offsetY);

    static interface IBookLoadTask {

        void setProgressDialogMessage(int resourceID, Object... args);

    }

}
