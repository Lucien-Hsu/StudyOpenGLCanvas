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

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.textureFilter.ContrastFilter;
import com.chillingvan.canvasgl.util.Loggers;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.GLView;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.canvasgl.textureFilter.PixelationFilter;
import com.chillingvan.canvasglsample.R;

import java.io.IOException;

public class TextureCameraActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreviewTextureView cameraTextureView;
    private PreviewConsumerTextureView previewConsumerTextureView;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_canvas);

        Log.d("TAG", "[TextureCameraActivity]");

        imageView = (ImageView) findViewById(R.id.image_v);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Loggers.d("TextureCameraActivity", String.format("onResume: "));
        openCamera();
        initCameraTexture();
        cameraTextureView.onResume();
        previewConsumerTextureView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Loggers.d("TextureCameraActivity", String.format("onPause: "));
        releaseCamera();
        cameraTextureView.onPause();
        previewConsumerTextureView.onPause();
    }

    /**
     * 開啟相機
     */
    private void openCamera() {
        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            //取得第 i 個相機資訊
            Camera.getCameraInfo(i, info);
            //若此相機為前鏡頭
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                //開啟相機
                mCamera = Camera.open(i);
                break;
            }
        }

        if (mCamera == null) {
            mCamera = Camera.open();    // opens first back-facing camera
        }

        Camera.Parameters parms = mCamera.getParameters();
        mCamera.setDisplayOrientation(270);

        CameraUtils.choosePreviewSize(parms, 1280, 720);
    }

    /**
     * 顯示畫面預覽
     */
    private void initCameraTexture() {
        //左畫面，用來顯示原畫面
        cameraTextureView = (CameraPreviewTextureView) findViewById(R.id.camera_texture);
        //右畫面，用來顯示套用濾鏡的畫面
        previewConsumerTextureView = (PreviewConsumerTextureView) findViewById(R.id.camera_texture2);
        cameraTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraTextureView.getDrawingBitmap(new Rect(0, 0, v.getWidth(), v.getHeight()), new GLView.GetDrawingCacheCallback() {
                    @Override
                    public void onFetch(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                });

            }
        });

        //點擊右畫面可拍照
        previewConsumerTextureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previewConsumerTextureView.getDrawingBitmap(new Rect(0, 0, v.getWidth(), v.getHeight()), new GLView.GetDrawingCacheCallback() {
                    @Override
                    public void onFetch(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });

        //為右畫面指定濾鏡，這邊的引數為馬賽克濾鏡
        previewConsumerTextureView.setTextureFilter(new PixelationFilter(15));

        //左畫面有產生內容監聽器則做？
        cameraTextureView.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                //指定 openGL 內容給右畫面
                previewConsumerTextureView.setSharedEglContext(eglContext);
            }
        });

        //當 SurfaceTexture 準備好則做
        cameraTextureView.setOnSurfaceTextureSet(new GLSurfaceTextureProducerView.OnSurfaceTextureSet() {
            @Override
            public void onSet(SurfaceTexture surfaceTexture, RawTexture surfaceTextureRelatedTexture) {
                Loggers.d("TextureCameraActivity", String.format("onSet: "));

                previewConsumerTextureView.addConsumeGLTexture(new GLTexture(surfaceTextureRelatedTexture, surfaceTexture));
                //若有可用的幀則做
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        //指定左畫面進行渲染
                        cameraTextureView.requestRenderAndWait();
                        //指定右畫面進行渲染
                        previewConsumerTextureView.requestRenderAndWait();
                    }
                });


                try {
                    //設定相機的預覽用 SurfaceTexture
                    mCamera.setPreviewTexture(surfaceTexture);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
                //開啟相機預覽
                mCamera.startPreview();
            }
        });
    }

    /**
     * 釋放相機資源
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    //[按鈕] 改變預覽畫面尺寸
    public void onClickChangeSize(View view) {
        if (cameraTextureView.getScaleY() < 1) {
            cameraTextureView.setScaleY(1.5f);
            previewConsumerTextureView.setScaleY(1.5f);
        } else {
            cameraTextureView.setScaleY(0.7f);
            previewConsumerTextureView.setScaleY(0.7f);
        }
    }

    //[按鈕] 改變UI尺寸
    public void onClickChangeLayoutSize(View view) {
        ViewGroup.LayoutParams layoutParams = cameraTextureView.getLayoutParams();
        ViewGroup.LayoutParams consumerLayoutParams = previewConsumerTextureView.getLayoutParams();
        if (layoutParams.height < 500) {
            layoutParams.height += 50;
            cameraTextureView.setLayoutParams(layoutParams);
            consumerLayoutParams.height += 50;
            previewConsumerTextureView.setLayoutParams(consumerLayoutParams);
        } else {
            layoutParams.height -= 50;
            cameraTextureView.setLayoutParams(layoutParams);
            consumerLayoutParams.height -= 50;
            previewConsumerTextureView.setLayoutParams(consumerLayoutParams);
        }
    }

    //[按鈕] 旋轉 TextureView
    public void onClickRotateTextureView(View view) {
        cameraTextureView.setRotation(cameraTextureView.getRotation() + 90);
    }

    //[按鈕] 旋轉 TextureView 內的預覽畫面
    public void onClickRotateSurface(View view) {
        cameraTextureView.rotateSurface(90);
    }
}
