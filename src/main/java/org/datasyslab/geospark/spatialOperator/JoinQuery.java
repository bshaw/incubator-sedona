/**
 * FILE: JoinQuery.java
 * PATH: org.datasyslab.geospark.spatialOperator.JoinQuery.java
 * Copyright (c) 2017 Arizona State University Data Systems Lab.
 * All rights reserved.
 */
package org.datasyslab.geospark.spatialOperator;

import java.io.Serializable;
import java.util.HashSet;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.datasyslab.geospark.joinJudgement.PointByPolygonJudgement;
import org.datasyslab.geospark.joinJudgement.PointByPolygonJudgementUsingIndex;
import org.datasyslab.geospark.joinJudgement.PointByRectangleJudgement;
import org.datasyslab.geospark.joinJudgement.PointByRectangleJudgementUsingIndex;
import org.datasyslab.geospark.joinJudgement.PolygonByPolygonJudgement;
import org.datasyslab.geospark.joinJudgement.PolygonByPolygonJudgementUsingIndex;
import org.datasyslab.geospark.joinJudgement.RectangleByRectangleJudgement;
import org.datasyslab.geospark.joinJudgement.RectangleByRectangleJudgementUsingIndex;
import org.datasyslab.geospark.spatialRDD.PointRDD;
import org.datasyslab.geospark.spatialRDD.PolygonRDD;
import org.datasyslab.geospark.spatialRDD.RectangleRDD;

/**
 * 
 * @author Arizona State University DataSystems Lab
 *
 */

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;

import scala.Tuple2;

// TODO: Auto-generated Javadoc
//todo: Replace older join query class.


/**
 * The Class JoinQuery.
 */
public class JoinQuery implements Serializable{

	/** The polygon RDD. */
	public PolygonRDD polygonRDD;
	
	/** The rectangle RDD. */
	public RectangleRDD rectangleRDD;
	
	/** The distance. */
	double distance=0.0;
	
	/** The sc. */
	JavaSparkContext sc;
	
	/**
	 * Instantiates a new join query.
	 *
	 * @param sc the sc
	 * @param pointRDD the point RDD
	 * @param rectangleRDDUnpartitioned the rectangle RDD unpartitioned
	 */
	public JoinQuery(JavaSparkContext sc,PointRDD pointRDD, RectangleRDD rectangleRDDUnpartitioned)
	{
        if(pointRDD.gridPointRDD == null) {
            throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
        }
		this.rectangleRDD=rectangleRDDUnpartitioned;
		this.rectangleRDD.rawRectangleRDD=rectangleRDDUnpartitioned.rawRectangleRDD;
		this.rectangleRDD.SpatialPartition(pointRDD.grids);
		this.sc=sc;
		//this.rectangleRDD.gridRectangleRDD.persist(StorageLevel.MEMORY_ONLY());
	}
	
	/**
	 * Instantiates a new join query.
	 *
	 * @param sc the sc
	 * @param objectRDD the object RDD
	 * @param rectangleRDDUnpartitioned the rectangle RDD unpartitioned
	 */
	public JoinQuery(JavaSparkContext sc,RectangleRDD objectRDD, RectangleRDD rectangleRDDUnpartitioned)
	{
        if(objectRDD.gridRectangleRDD == null) {
            throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
        }
		this.rectangleRDD=rectangleRDDUnpartitioned;
		this.rectangleRDD.rawRectangleRDD=rectangleRDDUnpartitioned.rawRectangleRDD;
		this.rectangleRDD.SpatialPartition(objectRDD.grids);
		this.sc=sc;
	}
	
	/**
	 * Instantiates a new join query.
	 *
	 * @param sc the sc
	 * @param pointRDD the point RDD
	 * @param polygonRDDUnpartitioned the polygon RDD unpartitioned
	 */
	public JoinQuery(JavaSparkContext sc,PointRDD pointRDD, PolygonRDD polygonRDDUnpartitioned)
	{
        if(pointRDD.gridPointRDD == null) {
            throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
        }
		this.polygonRDD=polygonRDDUnpartitioned;
		this.polygonRDD.rawPolygonRDD=polygonRDDUnpartitioned.rawPolygonRDD;
		this.polygonRDD.SpatialPartition(pointRDD.grids);
		this.sc=sc;
	}

	/**
	 * Instantiates a new join query.
	 *
	 * @param sc the sc
	 * @param objectRDD the object RDD
	 * @param polygonRDDUnpartitioned the polygon RDD unpartitioned
	 */
	public JoinQuery(JavaSparkContext sc,PolygonRDD objectRDD, PolygonRDD polygonRDDUnpartitioned)
	{
        if(objectRDD.gridPolygonRDD == null) {
            throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
        }
		this.polygonRDD=polygonRDDUnpartitioned;
		this.polygonRDD.rawPolygonRDD=polygonRDDUnpartitioned.rawPolygonRDD;
		this.polygonRDD.SpatialPartition(objectRDD.grids);
		this.sc=sc;
	}

    /**
     * Spatial join query using index.
     *
     * @param pointRDD the point RDD
     * @param rectangleRDD the rectangle RDD
     * @return the java pair RDD
     */
    public JavaPairRDD<Envelope, HashSet<Point>> SpatialJoinQueryUsingIndex(PointRDD pointRDD,RectangleRDD rectangleRDD) {

        //Check if rawPointRDD have index.
        if(pointRDD.indexedRDD == null) {
            throw new NullPointerException("Need to invoke buildIndex() first, indexedRDD is null");
        }
        if(pointRDD.gridPointRDD == null) {
            throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
        }
 
        JavaPairRDD<Integer, Tuple2<Iterable<STRtree>, Iterable<Envelope>>> cogroupResult = pointRDD.indexedRDD.cogroup(this.rectangleRDD.gridRectangleRDD);

        //flatMapToPair, use HashSet.

        JavaPairRDD<Envelope, HashSet<Point>> joinResultWithDuplicates = cogroupResult.flatMapToPair(new PointByRectangleJudgementUsingIndex(pointRDD.grids.size()));
        
        JavaPairRDD<Envelope, HashSet<Point>> joinListResultAfterAggregation = aggregateJoinResultPointByRectangle(joinResultWithDuplicates);

        return joinListResultAfterAggregation;
    }

    /**
     * Spatial join query.
     *
     * @param pointRDD the point RDD
     * @param rectangleRDD the rectangle RDD
     * @return the java pair RDD
     */
    public JavaPairRDD<Envelope, HashSet<Point>> SpatialJoinQuery(PointRDD pointRDD,RectangleRDD rectangleRDD) {
        //todo: Add logic, if this is cached, no need to calculate it again.

        if(pointRDD.gridPointRDD == null) {
            throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
        }
        
        JavaPairRDD<Integer, Tuple2<Iterable<Point>, Iterable<Envelope>>> cogroupResult = pointRDD.gridPointRDD.cogroup(this.rectangleRDD.gridRectangleRDD);
            

        //flatMapToPair, use HashSet.

        JavaPairRDD<Envelope, HashSet<Point>> joinResultWithDuplicates = cogroupResult.flatMapToPair(new PointByRectangleJudgement(pointRDD.grids.size()));
        
        JavaPairRDD<Envelope, HashSet<Point>> joinListResultAfterAggregation = aggregateJoinResultPointByRectangle(joinResultWithDuplicates);
        
        return joinListResultAfterAggregation;
        

    }
    
 

    /**
     * Spatial join query using index.
     *
     * @param objectRDD the object RDD
     * @param rectangleRDD the rectangle RDD
     * @return the java pair RDD
     */
    public JavaPairRDD<Envelope, HashSet<Envelope>> SpatialJoinQueryUsingIndex(RectangleRDD objectRDD,RectangleRDD rectangleRDD) {

        //Check if rawPointRDD have index.
        if(objectRDD.indexedRDD == null) {
            throw new NullPointerException("Need to invoke buildIndex() first, indexedRDD is null");
        }
        if(objectRDD.gridRectangleRDD == null) {
            throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
        }
        
        JavaPairRDD<Integer, Tuple2<Iterable<STRtree>, Iterable<Envelope>>> cogroupResult = objectRDD.indexedRDD.cogroup(this.rectangleRDD.gridRectangleRDD);

        JavaPairRDD<Envelope, HashSet<Envelope>> joinResultWithDuplicates = cogroupResult.flatMapToPair(new RectangleByRectangleJudgementUsingIndex(objectRDD.grids.size()));
        //AggregateByKey?
        JavaPairRDD<Envelope, HashSet<Envelope>> joinListResultAfterAggregation = aggregateJoinResultRectangleByRectangle(joinResultWithDuplicates);

        return joinListResultAfterAggregation;
    }

    
    /**
     * Spatial join query.
     *
     * @param objectRDD the object RDD
     * @param rectangleRDD the rectangle RDD
     * @return the java pair RDD
     */
    public JavaPairRDD<Envelope, HashSet<Envelope>> SpatialJoinQuery(RectangleRDD objectRDD,RectangleRDD rectangleRDD) {
        //todo: Add logic, if this is cached, no need to calculate it again.
       // JavaPairRDD<Integer, Envelope> tmpGridRDDForQuerySet = getIntegerEnvelopeJavaPairRDD( pointRDD, rectangleRDD);

        //cogroup
    	
        if(objectRDD.gridRectangleRDD == null) {
            throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
        }
        JavaPairRDD<Integer, Tuple2<Iterable<Envelope>, Iterable<Envelope>>> cogroupResult = objectRDD.gridRectangleRDD.cogroup(this.rectangleRDD.gridRectangleRDD);

        JavaPairRDD<Envelope, HashSet<Envelope>> joinResultWithDuplicates = cogroupResult.flatMapToPair(new RectangleByRectangleJudgement(objectRDD.grids.size()));

        JavaPairRDD<Envelope, HashSet<Envelope>> joinListResultAfterAggregation = aggregateJoinResultRectangleByRectangle(joinResultWithDuplicates);
            
        return joinListResultAfterAggregation;

    }
    

   
    /**
     * Spatial join query.
     *
     * @param pointRDD the point RDD
     * @param polygonRDD the polygon RDD
     * @return the java pair RDD
     */
    public JavaPairRDD<Polygon, HashSet<Point>> SpatialJoinQuery(PointRDD pointRDD,PolygonRDD polygonRDD) {
    	//todo: Add logic, if this is cached, no need to calculate it again.
    	// JavaPairRDD<Integer, Envelope> tmpGridRDDForQuerySet = getIntegerEnvelopeJavaPairRDD( pointRDD, rectangleRDD);

    	//cogroup
   	
       if(pointRDD.gridPointRDD == null) {
           throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
       }

       JavaPairRDD<Integer, Tuple2<Iterable<Point>, Iterable<Polygon>>> cogroupResult = pointRDD.gridPointRDD.cogroup(this.polygonRDD.gridPolygonRDD);

       JavaPairRDD<Polygon, HashSet<Point>> joinResultWithDuplicates = cogroupResult.flatMapToPair(new PointByPolygonJudgement());
       
       JavaPairRDD<Polygon, HashSet<Point>> joinListResultAfterAggregation = aggregateJoinResultPointByPolygon(joinResultWithDuplicates);
       
       return joinListResultAfterAggregation;

   }


   
    /**
     * Spatial join query using index.
     *
     * @param pointRDD the point RDD
     * @param polygonRDD the polygon RDD
     * @return the java pair RDD
     */
    public JavaPairRDD<Polygon, HashSet<Point>> SpatialJoinQueryUsingIndex(PointRDD pointRDD,PolygonRDD polygonRDD) {

       //Check if rawPointRDD have index.
       if(pointRDD.indexedRDD == null) {
           throw new NullPointerException("Need to invoke buildIndex() first, indexedRDD is null");
       }
       if(pointRDD.gridPointRDD == null) {
           throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
       }

       JavaPairRDD<Integer, Tuple2<Iterable<STRtree>, Iterable<Polygon>>> cogroupResult = pointRDD.indexedRDD.cogroup(this.polygonRDD.gridPolygonRDD);

       JavaPairRDD<Polygon, HashSet<Point>> joinResultWithDuplicates = cogroupResult.flatMapToPair(new PointByPolygonJudgementUsingIndex());

       //AggregateByKey?
       JavaPairRDD<Polygon, HashSet<Point>> joinListResultAfterAggregation = aggregateJoinResultPointByPolygon(joinResultWithDuplicates);

       return joinListResultAfterAggregation;
   }
   

   
    /**
     * Spatial join query using index.
     *
     * @param objectRDD the object RDD
     * @param windowRDD the window RDD
     * @return the java pair RDD
     */
    public JavaPairRDD<Polygon, HashSet<Polygon>> SpatialJoinQueryUsingIndex(PolygonRDD objectRDD,PolygonRDD windowRDD) {

       //Check if rawPointRDD have index.
       if(objectRDD.indexedRDD == null) {
           throw new NullPointerException("Need to invoke buildIndex() first, indexedRDD is null");
       }
       if(objectRDD.gridPolygonRDD == null) {
           throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
       }
       JavaPairRDD<Integer, Tuple2<Iterable<STRtree>, Iterable<Polygon>>> cogroupResult = objectRDD.indexedRDD.cogroup(this.polygonRDD.gridPolygonRDD);

       JavaPairRDD<Polygon, HashSet<Polygon>> joinResultWithDuplicates = cogroupResult.flatMapToPair(new PolygonByPolygonJudgementUsingIndex(objectRDD.grids.size()));

       JavaPairRDD<Polygon, HashSet<Polygon>> joinListResultAfterAggregation = aggregateJoinResultPolygonByPolygon(joinResultWithDuplicates);

       return joinListResultAfterAggregation;
   }


    /**
     * Spatial join query.
     *
     * @param objectRDD the object RDD
     * @param polygonRDD the polygon RDD
     * @return the java pair RDD
     */
    public JavaPairRDD<Polygon, HashSet<Polygon>> SpatialJoinQuery(PolygonRDD objectRDD,PolygonRDD polygonRDD) {
       //todo: Add logic, if this is cached, no need to calculate it again.
      // JavaPairRDD<Integer, Envelope> tmpGridRDDForQuerySet = getIntegerEnvelopeJavaPairRDD( pointRDD, rectangleRDD);

       //cogroup
   	
       if(objectRDD.gridPolygonRDD == null) {
           throw new NullPointerException("Need to do spatial partitioning first, gridedSRDD is null");
       }
   
       JavaPairRDD<Integer, Tuple2<Iterable<Polygon>, Iterable<Polygon>>> cogroupResult = objectRDD.gridPolygonRDD.cogroup(this.polygonRDD.gridPolygonRDD);
       
       JavaPairRDD<Polygon, HashSet<Polygon>> joinResultWithDuplicates = cogroupResult.flatMapToPair(new PolygonByPolygonJudgement(objectRDD.grids.size()));
       
       JavaPairRDD<Polygon, HashSet<Polygon>> joinListResultAfterAggregation = aggregateJoinResultPolygonByPolygon(joinResultWithDuplicates);
       
       return joinListResultAfterAggregation;

   }  

   
   
    private static JavaPairRDD<Envelope, HashSet<Point>> aggregateJoinResultPointByRectangle(JavaPairRDD<Envelope, HashSet<Point>> joinResultBeforeAggregation) {
        //AggregateByKey?
        JavaPairRDD<Envelope, HashSet<Point>> joinResultAfterAggregation = joinResultBeforeAggregation.reduceByKey(new Function2<HashSet<Point>, HashSet<Point>, HashSet<Point>>() {
            @Override
            public HashSet<Point> call(HashSet<Point> points, HashSet<Point> points2) throws Exception {
                points.addAll(points2);
                return points;
            }
        });
     
        return joinResultAfterAggregation.mapValues(new Function<HashSet<Point>, HashSet<Point>>() {
            @Override
            public HashSet<Point> call(HashSet<Point> points) throws Exception {
                return new HashSet<Point>(points);
            }
        });
    }
    private static JavaPairRDD<Envelope, HashSet<Envelope>> aggregateJoinResultRectangleByRectangle(JavaPairRDD<Envelope, HashSet<Envelope>> joinResultBeforeAggregation) {
        //AggregateByKey?
        JavaPairRDD<Envelope, HashSet<Envelope>> joinResultAfterAggregation = joinResultBeforeAggregation.reduceByKey(new Function2<HashSet<Envelope>, HashSet<Envelope>, HashSet<Envelope>>() {
            @Override
            public HashSet<Envelope> call(HashSet<Envelope> objects, HashSet<Envelope> objects2) throws Exception {
            	objects.addAll(objects2);
                return objects;
            }
        });
     
        return joinResultAfterAggregation.mapValues(new Function<HashSet<Envelope>, HashSet<Envelope>>() {
            @Override
            public HashSet<Envelope> call(HashSet<Envelope> objects) throws Exception {
                return new HashSet<Envelope>(objects);
            }
        });
    }
    private static JavaPairRDD<Polygon, HashSet<Point>> aggregateJoinResultPointByPolygon(JavaPairRDD<Polygon, HashSet<Point>> joinResultBeforeAggregation) {
            //AggregateByKey?
            JavaPairRDD<Polygon, HashSet<Point>> joinResultAfterAggregation = joinResultBeforeAggregation.reduceByKey(new Function2<HashSet<Point>, HashSet<Point>, HashSet<Point>>() {
                @Override
                public HashSet<Point> call(HashSet<Point> points, HashSet<Point> points2) throws Exception {
                    points.addAll(points2);
                    return points;
                }
            });
        return joinResultAfterAggregation.mapValues(new Function<HashSet<Point>, HashSet<Point>>() {
            @Override
            public HashSet<Point> call(HashSet<Point> points) throws Exception {
                return new HashSet<Point>(points);
            }
        });
    }
    private static JavaPairRDD<Polygon, HashSet<Polygon>> aggregateJoinResultPolygonByPolygon(JavaPairRDD<Polygon, HashSet<Polygon>> joinResultBeforeAggregation) {
            //AggregateByKey?
            JavaPairRDD<Polygon, HashSet<Polygon>> joinResultAfterAggregation = joinResultBeforeAggregation.reduceByKey(new Function2<HashSet<Polygon>, HashSet<Polygon>, HashSet<Polygon>>() {
                @Override
                public HashSet<Polygon> call(HashSet<Polygon> objects, HashSet<Polygon> objects2) throws Exception {
                	objects.addAll(objects2);
                    return objects;
                }
            });
         
            return joinResultAfterAggregation.mapValues(new Function<HashSet<Polygon>, HashSet<Polygon>>() {
                @Override
                public HashSet<Polygon> call(HashSet<Polygon> objects) throws Exception {
                    return new HashSet<Polygon>(objects);
                }
            });
        }




}