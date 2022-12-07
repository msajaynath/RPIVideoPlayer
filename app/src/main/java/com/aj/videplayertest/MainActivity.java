package com.aj.videplayertest;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.aj.videplayertest.ml.Model;
import com.aj.videplayertest.models.Schedule;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private  Model model;
    private  ImageProcessor imageProcessor;
    private VideoView videoView;
    private TextView logText;
    private List<Schedule> scheduleList;
    private int start =28000, stop =31800;
    private int schedule  = 0;
    private long timepassed  = 0;
    private int IMAGE_SIZE = 224;
    private List<Bitmap> bitmapList;
    private ClientSocket socket;
    private int Threshold = 15;
    private int CurrentInferncedCounter = 0;
    Timer timer ;
    private TensorImage tensorImage;
    private TensorBuffer inputFeature0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            try {
                setContentView(R.layout.activity_main);
                videoView = findViewById(R.id.videoView);
                logText = findViewById(R.id.logText);
                LoadModel();
                bitmapList = new ArrayList<>();
                LoadSchedule();
                //videoView.setClickable(false);
                // videoView.onTouchEvent(null);
                MediaController mediaController = new MediaController(this);
                mediaController.setVisibility(View.GONE);
                mediaController.setAnchorView(videoView);
                Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video);
                videoView.setMediaController(mediaController);
                videoView.setVideoURI(uri);
                videoView.requestFocus();
                videoView.start();

                videoView.seekTo(0);
                timepassed = System.currentTimeMillis();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long timeout = System.currentTimeMillis() - timepassed;
                                if (CurrentInferncedCounter >= Threshold) {
                                    schedule++;
                                    updateText("Moving to " + schedule);
                                    resetCounters();
                                    return;

                                }
                                if (timeout >= scheduleList.get(schedule).getTimeout()) {
                                    if (schedule >= 14) {
                                        schedule = 0;
                                    } else {
                                        schedule++;

                                    }
                                    timepassed = System.currentTimeMillis();
                                }
                                Schedule s = scheduleList.get(schedule);
                                if (videoView != null) {
                                    if (videoView.getCurrentPosition() >= s.getEnd()) {
                                        videoView.seekTo(s.getStart());
                                        Log.d("testlog", s.getStart() + " ");
                                    }
                                }
                            }
                        });
                    }
                }, 0, 1);

                this.socket = new ClientSocket(getApplicationContext(), new OnClientConnected() {
                    @Override
                    public void onMessageRecived() {

                    }

                    @Override
                    public void onClientConnected() {
                        updateText("Client connectedd");
                    }

                    @Override
                    public void onClientDisconnected(String message) {
                        updateText(message);
                    }

                    @Override
                    public void onClientError(String message) {

                    }

                    @Override
                    public void updateFPS(String message) {

                    }

                    @Override
                    public void onFrameRecieved(Bitmap frame) {
                        bitmapList.add(frame);
                    }


                });

                Thread t1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {

                            if (bitmapList.size() > 0) {
                                Bitmap b = bitmapList.get(0);
                                bitmapList.remove(0);
                                if (schedule >= 4 && schedule <= 12) {

                                    ClassifyImageNew(b, false);
                                }

                            }
                            if (bitmapList.size() > 50) {
                                for (int i = 0; i < 25; i++) {
                                    bitmapList.remove(0);
                                }
                            }
                        }
                    }
                });
                t1.start();

            }
            catch (Exception e)
            {
                updateText(e.getMessage());
            }

    }
    private void updateText(String message)
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                logText.setText(message);
               // Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
            }
        });

    }
    private  void LoadSchedule()
    {   int limit =  100;
        scheduleList = new ArrayList<>();
        scheduleList.add(new Schedule(0,4000,4000,4000));
        scheduleList.add(new Schedule(4001,12000,8000,8000));
        scheduleList.add(new Schedule(12001,28000,16000,16000));
        scheduleList.add(new Schedule(28001,32000-limit,11400,4000));
        scheduleList.add(new Schedule(32001,36000-limit,11400,4000));
        scheduleList.add(new Schedule(36001,40000-limit,11400,4000));
        scheduleList.add(new Schedule(40001,44000-limit,11400,4000));
        scheduleList.add(new Schedule(44001,48000-limit,11400,4000));
        scheduleList.add(new Schedule(48001,52000-limit,11400,4000));
        scheduleList.add(new Schedule(52001,56000-limit,11400,4000));
        scheduleList.add(new Schedule(56001,60000-limit,11400,4000));
        scheduleList.add(new Schedule(60001,64000-limit,11400,4000));
        scheduleList.add(new Schedule(64001,68000-limit,11400,4000));
        scheduleList.add(new Schedule(68001,72000-limit,11400,4000));
        scheduleList.add(new Schedule(72001,76000-limit,11400,4000));
    }

    private void LoadModel()
    {
        try {
            model = Model.newInstance(getApplicationContext());
            inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3);
            byteBuffer.order(ByteOrder.nativeOrder());
            imageProcessor =
                    new ImageProcessor.Builder()
                            .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                            .build();
            tensorImage = new TensorImage(DataType.UINT8);
            // Creates inputs for reference.

        }
        catch(Exception e)
        {
          ///  updateTextError(e.getMessage());

        }
    }
    private void resetCounters()
    {
        timepassed = System.currentTimeMillis();
        CurrentInferncedCounter = 0;

    }

    private String ClassifyImageNew(Bitmap inputImage,boolean saveImage) {
        String returnString = "";
        try {
            if(inputImage != null) {
                tensorImage.load(inputImage);
                tensorImage = imageProcessor.process(tensorImage);
                ByteBuffer byteBufferNew = tensorImage.getBuffer();
                inputFeature0.loadBuffer(byteBufferNew);

                // Runs model inference and gets result.
                Model.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                int[] confidences = outputFeature0.getIntArray();
                // find the index of the class with the biggest confidence.
                int maxPos = 0;
                float maxConfidence = 0;
                for (int i = 0; i < confidences.length; i++) {
                    if (confidences[i] > maxConfidence) {
                        maxConfidence = confidences[i];
                        maxPos = i;
                    }
                }

                if(schedule>=4 && schedule<=12) {
                    updateText("Max confidence :"+maxConfidence +" Max pos ="+maxPos);
                    if (confidences[schedule - 4] >= 254) {
                        CurrentInferncedCounter++;
                    }
                }
            }
            // Releases model resources if no longer used.
            //model.close();
        } catch (Exception e) {
            ////updateTextError(e.getMessage());
            // TODO Handle the exception
        }
        return  returnString;
    }

}