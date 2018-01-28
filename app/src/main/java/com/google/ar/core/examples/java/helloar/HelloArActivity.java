/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.Trackable.TrackingState;
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer.BlendMode;
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {
    private static final String TAG = HelloArActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView mSurfaceView;

    private Session mSession;
    private GestureDetector mGestureDetector;
    private Snackbar mMessageSnackbar;
    private DisplayRotationHelper mDisplayRotationHelper;

    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer mVirtualObject = new ObjectRenderer();
    private final ObjectRenderer arrowObjectRenderer = new ObjectRenderer();
    private final ObjectRenderer mVirtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer mPlaneRenderer = new PlaneRenderer();
    private final PointCloudRenderer mPointCloud = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];

    // Tap handling and UI.
    private final ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private final ArrayBlockingQueue<MotionEvent> mQueuedDoubleTaps = new ArrayBlockingQueue<>(16);
    private final ArrayList<Anchor> mAnchors = new ArrayList<>();
    private final ArrayList<Anchor> arrowAnchor = new ArrayList<>();

    float[] transform = {0f,0f,0f};
    float[] rotation = {0f,0f,0f};


    Pose pose ;//= new Pose(transform, rotation);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        ///
        ///
        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        outputDestinations = (TextView) (findViewById(R.id.outputDestinations));
        typeDestination = (EditText) (findViewById(R.id.typeDestination));
        percentageOutput = (TextView) (findViewById(R.id.percentageOutput));

        Button selectDestinationButton = (Button) (findViewById(R.id.selectDestinationButton));

        // Creates a new array list of object events

        events.add(new Event("Marauder Zone", "Saturday", "9:00 AM to 4:00 PM", "BSB", "43.262259", "-79.919985"));
        events.add(new Event("Faculty Swag Distribution", "Saturday", "10:00 AM to 4:00 PM", "ETB", "43.258226", "-79.920013"));
        events.add(new Event("SOCS Opening Ceremonies", "Saturday", "1:00 PM to 1:30 PM", "MDCL", "43.261335", "-79.916970"));
        events.add(new Event("Residence Dinner: McKay", "Saturday", "5:00 PM to 5:45 PM", "Thode", "43.261070", "-79.922472"));

        outputDestinations.setText("1. BSB, 2. ETB, 3. MDCL, 4. Thode");

        selectDestinationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int destination = Integer.parseInt(typeDestination.getText().toString());

                if (destination == 1) {
                    goalLatitude = Double.parseDouble(events.get(0).getLatitude());
                    goalLongitude = Double.parseDouble(events.get(0).getLongitude());
                } else if (destination == 2) {
                    goalLatitude = Double.parseDouble(events.get(1).getLatitude());
                    goalLongitude = Double.parseDouble(events.get(1).getLongitude());
                } else if (destination == 3) {
                    goalLatitude = Double.parseDouble(events.get(2).getLatitude());
                    goalLongitude = Double.parseDouble(events.get(2).getLongitude());
                } else if (destination == 4) {
                    goalLatitude = Double.parseDouble(events.get(3).getLatitude());
                    goalLongitude = Double.parseDouble(events.get(3).getLongitude());
                }

                outputDestinations.setText("Lat: " + goalLatitude + ", Long: " + goalLongitude);
            }
        });
        ///
        ///


        // Set up tap listener.
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                onDoubleTapUp(e);
                return super.onDoubleTap(e);
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Exception exception = null;
        String message = null;
        try {
            mSession = new Session(/* context= */ this);
        } catch (UnavailableArcoreNotInstalledException e) {
            message = "Please install ARCore";
            exception = e;
        } catch (UnavailableApkTooOldException e) {
            message = "Please update ARCore";
            exception = e;
        } catch (UnavailableSdkTooOldException e) {
            message = "Please update this app";
            exception = e;
        } catch (Exception e) {
            message = "This device does not support AR";
            exception = e;
        }

        if (message != null) {
            showSnackbarMessage(message, true);
            Log.e(TAG, "Exception creating session", exception);
            return;
        }

        // Create default config and check if supported.
        Config config = new Config(mSession);
        if (!mSession.isSupported(config)) {
            showSnackbarMessage("This device does not support AR", true);
        }
        mSession.configure(config);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            if (mSession != null) {
                showLoadingMessage();
                // Note that order matters - see the note in onPause(), the reverse applies here.
                mSession.resume();
            }
            mSurfaceView.onResume();
            mDisplayRotationHelper.onResume();
        } else {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mDisplayRotationHelper.onPause();
        mSurfaceView.onPause();
        if (mSession != null) {
            mSession.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        switch (requestCode) {
            case PERMISSION_LOCATION: {
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationServices();
                    Log.v("DONKEY", "Permission Granted - Starting Services");
                } else {
                    //Show a dialog saying something like "I can't see your location"
                    Log.v("DONKEY", "Permission not granted");
                }
            }
        }

        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }



    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedDoubleTaps.offer(e);
    }

    private void onDoubleTapUp(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/ this);
        if (mSession != null) {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
        }

        // Prepare the other rendering objects.
        try {
            arrowObjectRenderer.createOnGlThread(this, "arrow.obj", "arrow.png");
            arrowObjectRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            mVirtualObject.createOnGlThread(/*context=*/this, "meeseeks.obj", "arrow.png");
            mVirtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            mVirtualObjectShadow.createOnGlThread(/*context=*/this,
                "andy_shadow.obj", "andy_shadow.png");
            mVirtualObjectShadow.setBlendMode(BlendMode.Shadow);
            mVirtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            mPlaneRenderer.createOnGlThread(/*context=*/this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        mPointCloud.createOnGlThread(/*context=*/this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


        if (mSession == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();
            Camera camera = frame.getCamera();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = mQueuedSingleTaps.poll();
            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Trackable trackable = hit.getTrackable();
                    if (trackable instanceof Plane
                            && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.
                        if (mAnchors.size() >= 20) {
                            mAnchors.get(0).detach();
                            mAnchors.remove(0);
                        }
                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor is created on the Plane to place the 3d model
                        // in the correct position relative both to the world and to the plane.
                        mAnchors.add(hit.createAnchor());

                        // Hits are sorted by depth. Consider only closest hit on a plane.
                        break;
                    }
                }
            }

            MotionEvent doubleTap = mQueuedDoubleTaps.poll();
            if (doubleTap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(doubleTap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Trackable trackable = hit.getTrackable();
                    //if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.
                        if (arrowAnchor.size() >= 1) {
                            arrowAnchor.get(0).detach();
                            arrowAnchor.remove(0);
                        }
                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor is created on the Plane to place the 3d model
                        // in the correct position relative both to the world and to the plane.
                        arrowAnchor.add(hit.createAnchor());

                        // Hits are sorted by depth. Consider only closest hit on a plane.
                        break;
                    //}
                }
            }




            // Draw background.
            mBackgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize tracked points.
            PointCloud pointCloud = frame.acquirePointCloud();
            mPointCloud.update(pointCloud);
            mPointCloud.draw(viewmtx, projmtx);

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (mMessageSnackbar != null) {
                for (Plane plane : mSession.getAllTrackables(Plane.class)) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                            && plane.getTrackingState() == TrackingState.TRACKING) {
                        hideLoadingMessage();
                        break;
                    }
                }
            }

            // Visualize planes.
            mPlaneRenderer.drawPlanes(
                mSession.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            // Visualize anchors created by touch.
            float scaleFactor = .20f;
            for (Anchor anchor : mAnchors) {
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                anchor.getPose().toMatrix(mAnchorMatrix, 0);

                // Update and draw the model and its shadow.
                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);
                mVirtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);
            }

            //arrowObjectRenderer. TODO update the arrow with the anchors view and projection matrix
            //TODO draw the arrow
            scaleFactor = .10f;

            for (Anchor anchor : arrowAnchor) {
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    //continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.

                rotation[0] += 0.0005f;

                //pose = new Pose(transform, XYZtoQuaternion(rotation[0], rotation[1], rotation[2]));
                anchor.getPose().toMatrix(mAnchorMatrix, 0);
                //pose.toMatrix(mAnchorMatrix, 0);

                // Update and draw the model and its shadow.
                arrowObjectRenderer.updateModelMatrix(mAnchorMatrix, scaleFactor);
                arrowObjectRenderer.draw(viewmtx, projmtx, lightIntensity);
                //arrowObjectRenderer.updateModelMatrix(anchorHard, scaleFactor);
                //arrowObjectRenderer.draw(viewHard, projHard, lightIntensity);

                String proj = "";
                String view = "";
                String anc = "";

                for (int i = 0; i < projmtx.length; i++) {
                    proj += projmtx[i] + "f,";
                    view += viewmtx[i] + "f,";
                    anc += mAnchorMatrix[i] +"f,";
                }

//                Log.d("View matrix", view);
//                Log.d("Proj matrix", proj);
//                Log.d("Anchor matrix", anc);

            }


        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    public float[] XYZtoQuaternion (float X, float Y, float Z) {

        float pitch = Y;
        float yaw = X;
        float roll = Z;

        double c1 = Math.cos(yaw / 2);
        double s1 = Math.sin(yaw / 2);
        double c2 = Math.cos(pitch / 2);
        double s2 = Math.sin(pitch / 2);
        double c3 = Math.cos(roll / 2);
        double s3 = Math.sin(roll / 2);
        double c1c2 = c1 * c2;
        double s1s2 = s1 * s2;

        float w = (float)(c1c2 * c3 - s1s2 * s3);
        float x = (float)(c1c2 * s3 + s1s2 * c3);
        float y = (float)(s1 * c2 * c3 + c1 * s2 * s3);
        float z = (float)(c1 * s2 * c3 - s1 * c2 * s3);

        float[] quaternion = {x,y,z,w};
        return quaternion;
    }


    float[] viewHard = {
            0.029422224f,0.9326214f,-0.35965487f, 0.0f,
            -0.9940906f, 0.064914346f,0.08700612f,0.0f,
            0.10449058f,0.3549696f, 0.9290201f,0.0f,
            0.014407033f,0.34372908f,0.014740458f,1.0f
    };
//    float[] viewHard = {
//        1.0f,   0.0f,   0.0f,   0.0f,
//        0.0f,   1.0f,   0.0f,   0.0f,
//        0.0f,   0.0f,   1.0f,   0.0f,
//        0.0f,   0.0f,   0.0f,   1.0f
//    };

    float[] projHard = {
            3.1810021f,0.0f,0.0f,0.0f,
            0.0f,1.5475148f,0.0f,0.0f,
            -0.04225221f,-0.01383021f,-1.002002f,-1.0f,
            0.0f,0.0f,-0.2002002f,0.0f
    };
//    float[] projHard = {
//            1.0f,   0.0f,   0.0f,   0.0f,
//            0.0f,   1.0f,   0.0f,   0.0f,
//            0.0f,   0.0f,   1.0f,   0.0f,
//            0.0f,   0.0f,   0.0f,   1.0f
//    };
//
//    float[] anchorHard = {
//            0.86729115f,4.247659E-17f,-0.4978012f,0.0f,
//            1.6543612E-24f,1.0f,8.532843E-17f,0.0f,
//            0.4978012f,-7.4004594E-17f,0.86729115f,0.0f,
//            -0.23206054f,-0.05922849f,-0.8389742f,1.0f
//    };

    float[] anchorHard = {
        1.0f,   0.0f,   0.0f,   0.0f,
        0.0f,   1.0f,   0.0f,   0.0f,
        0.0f,   0.0f,  -1.0f,   0.0f,
        0.0f,   0.0f,   0.0f,   1.0f
    };

    //0.86729115f,4.247659E-17f,-0.4978012f,0.0f,1.6543612E-24f,1.0f,8.532843E-17f,0.0f,0.4978012f,-7.4004594E-17f,0.86729115f,0.0f,-0.23206054f,-0.05922849f,-0.8389742f,1.0f
    boolean test = false;

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        mMessageSnackbar = Snackbar.make(
            HelloArActivity.this.findViewById(android.R.id.content),
            message, Snackbar.LENGTH_INDEFINITE);
        mMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        if (finishOnDismiss) {
            mMessageSnackbar.setAction(
                "Dismiss",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMessageSnackbar.dismiss();
                    }
                });
            mMessageSnackbar.addCallback(
                new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        finish();
                    }
                });
        }
        mMessageSnackbar.show();
    }

    private void showLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showSnackbarMessage("Searching for surfaces...", false);
            }
        });
    }

    private void hideLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMessageSnackbar != null) {
                    mMessageSnackbar.dismiss();
                }
                mMessageSnackbar = null;
            }
        });
    }


    ///////////
    //////////
    //////////
    final int PERMISSION_LOCATION = 111;

    ArrayList<Event> events = new ArrayList<>();

    TextView outputDestinations;
    EditText typeDestination;
    TextView percentageOutput;

    double goalLatitude;
    double goalLongitude;

    private GoogleApiClient mGoogleApiClient;


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
            Log.v("DONKEY", "Requesting Permissions");
        } else {
            Log.v("Donkey", "starting location services from onConnected");
            startLocationServices();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        percentageOutput.setText("Lat: " + latitude + ", Long: " + longitude);

        Log.v("WORK", "Lat:" + latitude + " - Long:" + longitude);

    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();

    }


    public void startLocationServices() {
        Log.v("DONKEY", "starting location services called");

        try {
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this);
            Log.v("DONKEY", "Requestion Location Updates");
        } catch (SecurityException exception) {
            //Should dialog to user
            Log.v("DONKEY", exception.toString());
        }


    }
}
