package sample.camera;

import java.io.File;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;

import camus.service.geo.Point;
import camus.service.image.Color;
import opencvj.Mats;
import opencvj.OpenCvJSystem;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.camera.OpenCvJCamera;
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
public class TestOpenCvJCamera {
	public static final void main(String[] args) throws Exception {
    	CommandLineParser parser = new CommandLineParser("opencv_camera ");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
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
        
        ConfigNode config = OpenCvJSystem.getConfigNode(cl.getOptionString("camera").getOrElse("highgui"));
//        ConfigNode config = OpenCvJSystem.getConfigNode(cl.getOptionString("camera", "xtion/depth")); 
//      Config config = OpenCvJSystem.getConfig(cl.getOptionString("camera", "xtion.color"));
        
        // creates target test object and dependent ones
        //
		OpenCvJCamera camera = OpenCvJSystem.createOpenCvJCamera(config);

		OpenCvView window = OpenCvViewManager.getView("camera", camera.getSize());

		Mat image = new Mat();
		camera.open();
		try {
			FramePerSecondMeasure fpsMeasure = new FramePerSecondMeasure(0.01);
			
//			wdriver.start();
			while ( window.getVisible() ) {
				fpsMeasure.startFrame();
				camera.capture(image);
				fpsMeasure.stopFrame();
				
				window.draw(Mats.toBufferedImage(image));
				window.drawString(String.format("fps=%.0f", fpsMeasure.getFps()),
									new Point(10, 17), 17, Color.GREEN);
				window.updateView();
				
//	    		Runtime.getRuntime().gc();
			}
			System.out.println(String.format("fps=%.1f", fpsMeasure.getFps()));
		}
		finally {
			image.release();

			if ( camera instanceof Initializable ) {
				((Initializable)camera).destroyQuietly();
			}
			
			OpenCvJSystem.shutdown();
		}
	}
}