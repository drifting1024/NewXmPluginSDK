package com.ryeex.groot.lib.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    /**
     * 解压文件到文件所在目录,默认不把压缩文件作为一层目录
     *
     * @param zipFile 被解压的文件
     * @throws IOException
     */
    public static void unzip(File zipFile) throws IOException {
        new ZipDecompress().unzip(zipFile);
    }

    /**
     * 解压文件到文件所在目录
     *
     * @param zipFile           被解压的文件
     * @param retainZipAsFolder 是否把压缩文件作为一层目录
     * @throws IOException
     */
    public static void unzip(File zipFile, boolean retainZipAsFolder) throws IOException {
        new ZipDecompress().unzip(zipFile, retainZipAsFolder);
    }

    /**
     * 解压文件,默认不把压缩文件作为一层目录
     *
     * @param zipFile 被解压的文件
     * @param destDir 目标目录
     * @throws IOException
     */
    public static void unzip(File zipFile, File destDir) throws IOException {
        new ZipDecompress().unzip(zipFile, destDir);
    }

    /**
     * 解压文件
     *
     * @param zipFile           被解压的文件
     * @param destDir           目标目录
     * @param retainZipAsFolder 是否把压缩文件作为一层目录
     * @throws IOException
     */
    public static void unzip(File zipFile, File destDir, boolean retainZipAsFolder) throws IOException {
        new ZipDecompress().unzip(zipFile, destDir, retainZipAsFolder);
    }

    /**
     * 将多个(1~n)个文件或者目录压缩至目标文件
     *
     * @param destZipFile 目标压缩文件
     * @param fileOrDirs  多个被压缩的文件或目录
     * @throws IOException
     */
    public static void zip(File destZipFile, File... fileOrDirs) throws IOException {
        new ZipCompress().zip(destZipFile, fileOrDirs);
    }

    /**
     * Zip解压缩
     */
    public static class ZipDecompress {

        private static int buf = 4096;

        /**
         * 解压文件到文件所在目录,默认不把压缩文件作为一层目录
         *
         * @param srcFile 被解压的文件
         * @throws IOException
         */
        public void unzip(File srcFile) throws IOException {
            unzip(srcFile, srcFile.getParentFile(), false);
        }

        /**
         * 解压文件到文件所在目录
         *
         * @param srcFile           被解压的文件
         * @param retainZipAsFolder 是否把压缩文件作为一层目录
         * @throws IOException
         */
        public void unzip(File srcFile, boolean retainZipAsFolder) throws IOException {
            unzip(srcFile, srcFile.getParentFile(), retainZipAsFolder);
        }

        /**
         * 解压文件,默认不把压缩文件作为一层目录
         *
         * @param srcFile 被解压的文件
         * @param destDir 目标目录
         * @throws IOException
         */
        public void unzip(File srcFile, File destDir) throws IOException {
            unzip(srcFile, destDir, false);
        }

        /**
         * 解压文件
         *
         * @param srcFile           被解压的文件
         * @param destDir           目标目录
         * @param retainZipAsFolder 是否把压缩文件作为一层目录
         * @throws IOException
         */
        public void unzip(File srcFile, File destDir, boolean retainZipAsFolder) throws IOException {
            if (retainZipAsFolder) {
                String fileName = srcFile.getName();
                int extIndex = fileName.lastIndexOf('.');
                String folderName;
                if (extIndex != -1) {
                    folderName = fileName.substring(0, extIndex);
                } else {
                    folderName = fileName;
                }
                destDir = new File(destDir, folderName);
            }
            if (!destDir.exists()) {
                if (!destDir.mkdirs()) { // 创建目录失败
                    throw new IOException(String.format("创建目录%s失败", destDir.getPath()));
                }
            }

            try {
                ZipFile zipFile = new ZipFile(srcFile);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                int len;
                byte[] data = new byte[buf];

                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();

                    File outFile = new File(destDir, zipEntry.getName());
                    if (zipEntry.isDirectory()) { // 解压目录
                        outFile.mkdirs();
                    } else { // 解压文件

                        if (!outFile.getParentFile().exists())
                            outFile.getParentFile().mkdirs();
                        if (!outFile.exists())
                            outFile.createNewFile();

                        BufferedInputStream bis = null;
                        BufferedOutputStream bos = null;
                        try {
                            bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                            bos = new BufferedOutputStream(new FileOutputStream(outFile));

                            while ((len = bis.read(data)) != -1)
                                bos.write(data, 0, len);
                            bos.flush();
                        } finally {
                            if (bos != null)
                                bos.close();
                            if (bis != null)
                                bis.close();
                        }
                    }
                }
            } catch (IOException e) {
                throw e;
            }
        }

    }

    /**
     * Zip压缩
     */
    public static class ZipCompress {

        private static int buf = 4096;

        /**
         * 将多个(1~n)个文件或者目录压缩至目标文件
         *
         * @param destZipFile 目标压缩文件
         * @param fileOrDirs  多个被压缩的文件或目录
         * @throws IOException
         */
        public void zip(File destZipFile, File... fileOrDirs) throws IOException {
            ZipOutputStream zos = null;
            try {
                zos = new ZipOutputStream(new FileOutputStream(destZipFile));
                for (File fileOrDir : fileOrDirs) {
                    if (fileOrDir.isDirectory()) {
                        zipDir(fileOrDir, fileOrDir, zos);
                    } else {
                        zipFile(fileOrDir, fileOrDir.getParentFile(), zos);
                    }
                }
            } finally {
                if (zos != null) {
                    zos.close();
                }
            }
        }

        /**
         * 压缩目录
         *
         * @param dir     被压缩的目录
         * @param baseDir 被压缩的文件的根目录(用于记录压缩文件的层次结构)
         * @param zos     压缩文件流
         * @throws IOException
         */
        private void zipDir(File dir, File baseDir, ZipOutputStream zos) throws IOException {
            File[] subFiles = dir.listFiles();
            for (File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    zipDir(subFile, baseDir, zos);
                } else {
                    zipFile(subFile, baseDir, zos);
                }
            }
        }

        /**
         * 压缩文件
         *
         * @param srcFile 被压缩的文件
         * @param baseDir 被压缩的文件的根目录(用于记录压缩文件层次结构)
         * @param zos     压缩文件流
         * @throws IOException
         */
        private void zipFile(File srcFile, File baseDir, ZipOutputStream zos) throws IOException {
            InputStream in = new FileInputStream(srcFile);
            String entry = srcFile.getPath().substring(baseDir.getPath().length() + 1);
            ZipEntry zipEntry = new ZipEntry(entry);
            zos.putNextEntry(zipEntry);
            byte[] data = new byte[buf];
            int len;
            while ((len = in.read(data)) != -1) {
                zos.write(data, 0, len);
            }
            zos.flush();
            in.close();
        }

    }

}


