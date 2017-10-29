package com.recommender.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;

public class CollaborativeFiltering {
	private static final int numTrainingUsers = 661; 
	private static final int numUsers = 671;
	private static PopulateMovieDB pmdb;
	private ArrayList<Pair<String, Double>> userIdSimilarityPairs;
	private Map<String, Double> predictedRatings;
	
	private static final Logger logger = LoggerFactory.getLogger(CollaborativeFiltering.class);
	
	public CollaborativeFiltering() {
		pmdb = PopulateMovieDB.getInstance();
		userIdSimilarityPairs = new ArrayList<>();
		predictedRatings = new HashMap<>();
	}
	
	private Map<String, ValueDifference<Double>> getCommonMovies(String userIdX, String userIdY) {
		Map<String, Double> userXMovies = pmdb.getUserMovieMap(userIdX);
		Map<String, Double> userYMovies = pmdb.getUserMovieMap(userIdY);
		MapDifference<String, Double> diff = Maps.difference(userXMovies, userYMovies);
		Map<String, ValueDifference<Double>> commonMovies = diff.entriesDiffering();
		return commonMovies;
	}
	
	private Map<String, ValueDifference<Double>> getCommonMovies(String userIdX, final Map<String, Double> userMovies) {
		Map<String, Double> userXMovies = pmdb.getUserMovieMap(userIdX);
		Map<String, String> movieTitleToIdMap = pmdb.getMovieTitleToIdMap();
		Map<String, Double> userYMovies = new HashMap<>();
		userMovies.forEach((movieTitle, movieRating) -> {
			userYMovies.put(movieTitleToIdMap.get(movieTitle), movieRating);
		});
		MapDifference<String, Double> diff = Maps.difference(userXMovies, userYMovies);
		Map<String, ValueDifference<Double>> commonMovies = diff.entriesDiffering();
		return commonMovies;
	}
	
	private Double getCosineSimilarity(Map<String, ValueDifference<Double>> commonMovies) {
		Double numerator = 0.0, l2normX = 0.0, l2normY = 0.0;
		for(Map.Entry<String, ValueDifference<Double>> entry : commonMovies.entrySet()) {
			ValueDifference<Double> vd = entry.getValue();
			numerator += vd.leftValue() * vd.rightValue();
			l2normX += vd.leftValue() * vd.leftValue();
			l2normY += vd.rightValue() * vd.rightValue();
		}
		
		Double cosineSimilarity = 0.0;
		if(l2normX == 0.0 || l2normY == 0.0) {
			return cosineSimilarity;
		}
		l2normX = Math.sqrt(l2normX);
		l2normY = Math.sqrt(l2normY);
		cosineSimilarity = numerator / (l2normX * l2normY);
		return cosineSimilarity;
	}
	
	private Double getPearsonCorrelationCoefficient(Map<String, ValueDifference<Double>> commonMovies) {
		Double covarianceLR = 0.0, standardDeviationL = 0.0, standardDeviationR = 0.0, meanRatingL = 0.0, meanRatingR = 0.0;
		
		for(Map.Entry<String, ValueDifference<Double>> entry : commonMovies.entrySet()) {
			ValueDifference<Double> vd = entry.getValue();
			meanRatingL += vd.leftValue();
			meanRatingR += vd.rightValue();
		}
		meanRatingL /= commonMovies.size();
		meanRatingR /= commonMovies.size();
		
		for(Map.Entry<String, ValueDifference<Double>> entry : commonMovies.entrySet()) {
			ValueDifference<Double> vd = entry.getValue();
			covarianceLR += (vd.leftValue() - meanRatingL) * (vd.rightValue() - meanRatingR);
			standardDeviationL += Math.pow((vd.leftValue() - meanRatingL) , 2);
			standardDeviationR += Math.pow((vd.rightValue() - meanRatingR), 2);
		}
		
		Double pearsonCorrelationCoefficient = 0.0;
		if(standardDeviationL == 0.0 || standardDeviationR == 0.0) {
			return pearsonCorrelationCoefficient;
		}
		standardDeviationL = Math.sqrt(standardDeviationL);
		standardDeviationR = Math.sqrt(standardDeviationR);
		pearsonCorrelationCoefficient = covarianceLR / (standardDeviationL * standardDeviationR);
		return pearsonCorrelationCoefficient;
	}
	
	public void getSimilarUsers(String userId) {
		userIdSimilarityPairs.clear();
		predictedRatings.clear();
		
		for(int i = 1; i <= numTrainingUsers; ++i) {
			Double cosineSimilarity = getCosineSimilarity(getCommonMovies(String.valueOf(i), userId));
			//Double pearsonCorrelationCoefficient = getPearsonCorrelationCoefficient(getCommonMovies(String.valueOf(i), userId));
			Pair<String, Double> userIdSimilarityPair = Pair.of(String.valueOf(i), cosineSimilarity);
			//Pair<String, Double> userIdSimilarityPair = Pair.of(String.valueOf(i), pearsonCorrelationCoefficient);
			userIdSimilarityPairs.add(userIdSimilarityPair);
		}
		Collections.sort(userIdSimilarityPairs, Comparator.comparing(Pair<String, Double>::getRight).reversed());
	}
	
	public void getSimilarUsers(final Map<String, Double> userYMovies) {
		userIdSimilarityPairs.clear();
		predictedRatings.clear();
		
		for(int i = 1; i <= numUsers; ++i) {
			Double cosineSimilarity = getCosineSimilarity(getCommonMovies(String.valueOf(i), userYMovies));
			//Double pearsonCorrelationCoefficient = getPearsonCorrelationCoefficient(getCommonMovies(String.valueOf(i), userId));
			Pair<String, Double> userIdSimilarityPair = Pair.of(String.valueOf(i), cosineSimilarity);
			//Pair<String, Double> userIdSimilarityPair = Pair.of(String.valueOf(i), pearsonCorrelationCoefficient);
			userIdSimilarityPairs.add(userIdSimilarityPair);
		}
		Collections.sort(userIdSimilarityPairs, Comparator.comparing(Pair<String, Double>::getRight).reversed());
	}
	
	public Map<String, Double> getMoviePredictions() {
		Map<String, Map<String, String>> movies = pmdb.getMoviesRowMap();
		logger.info("Inside CollaborativeFIltering getMoviePredictions");
		movies.forEach((movieId, v) -> {
			Double k = 0.0, predictedRating = 0.0;
			for(int i = 0; i < userIdSimilarityPairs.size(); ++i) {
				logger.info("Cosine similarity {}", userIdSimilarityPairs.get(i).getRight());
				if(userIdSimilarityPairs.get(i).getRight() >= 1.0) {
					String userId = userIdSimilarityPairs.get(i).getLeft();
					Map<String, Double> userMovies = pmdb.getUserMovieMap(userId);
					if(userMovies.containsKey(movieId)) {
						k += Math.abs(userIdSimilarityPairs.get(i).getRight());
						predictedRating += userIdSimilarityPairs.get(i).getRight() * userMovies.get(movieId);
					}
				}
			}
			if(k > 0.0) {
				predictedRating /= k;
			}
			//logger.info("movieId {}, predRating {}", movieId, predictedRating);
			predictedRatings.put(movieId, predictedRating);
		});
		return predictedRatings;
	}
	
	public Map<String, Double> getUserMovieMap(String userId){
		return pmdb.getUserMovieMap(userId);
	}
	
	public Map<String, String> getMovieIdToTitleMap(){
		return pmdb.getMovieIdToTitleMap();
	}
	
	public Double meanSquaredError(String userId, final Map<String, Double> predictedRatings) {
		Double meanSquaredError = 0.0;
		Map<String, Double> userMovieMap = getUserMovieMap(userId);
		for(Map.Entry<String, Double> entry : userMovieMap.entrySet()) {
			Double squaredError = Math.abs(entry.getValue() - predictedRatings.get(entry.getKey()));
			squaredError *= squaredError;
			meanSquaredError += squaredError / userMovieMap.size();
		}
		return meanSquaredError;
	}
	
	public ArrayList<Pair<String, Double>> getUserIdSimilarityPairs(){
		return userIdSimilarityPairs;
	}
}
