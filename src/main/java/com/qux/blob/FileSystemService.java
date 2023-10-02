package com.qux.blob;

import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.web.RoutingContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qux.util.CacheService;

public class FileSystemService implements IBlobService{


    private Logger logger = LoggerFactory.getLogger(FileSystemService.class);


    private final String imageFolder;
    private final String mvvmRuntimesFolder;
    
    public FileSystemService(String imageFolder, String mvvmRuntimesFolder) {
        this.imageFolder = imageFolder;
        this.mvvmRuntimesFolder = mvvmRuntimesFolder;
    }

    public void setBlob(RoutingContext event, String source, String target, Handler<Boolean> handler) {
        logger.info("setBlob() > enter");
        FileSystem fs = event.vertx().fileSystem();
        fs.move(source, target , moveResult-> {
            if (moveResult.succeeded()) {
                handler.handle(true);
            } else {
                handler.handle(false);
            }
        });
    }

    @Override
    public void copyBlob(RoutingContext event, String source, String target, Handler<Boolean> handler) {
        logger.info("copyBlob() > enter " + source + " to " + target);
        FileSystem fs = event.vertx().fileSystem();
        String sourceFile = imageFolder + "/" + source;
        String targetFile = imageFolder + "/" + target;
        fs.copy(sourceFile, targetFile, fileResult ->{
            if(!fileResult.succeeded()){
               handler.handle(true);
            } else {
                logger.error("copyBlob() > error ", fileResult.cause());
                handler.handle(false);
            }
        });
    }
    
    public void getImageBlob(RoutingContext event, String imageFullPath) {
        logger.info("getImageBlob() > enter");
        
        FileSystem fs = event.vertx().fileSystem();
        fs.exists(imageFullPath, exists-> {
            if(exists.succeeded() && exists.result()){
            	final Path path = Paths.get(imageFullPath);
                final int count = path.getNameCount();
                final String tag = path.getName(count - 2).toString() + path.getName(count - 1).toString();
                
                logger.info("getImageBlob() > stream > " + imageFullPath);
                event.response().putHeader("Cache-Control", "no-transform,public,max-age=86400,s-maxage=86401");
                event.response().putHeader("ETag", tag);
                event.response().sendFile(imageFullPath);
            } else {
                logger.info("getImageBlob() > not found > " + imageFullPath);
                event.response().setStatusCode(404);
                event.response().end();
            }
        });
    }

    public void getImageBlob(RoutingContext event, String folder, String image) {
    	getImageBlob(event, imageFolder +"/" + folder + "/" + image);
    }
    
    public String createFolders(RoutingContext event, String[] folderNames) {
        logger.info("createFolder() > enter > " + folderNames);
        FileSystem fs = event.vertx().fileSystem();
        String folder = "";
        for (String folderName: folderNames) {
        	folder += "/" + folderName;
        }
        fs.mkdirsBlocking(folder);
        return folder;
    }
    
    public String createMvvmRuntimesFolders(RoutingContext event, String[] folderNames) {
    	return createFolders(event, Stream.concat(Stream.of(mvvmRuntimesFolder), Arrays.stream(folderNames)).toArray(String[]::new)); // just for prepending a String to an array of Strings, but I wanted to be in just one line... - @TODO could be best to do this more optimised though (maybe in an util function for this...)... 
    }

    public String createSubImageFolder(RoutingContext event, String folderName) {
        logger.info("createSubImageFolder() > enter > " + folderName);
        FileSystem fs = event.vertx().fileSystem();
        String folder = imageFolder +"/" + folderName;
        fs.mkdirsBlocking(folder);
        return folder;
    }


    public void deleteFile(RoutingContext event, String folder, String fileName, Handler<Boolean> handler) {
        String file = imageFolder +"/" + folder + "/" + fileName;
        FileSystem fs = event.vertx().fileSystem();
        fs.delete(file, deleteResult->{
            if(!deleteResult.succeeded()){
                logger.error("delete() > Could not delete from file system !" + file);
            }
            if (handler != null) {
                handler.handle(deleteResult.succeeded());
            }
        });
    }
}
