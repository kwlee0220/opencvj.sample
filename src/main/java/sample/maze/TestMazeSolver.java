package sample.maze;

import java.io.File;

import camus.service.image.Color;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import opencvj.MatConvas;
import opencvj.OpenCvJ;
import opencvj.OpenCvJSystem;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.camera.OpenCvJCamera;
import opencvj.camera.OpenCvJCameraFactory;
import opencvj.features2d.maze.MazeInfo;
import opencvj.features2d.maze.MazeSolver;
import opencvj.misc.PerspectiveTransform;
import opencvj.projector.CameraProjectorComposite;
import opencvj.projector.OpenCvBeamProjector;
import sample.TestOpenCvJ;
import utils.CommandLine;
import utils.CommandLineParser;
import utils.FramePerSecondMeasure;
import utils.Initializable;
import utils.Log4jConfigurator;
import utils.config.ConfigNode;
import utils.io.IOUtils;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TestMazeSolver {
	public static final void main(String[] args) throws Exception {
    	Log4jConfigurator.configure("log4j.properties");
    	
    	CommandLineParser parser = new CommandLineParser("test_maze_solver ");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
    	parser.addArgOption("projector", "config path", "target projector config path");
    	parser.addArgOption("calib", "config path", "camera-projector config path");
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
        ConfigNode prjConfig = OpenCvJSystem.getConfigNode(cl.getOptionString("projector").getOrElse("projector")); 
        ConfigNode cpcConfig = OpenCvJSystem.getConfigNode(cl.getOptionString("calib").getOrElse("highgui_projector_calib"));
        ConfigNode mazeConfig = OpenCvJSystem.getConfigNode(cl.getOptionString("maze").getOrElse("maze")); 
        
        // creates target test object and dependent ones
        //
        OpenCvJCameraFactory cameraFact = OpenCvJSystem.createOpenCvJCameraFactory(cameraConfig);
		OpenCvBeamProjector prj = OpenCvBeamProjector.create(prjConfig);
		CameraProjectorComposite cpc = CameraProjectorComposite.create(cameraFact, prj, cpcConfig);
		MazeSolver solver = MazeSolver.create(cpc, mazeConfig);
    	
		Mat image = new Mat();

		prj.setPower(true);
		
		OpenCvJCamera camera = cameraFact.createCamera();
		camera.open();
    	try {
    		camera.dropFrames(10);
        	
			FramePerSecondMeasure captureFps = new FramePerSecondMeasure(0.01);
			FramePerSecondMeasure detectFps = new FramePerSecondMeasure(0.01);

			OpenCvView window = OpenCvViewManager.getView("maze", camera.getSize());
	    	while ( window.getVisible() ) {
				captureFps.startFrame();
	    		camera.capture(image);
				captureFps.stopFrame();

	    		detectFps.startFrame();
	    		solver.process(image);
	    		detectFps.stopFrame();
	    		
	    		MatConvas convas = new MatConvas(image);
	    		
    			MazeInfo maze = solver.getMazeInfo();
	    		if ( maze != null ) {
		    		Point[] corners = solver.getMazeCornersInImage();
	    			convas.drawContour(corners, OpenCvJ.RED, 2);
	    			
	    			if ( maze.m_corners != null ) {
		    			PerspectiveTransform trans = PerspectiveTransform.createPerspectiveTransform(
		    												maze.m_imageEntry.corners, maze.m_corners);
		    			Point[] sol = trans.perform(maze.m_solutionPath);
		    			convas.drawOpenContour(sol, OpenCvJ.RED, 2);
	    			}
	    		}
	    			
	    		window.draw(convas);
	    		window.draw(String.format("fps: capture=%.0f detect=%.0f",
										captureFps.getFps(), detectFps.getFps()),
										new Point(10, 17), 17, Color.GREEN);
	    		window.updateView();
	    	}
    	}
    	finally {
    		image.release();
    		cpc.destroy();
    		IOUtils.closeQuietly(solver);

    		Initializable.destroyQuietly(prj, camera);
			OpenCvJSystem.shutdown();
    	}
	}
}
