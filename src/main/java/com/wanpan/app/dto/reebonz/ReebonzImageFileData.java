package com.wanpan.app.dto.reebonz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReebonzImageFileData {
    String keyName;
    String fileNameWithExtension;
    InputStream inputStream;
}
