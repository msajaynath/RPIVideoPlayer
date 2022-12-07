package com.aj.videplayertest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.aj.videplayertest.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ClientSocket implements Runnable
{
    private int IMAGE_SIZE = 224;
    private Thread thread;
    private Context applicationContext;
    private Socket socket;
    private DataInputStream dataInputStream;
//    private DataOutputStream dataOutputStream;
    private OnClientConnected clientConnected;
    private long packetCounter =0 ;
    private boolean running = true;
    private boolean recording = false;
    private String directoryName  = "/RawImages";
    public ClientSocket(Context applicationContext, OnClientConnected clientConnected)
    {
        this.thread = new Thread( this );
        this.thread.setPriority( Thread.NORM_PRIORITY );
        this.thread.start();
        this.clientConnected = clientConnected;
        this.running = true;
        this.applicationContext = applicationContext;
       ///// CreateDirectoryIfNotExist();
    }

    private void CreateDirectoryIfNotExist()
    {
        File dir = new File(Environment.getExternalStorageDirectory() + directoryName);
        if(!dir.exists()) {
            dir.mkdir();
        }
    }

    public void stop()
    {
        this.running = false;
    }
    public void start()
    {
        if(!this.running){
            this.running = true;
            this.run();
        }
    }

    public void ToggleRecording(boolean flag){
        this.recording = flag;
    }

    public boolean isRunning()
    {
        return  running;
    }

    @Override
    public void run()
    {       // TestRun("T10");



        try {


            // create a server socket
            long packetCount = 1;
            while (true&&running) {
                try {
                    long start = System.currentTimeMillis();
                  //  this.clientConnected.onClientDisconnected("connected to server");

                    // create new socket and connect to the server
                    this.socket = new Socket("127.0.0.1", 12345);
                    //this.socket.setSoTimeout(500);
                    this.dataInputStream = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
                    byte[] byteArray = new byte[1024];
                    int read;

                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                    ///this.clientConnected.onClientDisconnected("connected to server");
                    while ((read = this.dataInputStream.read(byteArray)) != -1) {
                        byteBuffer.write(byteArray, 0, read);
                    }
                    byte[] byteArrayFinal = byteBuffer.toByteArray();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(byteArrayFinal, 0, byteArrayFinal.length);

                    this.clientConnected.onFrameRecieved(bitmap);
                    byteBuffer.close();
                    socket.close();
//                    long stop = System.currentTimeMillis();
//                    double FPS = (1.0/(stop - start))*1000;
//                    this.clientConnected.updateFPS("FPS  = " + Math.round(FPS));
                }
                catch (Exception e)
                {

                }



            }



        } catch (Exception e) {
            running = false;
            System.out.println("failed to read data");
            e.printStackTrace();
            this.clientConnected.onClientDisconnected("disconnected");

        }
        running = false;
        this.clientConnected.onClientDisconnected("Client Disconnected");



    }






    private String ClassifyImageNew(Bitmap inputImage,boolean saveImage) {
        String returnString = "";
        try {
            Bitmap image  = Bitmap.createScaledBitmap(inputImage, IMAGE_SIZE, IMAGE_SIZE, false);
            Model model = Model.newInstance(applicationContext);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3);
            byteBuffer.order(ByteOrder.nativeOrder());
            ImageProcessor imageProcessor =
                    new ImageProcessor.Builder()
                            .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                            .build();
            TensorImage tensorImage = new TensorImage(DataType.UINT8);
            tensorImage.load(inputImage);
            tensorImage = imageProcessor.process(tensorImage);
            ByteBuffer byteBufferNew =  tensorImage.getBuffer();

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
            String[] classes = {"Class 1", "Class 2", "Class 3","Class 4","Class 5","Class 6","Class 7","Class 8","Class 9"};
            //    result.setText(classes[maxPos]);
            //Log.d("Inference result ",classes[maxPos]);
            // Log.d("Inference result  ","Max Confidence = "+maxConfidence);
            returnString =  ","+ classes[maxPos] + ", Confidence =  " + maxConfidence/255;

           // maxConfidence = maxConfidence*100;
            this.clientConnected.onClientError( classes[maxPos] + " confidence = " + maxConfidence);
            if(saveImage) {
                saveImage(inputImage, confidences, classes, maxConfidence, maxPos);
            }



            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.d("test",e.getMessage());
            // TODO Handle the exception
        }
        return  returnString;
    }

    private void saveImage(Bitmap bitmap,int [] confidences, String[] classes,float maxConfidence,int maxpos)
    {

        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.getDefault());
        String formattedDate = df.format(c);
        String filename = formattedDate+""+classes[maxpos]+"_"+maxConfidence ;
        for(int i=0;i< classes.length;i++)
        {
            filename = filename +" "+classes[i]+"_"+confidences[i];
        }
        File dir = new File(Environment.getExternalStorageDirectory() + directoryName);

        File dest = new File(dir, filename+".png");
        try {
            FileOutputStream out = new FileOutputStream(dest);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class MyTask extends AsyncTask<Bitmap, Void, String> {

        @Override
        protected String doInBackground(Bitmap... params) {
            Bitmap url = params[0];
            ClassifyImageNew(url,false);
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // do something with result
        }
    }
}