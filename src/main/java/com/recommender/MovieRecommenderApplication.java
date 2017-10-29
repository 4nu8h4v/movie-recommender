package com.recommender;

import com.recommender.resources.MovieSearchResource;
import com.recommender.resources.RecommenderResource;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class MovieRecommenderApplication extends Application<MovieRecommenderConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MovieRecommenderApplication().run(args);
    }

    @Override
    public String getName() {
        return "testProject";
    }

    @Override
    public void initialize(final Bootstrap<MovieRecommenderConfiguration> bootstrap) {
    	bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
    }

    @Override
    public void run(final MovieRecommenderConfiguration configuration,
                    final Environment environment) {
        RecommenderResource recommenderResource = new RecommenderResource();  
        MovieSearchResource movieSearchResource = new MovieSearchResource();
        environment.jersey().register(recommenderResource);
        environment.jersey().register(movieSearchResource);
    }

}
