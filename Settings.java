package mycelium_3d;

public class Settings
{
	/**
	 * <p>doesn't display any points with a z-value less than zThresholdMin</p>
	 * <p>also hyphae are only spawned at positions whose z-values are bigger than zThresholdMin</p>
	 */
	public static int zThresholdMin = 0;
	/**
	 * <p>doesn't display any points with a z-value greater than zThresholdMax</p>
	 * <p>also hyphae are only spawned at positions whose z-values are less than zThresholdMax</p>
	 */
	public static int zThresholdMax = 2000;
	/**
	 * we don't want to draw every single point recorded by the camera, so only draw every ... point
	 */
	public static int steps = 3;  // to speed up the drawing, only draw every ... point
	
	// Hypha settings
	/**
	 * when extending (normal growth) consider only points within {@link #extensionAngle} degrees to the left and right
	 */
	public static float extensionAngle = (float) Math.toRadians(35);
	/**
	 * when branching (spawning a new hypha) consider only points that are either {@link #branchingAngle} degrees left or right
	 * and with a standard deviation of {@link #branchingAngleDeviation}
	 */
	public static float branchingAngle = (float) Math.toRadians(56);
	/**
	 * standard deviation of {@link #branchingAngle} when branching
	 */
	//public static float branchingAngleDeviation = (float) Math.toRadians(17);
	/**
	 * minimum value the target point in {@link MainApp.substrateMap} should have - 
	 * also raises the possibility a branch can occur if premises are met
	 */
	public static float branchingThreshold    = 10;
	/**
	 * length of one entity of a hypha (gets altered randomly by {@link #lengthDeviationFactor})
	 */
	public static float substrateLengthFactor = 2;
	/**
	 * length of one entity of a hypha gets multiplied with a random value within ±(1+{@link #lengthDeviationFactor})
	 */
	public static float lengthDeviationFactor    = (float) .3;
	/**
	 * when a hypha extends to a certain point, then the value at its position of {@link #substrateMap}
	 * will get multiplied with this value (which alters the likeliness of other hyphae extending there)
	 */
	public static float substrateReductionFactor = (float) .6;
	/**
	 * defines after how many rounds of extending a hypha can branch again
	 */
	public static int   lastBranchThreshold = 5;
	/**
	 * number of directions to consider when choosing a new direction when extending
	 */
	public static int   numOfDirections = 6;
	/**
	 * when extending only points whose z-value-difference to the current one is less than {@link #maxDeltaZ} will be considered
	 */
	public static int   maxDeltaZ = 75;
}
