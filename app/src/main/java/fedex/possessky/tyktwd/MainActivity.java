package fedex.possessky.tyktwd;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Html;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;
import com.symbol.emdk.barcode.StatusData.ScannerStates;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class MainActivity extends Activity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener, OnCheckedChangeListener {

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private TextView textViewData, cityView;
    private TextView textViewStatus = null;
    private TextView textViewCurrent = null;
    private ImageView pointView = null;
    private EditText badgeId = null;
    private Spinner spinnerScannerDevices = null;
    private Button nextScreen, manualBtn;
    private List<ScannerInfo> deviceList = null;
    private int scannerIndex = 0;
    private int defaultIndex = 0;
    private int dataLength = 0;
    private String statusString = "";
    private boolean bSoftTriggerSelected = false;
    private boolean bDecoderSettingsChanged = false;
    private boolean bExtScannerDisconnected = false;
    private final Object lock = new Object();
    private Window window;
    private Bundle scanData;
    private int score = 0;
    private boolean cityScan = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        window = this.getWindow();
        window.setStatusBarColor(this.getResources().getColor(R.color.black));
        deviceList = new ArrayList<>();
        pointView = findViewById(R.id.pointView);
        textViewData = findViewById(R.id.textViewData);
        cityView = findViewById(R.id.cityView);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewCurrent = findViewById(R.id.currentScan);
        spinnerScannerDevices = findViewById(R.id.spinnerScannerDevices);
        nextScreen = findViewById(R.id.nextStep);
        manualBtn = findViewById(R.id.manualEntry);
        setBarcodeInfo();
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!");
            return;
        }

        addSpinnerScannerDevicesListener();

        textViewData.setMovementMethod(new ScrollingMovementMethod());

        nextScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanData = new Bundle();
                String[] fullList = textViewData.getText().toString().split("\n");

                //Testing the Array
                for(int i = 0; i < fullList.length;i++){

                    Log.println(Log.ASSERT, "String array element: "+ i,fullList[i]);
                }
                scanData.putStringArray("SCAN_DATA",fullList);

            }
        });

        final TextView _tv =  findViewById( R.id.timer );
        new CountDownTimer(1*60000, 1000) {

            public void onTick(long millisUntilFinished) {
                _tv.setText(new SimpleDateFormat("mm:ss").format(new Date( millisUntilFinished)));
            }
            //when game is finished, take data and move it to final screen.
            public void onFinish() {
                Intent endGame = new Intent(getApplicationContext(),Results.class);
                endGame.putExtra("finalScore", textViewCurrent.getText().toString());
                endGame.putExtra("mistakes",wrongScans);
                endGame.putExtra("totalScans",totalScans);
                startActivity(endGame);
            }
        }.start();

    }


    /**
     *
     * Zebra Methods to use scanner.
     *
     */

    @Override
    public void onOpened(EMDKManager emdkManager) {
        updateStatus("EMDK open success!");
        this.emdkManager = emdkManager;
        // Acquire the barcode manager resources
        initBarcodeManager();
        // Enumerate scanner devices
        enumerateScannerDevices();
        // Set default scanner
        spinnerScannerDevices.setSelection(defaultIndex);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // The application is in foreground
        if (emdkManager != null) {
            // Acquire the barcode manager resources
            initBarcodeManager();
            // Enumerate scanner devices
            enumerateScannerDevices();
            // Set selected scanner
            spinnerScannerDevices.setSelection(scannerIndex);
            // Initialize scanner
            initScanner();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        // The application is in background
        // Release the barcode manager resources
        deInitScanner();
        deInitBarcodeManager();
    }
    @Override
    public void onClosed() {
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
        updateStatus("EMDK closed unexpectedly! Please close and restart the application.");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }
    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanData> scanData = scanDataCollection.getScanData();
            for(ScanData data : scanData) {
                updateData(data.getData());
            }
        }
    }
    @Override
    public void onStatus(StatusData statusData) {
        ScannerStates state = statusData.getState();
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" is enabled and idle...";
                updateStatus(statusString);
                // set trigger type
                if(bSoftTriggerSelected) {
                    scanner.triggerType = TriggerType.SOFT_ONCE;
                    bSoftTriggerSelected = false;
                } else {
                    scanner.triggerType = TriggerType.HARD;
                }
                // set decoders
                if(bDecoderSettingsChanged) {
                    setDecoders();
                    bDecoderSettingsChanged = false;
                }
                // submit read
                if(!scanner.isReadPending() && !bExtScannerDisconnected) {
                    try {
                        scanner.read();
                    } catch (ScannerException e) {
                        updateStatus(e.getMessage());
                    }
                }
                break;
            case WAITING:
                statusString = "Scanner is waiting for trigger press...";
                updateStatus(statusString);
                break;
            case SCANNING:
                statusString = "Scanning...";
                updateStatus(statusString);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName()+" is disabled.";
                updateStatus(statusString);
                break;
            case ERROR:
                statusString = "An error has occurred.";
                updateStatus(statusString);
                break;
            default:
                break;
        }
    }
    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState) {
        String status;
        String scannerName = "";
        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();
        if (deviceList.size() != 0) {
            scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }
        if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {
            switch(connectionState) {
                case CONNECTED:
                    bSoftTriggerSelected = false;
                    synchronized (lock) {
                        initScanner();
                        bExtScannerDisconnected = false;
                    }
                    break;
                case DISCONNECTED:
                    bExtScannerDisconnected = true;
                    synchronized (lock) {
                        deInitScanner();
                    }
                    break;
            }
            status = scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
        }
        else {
            bExtScannerDisconnected = false;
            status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
        }
    }
    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
        bDecoderSettingsChanged = true;
        cancelRead();
    }
    private void initScanner() {
        if (scanner == null) {
            if ((deviceList != null) && (deviceList.size() != 0)) {
                if (barcodeManager != null)
                    scanner = barcodeManager.getDevice(deviceList.get(scannerIndex));
            }
            else {
                updateStatus("Failed to get the specified scanner device! Please close and restart the application.");
                return;
            }
            if (scanner != null) {
                scanner.addDataListener(this);
                scanner.addStatusListener(this);
                try {
                    scanner.enable();
                } catch (ScannerException e) {
                    updateStatus(e.getMessage());
                    deInitScanner();
                }
            }else{
                updateStatus("Failed to initialize the scanner device.");
            }
        }
    }
    private void deInitScanner() {
        if (scanner != null) {
            try{
                scanner.disable();
                scanner.release();
            } catch (Exception e) {
                updateStatus(e.getMessage());
            }
            scanner = null;
        }
    }
    private void initBarcodeManager(){
        barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);
        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }
    }
    private void deInitBarcodeManager(){
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.BARCODE);
        }
    }
    private void addSpinnerScannerDevicesListener() {
        spinnerScannerDevices.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int position, long arg3) {
                if ((scannerIndex != position) || (scanner==null)) {
                    scannerIndex = position;
                    bSoftTriggerSelected = false;
                    bExtScannerDisconnected = false;
                    deInitScanner();
                    initScanner();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }
    private void enumerateScannerDevices() {
        if (barcodeManager != null) {
            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;
            deviceList = barcodeManager.getSupportedDevicesInfo();
            if ((deviceList != null) && (deviceList.size() != 0)) {
                Iterator<ScannerInfo> it = deviceList.iterator();
                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if(scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            }
            else {
                updateStatus("Failed to get the list of supported scanner devices! Please close and restart the application.");
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, friendlyNameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerScannerDevices.setAdapter(spinnerAdapter);
        }
    }
    private void setDecoders() {
        if (scanner != null) {
            try {
                ScannerConfig config = scanner.getConfig();
                // Set EAN8
                config.decoderParams.ean8.enabled = true;
                // Set EAN13
                config.decoderParams.ean13.enabled = true;
                // Set Code39
                config.decoderParams.code39.enabled = true;
                //Set Code128
                config.decoderParams.code128.enabled = true;
                scanner.setConfig(config);
            } catch (ScannerException e) {
                updateStatus(e.getMessage());
            }
        }
    }
    public void softScan(View view) {
        bSoftTriggerSelected = true;
        cancelRead();
    }
    private void cancelRead(){
        if (scanner != null) {
            if (scanner.isReadPending()) {
                try {
                    scanner.cancelRead();
                } catch (ScannerException e) {
                    updateStatus(e.getMessage());
                }
            }
        }
    }
    private void updateStatus(final String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText("" + status);
            }
        });
    }
    //Disables back button
    @Override
    public void onBackPressed(){

    }


    /**
     * Main game code. UI is updated on a thread. Logs are for development purposes only.
     */


    long startTime;
    int wrongScans = 0;
    int totalScans = 0;
    long endTime;
    BarCode scannedCode = new BarCode();
    BarCode loggedCode = new BarCode();
    private void updateData(final String result){
        Log.println(Log.ASSERT, "\tResult Variable: ", result);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                totalScans++;
                String matchedCity = "";
                if(!cityScan) {
                    int codeTest = 0;
                    try {
                        codeTest = Integer.parseInt(result);
                    } catch (InputMismatchException e) {
                        e.printStackTrace();
                    }
                    scannedCode.setCode(codeTest);
                    cityScan = true;
                    startTime = System.currentTimeMillis();

                    while (codeTest != codes[i].getIntCode()) {
                        ++i;
                    }
                    Log.println(Log.ASSERT, "\tBarcode Object: ", Integer.toString(codes[i].getIntCode()));
                    Log.println(Log.ASSERT, "\tMatched Code City: ", codes[i].getCity());
                    //double check code
                    Log.println(Log.ASSERT, "\tCheck Barcode ID:", codes[i].getCode());

                    loggedCode = codes[i];

                    String newRes = Integer.toString(score);
                    textViewCurrent.setText(newRes);
                    cityView.setText(codes[i].getCity());
                }
                else if(cityScan){
                    scannedCode.setCity(result);
                    Log.println(Log.ASSERT, "\tCity on file: ", matchedCity);
                    Log.println(Log.ASSERT, "\tCity scanned: ", result);
                    String ctiy1 = loggedCode.getCity();
                    String city2 = result;

                    if(ctiy1.equals(city2)){
                        cityView.setText("Correct!");

                        endTime = System.currentTimeMillis();
                        long seconds = (endTime - startTime) / 1000;
                            //500 points
                            if(seconds < 5){
                                score += 500;
                            }
                            //400 points
                            else if(seconds > 5 && score < 6){
                                score += 400;
                            }
                            //300 points
                            else if(seconds > 6 && score < 7){
                                score += 300;
                            }
                            //200 points
                            else if(seconds > 7 && score < 8){
                                score += 200;
                            }
                            //100 points
                            else if(seconds > 8 && score < 9){
                                score += 100;
                            }
                            else{
                                score += 50;
                            }
                    }
                    else{
                        Log.println(Log.ASSERT, "\tWRONG SCAN: ","SCAN AND CODE DO NOT MATCH" );
                        wrongScans++;
                        Log.println(Log.ASSERT, "\tScanned City: ", scannedCode.getCity());
                        Log.println(Log.ASSERT,"\tCoded City: ",loggedCode.getCity());

                        cityView.setText("Wrong!");
                        score += -100;

                    }
                    cityScan = false;
                    i = 0;
                    textViewCurrent.setText(Integer.toString(score));
                }
            }
        });
    }


    /**
     * Creates array of BarCode objects.
     */
    private String[] cities = new String[5];
    private BarCode[] codes = new BarCode[30];
    private void setBarcodeInfo(){

      cities[0] = "Boston";
      cities[1] = "Philadelphia";
      cities[2] = "Seattle";
      cities[3] = "Pittsburgh";
      cities[4] = "Miami";

        for(int i = 0; i < 30; i++){
            codes[i] = new BarCode();
            //randomizes barcode assignment
            int randomNum = (int)(Math.random() * ((4) + 1));
            codes[i].setCode(100000+i);
            codes[i].setCity(cities[randomNum]);
        }
        //don't know why this is here. Not going to delete though.
        codes[0].setCity(cities[0]);
    }
}