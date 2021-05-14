package com.chillingvan.canvasglsample.multi;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.canvasgl.textureFilter.LightenBlendFilter;
import com.chillingvan.canvasgl.textureFilter.PixelationFilter;
import com.chillingvan.canvasgl.textureFilter.TwoTextureFilter;

import java.util.List;

/**
 * Created by Chilling on 2018/5/19.
 */
public class MultiVideoTexture extends GLMultiTexProducerView {

    //設定雙圖片混合濾鏡
    //這邊設定的濾鏡會取兩張貼圖中最亮的顏色顯示
    private TwoTextureFilter textureFilter = new LightenBlendFilter();

    public MultiVideoTexture(Context context) {
        super(context);
    }

    public MultiVideoTexture(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiVideoTexture(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        super.onSurfaceTextureAvailable(surface, width, height);
        if (mSharedEglContext == null) {
            setSharedEglContext(EglContextWrapper.EGL_NO_CONTEXT_WRAPPER);
        }
    }

    @Override
    protected int getInitialTexCount() {
        return 3;
    }

    @Override
    protected int getRenderMode() {
        return GLThread.RENDERMODE_CONTINUOUSLY;
    }

    @Override
    protected void onGLDraw(ICanvasGL canvas, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
        int size = producedTextures.size();
        for (int i = 0; i < producedTextures.size(); i++) {
            GLTexture texture = producedTextures.get(i);
            int left = getWidth() * i / size;
            RawTexture rawTexture = texture.getRawTexture();
            rawTexture.setIsFlippedVertically(true);
            // An example for two texture filter with RawTexture
            if (i == 2) {
                //指定濾鏡要用的第二張貼圖
                //這邊若get(0)是用左方卡通影片，若get(0)是用左方卡通影片
                textureFilter.setSecondRawTexture(producedTextures.get(0));

                //繪製影像到 Surface
                //引數6：設定濾鏡，這邊採用 TwoTextureFilter，此濾鏡會結合另一張 texture 來合成
                canvas.drawSurfaceTexture(rawTexture, texture.getSurfaceTexture(), left, 0, left + getWidth()/size, getHeight(), textureFilter);
            } else {
                canvas.drawSurfaceTexture(rawTexture, texture.getSurfaceTexture(), left, 0, left + getWidth()/size, getHeight());
            }
        }
    }
}
