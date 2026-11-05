package com.serviceplus.filemanagement.service;

import java.io.InputStream;

public interface ValidationService {

    boolean validateMime(InputStream is);
    boolean validateSize(long bytesRead);

}
