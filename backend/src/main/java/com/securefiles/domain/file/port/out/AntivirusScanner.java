package com.securefiles.domain.file.port.out;

import com.securefiles.domain.file.model.AntivirusScanResult;
import java.io.InputStream;

public interface AntivirusScanner {

    AntivirusScanResult scan(InputStream content);
}
