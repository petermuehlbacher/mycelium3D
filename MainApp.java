package mycelium_3d;

import java.io.PrintWriter;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;

import processing.core.*;
import SimpleOpenNI.*;
import controlP5.*;

@SuppressWarnings("serial")
public class MainApp extends PApplet {
	SimpleOpenNI context;
	ControlP5 cp5;
	Range  depthSlider;
	Toggle pointsToggle;
	Toggle growingToggle;
	Slider zoomSlider;
	Slider stepSlider;
	
	float        zoomF = 0.3f;
	float        rotX  = radians(180);  // by default rotate the hole scene 180deg around the x-axis, 
										// the data from openni comes upside down
	float        rotY  = radians(0);

	static PVector[] realWorldMap;
	static int[][]   substrateMap;
	static int[]	 depthMap;
	
	static int   depthWidth;
	static int   depthHeight;

	static ArrayList<Hypha> hyphaeTips = new ArrayList<Hypha>();
	static ArrayList<Hypha> hyphaeSpawns = new ArrayList<Hypha>();
	static ArrayList<Hypha> newHyphaeTips = new ArrayList<Hypha>();

	boolean updateContext = true;
	boolean growing    = false;
	boolean drawPoints = true;

	public void setup()
	{
		size(1024,768,P3D);
		//frameRate(4);

		context = new SimpleOpenNI(this);
		
		if(context.isInit() == false)
		{
			println("Can't init SimpleOpenNI, maybe the camera is not connected!"); 
			exit();
			return;  
		}

		// disable mirror
		context.setMirror(false);

		// enable depthMap generation 
		context.enableDepth();

		depthWidth  = context.depthWidth();
		depthHeight = context.depthHeight();

		substrateMap = new int[depthWidth][depthHeight];

		stroke(255,255,255);
		smooth();
		//perspective(radians(45), width/height, 10,150000);
		
		cp5 = new ControlP5(this); 
		depthSlider = cp5.addRange("rangeController")
         // disable broadcasting since setRange and setRangeValues will trigger an event
         .setBroadcast(false) 
         .setPosition(50,50)
         .setSize(400,20)
         .setHandleSize(20)
         .setRange(0,7000)
         .setRangeValues(Settings.zThresholdMin, Settings.zThresholdMax)
         // after the initialization we turn broadcast back on again
         .setBroadcast(true)
         .setColorForeground(color(255,40))
         .setColorBackground(color(255,40));
		
		pointsToggle = cp5.addToggle("pointsToggle")
	     .setPosition(50,100)
	     .setSize(50,20)
	     .setValue(true)
	     .setMode(ControlP5.SWITCH)
	     .setColorForeground(color(255,40))
         .setColorBackground(color(255,40));
		
		growingToggle = cp5.addToggle("growingToggle")
	     .setPosition(150,100)
	     .setSize(50,20)
	     .setValue(false)
	     .setMode(ControlP5.SWITCH)
	     .setColorForeground(color(255,40))
         .setColorBackground(color(255,40));
		
		zoomSlider = cp5.addSlider("zoomSlider")
	     .setPosition(50, 150)
	     .setSize(400,20)
	     .setRange((float) 0.2, 1)
	     .setValue((float) 0.3)
	     .setColorForeground(color(255,40))
         .setColorBackground(color(255,40));
		
		stepSlider = cp5.addSlider("stepSlider")
	     .setPosition(50, 200)
	     .setSize(400,20)
	     .setRange(1,10)
	     .setValue(3)
	     .setNumberOfTickMarks(10)
	     .setColorForeground(color(255,40))
         .setColorBackground(color(255,40));
	}

	public void draw()
	{
		// update the cam
		if (updateContext) context.update();

		background(0,0,0);

		pushMatrix();
		translate(width/2, height/2, 0);
		rotateX(rotX);
		rotateY(rotY);
		scale(zoomF);

		depthMap = context.depthMap();

		translate(0,0,-1000);  // set the rotation center of the scene 1000 in front of the camera

		stroke(255);

		realWorldMap = context.depthMapRealWorld();

		// draw point cloud
		if (drawPoints)
		{
			int index;
			PVector realWorldPoint;
			beginShape(POINTS);
			for(int y = 0; y < depthHeight; y += Settings.steps)
			{
				for(int x = 0; x < depthWidth; x += Settings.steps)
				{
					index = xyToIndex(x, y);

					if(depthMap[index] > Settings.zThresholdMin && depthMap[index] < Settings.zThresholdMax)
					{
						realWorldPoint = realWorldMap[index];
						vertex(realWorldPoint.x, realWorldPoint.y, realWorldPoint.z);
					}
				}
			} 
			endShape();
		}

		if (!updateContext) // context doesn't update anymore --> we can compute things
		{
			// EXPAND
			if (hyphaeTips.size() != 0 && growing)
			{
				for (Hypha hypha : hyphaeTips)
				{
					Hypha newHypha = hypha.grow();
					if (newHypha != null)
					{
						newHyphaeTips.add(newHypha);
					}
				}
				
				hyphaeTips = newHyphaeTips;
				newHyphaeTips = new ArrayList<Hypha>(); // reset
			}

			// DRAW
			for (Hypha spawn : hyphaeSpawns)
			{
				Hypha currChild = spawn;
				PVector origin = realWorldMap[xyToIndex(Math.round(spawn.x), Math.round(spawn.y))];
				
				noFill();
				strokeWeight(2);
				beginShape();
				curveVertex(origin.x, origin.y, origin.z);

				while (currChild.child != null)
				{
					PVector end = realWorldMap[xyToIndex(Math.round(currChild.x + currChild.v.x), Math.round(currChild.y + currChild.v.y))];
					curveVertex(end.x, end.y, end.z);
					currChild = currChild.child;
				}
				endShape();
			}
		}
		popMatrix();
	}

	public static int xyToIndex(int x, int y)
	{
		return x + y * depthWidth;
	}

	public static PVector indexToPoint(int index)
	{
		int x = index % depthWidth;
		int y = (index - x) / depthWidth;
		return new PVector(x, y);
	}
	
	public void controlEvent(ControlEvent theControlEvent)
	{
		if (theControlEvent.isFrom("rangeController"))
		{
			Settings.zThresholdMin = (int) (theControlEvent.getController().getArrayValue(0));
			Settings.zThresholdMax = (int) (theControlEvent.getController().getArrayValue(1));
		}
	}
	
	public void pointsToggle(boolean flag)
	{
		drawPoints = flag;
	}
	
	public void growingToggle(boolean flag)
	{
		growing = flag;
	}
	
	public void zoomSlider(float zoom)
	{
		zoomF = zoom;
	}
	
	public void stepSlider(int steps)
	{
		Settings.steps = steps;
	}

	public void keyPressed()
	{
		switch(keyCode)
		{
		case ENTER:
			if (updateContext)
			{
				growing = true;
				cp5.getController("growingToggle").setValue(1);
				drawPoints = false;
				cp5.getController("pointsToggle").setValue(0);
				hyphaeSpawns = new ArrayList<Hypha>();

				// populate substrateMap
				// first populate corner points
				substrateMap[0][0]                            = abs(depthMap[xyToIndex(1, 0)] + depthMap[xyToIndex(0, 1)] - 2 * depthMap[xyToIndex(0, 0)]);
				substrateMap[depthWidth - 1][0]               = abs(depthMap[xyToIndex(depthWidth - 2, 0)]  + depthMap[xyToIndex(depthWidth - 1, 1)]  - 2 * depthMap[xyToIndex(depthWidth  - 1, 0)]);
				substrateMap[0][depthHeight - 1]              = abs(depthMap[xyToIndex(1, depthHeight - 1)] + depthMap[xyToIndex(0, depthHeight - 2)] - 2 * depthMap[xyToIndex(0, depthHeight - 1)]);
				substrateMap[depthWidth - 1][depthHeight - 1] = abs(depthMap[xyToIndex(depthWidth - 2, depthHeight - 1)] + depthMap[xyToIndex(depthWidth - 1, depthHeight - 2)] - 2 * depthMap[xyToIndex(depthWidth - 1, depthHeight - 1)]);

				// then populate other edge points
				// start left to right (top and bottom simultaneously)
				for (int x = 1; x < depthWidth - 1; x++)
				{
					substrateMap[x][0]               = abs(depthMap[xyToIndex(x + 1, 0)]               + depthMap[xyToIndex(x - 1, 0)]               + depthMap[xyToIndex(x, 1)]               - 3 * depthMap[xyToIndex(x, 0)]);
					substrateMap[x][depthHeight - 1] = abs(depthMap[xyToIndex(x + 1, depthHeight - 1)] + depthMap[xyToIndex(x - 1, depthHeight - 1)] + depthMap[xyToIndex(x, depthHeight - 2)] - 3 * depthMap[xyToIndex(x, depthHeight - 1)]);
				}

				// start top to bottom (left and right simultaneously)
				for (int y = 1; y < depthHeight - 1; y++)
				{
					substrateMap[0][y]              = abs(depthMap[xyToIndex(0, y - 1)]              + depthMap[xyToIndex(0, y + 1)]              + depthMap[xyToIndex(1, y)]              - 3 * depthMap[xyToIndex(0, y)]);
					substrateMap[depthWidth - 1][y] = abs(depthMap[xyToIndex(depthWidth - 1, y - 1)] + depthMap[xyToIndex(depthWidth - 1, y + 1)] + depthMap[xyToIndex(depthWidth - 2, y)] - 3 * depthMap[xyToIndex(depthWidth - 1, y)]);
				}

				// populate all inner points (the remaining ones)
				for (int x = 1; x < depthWidth - 2; x++)
				{
					for (int y = 1; y < depthHeight - 2; y++)
					{
						substrateMap[x][y] = max(1, abs(depthMap[xyToIndex(x - 1, y)] 
						  + depthMap[xyToIndex(x + 1, y)]
						  + depthMap[xyToIndex(x, y - 1)]
						  + depthMap[xyToIndex(x, y + 1)]
					  - 4 * depthMap[xyToIndex(x, y)]));
					}
				}
				
				for (int x = depthWidth/20; x < depthWidth; x += depthWidth/20)
				{
					for (int y = depthHeight/20; y < depthHeight; y += depthHeight/20)
					{
						if (depthMap[xyToIndex(x, y)] > Settings.zThresholdMin && depthMap[xyToIndex(x, y)] < Settings.zThresholdMax)
						{
							hyphaeSpawns.add(new Hypha(x, y, PVector.random2D(), 0));
						}
					}
				}
				hyphaeTips = hyphaeSpawns;
			}
			else
			{
				drawPoints = true;
				cp5.getController("pointsToggle").setValue(1);
				cp5.getController("growingToggle").setValue(0);
			}
			updateContext = !updateContext;
			break;
		case 'S':
			int counter = 0;
			PrintWriter output = createWriter("mycelium_"+System.currentTimeMillis()+".svg");
			//output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			//output.println("<svg xmlns=\"http://www.w3.org/2000/svg\">");
			for (Hypha spawn : hyphaeSpawns)
			{
				Hypha currChild = spawn;
				PVector origin = realWorldMap[xyToIndex(Math.round(spawn.x), Math.round(spawn.y))];
				
				output.print("<path id=\"i"+(counter++)+"\" d=\"M"+(origin.x + 500)+","+(origin.y - 500)*-1);

				while (currChild.child != null)
				{
					PVector end = realWorldMap[xyToIndex(Math.round(currChild.x + currChild.v.x), Math.round(currChild.y + currChild.v.y))];
					output.print(" L"+(end.x + 500)+","+(end.y - 500)*-1);
					currChild = currChild.child;
				}
				output.println("\" />"); // end of path tag
			}
			//output.println("</svg>");
			output.flush();
			output.close();
			break;
		}
		
	}

	public void mouseDragged() 
	{
		rotY += radians((pmouseX - mouseX) / 5);
		rotX += radians((pmouseY - mouseY) / 5);
	}

}
