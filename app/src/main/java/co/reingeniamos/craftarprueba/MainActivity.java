package co.reingeniamos.craftarprueba;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.craftar.CraftARActivity;
import com.craftar.CraftARCamera;
import com.craftar.CraftARCameraView;
import com.craftar.CraftARCloudRecognition;
import com.craftar.CraftARCloudRecognitionError;
import com.craftar.CraftARImage;
import com.craftar.CraftARImageHandler;
import com.craftar.CraftARItem;
import com.craftar.CraftARItemAR;
import com.craftar.CraftARResponseHandler;
import com.craftar.CraftARSDK;
import com.craftar.CraftARSDKException;
import com.craftar.CraftARTracking;


import java.io.File;
import java.util.ArrayList;


public class MainActivity extends CraftARActivity implements CraftARResponseHandler, CraftARImageHandler, View.OnClickListener {

    private final static String COLLECTION_TOKEN="401230a1e4be4be0";
    public CraftARCloudRecognition mCloudRecognition;
    CraftARTracking mCraftARTracking;
    private View mScanningLayout;
    CraftARCamera mCamera;
    private View mTapToScanLayout;
    private View mTapToScanLayoutQR;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPostCreate() {
        View mainLayout= (View) getLayoutInflater().inflate(R.layout.activity_main, null);
        CraftARCameraView cameraView = (CraftARCameraView) mainLayout.findViewById(R.id.camera_preview);
        super.setCameraView(cameraView);
        setContentView(mainLayout);
        mScanningLayout = findViewById(R.id.layout_scanning);
        mTapToScanLayout = findViewById(R.id.tap_to_scan);
        mTapToScanLayout.setClickable(true);
        mTapToScanLayout.setOnClickListener(this);
        mTapToScanLayoutQR = findViewById(R.id.tap_to_scan_qr);
        mTapToScanLayoutQR.setClickable(true);
        mTapToScanLayoutQR.setOnClickListener(this);

        // Initialize the SDK.
        CraftARSDK.init(getApplicationContext(), this);

        //Get the camera to be able to do single-shot (if you just use finder-mode, this is not necessary)
        mCamera= CraftARSDK.getCamera();
        mCamera.setImageHandler(this); //Tell the camera who will receive the image after takePicture()

        // Obtain the CraftARCloudRecognition module.
        mCloudRecognition= CraftARSDK.getCloudRecognition();
        // Indicate mCloudRecognition that this Activity is the handler that will receive the responses
        //  from the Cloud Image Recognition Service.
        mCloudRecognition.setResponseHandler(this);

        // Set your collection token
        mCloudRecognition.setCollectionToken(COLLECTION_TOKEN);

        //Start finder mode
        //mCloudRecognition.startFinding();

        //Obtain the tracking module
        mCraftARTracking = CraftARSDK.getTracking();

        mCloudRecognition.connect(COLLECTION_TOKEN);

    }

    @Override
    public void searchCompleted(ArrayList<CraftARItem> craftARItems) {
        if(craftARItems.size()==0){
            Log.i("imagen", "Imagen no reconocida");
            mScanningLayout.setVisibility(View.GONE);
            mTapToScanLayout.setVisibility(View.VISIBLE);
            mTapToScanLayoutQR.setVisibility(View.VISIBLE);
            mCamera.restartCameraPreview();
            Toast.makeText(getBaseContext(), getString(R.string.imagen_no_encontrada), Toast.LENGTH_SHORT).show();
        }else{
            Log.i("imagen", "Imagen reconocida");
            CraftARItem item = craftARItems.get(0);
            if (item.isAR()) {
                try {
                    // Stop Finding
                    mCloudRecognition.stopFinding();

                    // Cast the found item to an AR item
                    CraftARItemAR myARItem = (CraftARItemAR)item;
                    // Add content to the tracking SDK and start AR experience
                    mCamera.restartCameraPreview();
                    mCraftARTracking.addItem(myARItem);
                    mCraftARTracking.startTracking();
                    mScanningLayout.setVisibility(View.GONE);
                    mTapToScanLayout.setVisibility(View.VISIBLE);
                    mTapToScanLayoutQR.setVisibility(View.VISIBLE);
                } catch (CraftARSDKException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void connectCompleted() {
        Log.i("imagen", "Collection token is valid");
    }

    @Override
    public void requestFailedResponse(int i, CraftARCloudRecognitionError craftARCloudRecognitionError) {
        Log.d("imagen", craftARCloudRecognitionError.getErrorMessage());
        Toast.makeText(getBaseContext(), getString(R.string.error_al_conectarse), Toast.LENGTH_SHORT).show();

        mScanningLayout.setVisibility(View.GONE);
        mTapToScanLayout.setVisibility(View.VISIBLE);
        mTapToScanLayoutQR.setVisibility(View.VISIBLE);
        mCamera.restartCameraPreview();
    }

    //Callback received for SINGLE-SHOT only (after takePicture).
    @Override
    public void requestImageReceived(CraftARImage image) {
        Log.i("imagen", "Imagen capturada");
        mCloudRecognition.searchWithImage(COLLECTION_TOKEN,image);
    }

    @Override
    public void requestImageError(String error) {
        //Take picture failed
        Log.i("imagen", error);
        Toast.makeText(getBaseContext(), getString(R.string.recognition_only_toast_picture_error), Toast.LENGTH_SHORT).show();
        mScanningLayout.setVisibility(View.GONE);
        mTapToScanLayout.setVisibility(View.VISIBLE);
        mTapToScanLayoutQR.setVisibility(View.VISIBLE);
        mCamera.restartCameraPreview();
    }

    @Override
    public void onClick(View v) {
        if (v == mTapToScanLayout) {
            mTapToScanLayout.setVisibility(View.GONE);
            mScanningLayout.setVisibility(View.VISIBLE);
            mTapToScanLayoutQR.setVisibility(View.GONE);
            mCamera.takePicture();

        }
        if (v == mTapToScanLayoutQR) {
            //se inicia el barcode Scanner
            IntentIntegrator.initiateScan(MainActivity.this);
        }
    }

    //Marcamos lo que queremos que haga una vez haya leido el código
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            switch(requestCode) {
            case IntentIntegrator.REQUEST_CODE:
            {
                if (resultCode == RESULT_CANCELED){
                }
                else
                {
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    String textResult = scanResult.getContents();
                    String path =  Environment.getExternalStorageDirectory() + "/" + textResult;
                    Toast.makeText(getApplicationContext(),path,Toast.LENGTH_LONG
                    ).show();
                    File archivo = new File(path);
                    String mime = getMimeType(path);
                    Intent intent = new Intent(Intent.ACTION_VIEW);//, Uri.parse(textResult));
                    intent.setDataAndType(Uri.fromFile(archivo), mime);
                    startActivity(intent);
                }
                break;
            }
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
