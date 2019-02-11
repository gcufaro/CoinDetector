package org.opencv.samples.colorblobdetect;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.OutputStream;

import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;

import android.graphics.Bitmap;
import org.opencv.android.Utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static java.lang.System.out;

/**
 * This class is a controller for puzzle game.
 * It converts the image from Camera into the shuffled image
 */
public class ResultProcessor {

    private static final int GRID_SIZE_X = 6;
    private static final int GRID_SIZE_Y = 3;

    private static final int GRID_AREA = GRID_SIZE_X * GRID_SIZE_Y;
    private static final int GRID_EMPTY_INDEX = GRID_AREA - 1;
    private static final String TAG = "Puzzle15Processor";

    //define colors
    private static final Scalar COLOR_GREY = new Scalar(0x33, 0x33, 0x33, 0xFF);
    private static final Scalar COLOR_BLACK = new Scalar(0x00, 0x00, 0x00, 0xFF);
    private static final Scalar COLOR_WHITE = new Scalar(0xFF, 0xFF, 0xFF, 0xFF);
    private static final Scalar COLOR_RED = new Scalar(0xFF, 0x00, 0x00, 0xFF);

    private int screenWidth;
    private int screenHeight;

    private float red;
    private float green;
    private float blue;

    private int[]   mIndexes;
    private int[]   mTextWidths;
    private int[]   mTextHeights;

    private Mat mRgba15;
    private Mat[] mCells15;
    private boolean mShowTileNumbers = true;

    public ResultProcessor() {
        mTextWidths = new int[GRID_AREA];
        mTextHeights = new int[GRID_AREA];

        mIndexes = new int [GRID_AREA];

        for (int i = 0; i < GRID_AREA; i++)
            mIndexes[i] = i;
    }


    /* This method is to make the processor know the size of the frames that
     * will be delivered via puzzleFrame.
     * If the frames will be different size - then the result is unpredictable
     */
    public synchronized void prepareGameSize(int width, int height) {
        mRgba15 = new Mat(height, width, CvType.CV_8UC4);
        mCells15 = new Mat[GRID_AREA];

        screenWidth=width;
        screenHeight=height;

        for (int i = 0; i < GRID_SIZE_Y; i++) {
            for (int j = 0; j < GRID_SIZE_X; j++) {
                int k = i * GRID_SIZE_X + j;
                int colEnd;
                if(j!=(GRID_SIZE_X-1)){
                  colEnd=(j+1) * height / GRID_SIZE_Y;
                }else{
                    colEnd=width;
                }
                mCells15[k] = mRgba15.submat(
                        i * height / GRID_SIZE_Y, (i + 1) * height / GRID_SIZE_Y,
                        j * height / GRID_SIZE_Y, colEnd);
            }
        }

        for (int i = 0; i < GRID_AREA; i++) {
            Size s = Imgproc.getTextSize(Integer.toString(i + 1), 3/* CV_FONT_HERSHEY_COMPLEX */, 3, 4, null);
            mTextHeights[i] = (int) s.height;
            mTextWidths[i] = (int) s.width;
        }
    }

    /* this method to be called from the outside. it processes the frame and shuffles
     * the tiles as specified by mIndexes array
     */
    public synchronized Mat assemblyFrame(ArrayList<Mat> matCollection) {
        // copy tiles
        //i: index of number of tile in screen
        //idx: index of number of coin photo of ArrayList

        int idx = 0;


        for (int i = 0; i < GRID_AREA; i++){
            if (((i+1) % GRID_SIZE_X) == 0) {
                mCells15[i].setTo(COLOR_BLACK);
            }else if (idx>=matCollection.size()){
                mCells15[i].setTo(COLOR_GREY);
            }else{
                matCollection.get(idx).copyTo(mCells15[i]);

                Imgproc.putText(mCells15[i], Integer.toString(1 + idx), new Point((screenHeight / GRID_SIZE_Y - mTextWidths[idx])/2,
                        (screenHeight / GRID_SIZE_Y + mTextHeights[idx])/2), 3/* CV_FONT_HERSHEY_COMPLEX */, 3, COLOR_WHITE, 4);

                idx++;
            }
        }

        //draw grid
        int rows = screenHeight;
        drawGrid(rows, mRgba15);

        return mRgba15;
    }

    public synchronized Mat getFrame(Mat inputPicture, Mat circles, int index) {
        int squareSize=screenHeight / GRID_SIZE_Y;


        double[] vCircle = circles.get(0, index);
        int xCircle = (int) Math.round(vCircle[0]);
        int yCircle = (int) Math.round(vCircle[1]);
        int radius = (int) Math.round(vCircle[2]);

        Mat myCoin = new Mat();
        Mat mask = new Mat();
        Mat myCoinScaled = new Mat();
        Mat blackMat = new Mat(squareSize, squareSize, CvType.CV_8UC4);
        blackMat.setTo(COLOR_BLACK);

        Mat subMat = inputPicture.submat(checkRow(yCircle-radius),checkRow(yCircle+radius),
                checkCols(xCircle-radius),checkCols(xCircle+radius));


        if((subMat.cols()>0)&&(subMat.rows()>0)) {
            subMat.copyTo(myCoin);
            subMat.copyTo(mask);
        }else{
            blackMat.copyTo(myCoin);
            blackMat.copyTo(mask);
        }


        mask.setTo(COLOR_BLACK);
        Point centerMask = new Point(radius, radius);


        Imgproc.circle(mask,centerMask, radius, COLOR_WHITE, -1);
        Core.bitwise_and(mask,myCoin,mask);

        Bitmap myCoinBitmap;
        Bitmap maskBitmap;
        Bitmap ScaledBitmap;

        //create bitmap of coin photo (only for debugging)
        myCoinBitmap = Bitmap.createBitmap(myCoin.cols(), myCoin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(myCoin, myCoinBitmap);

        //create bitmap of mask (that contains processed coin)
        maskBitmap = Bitmap.createBitmap(mask.cols(), mask.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mask, maskBitmap);

        //scale bitmap to show in screen
        ScaledBitmap = Bitmap.createScaledBitmap(maskBitmap, squareSize, squareSize, false);
        Utils.bitmapToMat(ScaledBitmap, myCoinScaled);

        //getHist(myCoinScaled);

        int colour = 0;
        int red = 0;
        int blue = 0;
        int green = 0;

        for( int i = squareSize/4; i < 3*squareSize/4; i++ ) {
            for (int j = squareSize/4; j < 3*squareSize/4; j++) {
                colour = ScaledBitmap.getPixel(i, j);
                red = red + Color.red(colour);
                green = green + Color.green(colour);
                blue = blue + Color.blue(colour);

            }
        }
        red = red / ((squareSize/2) * (squareSize/2));
        green = green / ((squareSize/2) * (squareSize/2));
        blue = blue / ((squareSize/2) * (squareSize/2));

        return myCoinScaled;
    }

    private void drawGrid(int rows, Mat drawMat) {
        int squareSize=rows / GRID_SIZE_Y;
        int xLimit=squareSize*(GRID_SIZE_X-1);

        //draw horizontal lines
        for (int i = 1; i < GRID_SIZE_Y; i++) {
            Imgproc.line(drawMat, new Point(0, i * squareSize), new Point(xLimit, i* squareSize), COLOR_RED, 3);
        }

        //draw vertical lines
        for (int i = 1; i < GRID_SIZE_X; i++) {
            Imgproc.line(drawMat, new Point(i * squareSize, 0), new Point(i * squareSize, rows), COLOR_RED, 3);
        }
    }

    //check if row is inside bounds, if not, return limits
    private int checkRow(int rows){
        if((rows<0)){
            return 0;
        }else if(rows>screenHeight){
            return screenHeight;
        }else{
            return rows;
        }
    }

    //check if cols is inside bounds, if not, return limits
    private int checkCols(int cols){
        if((cols<0)){
            return 0;
        }else if(cols>screenWidth){
            return screenWidth;
        }else{
            return cols;
        }
    }

    private void getHist(Mat drawMat) {
        List<Mat> bgrPlanes = new ArrayList<>();
        Core.split(drawMat, bgrPlanes);

        int histSize = 256;
        float[] range = {0, 256}; //the upper boundary is exclusive
        MatOfFloat histRange = new MatOfFloat(range);

        boolean accumulate = false;
        Mat bHist = new Mat(), gHist = new Mat(), rHist = new Mat();
        Imgproc.calcHist(bgrPlanes, new MatOfInt(0), new Mat(), bHist, new MatOfInt(histSize), histRange, accumulate);
        Imgproc.calcHist(bgrPlanes, new MatOfInt(1), new Mat(), gHist, new MatOfInt(histSize), histRange, accumulate);
        Imgproc.calcHist(bgrPlanes, new MatOfInt(2), new Mat(), rHist, new MatOfInt(histSize), histRange, accumulate);


        int histW = 512, histH = 400;
        int binW = (int) Math.round((double) histW / histSize);
        Mat histImage = new Mat( histH, histW, CvType.CV_8UC3, new Scalar( 0,0,0) );

        Core.normalize(bHist, bHist, 0, histImage.rows(), Core.NORM_MINMAX);
        Core.normalize(gHist, gHist, 0, histImage.rows(), Core.NORM_MINMAX);
        Core.normalize(rHist, rHist, 0, histImage.rows(), Core.NORM_MINMAX);

        float[] bHistData = new float[(int) (bHist.total() * bHist.channels())];
        bHist.get(0, 0, bHistData);
        float[] gHistData = new float[(int) (gHist.total() * gHist.channels())];
        gHist.get(0, 0, gHistData);
        float[] rHistData = new float[(int) (rHist.total() * rHist.channels())];
        rHist.get(0, 0, rHistData);
        for( int i = 1; i < histSize; i++ ) {
            Imgproc.line(histImage, new Point(binW * (i - 1), histH - Math.round(bHistData[i - 1])),
                    new Point(binW * (i), histH - Math.round(bHistData[i])), new Scalar(255, 0, 0), 2);
            Imgproc.line(histImage, new Point(binW * (i - 1), histH - Math.round(gHistData[i - 1])),
                    new Point(binW * (i), histH - Math.round(gHistData[i])), new Scalar(0, 255, 0), 2);
            Imgproc.line(histImage, new Point(binW * (i - 1), histH - Math.round(rHistData[i - 1])),
                    new Point(binW * (i), histH - Math.round(rHistData[i])), new Scalar(0, 0, 255), 2);
        }

        Bitmap resultBitmapHist = Bitmap.createBitmap(histImage.cols(), histImage.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(histImage, resultBitmapHist);

        Bitmap resultBitmap = Bitmap.createBitmap(histImage.cols(), histImage.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(histImage, resultBitmap);
        int dummy=0;

        red=findAverageWithoutUsingStream(rHistData);
        green=findAverageWithoutUsingStream(gHistData);
        blue=findAverageWithoutUsingStream(bHistData);

    }

    private static float findSumWithoutUsingStream(float[] array) {
        float sum = 0;
        for (float value : array) {
            sum += value;
        }
        return sum;
    }

    private static float findAverageWithoutUsingStream(float[] array) {
        float sum = findSumWithoutUsingStream(array);
        return sum / array.length;
    }



}
