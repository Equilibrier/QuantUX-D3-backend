package com.qux.util;


import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.HashMap;
import java.util.Map;

import com.qux.model.AppPart;


public class CacheService {
    private Map<String, JsonObject> metaProjectCache = new HashMap<>();
    private MongoClient mongo = null;

    public CacheService(MongoClient db) {
    	mongo = db;
    }
    
    @FunctionalInterface
    public interface CacheValueCallback<T> {
        void onComplete(T result, Exception error);
    }
    
    private void storeProjectMeta(String appID, JsonObject json) {
        JsonObject cachedJson = new JsonObject();
        cachedJson.put("name", json.getString("name"));
        cachedJson.put("_id", json.getString("_id"));
        cachedJson.put("version", json.getInteger("version"));
        cachedJson.put("description", json.getString("description"));
        cachedJson.put("is_d3_mvvm", json.getBoolean("is_d3_mvvm"));
        cachedJson.put("mvvm_type", json.getString("mvvm_type"));
        cachedJson.put("mvvm_repo_name", json.getString("mvvm_repo_name"));
        metaProjectCache.put(appID, cachedJson);
    }
    private JsonObject retrieveProjectMeta(String appID) {
        return metaProjectCache.get(appID);
    }
    
    public void getProjectMeta(String appID, CacheValueCallback<JsonObject> resultsProviderClbk) {
		if (metaProjectCache.containsKey(appID)) {
    		resultsProviderClbk.onComplete(retrieveProjectMeta(appID), null);
    	}
    	else {
    		final String appTable = DB.getAppTable(); 
	    	mongo.findOne(appTable, AppPart.appByIdQuery(appID), null, res -> {
				
				if(res.succeeded()){
					
					JsonObject json = res.result();
					if(json!=null){
						storeProjectMeta(appID, json);
						resultsProviderClbk.onComplete(retrieveProjectMeta(appID), null);
					} else {
						resultsProviderClbk.onComplete(null, new Exception(appID + " record from " + appTable + " is inexistent - inexistent project"));
					}
				} else {
					resultsProviderClbk.onComplete(null, new Exception("could not retrieve " + appTable + " table for some reason - error 404"));
				}
			});
    	}
    }
}