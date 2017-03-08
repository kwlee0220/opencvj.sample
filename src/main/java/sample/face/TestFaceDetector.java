package sample.face;

import java.io.File;

import camus.service.geo.Rectangle;
import camus.service.image.Color;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import opencvj.OpenCvJSystem;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.camera.OpenCvJCamera;
import opencvj.face.SimpleOpenCvJFaceDetector;
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
public class TestFaceDetector {
	public static final void main(String[] args) throws Exception {
    	Log4jConfigurator.configure("log4j.properties");
    	
    	CommandLineParser parser = new CommandLineParser("face_detector");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
    	parser.addArgOption("face", "face config path", "face detector config path");
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
        ConfigNode faceConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("face", "face"));

        // creates target test object and dependent ones
        //
		OpenCvJCamera camera = OpenCvJCamera.create(cameraConfig);
		SimpleOpenCvJFaceDetector detector = SimpleOpenCvJFaceDetector.create(OpenCvJSystem.getOpenCvJLoader(),
																				faceConfig);
    	detector.setCascadeFile(new File(OpenCvJSystem.getConfigDir(),
    									"haarcascades/haarcascade_frontalface_alt.xml"));
    	detector.initialize();
		
		Mat image = new Mat();
		
		camera.open();
    	try {
			FramePerSecondMeasure captureFps = new FramePerSecondMeasure(0.01);
			FramePerSecondMeasure detectFps = new FramePerSecondMeasure(0.01);

	    	OpenCvView window = OpenCvViewManager.getView("camera", camera.getSize());
	    	while ( window.getVisible() ) {
    			captureFps.startFrame();
    			camera.capture(image);
    			captureFps.stopFrame();

	    		detectFps.startFrame();
	    		Rectangle[] faces = detector.detectFace(image, null);
	    		detectFps.stopFrame();
    				
	    		window.draw(image);
	    		for ( Rectangle face: faces ) {
	    			window.drawRect(face, Color.RED, 2);
	    		}
				window.draw(String.format("fps: capture=%.0f detect=%.0f",
							captureFps.getFps(), detectFps.getFps()),
							new Point(10, 17), 17, Color.GREEN);
	    		window.updateView();
	    	}
    	}
    	finally {
    		image.release();
    		detector.destroy();

			if ( camera instanceof Initializable ) {
				((Initializable)camera).destroy();
			}
			OpenCvJSystem.shutdown();
    	}
	}
}
