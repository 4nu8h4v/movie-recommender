package com.recommender.core;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class PopulateMovieDB {
	private Table<String, String, Double> users;
	private Table<String, String, String> movies;
	private static PopulateMovieDB pmdb;
	
	private PopulateMovieDB(){
		users =  HashBasedTable.create();
		movies = HashBasedTable.create();
		readMovies();
		readRatings();
	}
	
	public static PopulateMovieDB getInstance() {
		if(pmdb == null) {
			pmdb = new PopulateMovieDB();
		}
		return pmdb;
	}
	
	private void readRatings() {
		Reader in = null;
		try {
			in = new FileReader("C:\\Users\\ARC\\Downloads\\Compressed\\ml-latest-small\\ratings.csv");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterable<CSVRecord> records = null;
		try {
			records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (CSVRecord record : records) {
		    String userId = record.get("userId");
		    String movieId = record.get("movieId");
		    String rating = record.get("rating");
		    users.put(userId, movieId, Double.valueOf(rating));
		}		        
	}
	
	private void readMovies() {
		Reader in = null;
		try {
			in = new FileReader("C:\\Users\\ARC\\Downloads\\Compressed\\ml-latest-small\\movies.csv");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterable<CSVRecord> records = null;
		try {
			records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (CSVRecord record : records) {
		    String movieId = record.get("movieId");
		    String title = record.get("title");
		    String genres = record.get("genres");
		    movies.put(movieId, title, genres);
		}		    
	}
	
	public Map<String, Double> getUserMovieMap(String userId){
		return users.row(userId);
	}
	
	public Map<String, String> getMovieInfo(String movieId){
		return movies.row(movieId);
	}
	
	public Map<String, Map<String, String>> getMoviesRowMap(){
		return movies.rowMap();
	}
	
	public Map<String, String> getMovieIdToTitleMap(){
		Map<String, Map<String, String>> moviesRowMap = getMoviesRowMap();
		Map<String, String> movieIdToNameMap = new HashMap<>();
		moviesRowMap.forEach((movieId, vMap) -> {
			vMap.forEach((movieTitle, movieRating) -> {
				movieIdToNameMap.put(movieId, movieTitle);
			});
		});
		return movieIdToNameMap;
	}
	
	public Map<String, String> getMovieTitleToIdMap(){
		Map<String, Map<String, String>> moviesRowMap = getMoviesRowMap();
		Map<String, String> movieTitleToIdMap = new HashMap<>();
		moviesRowMap.forEach((movieId, vMap) -> {
			vMap.forEach((movieTitle, movieRating) -> {
				movieTitleToIdMap.put(movieTitle, movieId);
			});
		});
		return movieTitleToIdMap;
	}
}
