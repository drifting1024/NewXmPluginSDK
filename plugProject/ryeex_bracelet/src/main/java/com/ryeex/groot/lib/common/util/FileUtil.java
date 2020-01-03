package com.ryeex.groot.lib.common.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by chenhao on 17/4/11.
 */

public class FileUtil {
    public static String readTxtFromFile(String filePath) {
        String path = filePath;
        String content = ""; // 文件内容字符串
        // 打开文件
        File file = new File(path);
        // 如果path是传递过来的参数，可以做一个非目录的判断
        if (file.isFile()) {
            try {
                InputStream instream = new FileInputStream(file);
                if (instream != null) {
                    InputStreamReader inputreader = new InputStreamReader(instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String line;
                    // 分行读取
                    while ((line = buffreader.readLine()) != null) {
                        content += line + "\n";
                    }
                    instream.close();
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }
        return content;
    }

    public static byte[] readFile(String filePath) {
        byte[] buffer = null;
        try {
            FileInputStream fin = new FileInputStream(filePath);
            int length = fin.available();
            buffer = new byte[length];
            fin.read(buffer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    public static void copyAsserts(Context ctx, String assetsPath, String targetPath)
            throws IOException {
        InputStream is = null;
        is = ctx.getResources().getAssets().open(assetsPath);
        File f = createFileWhetherExists(targetPath);
        FileOutputStream fo = new FileOutputStream(f);
        int len = -1;
        byte[] bt = new byte[2048];
        while ((len = is.read(bt)) != -1) {
            fo.write(bt, 0, len);
        }
        fo.flush();
        is.close();
        fo.close();
    }

    public static boolean copyFileToFile(String filePath, String targetFilePath) {
        InputStream is = null;
        try {
            is = new FileInputStream(new File(filePath));
            File f = createFileWhetherExists(targetFilePath);
            FileOutputStream fo = new FileOutputStream(f);
            int len = -1;
            byte[] bt = new byte[2048];
            while ((len = is.read(bt)) != -1) {
                fo.write(bt, 0, len);
            }
            fo.flush();
            is.close();
            fo.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static File createFileWhetherExists(String filePath) {
        File file = new File(filePath);

        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
        }

        return file;
    }

    public static boolean deleteFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        // return FileUtil.safeDelSingleFile(file);
        return file.exists() && !file.isDirectory() && file.delete();
    }

    public static void deleteDirectory(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    if (subFile != null && subFile.exists()) {
                        deleteDirectory(subFile.getAbsolutePath());
                    }
                }
            }
        }
        // FileUtil.safeDelSingleFile(file);
        file.delete();
    }

    public static boolean fileExists(String path) {
        return !TextUtils.isEmpty(path) && new File(path).exists();
    }

    public static void makeFileDirectories(String updatePath) {
        if (TextUtils.isEmpty(updatePath)) {
            return;
        }

        File out = new File(updatePath);
        File dir = out.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

    }

    public static File createFileIfNotExists(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        File file = new File(filePath);

        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            //
        }

        return file;
    }

    public static void createDirIfNotExists(String dirPath) {
        final File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
    }

    public static boolean writeBytes(String filePath, byte[] byteArr) {
        try {
            File file = new File(filePath);
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(byteArr);
            outputStream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void appendString(String filePath, String content) {
        try {
            FileWriter writer = new FileWriter(filePath, true);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] readBytes(String filePath) {
        byte byteArr[];
        try {
            File file = new File(filePath);
            int size = (int) file.length();
            byteArr = new byte[size];

            FileInputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            bufferedInputStream.read(byteArr, 0, byteArr.length);
            bufferedInputStream.close();
        } catch (IOException e) {
            return null;
        }
        return byteArr;
    }

    public static String getFilePathByUri(Context context, Uri uri) {
        String path = null;
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
            return path;
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];
                        return path;
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    path = getDataColumn(context, contentUri, null, null);
                    return path;
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                    return path;
                }
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
