import java.util.ArrayList;
import java.util.TreeMap;


public class RegionData {
	public String 		regionName;
	public GeoRegion	region;
	public int			numRequiredTiles;
	public int			numExistingTiles;
	public ArrayList<Integer> requiredTiles = new ArrayList<Integer>();
	public ArrayList<Integer> existingTiles = new ArrayList<Integer>();

	public RegionData() {
		regionName = "unknown";
		region = new GeoRegion();
		numRequiredTiles = 0;
		numExistingTiles = 0;
	}
	public RegionData(String component[]) {
		numRequiredTiles = 0;
		numExistingTiles = 0;
		regionName = component[0];
		region = new GeoRegion();
		region.MakeUsingBoundingBox(Float.parseFloat(component[1]),Float.parseFloat(component[4]), Float.parseFloat(component[3]), Float.parseFloat(component[2]));
		requiredTiles = GeoTile.GetTileListForGeoRegion(region, null);
		numRequiredTiles = requiredTiles.size();
	}

}
