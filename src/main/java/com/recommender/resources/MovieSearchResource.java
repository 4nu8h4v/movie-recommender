package com.recommender.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.text.WordUtils;
import com.codahale.metrics.annotation.Timed;
import com.recommender.core.PopulateMovieDB;
import com.recommender.core.SuggestTree;

@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class MovieSearchResource {
	private static final Logger logger = LoggerFactory.getLogger(MovieSearchResource.class);
	private SuggestTree st;
	
	public MovieSearchResource() {
		st = new SuggestTree(15);
		PopulateMovieDB pmdb = PopulateMovieDB.getInstance();
		Map<String, Map<String, String>> movies = pmdb.getMoviesRowMap();
    	movies.forEach((movieId, vMap) -> {
    		vMap.forEach((movieTitle, movieRating) -> {
    			st.put(movieTitle, 1);
    		});
    	});
    	
	}
	
    @GET
    @Timed
    public List<String> getSuggestions(@QueryParam("title") Optional<String> title) {
    	logger.info("Inside MovieSearchResource getSuggestions");
    	List<String> allSuggestions = new ArrayList<>();

    	if(title.isPresent() == false || title.get().isEmpty()) {
    		return allSuggestions;
    	}
    	
    	String movieTitle = title.get();
    	movieTitle = WordUtils.capitalizeFully(movieTitle);
    	logger.info("movieTitle is {}", movieTitle);
    	allSuggestions.addAll(findMatches(movieTitle));
    	
    	if(movieTitle.startsWith("The", 0)) {
    		movieTitle = movieTitle.substring(3).trim();
    		logger.info("movieTitle is {}", movieTitle);
    		allSuggestions.addAll(findMatches(movieTitle));
    	}
    	else {
    		movieTitle = "The " + movieTitle.trim();
    		logger.info("movieTitle is {}", movieTitle);
    		allSuggestions.addAll(findMatches(movieTitle));
    	}
    	
    	return allSuggestions;
    }
    
    private List<String> findMatches(String title){
    	List<String> suggestions = new ArrayList<>();
    	SuggestTree.Node node = st.getAutocompleteSuggestions(title);
    	if(node == null) {
    		logger.info("No Suggestions");
    		//logger.info("Tree size: {}", st.size());
    		return suggestions;
    	}
    	
    	logger.info("Suggestions length {}", node.listLength());
    	for(int i = 0; i < node.listLength(); ++i) {
    		SuggestTree.Entry entry = node.getSuggestion(i);
    		suggestions.add(entry.getTerm());
    	}
       return suggestions;
    }
}
