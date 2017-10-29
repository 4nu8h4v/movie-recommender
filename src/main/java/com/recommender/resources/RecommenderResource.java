package com.recommender.resources;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.recommender.core.CollaborativeFiltering;

@Path("/rec")
//@Produces(MediaType.TEXT_PLAIN)
public class RecommenderResource {
	private static final Logger logger = LoggerFactory.getLogger(RecommenderResource.class);
	
    @GET
    @Timed
    public void getTestRecommendation() {
    	logger.debug("Start method getTestRecommendation");
    	CollaborativeFiltering cf = new CollaborativeFiltering();
    	Double mmse = 0.0;
    	for(int i = 662; i <= 671; ++i) {
        	cf.getSimilarUsers(Integer.toString(i));
        	Map<String, Double> predictedRatings = cf.getMoviePredictions();    	
        	Double mse = cf.meanSquaredError(Integer.toString(i), predictedRatings);	
        	logger.info("Mean Squared Error {}", mse);    	
        	/*ArrayList<Pair<String, Double>> usp = cf.getUserIdSimilarityPairs();
        	for(Pair<String, Double> p : usp) {
        		logger.info("{} : {} ",p.getLeft(),p.getRight());
        	}*/
        	mmse += mse;
    	}
    	mmse /= (671-662 + 1);
    	logger.info("Mean of Mean Squared Error {}", mmse);    		
    }
    
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getRecommendation(@FormDataParam("movie[]") List<FormDataBodyPart> movies) {
    	logger.debug("Start method getRecommendation");
    	CollaborativeFiltering cf = new CollaborativeFiltering();
    	Map<String, Double> userMovies = new HashMap<>();
    	for(FormDataBodyPart movie : movies) {
    		logger.info("Movie: {}", movie.getValue());
    		userMovies.put(movie.getValue(), 5.0);
    	}
    	cf.getSimilarUsers(userMovies);
    	Map<String, Double> predictedRatings = cf.getMoviePredictions();    
    	Map<String, Double> sortedPredictedRatings = predictedRatings.entrySet().stream()
    			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    	
    	int numMovies = 30;
    	List<String> recommendedMovies = new ArrayList<>();
    	Map<String, String> movieIdToTitleMap = cf.getMovieIdToTitleMap();
    	for(Map.Entry<String, Double> entry : sortedPredictedRatings.entrySet()) {
    		String movieTitle = movieIdToTitleMap.get(entry.getKey());
    		String movieYearString = movieTitle.substring(movieTitle.length() - 5, movieTitle.length() - 1);
    		Integer movieYear = Integer.valueOf(movieYearString);
    		if(movieYear > 2010) {
    			numMovies--;
    			recommendedMovies.add(movieTitle + ":" + movieYearString + ":" + Double.toString(entry.getValue()));
    		}
    		if(numMovies == 0) break;
    	}
    	return recommendedMovies;
    }
}
