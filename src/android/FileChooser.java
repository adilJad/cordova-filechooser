package com.megster.cordova;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.InputStream;
import org.json.JSONObject;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.io.IOException;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

public class FileChooser extends CordovaPlugin {

    private static final String TAG = "FileChooser";
    private static final String ACTION_OPEN = "open";
    private static final int PICK_FILE_REQUEST = 1;
    CallbackContext callback;

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        if (action.equals(ACTION_OPEN)) {
            chooseFile(callbackContext);
            return true;
        }

        return false;
    }

    public void chooseFile(CallbackContext callbackContext) {

        // type and title should be configurable

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        Intent chooser = Intent.createChooser(intent, "Select File");
        cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_FILE_REQUEST && callback != null) {

            if (resultCode == Activity.RESULT_OK) {

                Uri uri = data.getData();

                if (uri != null) {
                    try {
                        Context context=this.cordova.getActivity().getApplicationContext(); 
                        ContentResolver contentResolver = context.getContentResolver();
                        Cursor cursor = contentResolver.query(uri, null, null, null, null);
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        // int pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        String mimeType = contentResolver.getType(uri);
                        cursor.moveToFirst();
                        String name = cursor.getString(nameIndex);
                        String size = Long.toString(cursor.getLong(sizeIndex));

                        InputStream is = contentResolver.openInputStream(uri);
                        byte[] bytes = getBytes(is);
                        String encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);
                        
                        // String encodeFileToBase64Binary = encodeFileToBase64Binary(contentResolver, uri, cursor.getInt(sizeIndex));

                        JSONObject obj = new JSONObject();
                        obj = obj.put("size", size);
                        obj = obj.put("type", mimeType);
                        obj = obj.put("name", name);
                        obj = obj.put("file", encodedString);

                        Log.w(TAG, uri.toString());
                        callback.success(obj);

                    } catch (JSONException e) {
                        Log.e(TAG, "Exception: " + e);
                        callback.error("Exception: " + e);
                    } catch (IOException ee) {
                        Log.e(TAG, "Exception: " + ee);
                        callback.error("Exception: " + ee);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception: " + e);
                        callback.error("Exception: " + e);
                    }

                } else {

                    callback.error("File uri was null");

                }

            } else if (resultCode == Activity.RESULT_CANCELED) {

                // TODO NO_RESULT or error callback?
                PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                callback.sendPluginResult(pluginResult);

            } else {

                callback.error(resultCode);
            }
        }
    }

    private static byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}