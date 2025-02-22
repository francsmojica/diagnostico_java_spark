package minsait.ttaa.datio.engine;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.jetbrains.annotations.NotNull;

import static minsait.ttaa.datio.common.Common.*;
import static minsait.ttaa.datio.common.naming.PlayerInput.*;
import static minsait.ttaa.datio.common.naming.PlayerOutput.*;
import static org.apache.spark.sql.functions.*;

public class Transformer extends Writer {
    private SparkSession spark;

    public Transformer(@NotNull SparkSession spark) {
        this.spark = spark;
        Dataset<Row> df = readInput();

        df.printSchema();

        df = cleanData(df);
        df = exampleWindowFunction(df);
        df = rankOverWindow(df);
        df = potentialVsOverall(df);
        df = twoFilters(df);
        df = columnSelection(df);
        
        // for show 100 records after your transformations and show the Dataset schema
        df.show(210, false);
        df.printSchema();

        // Uncomment when you want write your final output
        //write(df);
    }

    private Dataset<Row> columnSelection(Dataset<Row> df) {
        return df.select(
        		shortName.column(),
                longName.column(),
                age.column(),
                heightCm.column(),
                weightKg.column(),
                nationality.column(),
                clubName.column(),
                overall.column(),
                potential.column(),
                teamPosition.column(),
                catHeightByPosition.column(),     
                playerCat.column(),
                potentialVsOverall.column()
        );
    }

    /**
     * @return a Dataset readed from csv file
     */
    public Dataset<Row> readInput() {
        Dataset<Row> df = spark.read()
                .option(HEADER, true)
                .option(INFER_SCHEMA, true)
                .csv(INPUT_PATH);
        return df;
    }

    /**
     * @param df
     * @return a Dataset with filter transformation applied
     * column team_position != null && column short_name != null && column overall != null
     */
    private Dataset<Row> cleanData(Dataset<Row> df) {
        df = df.filter(
                teamPosition.column().isNotNull().and(
                        shortName.column().isNotNull()
                ).and(
                        overall.column().isNotNull()
                )
        );

        return df;
    }

    /**
     * @param df is a Dataset with players information (must have team_position and height_cm columns)
     * @return add to the Dataset the column "cat_height_by_position"
     * by each position value
     * cat A for if is in 20 players tallest
     * cat B for if is in 50 players tallest
     * cat C for the rest
     */
    public Dataset<Row> exampleWindowFunction(Dataset<Row> df) {
        WindowSpec w = Window
                .partitionBy(teamPosition.column())
                .orderBy(heightCm.column().desc());

        Column rank = rank().over(w);

        Column rule = when(rank.$less(10), "A")
                .when(rank.$less(50), "B")
                .otherwise("C");

        df = df.withColumn(catHeightByPosition.getName(), rule);

        return df;
    }
    
    /* EJERCICIO 2 */
    public Dataset<Row> rankOverWindow(Dataset<Row> df) {
        WindowSpec w = Window
                .partitionBy(nationality.column())
                .orderBy(overall.column().desc());

        Column rank = rank().over(w);

        Column rule = when(rank.$less(10), "A")
                .when(rank.$less(20), "B")
                .when(rank.$less(50), "C")
                .otherwise("D");

        df = df.withColumn(playerCat.getName(), rule);

        return df;
    }
    
    /* EJERCICIO 3 */
    public Dataset<Row> potentialVsOverall(Dataset<Row> df) {
        Column rule_pvso = df.col("overall").divide(df.col("potential"));
    	
        df = df.withColumn(potentialVsOverall.getName(), rule_pvso);

        return df;
    }
    
    /* EJERCICIO 4 */
    public Dataset<Row> twoFilters(Dataset<Row> df) {
    	Column first = 	df.col("player_cat").equalTo("A");
    	Column second = df.col("player_cat").equalTo("B");
        Column third = 	df.col("player_cat").equalTo("C").and( df.col("po_vs_ov").$greater(1.15) );
        Column fourth = df.col("player_cat").equalTo("D").and( df.col("po_vs_ov").$greater(1.25) );
        		
    	Column condition = first.or( second )
    						 	.or( third )
    						 	.or( fourth ); 
    	    	
        df = df.filter( condition );

        return df;
    }
    
    /* EJERCICIO 5 */   
    
}
