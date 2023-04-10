package com.example.sensorsproject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.DataFormatException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    String TAG = MainActivity.class.getSimpleName();
    TextView availableSensorsView;
    SensorManager mSensorManager;
    List<Sensor> mListAllSensors;
    Sensor mPressureSensor;
    Sensor mTemperatureSensor;
    ListView listSensorValues;
    ArrayList<String> sensorValuesArray = new ArrayList<>();
    ArrayAdapter adapterSensorValues;
    String mCurrentPressureSensorValue = "";
    String mNewPressureSensorValue = "";
    boolean mPressureSensorValueChanged = false;
    String mCurrentTempSensorValue = "";
    String mNewTempSensorValue = "";
    boolean mTempSensorValueChanged = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        availableSensorsView = (TextView) findViewById(R.id.ListofSensorsAvailable);
        listSensorValues = (ListView) findViewById(R.id.sensorValuesList);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mListAllSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        mPressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mTemperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        adapterSensorValues = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, sensorValuesArray);
        listSensorValues.setAdapter(adapterSensorValues);

        for(int sensorid = 0; sensorid < mListAllSensors.size(); sensorid++) {
            availableSensorsView.append("\n" + "Sensor Number   : " + sensorid + "  " + mListAllSensors.get(sensorid).getName());
        }

        if(mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            mSensorManager.registerListener(this, mPressureSensor, 3000000);
        }
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            mSensorManager.registerListener(this, mTemperatureSensor, 3000000);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if ((checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED))
            {
                Log.d(TAG, "Permissions are granted");
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
            }
        }
        else {
            Log.d(TAG, "Permissions are granted");
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED))
            {
                Log.d(TAG, "Permissions are granted");
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1);
            }
        }
        else {
            Log.d(TAG, "Permissions are granted");
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED))
            {
                Log.d(TAG, "Permissions are granted");
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 1);
            }
        }
        else {
            Log.d(TAG, "Permissions are granted");
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if ((checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED))
            {
                Log.d(TAG, "Permissions are granted");
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 1);
            }
        }
        else {
            Log.d(TAG, "Permissions are granted");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mPressureSensorValueChanged = false;
        mTempSensorValueChanged = false;

        float sensorValueRead = event.values[0];
        
        //        TextView sensorValueHolder = new TextView(MainActivity.this);
        //        sensorValueHolder.setText("Pressure Value : " + sensorValueHolder.toString());

        String valueChanged = "00.00";
        Sensor sensorType = event.sensor;

        if(sensorType.getType() == Sensor.TYPE_PRESSURE)
        {
            mCurrentPressureSensorValue = mNewPressureSensorValue;
            mNewPressureSensorValue = Float.toString(sensorValueRead);
            if(!mCurrentPressureSensorValue.equals(mNewPressureSensorValue))
                mPressureSensorValueChanged = true;
            valueChanged = "Pressure Value : " + mNewPressureSensorValue;
        }
        if(sensorType.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            mCurrentTempSensorValue = mNewTempSensorValue;
            mNewTempSensorValue = Float.toString(sensorValueRead);
            if(!mCurrentTempSensorValue.equals(mNewTempSensorValue))
                mTempSensorValueChanged = true;
            valueChanged = "Temp Value : " + mNewTempSensorValue;
        }
        if(mPressureSensorValueChanged == true || mTempSensorValueChanged == true) {
            DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy , HH:mm:ss");
            String currDateTime = dateFormat.format(Calendar.getInstance().getTime());
            valueChanged = currDateTime + " " + valueChanged;
            sensorValuesArray.add(valueChanged);
            adapterSensorValues.notifyDataSetChanged();
            SendSensorValuesToServer sendToServer = new SendSensorValuesToServer();
            sendToServer.execute("10.0.2.2", valueChanged);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class SendSensorValuesToServer extends AsyncTask<String,Void,Boolean>
    {
        int port = 8080;
        Socket sensorClientSocket;
        String serverIP = "10.0.2.2";
        String TAGAsync = SendSensorValuesToServer.class.getSimpleName();
        @Override
        protected Boolean doInBackground(String... params) {
            serverIP = params[0];
            try {
                Log.d(TAGAsync, "Trying to connect to the Server " + serverIP + " to send Sensor Readings");
                sensorClientSocket = new Socket(serverIP, port);
                Log.d(TAGAsync, "Connected to the Server " + serverIP + " to start sending the Sensor Readings");

                String valueToServer = params[1];
                String ackReceived;
                DataOutputStream outputToServer = new DataOutputStream(sensorClientSocket.getOutputStream());
                InputStreamReader inputFromServer = new InputStreamReader(sensorClientSocket.getInputStream());
                BufferedReader ackBuffReceived = new BufferedReader(inputFromServer);

                outputToServer.writeBytes(valueToServer + "\n");

                ackReceived = ackBuffReceived.readLine();

                Log.d(TAGAsync, "Acknowledgement is received from the server " + serverIP + " as -----" + ackReceived + "----- for the sent value " + valueToServer);

            } catch (IOException exp) {

            }
            return null;
        }
    }
}