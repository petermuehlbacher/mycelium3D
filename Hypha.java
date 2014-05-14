package mycelium_3d;

import java.util.ArrayList;
import java.util.Random;

import processing.core.PVector;

public class Hypha
{
	public float x;
	public float y;
	public PVector v;
	public Hypha child = null;
	public boolean isWithinMap;
	public int iterationsSinceLastBranch;
	public float depth;
	
	private float angle;

	/**
	 * Initiates a new Hypha instance
	 * @param parent needed to access Processing's methods
	 * @param x x-coordinate in the 2D data (not to be mistaken for 3D data!)
	 * @param y y-coordinate in the 2D data (not to be mistaken for 3D data!)
	 * @param v direction (and magnitude) this hypha is heading at
	 * @param iterationsSinceLastBranch how often this hypha already has extended since the last branch (needed to calculate probability for branching)
	 */
	public Hypha(float x, float y, PVector v, int iterationsSinceLastBranch)
	{
		this.x = x;
		this.y = y;
		this.v = v;

		this.iterationsSinceLastBranch = iterationsSinceLastBranch + 1;

		this.angle = v.heading();

		this.isWithinMap = isWithinMap(x, y, v.x, v.y);
		
		if (this.isWithinMap) this.depth = Math.abs(getMapValues(x, y).z - getMapValues(x + v.x, y + v.y).z); // only check depth when knowing that points are within map - otherwise IndexOutOfBounds inc.
	}

	public Hypha grow()
	{
		ArrayList<Hypha> spawnedHyphae = new ArrayList<Hypha>();

		float substrateAtEnd = MainApp.substrateMap[Math.round(x + v.x)][Math.round(y + v.y)];

		if (substrateAtEnd > 0)
		{
			PVector newV, tmp;
			float newLength = Settings.substrateLengthFactor /*/ substrateAtEnd */* randomWithin(1 - Settings.lengthDeviationFactor, 1 + Settings.lengthDeviationFactor);
			float newAngle = this.angle; // just give it any value, so java may be sure a value has been assigned

			float[] probabilityLookup = new float[Settings.numOfDirections];

			///////////////
			// BRANCHING //
			///////////////
			if (substrateAtEnd > Settings.branchingThreshold && iterationsSinceLastBranch > Settings.lastBranchThreshold)
			{
				if (Settings.branchingThreshold / substrateAtEnd < randomWithin(0,1))
				{
					newV = v.get(); // clone v
					newV.rotate(randomWithin(-Settings.branchingAngle /*- branchingAngleDeviation*/, Settings.branchingAngle /*+ branchingAngleDeviation*/));
					newV.setMag(Settings.substrateLengthFactor / substrateAtEnd * randomWithin(1 - Settings.lengthDeviationFactor, 1 + Settings.lengthDeviationFactor)); // calculate different length - we don't want every hypha to look the same

					Hypha newHypha = new Hypha(x + v.x, y + v.y, newV, 0);
					if (newHypha.isWithinMap && newHypha.depth < Settings.maxDeltaZ)
					{
						MainApp.hyphaeSpawns.add(newHypha);
						MainApp.newHyphaeTips.add(newHypha);
					}
					MainApp.substrateMap[Math.round(x + v.x + newV.x)][Math.round(y + v.y + newV.y)] *= Settings.substrateReductionFactor;
					this.iterationsSinceLastBranch = 0;
				}
			}

			///////////////
			// EXTENDING //
			///////////////
			// CALCULATING RANDOM (THOUGH WEIGHTED) NEW DIRECTION
			// start with cloning the current direction, setting length and angle (start "left" and work right in numOfDirections steps)
			tmp = v.get();
			tmp.setMag(newLength);
			tmp.rotate(-Settings.extensionAngle);

			probabilityLookup[0] = 0;
			for (int i = 1; i < Settings.numOfDirections; i++)
			{
				tmp.rotate(2 * Settings.extensionAngle / Settings.numOfDirections); // rotate it so that after numOfDirections steps it will point towards this.angle+extensionAngle 
																  // (we're starting at this.angle-extensionAngle)
				if (isWithinMap(x + v.x, y + v.y, tmp.x, tmp.y) &&// avoid these pesky 0 points (some points have a z value of 0, resulting in strange graphics when being drawn)
						Math.abs(getMapValues(x + v.x, y + v.y).z - getMapValues(x + v.x + tmp.x, y + v.y + tmp.y).z) < Settings.maxDeltaZ)
				{
					probabilityLookup[i] = probabilityLookup[i-1] + 
						MainApp.substrateMap[Math.round(x + v.x + tmp.x)][Math.round(y + v.y + tmp.y)]; // add an according value to the probabilityLookup table 
																										// the greater the substrate value, the more will be added
				}
				else
				{
					probabilityLookup[i] = probabilityLookup[i-1]; // give it a probability of 0 to be selected
				}
			}

			float r = randomWithin(0, probabilityLookup[Settings.numOfDirections-1]); // now take a random value and let's look where it is located in the probabilityLookup table
			
			for (int i = 1; i < Settings.numOfDirections; i++)
			{
				if (r > probabilityLookup[i-1] && r < probabilityLookup[i]) // if we got a "winner" let's recompute its angle
				{
					newAngle = this.angle - Settings.extensionAngle + i * (2 * Settings.extensionAngle / Settings.numOfDirections);
					break;
				}
			}

			// NOW REALLY EXTEND
			newV = v.get(); // clone v
			newV.rotate(- this.angle + newAngle); // - this.angle in order to start from 0° and then set new angle
			newV.setMag(newLength);

			Hypha newHypha = new Hypha(x + v.x, y + v.y, newV, this.iterationsSinceLastBranch);

			if (newHypha.isWithinMap && newHypha.depth < Settings.maxDeltaZ)
			{
				spawnedHyphae.add(newHypha);
				MainApp.substrateMap[Math.round(newHypha.x)][Math.round(newHypha.y)] *= Settings.substrateReductionFactor; // reduce value of point where the hypha has grown to
				this.child = newHypha;
				return newHypha;
			}
		}
		return null;
	}

	/**
	 * returns Kinect's 3D data of a point with the given coordinates (xth point from left, yth from top)
	 * @param x take the xth point from left (float value gets rounded)
	 * @param y take the yth point from top  (float value gets rounded)
	 * @return 3D data of the given point
	 */
	private PVector getMapValues(float x, float y)
	{
		return MainApp.realWorldMap[MainApp.xyToIndex(Math.round(x), Math.round(y))];
	}
	
	private boolean isWithinMap(float x, float y, float vx, float vy)
	{
		return (x > 1 && x + vx > 1 &&
				y > 1 && y + vy > 1 &&
				x < MainApp.depthWidth  - 2 && x + vx < MainApp.depthWidth  - 2 &&
				y < MainApp.depthHeight - 2 && y + vy < MainApp.depthHeight - 2 &&
				getMapValues(x, y).z != 0 &&
				getMapValues(x + vx, y + vy).z != 0);
	}
	
	private float randomWithin(float min, float max)
	{
		Random random = new Random();
		return random.nextFloat() * (max - min) + min;
	}
}