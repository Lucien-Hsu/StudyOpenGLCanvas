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

package com.chillingvan.canvasgl.textureFilter;

import android.opengl.GLES20;
import android.support.annotation.FloatRange;
import android.util.Log;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.OpenGLUtil;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;

/**
 * Created by Chilling on 2016/11/1.
 */

public class PixelationFilter extends BasicTextureFilter implements OneValueFilter {

    //寬因數
    public static final String UNIFORM_IMAGE_WIDTH_FACTOR = "imageWidthFactor";
    //高因數
    public static final String UNIFORM_IMAGE_HEIGHT_FACTOR = "imageHeightFactor";
    //像素
    public static final String UNIFORM_PIXEL = "pixel";
    //shader
    //dx 表示 x 軸分為幾份
    //dy 表示 y 軸分為幾份
    public static final String PIXELATION_FRAGMENT_SHADER = "" +
            "precision highp float;\n" +

            " varying vec2 " + VARYING_TEXTURE_COORD + ";\n" +
            "uniform float " + UNIFORM_IMAGE_WIDTH_FACTOR + ";\n" +
            "uniform float " + UNIFORM_IMAGE_HEIGHT_FACTOR + ";\n" +
            " uniform float " + ALPHA_UNIFORM + ";\n" +
            "uniform sampler2D " + TEXTURE_SAMPLER_UNIFORM + ";\n" +
            "uniform float " + UNIFORM_PIXEL + ";\n" +
            "void main() {\n" +
            "" +
            "  vec2 uv  = " + VARYING_TEXTURE_COORD + ".xy;\n" +

            // x (螢幕長邊)要分成幾分之幾
            "  float dx = " + UNIFORM_PIXEL + " * " + UNIFORM_IMAGE_WIDTH_FACTOR + ";\n" +
            // y (螢幕短邊)要分成幾分之幾
            "  float dy = " + UNIFORM_PIXEL + " * " + UNIFORM_IMAGE_HEIGHT_FACTOR + ";\n" +

            //局部馬賽克
            "vec2 coord = uv;\n" +
            "if(uv.x > 0.3 && uv.x < 0.7 && uv.y > 0.3 && uv.y < 0.7 ){\n" +
            "  coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));\n" +
            "}\n" +

            //繪製方框


            "  vec4 tc = texture2D(" + TEXTURE_SAMPLER_UNIFORM + ", coord);\n" +
            "  gl_FragColor = vec4(tc);\n" +
            "    gl_FragColor *= " + ALPHA_UNIFORM + ";\n" +
            "}";

    private int mImageWidthFactorLocation;
    private int mImageHeightFactorLocation;
    private int mPixelLocation;
    private float mPixel;

    //引數為馬賽克的像素，數值越大馬賽克的顆粒就越大
    public PixelationFilter(@FloatRange(from = 1, to = 100) float pixel) {
        this.mPixel = pixel;
    }

    @Override
    public String getFragmentShader() {
        return PIXELATION_FRAGMENT_SHADER;
    }

    @Override
    public void onPreDraw(int program, BasicTexture texture, ICanvasGL canvas) {
        super.onPreDraw(program, texture, canvas);
        //1.取得 openGL 參數給 java 變數
        mImageWidthFactorLocation = GLES20.glGetUniformLocation(program, UNIFORM_IMAGE_WIDTH_FACTOR);
        mImageHeightFactorLocation = GLES20.glGetUniformLocation(program, UNIFORM_IMAGE_HEIGHT_FACTOR);
        mPixelLocation = GLES20.glGetUniformLocation(program, UNIFORM_PIXEL);

        //2.以得到的 java 變數設定 openGL 參數
        //texture 寬度像素數之倒數
        OpenGLUtil.setFloat(mImageWidthFactorLocation, 1.0f / texture.getWidth());
        //texture 高度像素數之倒數
        OpenGLUtil.setFloat(mImageHeightFactorLocation, 1.0f / texture.getHeight());
        //每個馬賽克之像素數
        OpenGLUtil.setFloat(mPixelLocation, mPixel);
        Log.d("TAG", "texture.getWidth(): " + (texture.getWidth()) + " texture.getHeight(): " + (texture.getHeight()) + " mPixelLocation: " + mPixel);
    }

    @Override
    public void setValue(@FloatRange(from = 1, to = 100) final float pixel) {
        mPixel = pixel;
    }
}
