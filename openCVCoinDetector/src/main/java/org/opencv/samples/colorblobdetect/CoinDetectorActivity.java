package org.opencv.samples.colorblobdetect;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.samples.colorblobdetect.ResultProcessor;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Toast;

import static org.opencv.imgproc.Imgproc.circle;



public class CoinDetectorActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private static int setScreenWidth = 1280;
    private static int setScreenHeight = 720;

    private boolean touchdetector=false;
    private boolean mIsColorSelected = false;

    private Mat mRgba;
    private Mat showResults;
    private Mat mGray;
    private Mat circles;
    private int screenWidth;
    private int screenHeight;

    private ResultProcessor myResultClass;

    //private Scalar               mBlobColorRgba;
    //private Scalar               mBlobColorHsv;
    //private ColorBlobDetector    mDetector;
    //private Mat                  mSpectrum;
    //private Size                 SPECTRUM_SIZE;
    //private Scalar               CONTOUR_COLOR;

    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewGray;
    private MenuItem mItemPreviewCanny;
    private MenuItem mItemPreviewFeatures;

    private CameraBridgeViewBase mOpenCvCameraView;

    /**
     * Start the Android Application and show display on the screen
     **/
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(CoinDetectorActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    /**
     * Instantiate Main Activity
     **/
    public CoinDetectorActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(setScreenWidth, setScreenHeight);
        mOpenCvCameraView.enableFpsMeter();

        myResultClass = new ResultProcessor();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        circles = new Mat();
        mRgba = new Mat();
        showResults = new Mat();

        screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        myResultClass.prepareGameSize(width,height);

/*        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);*/
    }

    public void onCameraViewStopped() {
//        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat returnFrame = new Mat();

        mRgba = inputFrame.rgba();
        showResults = Mat.zeros(mRgba.rows(), mRgba.cols(), CvType.CV_8UC1);

        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_BGRA2GRAY);
        Imgproc.medianBlur(mGray, mGray, 5);

        Imgproc.HoughCircles(mGray, circles, Imgproc.HOUGH_GRADIENT, 1, mGray.rows() / 8,
                100, 50, 5, 150);

        for (int i = 0; i < circles.cols(); i++) {
            double[] vCircle = circles.get(0, i);

            Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
            int radius = (int) Math.round(vCircle[2]);

            circle(mRgba, pt, radius, new Scalar(255, 0, 0), 2);
            circle(mRgba, pt, 3, new Scalar(0,0,255), 2);
        }


        String myString = "La cantidad de monedas presentes es:" + circles.cols() + touchdetector;

        Imgproc.putText(mRgba, myString, new Point(10, setScreenHeight - 30), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 4);

        if(touchdetector==false){
            returnFrame=mRgba;
        }

        if(touchdetector){
            showResults=myResultClass.puzzleFrame(inputFrame.rgba(),circles);
            returnFrame=showResults;
        }

        return returnFrame;
    }

    public boolean onTouch(View v, MotionEvent event) {
        if(touchdetector==false) {
            touchdetector = true;
        }else{
            touchdetector = false;
        }


        return false; // don't need subsequent touch events
    }

}
