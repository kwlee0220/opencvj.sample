package sample.blob;

import java.io.File;
import java.util.List;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;

import camus.service.geo.Point;
import camus.service.image.Color;
import opencvj.Mats;
import opencvj.OpenCvJSystem;
import opencvj.OpenCvJUtils;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.camera.OpenCvJCamera;
import opencvj.marker.MarkerDetector;
import sample.TestOpenCvJ;
import utils.CommandLine;
import utils.CommandLineParser;
import utils.FramePerSecondMeasure;
import utils.Initializable;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TestMarkerDetector {
	public static final void main(String[] args) throws Exception {
    	CommandLineParser parser = new CommandLineParser("test_marker_detector");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
    	parser.addArgOption("marker", "config path", "marker config path");
    	parser.addOption(new Option("h", "usage help"));
    	CommandLine cl = parser.parseArgs(args);
	    
	    if ( cl.hasOption("h") ) {
	    	cl.exitWithUsage(0);
	    }

	    String homeDirPath = cl.getOptionString("home").getOrElse(".");
	    File homeDir = new File(homeDirPath).getCanonicalFile();
        if ( !homeDir.isDirectory() ) {
            System.err.println("Invalid home directory: path=" + homeDirPath);
            cl.exitWithUsage(-1);
        }

		TestOpenCvJ.initialize(homeDir);
        
		ConfigNode cameraConfig = OpenCvJSystem.getConfigNode(cl.getOptionString("camera").getOrElse("highgui"));
		ConfigNode markerConfig = OpenCvJSystem.getConfigNode(cl.getOptionString("marker").getOrElse("marker"));
        
        // creates target test object and dependent ones
        //
		OpenCvJCamera camera = OpenCvJSystem.createOpenCvJCamera(cameraConfig);
		MarkerDetector detector = MarkerDetector.create(OpenCvJSystem.getOpenCvJLoader(), markerConfig);
    	
		Mat image = new Mat();
		
		camera.open();
    	try {
			FramePerSecondMeasure captureFps = new FramePerSecondMeasure(0.01);
			FramePerSecondMeasure detectFps = new FramePerSecondMeasure(0.01);

			OpenCvView window = OpenCvViewManager.getView("markers", camera.getSize());
	    	while ( window.getVisible() ) {
				captureFps.startFrame();
	    		camera.capture(image);
				captureFps.stopFrame();

	    		detectFps.startFrame();
	    		List<MarkerDetector.Info> infos = detector.detect(image);
	    		detectFps.stopFrame();
	    		
				window.draw(Mats.toBufferedImage(image));
				for ( MarkerDetector.Info info : infos ) {
					window.drawPolygon(OpenCvJUtils.toPolygon(info.corners), Color.RED, 2);
					for ( int i =0; i < info.corners.length; ++i ) {
						window.drawString(""+i, OpenCvJUtils.toPoint(info.corners[i]), 30, Color.RED);
					}
					window.drawString(String.format("%d", info.id), OpenCvJUtils.toPoint(info.center), 30, Color.GREEN);
				}
				window.drawString(String.format("fps: capture=%.0f detect=%.0f",
											captureFps.getFps(), detectFps.getFps()),
											new Point(10, 17), 17, Color.GREEN);
				window.updateView();
//	    		Runtime.getRuntime().gc();
	    	}
    	}
    	finally {
    		image.release();

			if ( camera instanceof Initializable ) {
				((Initializable)camera).destroy();
			}
    		
			OpenCvJSystem.shutdown();
    	}
	}
}
