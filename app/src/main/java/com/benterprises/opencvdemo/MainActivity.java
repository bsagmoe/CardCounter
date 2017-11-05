package com.benterprises.opencvdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final Scalar GREEN = new Scalar(0, 255,0,255);

    private CardCameraView          mOpenCvCameraView;
    private int                     mFound = 0; // used to update the TextView
    private int                     mPhotoIndex = 0; // used when taking photos of the cards to use as references
    private String[]                mPhotoNames = new String[56];
    private boolean                 mShouldTakePicture = false;

    private Mat                     mRgba;
    private Mat                     mGray;

    private List<MatAndCard>        mPairs;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private String getCardFileName(Card.Suit suit, Card.Denomination denomination) {
        return getApplicationContext().getFilesDir().getPath().concat("/").concat(denomination + "of" + suit + ".jpg");
    }

    private void initPairs() {

        List<MatAndCard> pairs = new ArrayList<>();

        for (Card.Suit suit: Card.Suit.values()) {
            for (Card.Denomination denomination: Card.Denomination.values()) {
                String path = getCardFileName(suit, denomination);

                // These ones are screwing up the model right now
                if (denomination == Card.Denomination.KING || denomination == Card.Denomination.QUEEN || denomination == Card.Denomination.JACK) {
                    new File(path).delete();
                }

                if(new File(path).exists()) {
                    Log.i(TAG, "reading " + denomination.toString() + "of" + suit.toString() + ".jpg" );
                    // We stored the preprocessed version of the image.
                    Mat card = Imgcodecs.imread(path, Imgcodecs.IMREAD_GRAYSCALE);
                    pairs.add(new MatAndCard(card, new Card(suit, denomination)));
                }
            }
        }

        mPairs = pairs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int index = 0;
        for (Card.Suit suit: Card.Suit.values()) {
            for (Card.Denomination denomination: Card.Denomination.values()) {
                mPhotoNames[index] = denomination.toString() + "of" + suit.toString() + ".jpg";
                index++;
            }
        }

        mOpenCvCameraView = (CardCameraView) findViewById(R.id.surface_view);
        mOpenCvCameraView.setVisibility(View.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
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
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        // Only init the pairs when we finally have something to compare them with.
        initPairs();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    private List<MatOfPoint> getCardContoursFromImage(Mat grayScale) {
        // preprocess the image to make it nicer to analyze
        Imgproc.blur(grayScale, grayScale, new Size(5, 5));
        Imgproc.threshold(grayScale, grayScale, 120, 255, Imgproc.THRESH_BINARY);

        // Find the contours in the image
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(grayScale, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Sort them based on how large they are (largest to smallest)
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return (int) Imgproc.contourArea(o2) - (int) Imgproc.contourArea(o1);
            }
        });

        // If there are contours, find the ones that are probably cards
        // just a heuristic, so it's not the best, but it'll do.
        if(contours.size() > 0) {
            boolean foundProbableCard = false;
            double firstCardSize = 0, currentSize = 0;

            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint contour = contours.get(i);

                // if it takes up more than 1/2 of the view, it's probably not a card
                if(Imgproc.contourArea(contour) > .5 * mRgba.size().area()) {
                    contours.remove(i);
                    i--;
                } else {
                    if(!foundProbableCard) {
                        firstCardSize = Imgproc.contourArea(contour);
                        foundProbableCard = true;
                    } else {
                        currentSize = Imgproc.contourArea(contour);
                        if(currentSize/firstCardSize < .5) {
                            contours.remove(i);
                            i--;
                        }
                    }
                }
            }
        }

        return contours;
    }

    private List<Mat> getTransformedCardBitmaps(List<MatOfPoint> contours, Mat colorImage) {
        List<Mat> foundCardBitmaps = new ArrayList<>();

        if(contours.size() > 0) {
            // They'll be saved as 480 x 720 images
            Point[] transformPoints = new Point[] { new Point(0,0), new Point(479, 0), new Point(479, 719), new Point(0, 479) };

            for( int i = 0; i < contours.size(); i++ ) {
                MatOfPoint contour = contours.get(i);

                // allocate space for the card and its approximation
                MatOfPoint2f card = new MatOfPoint2f();
                MatOfPoint2f approx = new MatOfPoint2f();

                // Read a contour and create a float matrix from it
                // Needs to be of type MatOfPoint2f for approxPolyDP to work
                contours.get(i).convertTo(card, CvType.CV_32F);
                double length = Imgproc.arcLength(card, true);

                // create an approximate representation of the card's contour
                Imgproc.approxPolyDP(card, approx, length * .02, true);

                // find a minimally bounding rectangle for this approximation
                RotatedRect cardRect = Imgproc.minAreaRect(approx);

                // Draw each of these rectangles so the user knows what's being recognized
                Point[] vertices = new Point[4];
                cardRect.points(vertices);  // write the vertices into this array

                for (int j = 0; j < 4; j++) {
                    Imgproc.line(mRgba, vertices[j], vertices[(j+1)%4], GREEN);
                }

                // Matricies used to do the affine transform
                MatOfPoint2f src = new MatOfPoint2f();
                MatOfPoint2f dst = new MatOfPoint2f();

                // Use the verticies of the rectangle for the transform
                Point[] verticies = new Point[4];
                cardRect.points(vertices);  // writes to vertices (not very Java-like)

                src.fromArray(vertices);
                dst.fromArray(transformPoints);

                Mat transform = Imgproc.getPerspectiveTransform(src, dst);
                Mat tempImage = new MatOfPoint2f();
                Imgproc.warpPerspective(colorImage, tempImage, transform, new Size(720, 480));
                foundCardBitmaps.add(tempImage);

                // The native stuff isn't garbage collected unless you do this
                card.release();
                approx.release();
                src.release();
                dst.release();
            }
        }

        return foundCardBitmaps;
    }

    // Used on both the found cards and the library of cards
    private Mat preprocess(Mat image) {
        Mat res = new Mat();
        Imgproc.cvtColor(image, res, COLOR_BGR2GRAY);
        Imgproc.blur(res, res, new Size(5, 5));
        Imgproc.threshold(res, res, 120, 255, ADAPTIVE_THRESH_GAUSSIAN_C);
        return res;
    }

    private Card findClosestCard(Mat card) {
        double min = Double.MAX_VALUE;
        int index = 0;
        Mat preprocessedCard = preprocess(card);

        for(int i = 0; i < mPairs.size(); i++) {
            Mat diff = new Mat();
            Log.i(TAG, "findClosestCard: " + preprocessedCard.channels() + " - " + mPairs.get(i).preprocessed.channels());
            Core.absdiff(preprocessedCard, mPairs.get(i).preprocessed, diff);
            Imgproc.blur(diff, diff, new Size(5,5));
            Imgproc.threshold(diff, diff, 200, 255, THRESH_BINARY);
            Scalar sum = Core.sumElems(diff);

            diff.release();

            Log.i(TAG, "sum: " + sum);

            if(sum.val[0] < min) {
                min = sum.val[0];
                index = i;
            }
        }

        preprocessedCard.release();

        if (mPairs.size() == 0) return null;
        return mPairs.get(index).card;
    }

    // Runs each time we get a new frame
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        List<MatOfPoint> contours = getCardContoursFromImage(mGray);
        final List<Mat> foundCards = getTransformedCardBitmaps(contours, inputFrame.rgba());

        final List<Card> cards = new ArrayList<>();
        int score = 0;

        for (Mat foundCard: foundCards) {
            Card closest = findClosestCard(foundCard);
            if(closest != null) {
                cards.add(closest);
                Log.i(TAG, "found: " + closest.toString());
                score += closest.getValue();
            }
        }

        mFound = foundCards.size();
        final int tempFound = mFound;
        final int tempScore = score;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.num_cards)).setText("Found " + tempFound + " cards, score is " + tempScore + ": " + cards.toString());
            }
        });

        if(mShouldTakePicture) {
            final String fileName = this.getFilesDir().getPath().concat("/" + mPhotoNames[mPhotoIndex]);
            mPhotoIndex++;

            Imgcodecs.imwrite(fileName, preprocess(foundCards.get(0)));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), fileName + " saved", Toast.LENGTH_SHORT).show();
                }
            });

            mShouldTakePicture = false;
        }

        return mRgba;
    }

    // This lets us save the preprocessed image from here instead of preprocessing it from an image file loaded from disk
    public void takePicture (View v) {
        mShouldTakePicture = true;
    }


}
