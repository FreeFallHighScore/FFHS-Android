package com.freefallhighscore.android.youtube;

import java.io.InputStream;
import java.util.List;

import com.google.api.client.util.Key;
import com.google.common.collect.Lists;

public class UploadRequestData {

	@Key public String privacy = "public";
	
	// required
	@Key public List<String> category = Lists.newArrayList("Sports", "freefallhighscore");
	@Key public String description;
	@Key public List<String> tags = Lists.newArrayList();
	
	// date, e.g.: 2011-08-11
	@Key public String recorded;
	
	// e.g. "San Francisco"
	@Key public String location;
	
	@Key public GeoCoordinates geoCoordinates;
	@Key public AccessControl accessControl;
	
	@Key public String title;
	
	// not part of the data. This gets added in the 'Slug' request header
	public String fileName;
	
	public InputStream fileData;
}
