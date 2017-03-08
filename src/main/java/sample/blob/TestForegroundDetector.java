package sample.blob;

import java.io.File;

import camus.service.image.Color;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import opencvj.Mats;
import opencvj.OpenCvJSystem;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.blob.Blobs;
import opencvj.blob.ForegroundDetector;
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
public class TestForegroundDetector {
	public static final void main(String[] args) throws Exception {
    	Log4jConfigurator.configure("log4j.properties");
    	
    	CommandLineParser parser = new CommandLineParser("foreground_detector ");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
    	parser.addArgOption("bgmodel", "config path", "background model config path");
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
//        ConfigNode cameraConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("camera", "xtion/depth")); 
        ConfigNode detectorConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("detector",
        																"color_bgmodel")); 
//																		"mouse/fingertip/hand")); 
        
        // creates target test object and dependent ones
        //
        OpenCvJCamera camera = OpenCvJSystem.createOpenCvJCamera(cameraConfig);
        ForegroundDetector detector = OpenCvJSystem.createForegroundDetector(detectorConfig);
		
		Point[] corners = null;
//		Rect roi = new Rect(new org.opencv.core.Point(160, 120), new Size(320, 240));
    	
		Mat image = new Mat();
		Mat mask = new Mat();

		camera.open();
    	try {
	        camera.dropFrames(10);
        	Blobs.learnBackground(camera, detector.getBackgroundModel(), 3*1000,
        							OpenCvViewManager.getView("bgmodel", camera.getSize()));
        	OpenCvViewManager.destroyView("bgmodel");
        	
			FramePerSecondMeasure captureFps = new FramePerSecondMeasure(0.01);
			FramePerSecondMeasure detectFps = new FramePerSecondMeasure(0.01);
			
			OpenCvView window = OpenCvViewManager.getView("fgblos", camera.getSize());
	    	while ( window.getVisible() ) {
				captureFps.startFrame();
	    		camera.capture(image);
				captureFps.stopFrame();

	    		detectFps.startFrame();
	    		detector.detectForeground(image, corners, mask);
	    		detectFps.stopFrame();
	    		
				window.draw(mask);
	    		window.draw(String.format("fps: capture=%.0f detect=%.0f",
										captureFps.getFps(), detectFps.getFps()),
										new Point(10, 17), 17, Color.GREEN);
	    		window.updateView();
	    	}
    	}
    	finally {
    		Mats.releaseAll(image, mask);

			if ( camera instanceof Initializable ) {
				((Initializable)camera).destroy();
			}
			OpenCvJSystem.shutdown();
    	}
	}
}
