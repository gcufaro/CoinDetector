package org.opencv.samples.colorblobdetect;


import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

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
    private static final Scalar GRID_EMPTY_COLOR = new Scalar(0x33, 0x33, 0x33, 0xFF);

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

/*        for (int i = 0; i < GRID_AREA; i++) {
            Size s = Imgproc.getTextSize(Integer.toString(i + 1), 3*//* CV_FONT_HERSHEY_COMPLEX *//*, 1, 2, null);
            mTextHeights[i] = (int) s.height;
            mTextWidths[i] = (int) s.width;
        }*/
    }

    /* this method to be called from the outside. it processes the frame and shuffles
     * the tiles as specified by mIndexes array
     */
    public synchronized Mat puzzleFrame(Mat inputPicture, Mat circles) {
        Mat[] cells = new Mat[GRID_AREA];

        //get frames
        for (int i = 0; i < GRID_SIZE_Y; i++) {
            for (int j = 0; j < GRID_SIZE_X; j++) {
                int k = i * GRID_SIZE_X + j;
                int colEnd;
                if(j!=(GRID_SIZE_X-1)){
                    colEnd=(j+1) * inputPicture.rows() / GRID_SIZE_Y;
                }else{
                    colEnd=inputPicture.cols();
                }
                cells[k] = inputPicture.submat(
                    i * inputPicture.rows() / GRID_SIZE_Y, (i + 1) * inputPicture.rows() / GRID_SIZE_Y,
                    j * inputPicture.rows()/ GRID_SIZE_Y, colEnd);
            }
        }

        // copy shuffled tiles
        for (int i = 0; i < GRID_AREA; i++) {
            int idx = mIndexes[i];
            if ((idx+1) % GRID_SIZE_X == 0)
                mCells15[i].setTo(GRID_EMPTY_COLOR);
            else {
                cells[idx].copyTo(mCells15[i]);

            }
        }

        //release memory
        for (int i = 0; i < GRID_AREA; i++)
            cells[i].release();

        //draw grid
        int rows = inputPicture.rows();
        int cols = (inputPicture.rows()/GRID_SIZE_Y)*(GRID_SIZE_X-1);


        drawGrid(cols, rows, mRgba15);

        return mRgba15;
    }


    private void drawGrid(int cols, int rows, Mat drawMat) {
        for (int i = 1; i < GRID_SIZE_Y; i++) {
            Imgproc.line(drawMat, new Point(0, i * rows / GRID_SIZE_Y), new Point(cols, i * rows / GRID_SIZE_Y), new Scalar(0, 255, 0, 255), 3);
        }
        for (int i = 1; i < GRID_SIZE_X; i++) {
            Imgproc.line(drawMat, new Point(i * cols / GRID_SIZE_X, 0), new Point(i * cols / GRID_SIZE_X, rows), new Scalar(0, 255, 0, 255), 3);
        }


    }


}
