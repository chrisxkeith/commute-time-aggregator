// Please credit chris.keith@gmail.com
package com.ckkeith.duration_collector;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import com.ckkeith.duration_collector.DurationCollector;
import com.ckkeith.duration_collector.DurationCollector.RouteEstimate;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.TravelMode;

public class ApiCollector {
    // https://stackoverflow.com/questions/40561264/how-to-use-google-maps-distance-matrix-java-api-to-obtain-closest-distance-betwe
    private static final String API_KEY = "YOUR_API_KEY";
    private static final GeoApiContext context = new GeoApiContext.Builder().apiKey(API_KEY).build();

    private static DistanceMatrix estimateRouteTime(Instant time, String departure, String arrival) throws Exception {
        DistanceMatrixApiRequest req = DistanceMatrixApi.newRequest(context);
        return req.departureTime(time)
            .origins(departure)
            .destinations(arrival)
            .mode(TravelMode.DRIVING)
            .language("en-EN")
            .await();
    }
    
    static public void collectDuration(RouteEstimate re, 
                DurationCollector durationCollector, Instant instant, 
                DurationCollector.CollectionParams collectionParams)
                        throws Exception {
        DistanceMatrix dm = estimateRouteTime(instant, collectionParams.homeLocation, collectionParams.workLocation);
        long minTimeInSeconds = Long.MAX_VALUE;
        for (DistanceMatrixRow dmr : dm.rows) {
            for (DistanceMatrixElement dme : dmr.elements) {
                if (dme.duration.inSeconds < minTimeInSeconds) {
                    minTimeInSeconds = dme.duration.inSeconds;
                }
            }
        }
        re.minEstimate = new Integer((int)minTimeInSeconds / 60);
        re.maxEstimate = new Integer((int)minTimeInSeconds / 60);
        re.rawData = "n/a";
        re.routeName = "n/a";
    }
}