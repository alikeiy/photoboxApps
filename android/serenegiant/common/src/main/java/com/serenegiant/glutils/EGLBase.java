package com.serenegiant.glutils;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Compatibility Shim for EGLBase to bridge legacy react-native-uvc-camera
 * with the updated serenegiant common library.
 */
public abstract class EGLBase {

    public interface IContext {
        Object getContext();
    }

    private static class ContextShim implements IContext {
        private final com.serenegiant.egl.EGLBase.IContext mContext;
        ContextShim(com.serenegiant.egl.EGLBase.IContext context) {
            this.mContext = context;
        }
        @Override
        public Object getContext() { return mContext; }
    }

    private final com.serenegiant.egl.EGLBase mDelegate;

    protected EGLBase(com.serenegiant.egl.EGLBase delegate) {
        this.mDelegate = delegate;
    }

    public static EGLBase createFrom(@Nullable final IContext sharedContext, final boolean withDepthBuffer, final boolean isRecordable) {
        com.serenegiant.egl.EGLBase.IContext shared = (sharedContext instanceof ContextShim) ? (com.serenegiant.egl.EGLBase.IContext)sharedContext.getContext() : null;
        return new EGLBaseImpl(com.serenegiant.egl.EGLBase.createFrom(shared, withDepthBuffer, isRecordable));
    }

    public abstract void release();
    public abstract void makeCurrent();
    public abstract void swap();
    public abstract IContext getContext();

    private static class EGLBaseImpl extends EGLBase {
        EGLBaseImpl(com.serenegiant.egl.EGLBase delegate) { super(delegate); }
        @Override public void release() { mDelegate.release(); }
        @Override public void makeCurrent() { mDelegate.makeCurrent(); }
        @Override public void swap() { mDelegate.swap(); }
        @Override public IContext getContext() { return new ContextShim(mDelegate.getContext()); }
    }
}
