// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.gui.PleaseWaitDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Asynchronous task for downloading and unpacking arbitrary file lists
 * Shows progress bar when downloading
 */
public class DownloadFileTask extends PleaseWaitRunnable{
    private final String address;
    private final File file;
    private final boolean mkdir;
    private final boolean unpack;

    /**
     * Creates the download task
     *
     * @param parent the parent component relative to which the {@link PleaseWaitDialog} is displayed
     * @param address the URL to download
     * @param file The destination file
     * @param mkdir {@code true} if the destination directory must be created, {@code false} otherwise
     * @param unpack {@code true} if zip archives must be unpacked recursively, {@code false} otherwise
     * @throws IllegalArgumentException if {@code parent} is null
     */
    public DownloadFileTask(Component parent, String address, File file, boolean mkdir, boolean unpack) {
        super(parent, tr("Downloading file"), false);
        this.address = address;
        this.file = file;
        this.mkdir = mkdir;
        this.unpack = unpack;
    }

    private static class DownloadException extends Exception {
        public DownloadException(String msg) {
            super(msg);
        }
    }

    private boolean canceled;
    private HttpURLConnection downloadConnection;

    private synchronized void closeConnectionIfNeeded() {
        if (downloadConnection != null) {
            downloadConnection.disconnect();
        }
        downloadConnection = null;
    }


    @Override
    protected void cancel() {
        this.canceled = true;
        closeConnectionIfNeeded();
    }

    @Override
    protected void finish() {}

    /**
     * Performs download.
     * @throws DownloadException if the URL is invalid or if any I/O error occurs.
     */
    public void download() throws DownloadException {
        OutputStream out = null;
        InputStream in = null;
        try {
            if (mkdir) {
                File newDir = file.getParentFile();
                if (!newDir.exists()) {
                    newDir.mkdirs();
                }
            }

            URL url = new URL(address);
            int size;
            synchronized(this) {
                downloadConnection = Utils.openHttpConnection(url);
                downloadConnection.setRequestProperty("Cache-Control", "no-cache");
                downloadConnection.connect();
                size = downloadConnection.getContentLength();
            }

            progressMonitor.setTicksCount(100);
            progressMonitor.subTask(tr("Downloading File {0}: {1} bytes...", file.getName(),size));

            in = downloadConnection.getInputStream();
            out = new FileOutputStream(file);
            byte[] buffer = new byte[32768];
            int count=0;
            int p1=0, p2=0;
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
                count+=read;
                if (canceled) break;
                p2 = 100 * count / size;
                if (p2!=p1) {
                    progressMonitor.setTicks(p2);
                    p1=p2;
                }
            }
            Utils.close(out);
            if (!canceled) {
                System.out.println(tr("Download finished"));
                if (unpack) {
                    System.out.println(tr("Unpacking {0} into {1}", file.getAbsolutePath(), file.getParent()));
                    unzipFileRecursively(file, file.getParent());
                    file.delete();
                }
            }
        } catch(MalformedURLException e) {
            String msg = tr("Warning: Cannot download file ''{0}''. Its download link ''{1}'' is not a valid URL. Skipping download.", file.getName(), address);
            System.err.println(msg);
            throw new DownloadException(msg);
        } catch (IOException e) {
            if (canceled)
                return;
            throw new DownloadException(e.getMessage());
        } finally {
            closeConnectionIfNeeded();
            Utils.close(out);
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException {
        if (canceled) return;
        try {
            download();
        } catch(DownloadException e) {
            e.printStackTrace();
        }
    }

    /**
     * Replies true if the task was canceled by the user
     *
     * @return {@code true} if the task was canceled by the user, {@code false} otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Recursive unzipping function
     * TODO: May be placed somewhere else - Tools.Utils?
     * @param file
     * @param dir
     * @throws IOException
     */
    public static void unzipFileRecursively(File file, String dir) throws IOException {
        OutputStream os = null;
        InputStream is = null;
        ZipFile zf = null;
        try {
            zf = new ZipFile(file);
            Enumeration<?> es = zf.entries();
            ZipEntry ze;
            while (es.hasMoreElements()) {
                ze = (ZipEntry) es.nextElement();
                File newFile = new File(dir, ze.getName());
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    is = zf.getInputStream(ze);
                    os = new BufferedOutputStream(new FileOutputStream(newFile));
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    Utils.close(os);
                    Utils.close(is);
                }
            }
        } finally {
            Utils.close(zf);
        }
    }
}

