package com.freefallhighscore.android.youtube;

import com.google.api.client.util.Key;

public class GeoCoordinates {
	// TODO: these are numeric in the request JSON, but are doubles
	// precise enough?
	@Key public Double latitude;
	@Key public Double longitude;
}
