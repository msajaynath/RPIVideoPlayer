package com.aj.videplayertest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.aj.videplayertest.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceMonitorActivity extends AppCompatActivity {
    private  Model model;
    private  ImageProcessor imageProcessor;
    TextView text,error,reconnectView,fps;
    private Server server;
    private ClientSocket socket;
    private ImageView preview;
    private Button stop,close;
    private Switch recordingSwitch;
    final int RECONNECT_AFTER = 10000;
    long timeelapsed = 0;
    private int IMAGE_SIZE = 224;
    private List<Bitmap> bitmapList;

    Timer timer ;
    private TensorImage tensorImage;
    private TensorBuffer inputFeature0;

    public DeviceMonitorActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_device_monitor);
        LoadModel();
        bitmapList = new ArrayList<>();
        text = findViewById(R.id.status);
        error = findViewById(R.id.statusError);
        preview = findViewById(R.id.preview);
        stop = findViewById(R.id.stop);
        reconnectView = findViewById(R.id.reconnectText);
        close = findViewById(R.id.close);
        fps = findViewById(R.id.fps);
        recordingSwitch = findViewById(R.id.recording);
        ShowOrHideButton(true);
        timeelapsed = System.currentTimeMillis();

        this.socket = new ClientSocket(getApplicationContext(), new OnClientConnected() {
            @Override
            public void onMessageRecived() {
//                runOnUiThread();
                // updateText("Client message");

            }

            @Override
            public void onClientConnected() {
                updateText("Client connected");
                ShowOrHideButton(false);



            }

            @Override
            public void onClientDisconnected(String message) {
                updateText(message);
                ShowOrHideButton(false);

            }

            @Override
            public void onClientError(String message) {

                updateTextError(message);
                stop.setVisibility(View.INVISIBLE);
                ShowOrHideButton(false);

            }

            @Override
            public void updateFPS(String message) {
                updateFPSText(message);
            }

            @Override
            public void onFrameRecieved(Bitmap frame) {
                DisplayPreview(frame);
                bitmapList.add(frame);
                //ClassifyImageNew(frame,false);
            }


        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                socket.stop();
            }
        });

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!socket.isRunning()) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - timeelapsed >= RECONNECT_AFTER) {
                        socket.start();
                        ShowOrHideButton(true);
                        updateTimerText("Reconnecting now");
                        timeelapsed = System.currentTimeMillis();

                    } else {
                        int reconnectinterval = (int) ((RECONNECT_AFTER - (currentTime - timeelapsed)) / 1000);
                        updateTimerText("Auto Reconnecting in " + reconnectinterval + " seconds.");

                    }


                }


            }
        }, 0, 1000);

    close.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(timer !=null) {
                timer = null;
            }
            if(socket != null)
            {
                socket = null;
            }
            finish();
        }
    });
        socket.ToggleRecording(false);

        recordingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if(b)
            {
                socket.ToggleRecording(true);
            }
            else
            {
                socket.ToggleRecording(false);
            }
        }
    });


        /////New thread for processing images

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true)
                {

                    if(bitmapList.size()>0)
                    {
                        Bitmap b=  bitmapList.get(0);
                        bitmapList.remove(0);
                        ClassifyImageNew(b,false);

                    }
                    if(bitmapList.size()>50)
                    {
                        for(int i =0;i<25;i++)
                        {
                            bitmapList.remove(0);
                        }
                    }
                }
            }
            });
        t1.start();


    }

    @Override
    protected void onStop() {
        super.onStop();
        if(socket !=null) {
            this.socket = null;
        }
        if(timer !=null) {
            timer = null;
        }
    }

    private void updateText(String message)
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                text.setText(message);
            }
        });


    }

    private void updateFPSText(String message)
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                fps.setText(message);
            }
        });


    }

    private void ShowOrHideButton(boolean hide)
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if(hide)
                {
                    stop.setVisibility(View.INVISIBLE);
                }

                else
                {
                    stop.setVisibility(View.VISIBLE);
                }




            }
        });
    }

    private void updateTimerText(String message)
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                reconnectView.setText(message);
            }
        });


    }

    private void updateTextError(String message)
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                error.setText(message);
            }
        });


    }

    private void DisplayPreview(Bitmap frame)
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
               // Bitmap bmp = BitmapFactory.decodeByteArray(frame, 0, frame.length);
                preview.setImageBitmap(frame);
            }
            });
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
            updateTextError(e.getMessage());

        }
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
               String[] classes = {"Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 9"};
               //    result.setText(classes[maxPos]);
               //Log.d("Inference result ",classes[maxPos]);
               // Log.d("Inference result  ","Max Confidence = "+maxConfidence);
               returnString = "," + classes[maxPos] + ", Confidence =  " + maxConfidence / 255;

               // maxConfidence = maxConfidence*100;
               updateTextError(classes[maxPos] + " confidence = " + maxConfidence);
//            if(saveImage) {
//                saveImage(inputImage, confidences, classes, maxConfidence, maxPos);
//            }


           }
            // Releases model resources if no longer used.
            //model.close();
        } catch (Exception e) {
            updateTextError(e.getMessage());
            // TODO Handle the exception
        }
        return  returnString;
    }
}