package com.chivumarius.facedetection;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.face.Contour;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class MainActivity extends AppCompatActivity {

    // ▼ "DECLARATION" OF "WIDGETS IDS" → FROM "ACTIVITY_MAIN. XML" FILE
    ImageView innerImage;
    TextView resultTv;
    private Uri image_uri;


    private static final int RESULT_LOAD_IMAGE = 123;
    public static final int IMAGE_CAPTURE_CODE = 654;
    private static final int PERMISSION_CODE = 321;


    // ▼ "DECLARATION" OF "FACE DETECTOR" ▼
    FaceDetector detector;




    // ▼ "ON CREATE()" METHOD ▼
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // ▼ "INITIALIZATION" OF "WIDGETS IDS" → FROM "ACTIVITY_MAIN. XML" FILE
        innerImage = findViewById(R.id.imageView2);
        resultTv = findViewById(R.id.textView2);


        // ▼ "SET ON CLICK LISTENER" METHOD → FOR "INNER IMAGE" ▼
        innerImage.setOnClickListener(new View.OnClickListener() {

            // ▼ "ON CLICK" METHOD ▼
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
            }
        });





        // ▼ "SET ON LONG CLICK LISTENER" METHOD → FOR "INNER IMAGE" ▼
        innerImage.setOnLongClickListener(new View.OnLongClickListener() {

            // ▼ "ON LONG CLICK" METHOD ▼
            @Override
            public boolean onLongClick(View v) {

                // ▼ "CALLING" THE "METHOD" ▼
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    // ▼ "CHECK:" IF "PERMISSION" WAS "GRANTED" ▼
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, PERMISSION_CODE);

                    } else {
                        // ▼ "CALLING" THE "METHOD" ▼
                        openCamera();
                    }

                } else {
                    // ▼ "CALLING" THE "METHOD" ▼
                    openCamera();
                }

                return false;
            }
        });





        // ▼ "ML-KIT" → "FACE DETECTION OPTIONS" → WITH "IMAGES" ("HIGH-ACCURACY")
        //      • "LANDMARKS DETECTION",
        //      • "FACE CLASSIFICATION" AND
        //      • "CONTOUR DETECTION" ▼
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();


        // ▼ "INSTANTIATION"" OF "FACE DETECTOR" ▼
        detector = FaceDetection.getClient(highAccuracyOpts);
    }




    // ▼ "ON CREATE()" METHOD ▼
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        // ▼ "CHECK:" IF "PERMISSION" WAS "GRANTED" ▼
        if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // ▼ "CALLING" THE "METHOD" ▼
            openCamera();

        } else {
            // ▼ "DISPLAY" A "TOAST MESSAGE" ▼
            Toast.makeText(
                    this,
                    "Permission not granted",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }



    // ▼ "OPEN CAMERA()" METHOD ▼
    private void openCamera() {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }



    // ▼ "ON ACTIVITY RESULT()" METHOD ▼
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        // ▼ "CHECK:" IF "IMAGE" WAS "SELECTED" ▼
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null){
            image_uri = data.getData();
            innerImage.setImageURI(image_uri);
            doFaceDetection();
        }


        // ▼ "CHECK": IF "CAMERA" WAS "SELECTED" ▼
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK){
            innerImage.setImageURI(image_uri);
            doFaceDetection();
        }
    }




    // ▼ "DO FACE DETECTION()" METHOD
    //    → TO PERFORM "IMAGE LABELLING" ▼
    public void doFaceDetection(){
        Bitmap inputImage = uriToBitmap(image_uri);
        final Bitmap mutableBmp = inputImage.copy(Bitmap.Config.ARGB_8888,true);
        Bitmap rotated = rotateBitmap(mutableBmp);
        innerImage.setImageBitmap(rotated);

        // ▼ "DEGREE" OF "IMAGE ROTATION" ▼
        InputImage image = InputImage.fromBitmap(rotated, 0);




        // ▼ "CANVAS" OBJECT ▼
        Canvas canvas = new Canvas(rotated);


        // ▼ CREATING A "FACE PAINT" OBJECT ▼
        Paint facePaint = new Paint();

        // ▼ "SETTING" THE "COLOR" OF THE "PAINT" OBJECT ▼
        facePaint.setColor(Color.RED);

        // ▼ "SETTING" THE "STROKE WIDTH" OF THE "PAINT" OBJECT ▼
        facePaint.setStyle(Paint.Style.STROKE);

        // ▼ "SETTING" THE "STROKE WIDTH" OF THE "PAINT" OBJECT ▼
        facePaint.setStrokeWidth(10);




        // ▼ CREATING A "EYE PAINT" OBJECT ▼
        Paint eyePaint = new Paint();

        // ▼ "SETTING" THE "COLOR" OF THE "PAINT" OBJECT ▼
        eyePaint.setColor(Color.YELLOW);

        // ▼ "SETTING" THE "STROKE WIDTH" OF THE "PAINT" OBJECT ▼
        eyePaint.setStyle(Paint.Style.STROKE);

        // ▼ "SETTING" THE "STROKE WIDTH" OF THE "PAINT" OBJECT ▼
        eyePaint.setStrokeWidth(2);




        // ▼ "CALLING" THE "PROCESS()" METHOD → FOR "DETECTOR" ▼
        Task<List<Face>> result =
                detector.process(image)
                        // ▼ "ON SUCCESS" METHOD ▼
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {

                                    // ▼ "ON SUCCESS" METHOD ▼
                                    @Override
                                    public void onSuccess(List<Face> faces) {

                                        // ▼ LOOP:" FOR" "ALL" "FACES" ▼
                                        for (Face face : faces) {

                                            // ▼ "GET" THE "FACE" ▼
                                            // ▼ "DRAWING" A "RECTANGLE" AROUND THE "DETECTED FACE" ▼
                                            Rect bounds = face.getBoundingBox();
                                            canvas.drawRect(bounds, facePaint);

                                            // ▼ "GET" THE "FACE LANDMARKS INFO" ▼
                                            float rotY = face.getHeadEulerAngleY();  // ► Head is Rotated to the Right rotY Degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // ► Head is Tilted Sideways rotZ Degrees



                                            // ▼ "LANDMARK DETECTION" WAS ENABLED
                                            //      → (MOUTH, EARS, EYES, CHEEKS AND NOSE ARE AVAILABLE) ▼

                                            // ▼ "GETTING" THE "FACE LANDMARKS INFO" → "LEFT EYE" ▼
                                            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                                            if (leftEye != null) {

                                                // ▼ "POSITION" OF "LEFT EYE" ▼
                                                PointF leftEyePos = leftEye.getPosition();
                                            }


                                            // ▼ "GETTING" THE "FACE LANDMARKS INFO" → "LEFT EAR" ▼
                                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {

                                                // ▼ "POSITION" OF "LEFT EAR" ▼
                                                PointF leftEarPos = leftEar.getPosition();
                                            }



                                            // ▼ "CONTOUR DETECTION" WAS "ENABLED"
                                            //      → WE GET THE LIST OF POINTS
                                            //      → THAT "SURROUND" THE "FACE" ▼

                                            // ▼ "GETTING" THE "CONTOUR" → FOR THE "LEFT EYE" ▼
                                            List<PointF> leftEyeContour =
                                                    face.getContour(FaceContour.LEFT_EYE).getPoints();


                                            // ▼  DRAWING THE "CONTOUR" → FOR THE "LEFT EYE" ▼
                                            //for(PointF point: leftEyeContour){
                                            //    // ▼ DRAWING THE "POINTS" ▼
                                            //    canvas.drawPoint(point.x, point.y, eyePaint);
                                            //}


                                            // ▼ "GETTING" THE "CONTOUR" → FOR THE "UPPER LIP BOTTOM" ▼
                                            List<PointF> upperLipBottomContour =
                                                    face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();



                                            // ▼ GETTING "ALL" THE "CONTOURS" → FOR THE "FACE" ▼
                                            List<FaceContour> faceContours = face.getAllContours();

                                            // ▼ DRAWING "EACH FACE CONTOUR "OBJECT ▼
                                            for(FaceContour faceContour: faceContours){
                                                // ▼ GETTING THE "CONTOUR" → FOR THE "FACE" ▼
                                                List<PointF> pointFS = faceContour.getPoints();

                                                // ▼  DRAWING THE "CONTOUR" → FOR THE "LEFT EYE" ▼
                                                for(PointF point: pointFS){
                                                    // ▼ DRAWING THE "POINTS" ▼
                                                    canvas.drawPoint(point.x, point.y, eyePaint);
                                                }
                                            }




                                            // ▼ "CLASSIFICATION" WAS "ENABLED"
                                            //      → WE WILL GET THE "SMILING PROBABILITY" ▼
                                            //      ► "SMILING PROBABILITY" IS BETWEEN: "0" AND "1"
                                            //      → ( "SMILING FACE" > "0.5"
                                            //      →   "SAD FACE" < "0.5" )
                                            if (face.getSmilingProbability() != null) {
                                                float smileProb = face.getSmilingProbability();

                                                // ▼ "DISPLAY" THE "SMILE PROBABILITY" ▼
                                                if (smileProb > 0.5) {
                                                    resultTv.setText("The Person is Smiling");
                                                } else {
                                                    resultTv.setText("The Person is Seriously");
                                                }
                                            }

                                            // ▼ "GETTING" THE "RIGHT EYE OPEN PROBABILITY"
                                            if (face.getRightEyeOpenProbability() != null) {
                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                            }


                                            // ▼ IF "FACE TRACKING" WAS "ENABLED"
                                            //      → WE WILL GET THE "FACE TRACKING ID" ▼
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();
                                            }

                                            // ▼ GETTING THE "FACE ID" ▼
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();
                                            }
                                        }

                                        // ▼ "DISPLAY" THE "IMAGE" ▼
                                        innerImage.setImageBitmap(rotated);
                                    }
                                })


                        // ▼ "ON FAILURE" METHOD ▼
                        .addOnFailureListener(
                                new OnFailureListener() {

                                    // ▼ "ON FAILURE" METHOD ▼
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // ▼ "ON FAILURE" METHOD ▼
                                        // ▼ "DISPLAY" THE "ERROR" MESSAGE ▼
                                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();

                                    }
                                });
    }



    // ▼ "ON DESTROY()" METHOD ▼
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }




    // ▼ "ROTATE BITMAP()" METHOD
    //    → TO ROTATE THE "IMAGE"
    //    → IF "IMAGE" IS "CAPTURED" ON "SAMSUNG DEVICE" ▼
    //    → (MOST "PHONE CAMERAS" ARE "LANDSCAPE",
    //    →  "MEANING" IF WE "TAKE" A "PHOTO" IN "PORTRAIT",
    //    →  THE "RESULTING PHOTO" WILL BE ROTATED "90 DEGREES")
    @SuppressLint("Range")
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;

        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }

        Log.d("tryOrientation",orientation+"");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return cropped;
    }




    // ▼ "URI TO BITMAP()" METHOD ▼
    //    → FOR "TAKING" THE "URI" OF THE "IMAGE"
    //    → AND RETURNING THE "BITMAP" ▼
    private Bitmap uriToBitmap(Uri selectedFileUri) {

        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  null;
    }

}