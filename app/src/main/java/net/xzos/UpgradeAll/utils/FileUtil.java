package net.xzos.UpgradeAll.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.server.log.LogUtil;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class FileUtil {

    private static final LogUtil Log = MyApplication.getServerContainer().getLog();

    private static final String TAG = "FileUtil";
    private static final String[] LogObjectTag = {"Core", TAG};

    public static boolean requestPermission(Activity activity, final int PERMISSIONS_REQUEST_READ_CONTACTS) {
        boolean havePermission = false;
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(MyApplication.getContext(), "请给予本软件 读写存储空间权限", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        } else
            havePermission = true;
        return havePermission;
    }

    public static void performFileSearch(@NonNull final Activity activity, final int READ_REQUEST_CODE, final String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        activity.startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public static void createFile(@NonNull final Activity activity, final int WRITE_REQUEST_CODE, String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        activity.startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    public static String uriToPath(@NonNull final Uri uri) {
        String path = uri.getPath();
        Log.d(LogObjectTag, TAG, String.format(" uriToPath: uri: %s, path: %s", uri, path));
        if (path != null && path.contains(":"))
            path = path.split(":")[1];
        return path;
    }

    public static boolean fileIsExistsByPath(final String path) {
        File file = new File(path);
        return file.exists();
    }

    public static boolean fileIsExistsByUri(final Uri uri) {
        boolean isExists;
        try {
            MyApplication.getContext().getContentResolver().openInputStream(uri);
            isExists = true;
        } catch (FileNotFoundException e) {
            isExists = false;
        }
        return isExists;
    }

    /**
     * 使用一个绝对地址和一个相对地址合成一个绝对地址，
     * 例如：/root/Download/hub 与 ../1.txt 合成 /root/Download/1.txt
     * 兼容本地文件地址和网址与于此类似的其他地址
     */
    public static String pathTransformRelativeToAbsolute(String absolutePath, String relativePath) {
        Log.e(LogObjectTag, TAG, String.format("pathTransformRelativeToAbsolute: absolutePath: %s, relativePath: %s", absolutePath, relativePath));
        if (!absolutePath.equals("/")) {
            if (absolutePath.endsWith("/"))
                absolutePath = absolutePath.substring(0, absolutePath.length() - 1);  // 去除末尾的 /
        }
        // 判断是否为相对地址
        if (relativePath.indexOf(".") == 0) {
            String[] relativePathList = relativePath.split("/");
            StringBuilder basePathBuilder = new StringBuilder(absolutePath);
            for (String item : relativePathList) {
                switch (item) {
                    case "..":
                        int index = basePathBuilder.lastIndexOf("/");
                        basePathBuilder = new StringBuilder(basePathBuilder.substring(0, index));
                        break;
                    case ".":
                        break;
                    default:
                        basePathBuilder.append("/").append(item);
                }
            }
            absolutePath = basePathBuilder.toString();
        } else if (!relativePath.startsWith("/"))
            absolutePath = absolutePath + relativePath;
        else if (relativePath.startsWith("/"))
            absolutePath = null;
        else absolutePath = relativePath;
        return absolutePath;
    }

    /**
     * 使用两个绝对地址合成一个相对地址，
     * 例如：/root/Download/hub (absolutePathRoot) 与 /root/Download/1.txt(absolutePathTarget) 合成 ../1.txt
     * 兼容本地文件地址和网址与于此类似的其他地址
     */
    public static String pathTransformAbsoluteToRelative(@NonNull String absolutePathRoot, String absolutePathTarget) {
        // 根节点输出
        if (absolutePathRoot.equals("/")) return absolutePathTarget;
        // 去除末尾的 /
        if (absolutePathRoot.endsWith("/"))
            absolutePathRoot = absolutePathRoot.substring(0, absolutePathRoot.length() - 1);
        if (!absolutePathTarget.equals("/") && absolutePathTarget.endsWith("/"))
            absolutePathTarget = absolutePathTarget.substring(0, absolutePathTarget.length() - 1);

        String[] rootPathList = absolutePathRoot.split("/");
        String[] targetPathList = absolutePathTarget.split("/");

        int splitIndex = 0;
        for (int i = 0; i < ((rootPathList.length < targetPathList.length) ? rootPathList.length : targetPathList.length); i++) {
            if (rootPathList[i].equals(targetPathList[i]))
                splitIndex = i;
            else break;
        }
        splitIndex += 1; // 换算第一个不同目录的索引
        StringBuilder relativePath = new StringBuilder();
        for (int i = splitIndex; i < rootPathList.length - 1; i++) {
            relativePath.append("../");
        }
        for (int i = splitIndex; i < targetPathList.length; i++) {
            relativePath.append(targetPathList[i]).append("/");
        }
        relativePath = new StringBuilder(relativePath.substring(0, relativePath.length() - 1)); // 去除结尾多余 /
        if (absolutePathRoot.startsWith("/") || absolutePathTarget.startsWith("/"))
            relativePath.insert(0, "./");
        return relativePath.toString();
    }

    @Nullable
    public static String readTextFromUri(Uri uri) {
        try {
            InputStream inputStream = MyApplication.getContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                inputStream.close();
                return stringBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean writeTextFromUri(@NonNull Uri uri, String text) {
        boolean writeSuccess = false;
        try {
            OutputStream outputStream = MyApplication.getContext().getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        outputStream));
                writer.write(text);
                writer.close();
                outputStream.close();
                writeSuccess = true;
            }
        } catch (IOException e) {
            Log.d(LogObjectTag, TAG, "writeTextFromUri: " + uri.getPath());
            Log.e(LogObjectTag, TAG, "writeTextFromUri: 写入文件异常: ERROR_MESSAGE: " + e.toString());
        }
        return writeSuccess;
    }

    public static void performCrop(@NonNull final Activity activity, final int PIC_CROP, final Uri picUri, final int aspectX, final int aspectY) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            // set crop properties here
            cropIntent.putExtra("crop", true);
            // indicate aspect of desired crop
            cropIntent.putExtra("aspectX", aspectX);
            cropIntent.putExtra("aspectY", aspectY);
            // retrieve data on return
            cropIntent.putExtra("return-data", false);
            // start the activity - we handle returning in onActivityResult
            activity.startActivityForResult(cropIntent, PIC_CROP);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException e) {
            // display an error message
            Log.d(LogObjectTag, TAG, "ERROR_MESSAGE: : " + e.toString());
        }
    }
}
