package sample.cpc;

import java.io.File;

import camus.service.geo.Point;
import camus.service.geo.Size2d;
import camus.service.image.Color;

import etri.service.image.BufferedImageConvas;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;

import opencvj.Mats;
import opencvj.OpenCvJSystem;
import opencvj.camera.OpenCvJCamera;
import opencvj.projector.OpenCvBeamProjector;
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
public class TestProjector {
	private static final int COUNT = 200;
	
	public static final void main(String[] args) throws Exception {
    	Log4jConfigurator.configure("log4j.properties");
    	
    	CommandLineParser parser = new CommandLineParser("projector ");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
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
        
        ConfigNode camConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("camera", "highgui")); 
        
        // creates target test object and dependent ones
        //
		OpenCvJCamera camera = OpenCvJCamera.create(camConfig);
		
		Mat image = new Mat();

		/*****************************/
		OpenCvBeamProjector prj = OpenCvBeamProjector.createMockProjector(-1, new Size2d(640, 480));
		prj.setVisible(true);
		Thread.sleep(2*1000);

		camera.open();
		prj.setPower(true);
		for ( int i =0; i < 100; ++i ) {
			camera.capture(image);
			prj.show(image);
		}
		prj.setPower(false);
		for ( int i =0; i < 100; ++i ) {
			camera.capture(image);
			prj.show(image);
		}
		prj.setPower(true);
		camera.close();
		Thread.sleep(2*1000);
		prj.destroy();
		
		/*****************************/
		
		int[] monitorIndices = OpenCvBeamProjector.getMonitorIndices();
		OpenCvBeamProjector[] projectors = new OpenCvBeamProjector[monitorIndices.length];
		for ( int i =0; i < projectors.length; ++i ) {
			projectors[i] = new OpenCvBeamProjector();
			projectors[i].setMonitorIndex(monitorIndices[i]);
			projectors[i].initialize();
			projectors[i].setVisible(true);
		}
		
		camera.open();
		try {
			camera.capture(image);
			
			for ( int i =0; i < projectors.length; ++i ) {
				projectors[i].setPower(true);
			}
			
			FramePerSecondMeasure captureFps = new FramePerSecondMeasure(0.01);
			for ( int j =0; j < COUNT; ++j ) {
				captureFps.startFrame();
				camera.capture(image);
				captureFps.stopFrame();

				for ( int i =0; i < projectors.length; ++i ) {
					BufferedImageConvas convas = new BufferedImageConvas(Mats.toBufferedImage(image));
					
					convas.drawString(String.format("%d", i), new Point(50,100), 100, Color.RED);
					convas.drawString(String.format("fps=%.0f", captureFps.getFps()),
									new Point(10, 17), 17, Color.GREEN);
					
					projectors[i].show(convas.getBufferedImage());
				}
			}
			System.out.println(String.format("fps=%.1f", captureFps.getFps()));
		}
		finally {
			image.release();

			for ( int i =0; i < projectors.length; ++i ) {
				projectors[i].destroy();
			}
			if ( camera instanceof Initializable ) {
				((Initializable)camera).destroy();
			}
			OpenCvJSystem.shutdown();
		}
	}
}