import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Reducer class for testing Model
 * 
 */
public class TestReducer extends Reducer<Text, FlightDetail, Text, Text> {
	
	/**
	 * Get the trained classifier
	 * 
	 */
	private Classifier getClassifier(String fileName) throws Exception {
		Path path = new Path("models/" + fileName);
	    FileSystem fileSystem = path.getFileSystem(new Configuration());
	    FSDataInputStream inputStream = fileSystem.open(path);
		return (Classifier) SerializationHelper.read(inputStream);
	}
	
	/**
	 * Get the dense instance from the given flight details
	 * 
	 */
	private Instance getInstance(FlightDetail flightDetail, Instances set) {
		Instance instance = new DenseInstance(9);
		instance.setValue(set.attribute(0), flightDetail.getDayOfMonth().get());
		instance.setValue(set.attribute(1), flightDetail.getDayOfWeek().get());
		instance.setValue(set.attribute(2), 
				flightDetail.getCarrierCode().toString().hashCode());
		instance.setValue(set.attribute(3), flightDetail.getOriginAirportId().get());
		instance.setValue(set.attribute(4), flightDetail.getDestAirportId().get());
		instance.setValue(set.attribute(5), flightDetail.getCRSDepTime().get());
		instance.setValue(set.attribute(6), flightDetail.getCRSArrTime().get());
		instance.setValue(set.attribute(8), flightDetail.getHoliday().get() + "");
		
		return instance;
	}
	
	/**
	 * Initialize an empty set
	 */
	private Instances initSet() {
		// Declare numeric attributes
		Attribute dayOfMonth = new Attribute("dayOfMonth");
		Attribute dayOfWeek = new Attribute("dayOfWeek");
		Attribute originAirportId = new Attribute("originAirportId");
		Attribute destAirportId = new Attribute("destAirportId");
		Attribute CRSDepTime = new Attribute("CRSDepTime");
		Attribute CRSArrTime = new Attribute("CRSArrTime");
		
		// Declare nominal attribute along with its values
		List<String> holidayAttributes = new ArrayList<String>();
		holidayAttributes.add("true");
		holidayAttributes.add("false");
		Attribute holiday = new Attribute("holiday", holidayAttributes);
		
		// Declare numeric attribute with a string converted to its hash value
		Attribute carrierCode = new Attribute("carrierCode");
		
		// Declare the class attribute along with its values
		List<String> classAttributes = new ArrayList<String>();
		classAttributes.add("true");
		classAttributes.add("false");
		Attribute delay = new Attribute("delay", classAttributes);
		
		// Declare the feature vector
		ArrayList<Attribute> wekaAttributes = new ArrayList<Attribute>();
		wekaAttributes.add(dayOfMonth);
		wekaAttributes.add(dayOfWeek);
		wekaAttributes.add(carrierCode);
		wekaAttributes.add(originAirportId);
		wekaAttributes.add(destAirportId);
		wekaAttributes.add(CRSDepTime);
		wekaAttributes.add(CRSArrTime);
		wekaAttributes.add(delay);
		wekaAttributes.add(holiday);
		
		// Create an empty set
		Instances set = new Instances("Model", wekaAttributes, 0);
		// Set class index
		set.setClassIndex(7);
		
		return set;
	}
	
	@Override
	public void reduce(Text key, Iterable<FlightDetail> values, Context context)  
			throws IOException, InterruptedException {
		
		Classifier classifier;
		Instances set = initSet();
		
		try {
			classifier = getClassifier(key.toString());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		for (FlightDetail flightDetail : values) {
			Instance instance = getInstance(flightDetail, set);
			instance.setDataset(set);
			try {
				double[] fDistribution = classifier.distributionForInstance(instance);
				String format = flightDetail.getFlightNumber().get() + "_" +
						flightDetail.getFlightDate().toString() + "_" +
						flightDetail.getCRSDepTime().get();
				if (fDistribution[0] > fDistribution[1]) {
					context.write(new Text(format + ",TRUE"), new Text(""));
				} else {
					context.write(new Text(format + ",FALSE"), new Text(""));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
