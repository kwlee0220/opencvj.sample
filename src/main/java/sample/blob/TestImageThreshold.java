package sample.blob;

import java.io.File;

import camus.service.image.Color;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import opencvj.Mats;
import opencvj.OpenCvJSystem;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.blob.ImageThreshold;
import opencvj.camera.OpenCvJCamera;
import sample.TestOpenCvJ;
import utils.CommandLine;
import utils.CommandLineParser;
import utils.FramePerSecondMeasure;
import utils.Initializable;
import utils.Log4jConfigurator;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TestImageThreshold {
	public static final void main(String[] args) throws Exception {
    	Log4jConfigurator.configure("log4j.properties");
    	
    	CommandLineParser parser = new CommandLineParser("test_image_threshold");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
    	parser.addArgOption("threshold", "config path", "threshold config path");
    	parser.addOption(new Option("h", "usage help"));
    	CommandLine cl = parser.parseArgs(args);
	    
	    if ( cl.hasOption("h") ) {
	    	cl.exitWithUsage(0);
	    }

	    String homeDirPath = cl.getOptionValue("home", ".");
	    File homeDir = new File(homeDirPath).getCanonicalFile();
        if ( !homeDir.isDirectory() ) {
            System.err.println("Invalid home directory: path=" + homeDirPath);
            cl.exitWithUsage(-1);
        }
	    
		TestOpenCvJ.initialize(homeDir);
        
        ConfigNode cameraConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("camera", "highgui")); 
        ConfigNode thresholdConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("threshold",
        																"board_tracker/board.threshold")); 
        
        // creates target test object and dependent ones
        //
		OpenCvJCamera camera = OpenCvJSystem.createOpenCvJCamera(cameraConfig);
		ImageThreshold threshold = OpenCvJSystem.createImageThreshold(thresholdConfig);
//		ImageThreshold threshold = new AdaptiveImageThreshold();
    	
		Mat image = new Mat();
		Mat mask = new Mat();
		Mat coloredMask = new Mat();

		camera.open();
    	try {
			FramePerSecondMeasure captureFps = new FramePerSecondMeasure(0.01);
			FramePerSecondMeasure detectFps = new FramePerSecondMeasure(0.01);
			
			OpenCvView window = OpenCvViewManager.getView("mask", camera.getSize());
	    	while ( window.getVisible() ) {
				captureFps.startFrame();
	    		camera.capture(image);
				captureFps.stopFrame();

	    		detectFps.startFrame();
	    		threshold.detect(image, mask);
	    		detectFps.stopFrame();
	    		
	    		Imgproc.cvtColor(mask, coloredMask, Imgproc.COLOR_GRAY2BGR);
	    		window.draw(mask);
				window.draw(String.format("fps: capture=%.0f detect=%.0f",
										captureFps.getFps(), detectFps.getFps()),
										new Point(10, 17), 17, Color.RED);
				window.updateView();
	    	}
    	}
    	finally {
    		Mats.releaseAll(image, mask, coloredMask);

			if ( camera instanceof Initializable ) {
				((Initializable)camera).destroy();
			}
    		
			OpenCvJSystem.shutdown();
    	}
	}
}
