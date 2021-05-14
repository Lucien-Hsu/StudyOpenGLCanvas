package com.chillingvan.canvasglsample.multi;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasglsample.R;
import com.chillingvan.canvasglsample.video.MediaPlayerHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 此範例有三個 Surface 並列
 * 一開始左邊的 Surface 會顯示鏡頭預覽畫面
 * 點擊按鈕後，左邊和中間的 Surface 會開始播放影片；右邊的 Surface 會顯示鏡頭預覽與右邊影片的混合影像，此影像採用 LightenBlendFilter 濾鏡
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MultiTextureActivity extends AppCompatActivity {

    private List<Surface> mediaSurfaces = new ArrayList<>();
    private List<MediaPlayerHelper> mediaPlayers = new ArrayList<>();
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    //此為自定義的 TextureView 裡面有指定右畫面要套用的濾鏡
    private MultiVideoTexture multiVideoTexture;

    {
        //指定要播放的兩個影片
        mediaPlayers.add(new MediaPlayerHelper(MediaPlayerHelper.TEST_VIDEO_MP4));
        mediaPlayers.add(new MediaPlayerHelper(MediaPlayerHelper.TEST_VIDEO_MP4_2));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_texture);
        //若版本太低則跳出不支援的提示
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(this, "This only support >= Lollipop, 21", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }

    /**
     * 打開相機
     * 初始化TextureView
     */
    @Override
    protected void onResume() {
        super.onResume();
        openCamera(1280, 720);
        initTextures();
        multiVideoTexture.onResume();
    }

    /**
     * 取得前鏡頭相機編號
     */
    private void setUpCameraOutputs(int width, int height) {
        //取得相機服務
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍歷取得前鏡頭
            for (String cameraId : manager.getCameraIdList()) {
                // 获取指定摄像头的特性
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a back facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }

                // 获取摄像头支持的配置属性
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }


                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.   获取最佳的预览尺寸
//                Size mPreviewSize = CameraUtils.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
//                        maxPreviewHeight, largest);

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Log.e("TAG", e.getMessage());
        }
    }

    /**
     * 打開相機
     */
    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        //取得前鏡頭相機編號
        setUpCameraOutputs(width, height);
        Activity activity = this;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            //打開指定編號之相機
            manager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    //抓出 CameraDevice
                    mCameraDevice = camera;
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    mCameraDevice = null;

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化 TextureView
     */
    private void initTextures() {
        //取得自訂義的 TextureView
        multiVideoTexture = findViewById(R.id.multi_texture);
        //監聽 TextureView
        multiVideoTexture.setSurfaceTextureCreatedListener(new GLMultiTexProducerView.SurfaceTextureCreatedListener() {
            @Override
            public void onCreated(List<GLTexture> glTextureList) {
                mediaSurfaces.clear();

                //根據 mediaPlayer 的數量加入同等數量的 Surface
                //這邊只有兩個 Surface，即第一個(左)和第二個(中)
                for (int i = 0; i < mediaPlayers.size(); i++) {
                    GLTexture glTexture = glTextureList.get(i);
                    mediaSurfaces.add(new Surface(glTexture.getSurfaceTexture()));
                }

                //建立Session，指定鏡頭的預覽影片在哪個 Surface 播放
                //這邊指定第三個 Surface，即最右邊的 Surface
                createCameraPreviewSession(glTextureList.get(2).getSurfaceTexture());
            }
        });
    }

    /**
     * 以取得的 TextureView 建立 Session
     */
    private void createCameraPreviewSession(SurfaceTexture texture) {
        try {
            assert texture != null;
            // We configure the size of default buffer to be the size of camera preview we want.
//            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            //設定 Surface
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            final CaptureRequest.Builder mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //指定 Surface 為輸出目標
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                //指定對焦模式
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                //建立 Request
                                // Finally, we start displaying the camera preview.
                                CaptureRequest mPreviewRequest = mPreviewRequestBuilder.build();
                                //在 Session 中連續發出 Request 供預覽
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(MultiTextureActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 釋放資源
     */
    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        multiVideoTexture.onPause();
        for (MediaPlayerHelper mediaPlayer : mediaPlayers) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        }
    }

    /**
     * 釋放資源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (MediaPlayerHelper mediaPlayer : mediaPlayers) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.release();
            }
        }
    }

    /**
     * 關閉相機
     */
    private void closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


    /**
     * 播放影片
     * 連結到 xml 按鈕
     */
    public void onClickStart(View view) {
        //此處只有左邊和中間的 Surface 會播放 mediaPlayer 的影片
        for (int i = 0; i < mediaPlayers.size(); i++) {
            final MediaPlayerHelper mediaPlayer = mediaPlayers.get(i);
            final Surface mediaSurface = mediaSurfaces.get(i);
            if ((mediaPlayer.isPlaying() || mediaPlayer.isLooping())) {
                continue;
            }
            //播放影片
            playMedia(mediaPlayer, mediaSurface);
        }
    }

    /**
     * 播放影片
     */
    private void playMedia(MediaPlayerHelper mediaPlayer, Surface mediaSurface) {
        mediaPlayer.playMedia(this, mediaSurface);
    }
}
