package org.deeplearning4j.examples.dataExamples;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.canova.api.records.reader.RecordReader;
import org.canova.api.records.reader.impl.CSVRecordReader;
import org.canova.api.split.FileSplit;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Read a csv file. Fit and plot the data using Deeplearning4J. 
 * 
 * @author Robert Altena  
 */
public class CSVPlotter {
	
	public static void main( String[] args ) throws IOException, InterruptedException
    {
    	String filename = "src/main/resources/DataExamples/CSVplotData.csv";
    	DataSet ds = ReadCSVDataset(filename);
    	
    	ArrayList<DataSet> DataSetList = new ArrayList<DataSet>();
    	DataSetList.add(ds);
    	
    	PlotDataset(DataSetList); //Plot the data, make sure we have the right data.
    	
    	MultiLayerNetwork net =FitStraightline(ds);
    	
    	// Get the min and max x values, using Nd4j 
    	NormalizerMinMaxScaler preProcessor = new NormalizerMinMaxScaler();
    	preProcessor.fit(ds);
        int nSamples = 50;
        INDArray x = Nd4j.linspace(preProcessor.getMin().getInt(0),preProcessor.getMax().getInt(0),nSamples).reshape(nSamples, 1);
        INDArray y = net.output(x);
        DataSet modeloutput = new DataSet(x,y);
        DataSetList.add(modeloutput);
        
    	PlotDataset(DataSetList);    //Plot data and model fit.
    }
    
	/**
	 * Fit a straight line using a neural network. 
	 * @param ds The dataset to fit.
	 * @return The network fitted to the data
	 */
	public static MultiLayerNetwork FitStraightline(DataSet ds){
		int seed = 12345;
		int iterations = 1;
		int nEpochs = 200;
		double learningRate = 0.00001;
		int numInputs = 1;
	    int numOutputs = 1;
		
	    //
	    // Hook up the 1 input to the one output. 
	    // The resulting model is a straight line. 
	    //
		MultiLayerConfiguration conf = new  NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numOutputs)
                        .activation("identity")
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation("identity")
                        .nIn(numOutputs).nOut(numOutputs).build())
                .pretrain(false).backprop(true).build();
		
		MultiLayerNetwork net = new MultiLayerNetwork(conf);
		net.init();
	    net.setListeners(new ScoreIterationListener(1));
	    
	    for( int i=0; i<nEpochs; i++ ){
	    	net.fit(ds);
	    }
	    
	    return net;
	}
	
    /**
     * Read a CSV file into a datasetIterator.
     * 
     * Use the correct constructor:
     * DataSetIterator Iter  = new RecordReaderDataSetIterator(rr,batchSize);    
     * returns the data as follows:
     * ===========INPUT===================
     *[[12.89, 22.70],
     * [19.34, 20.47],
     * [16.94,  6.08],
     *  [15.87,  8.42],
     *  [10.71, 26.18]]
     *  
     *  Which is not the way the framework likes its data.
     *  
     *  This one: 
     *   RecordReaderDataSetIterator(rr,batchSize, 1, 1, true); 
     *   returns
     *   ===========INPUT===================
     * [12.89, 19.34, 16.94, 15.87, 10.71]
     * =================OUTPUT==================
     * [22.70, 20.47,  6.08,  8.42, 26.18]
     *  
     *  This can be used as is for regression.
     */
	public static DataSet ReadCSVDataset(String filename) throws IOException, InterruptedException{
		int batchSize = 1000;
		RecordReader rr = new CSVRecordReader();
		rr.initialize(new FileSplit(new File(filename)));
    	
		DataSetIterator iter =  new RecordReaderDataSetIterator(rr,batchSize, 1, 1, true);
		return iter.next();
	}
	
	/**
	 * Generate an xy plot of the datasets provided.
	 */
	static void PlotDataset(ArrayList<DataSet> DataSetList ){
		
		XYSeriesCollection c = new XYSeriesCollection();
		
		int dscounter = 1; //use to name the dataseries
		for (DataSet ds : DataSetList)
		{
			INDArray features = ds.getFeatures();
			INDArray outputs= ds.getLabels();
		
			int nRows = features.rows(); 
			XYSeries series = new XYSeries("S" + dscounter);
			for( int i=0; i<nRows; i++ ){
				series.add(features.getDouble(i), outputs.getDouble(i));
			}
    	
			c.addSeries(series);
		}
    	
		XYDataset dataset = c;
		
		String title = "title";
		String xAxisLabel = "xAxisLabel";
		String yAxisLabel = "yAxisLabel";	
		PlotOrientation orientation = PlotOrientation.VERTICAL;
		boolean legend = false;
		boolean tooltips = false;
		boolean urls = false;
		JFreeChart chart =ChartFactory.createScatterPlot(title , xAxisLabel, yAxisLabel, dataset , orientation , legend , tooltips , urls);
    	JPanel panel = new ChartPanel(chart);
    	
    	 JFrame f = new JFrame();
    	 f.add(panel);
    	 f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
         f.pack();
         f.setTitle("Training Data");

         f.setVisible(true);
	}
}
