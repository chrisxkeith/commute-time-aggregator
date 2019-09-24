// Please credit chris.keith@gmail.com
package com.ckkeith.duration_collector;

import java.time.Instant;
import java.util.Calendar;

import com.ckkeith.duration_collector.DurationCollector;
import com.ckkeith.duration_collector.DurationCollector.RouteEstimate;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.TravelMode;

public class ApiCollector {
    
    static public RouteEstimate collectDuration(DurationCollector durationCollector, Calendar ts, 
                DurationCollector.CollectionParams collectionParams) throws Exception {
//        DistanceMatrix dm = estimateRouteTime(ts.toInstant(), collectionParams.homeLocation, collectionParams.workLocation);
        return null;
    }
}