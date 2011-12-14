package org.jdownloader.extensions.extraction;

import java.awt.Color;

public interface ArchiveFile {

    public static enum Status {
        IDLE,
        RUNNING,
        ERROR,
        SUCCESSFUL,
        ERROR_CRC,
        ERROR_NOT_ENOUGH_SPACE,
        ERRROR_FILE_NOT_FOUND
    }

    public boolean isComplete();

    public String getFilePath();

    public boolean isValid();

    public boolean delete();

    public boolean exists();

    public String getName();

    public void setStatus(Status error);

    public void setMessage(String plugins_optional_extraction_status_notenoughspace);

    public void setProgress(long value, long max, Color color);

}
