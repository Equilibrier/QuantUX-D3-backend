package com.qux.util.rest;

import java.io.IOException;
import java.nio.file.*;

import com.qux.util.Config;

import io.vertx.core.json.JsonObject;

public class MvvmServersInfoProvider {

    private final Path downloaderJsonFilePath;
    private final Path apigatewayJsonFilePath;
    private JsonObject downloaderServerConfiguration;
    private JsonObject apigatewayServerConfiguration;
    private JsonObject config;

    public MvvmServersInfoProvider(JsonObject config) throws IOException {
    	this.config = config;
        this.downloaderJsonFilePath = Paths.get(config.getString(Config.MVVM_RUNTIMES_FOLDER), config.getString(Config.MVVM_DOWNLOADER_JSON));
        this.apigatewayJsonFilePath = Paths.get(config.getString(Config.MVVM_RUNTIMES_FOLDER), config.getString(Config.MVVM_APIGATEWAY_JSON));
        loadFiles();

        // Initialize the file watcher in a separate thread
        new Thread(this::initializeWatcher).start();
    }
    
    private void loadDownloaderJson() throws IOException {
    	downloaderServerConfiguration = new JsonObject(Files.readString(downloaderJsonFilePath));
    }
    private void loadApiGatewayJson() throws IOException {
    	apigatewayServerConfiguration = new JsonObject(Files.readString(apigatewayJsonFilePath));
    }

    private void loadFiles() throws IOException {
    	loadDownloaderJson();
    	loadApiGatewayJson();
    }

    private void initializeWatcher() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            downloaderJsonFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            apigatewayJsonFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changedPath = (Path) event.context();
                    if (changedPath.equals(downloaderJsonFilePath.getFileName())) {
                    	loadDownloaderJson(); // Reload json file if it was modified
                    } 
                    else if (changedPath.equals(apigatewayJsonFilePath.getFileName())) {
                        loadApiGatewayJson(); // Reload json file if it was modified
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    ////////////////////////////////////////////////////
    
    public String getDownloaderServerUrl() {
    	final int port = downloaderServerConfiguration.getInteger("port");
    	return config.getString(Config.MVVM_DOWNLOADER_HOST) + ":" + port;
    }
    public String getApiGatewayServerUrl(String projectRepoName) {
    	final String serverPath = projectRepoName + "/" + config.getString(Config.MVVM_APIGATEWAY_PATH);
    	final int port = apigatewayServerConfiguration.getJsonObject("servers").getJsonObject(serverPath).getInteger("port");
    	return config.getString(Config.MVVM_APIGATEWAY_HOST) + ":" + port;
    }
}