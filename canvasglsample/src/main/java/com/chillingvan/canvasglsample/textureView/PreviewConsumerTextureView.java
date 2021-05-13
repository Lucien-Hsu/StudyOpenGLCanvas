/*
 *
 *  *
 *  *  * Copyright (C) 2016 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.chillingvan.canvasglsample.textureView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLMultiTexConsumerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.chillingvan.canvasglsample.R;

import java.util.List;

/**
 * Created by Chilling on 2016/11/5.
 */
//此類別為客製化的 View 用於在範例中呈現濾鏡套用效果
public class PreviewConsumerTextureView extends GLMultiTexConsumerView {

    //設定基礎濾鏡
    private TextureFilter textureFilter = new BasicTextureFilter();
    private Bitmap robot;

    public PreviewConsumerTextureView(Context context) {
        super(context);
    }

    public PreviewConsumerTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreviewConsumerTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //繪製
    @Override
    protected void onGLDraw(ICanvasGL canvas, List<GLTexture> consumedTextures) {
        //若有拿到 GLTexture 才繪製
        if (!consumedTextures.isEmpty()) {
            //取得第一張 GLTexture
            GLTexture consumedTexture = consumedTextures.get(0);
            //從 GLTexture 取得 SurfaceTexture
            SurfaceTexture sharedSurfaceTexture = consumedTexture.getSurfaceTexture();
            //從 GLTexture 取得 RawTexture，不可為 null
            RawTexture sharedTexture = consumedTexture.getRawTexture();

            //實際繪製 SurfaceTexture
            //指定貼圖,原點,寬高,濾鏡
            canvas.drawSurfaceTexture(sharedTexture, sharedSurfaceTexture, 0, 0, sharedTexture.getWidth(), sharedTexture.getHeight(), textureFilter);

            //繪製機器人圖示，越晚繪製的會顯示在越前面
            canvas.drawBitmap(robot, 0, 0 , 60, 60);
        }
    }

    //若有指定濾鏡則使用此濾鏡
    public void setTextureFilter(TextureFilter textureFilter) {
        this.textureFilter = textureFilter;
    }

    //初始化時取得機器人圖示
    @Override
    protected void init() {
        super.init();
        robot = BitmapFactory.decodeResource(getResources(), R.drawable.ic_robot);
    }

    public void clearConsumedTextures() {
        consumedTextures.clear();
    }
}
