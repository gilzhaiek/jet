package com.reconinstruments.camera;

/**
 * Callback interface for Camera preview changes on Activity pause/resume.
 * @author jin
 */
public interface OnPreviewChangedListener {
    public void onPreviewResumed();
    public void onPreviewPaused();
}
