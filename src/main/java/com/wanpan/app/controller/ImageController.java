package com.wanpan.app.controller;

import com.wanpan.app.service.ImageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@Slf4j
@RequestMapping({"/image"})
@AllArgsConstructor
public class ImageController {
//
//    private final ImageService imageService;
//
//    @GetMapping("/{stored-file-name}")
//    public ResponseEntity<Resource> getImage(
//            @PathVariable("stored-file-name") String storedFileName
//    ) throws IOException {
//        Resource resource = imageService.getObject(storedFileName);
//        return ResponseEntity.ok(resource);
//    }
//
//    @PostMapping
//    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile multipartFile) throws IOException, URISyntaxException {
//        //회사아이디-날짜.확장자
//        String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
//        String storedFileName = prefix + "--" + multipartFile.getOriginalFilename();
//        imageService.uploadObject(multipartFile, storedFileName);
//        URI uri = new URI(storedFileName);
//        return ResponseEntity.created(uri).body(uri.toASCIIString());
//    }
//
//    @DeleteMapping("/{stored-file-name}")
//    public ResponseEntity<Void> deleteImage(
//           @PathVariable("stored-file-name") String storedFileName
//    ) {
//        imageService.deleteObject(storedFileName);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/copy")
//    public ResponseEntity<Resource> copyImage(
//            @RequestParam("stored-file-name") String storedFileName,
//            @RequestParam("target-file-name") String targetFileName
//    ) throws IOException {
//        imageService.copyObject(storedFileName, targetFileName);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/move")
//    public ResponseEntity<Resource> moveImage(
//            @RequestParam("stored-file-name") String storedFileName,
//            @RequestParam("target-file-name") String targetFileName
//    ) throws IOException {
//        imageService.moveObject(storedFileName, targetFileName);
//        return ResponseEntity.ok().build();
//    }
//
//
//    @GetMapping("/1/{stored-file-name}")
//    public ResponseEntity<Resource> getTestImage(
//            @PathVariable("stored-file-name") String storedFileName
//    ) throws IOException {
//        Resource resource = imageService.getObject("/1/"+storedFileName);
//        return ResponseEntity.ok(resource);
//    }
//
//    @GetMapping("/tmp/{stored-file-name}")
//    public ResponseEntity<Resource> getTempTestImage(
//            @PathVariable("stored-file-name") String storedFileName
//    ) throws IOException {
//        Resource resource = imageService.getObject("/tmp/"+storedFileName);
//        return ResponseEntity.ok(resource);
//    }

}
