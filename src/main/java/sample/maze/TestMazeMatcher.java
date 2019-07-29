package sample.maze;

import java.io.File;
import java.util.List;
import java.util.Set;

import camus.service.image.Color;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.google.common.collect.Sets;

import opencvj.OpenCvJSystem;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.camera.OpenCvJCamera;
import opencvj.features2d.maze.MazeInfo;
import opencvj.features2d.maze.MazeMatcher;
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
public class TestMazeMatcher {
	public static final void main(String[] args) throws Exception {
    	Log4jConfigurator.configure("log4j.properties");
    	
    	CommandLineParser parser = new CommandLineParser("test_maze_matcher ");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
    	parser.addArgOption("maze", "config path", "MazeMatcher config path");
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
        ConfigNode mazeConfig = OpenCvJSystem.getConfigNode(cl.getOptionString("maze").getOrElse("maze")); 
        
        // creates target test object and dependent ones
        //
		OpenCvJCamera camera = OpenCvJSystem.createOpenCvJCamera(cameraConfig);
		MazeMatcher matcher = MazeMatcher.create(mazeConfig);
    	
		Mat image = new Mat();

		camera.open();
    	try {
    		camera.dropFrames(10);
        	
			FramePerSecondMeasure captureFps = new FramePerSecondMeasure(0.01);
			FramePerSecondMeasure detectFps = new FramePerSecondMeasure(0.01);
			
			OpenCvView window = OpenCvViewManager.getView("maze", camera.getSize());

    		Set<String> detecteds = Sets.newHashSet();
	    	while ( window.getVisible() ) {
				captureFps.startFrame();
	    		camera.capture(image);
	    		camera.capture(image);
				captureFps.stopFrame();

	    		detectFps.startFrame();
	    		List<MazeInfo> infos = matcher.matchAll(image);
	    		detectFps.stopFrame();

	    		window.draw(image);
	    		
	    		detecteds.clear();
	    		for ( MazeInfo info: infos ) {
	    			window.draw(info.m_corners, Color.RED, 2);
	    			window.draw(info.m_id, info.m_corners[0], 15, Color.GREEN);
	    			
	    			detecteds.add(info.m_id);
	    		}
	    		
	    		window.draw(String.format("fps: capture=%.0f detect=%.0f",
										captureFps.getFps(), detectFps.getFps()),
										new Point(10, 17), 17, Color.GREEN);
	    		window.updateView();
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
