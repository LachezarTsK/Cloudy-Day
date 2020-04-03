import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;

public class Solution {

  public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in);
    int numberOfTowns = scanner.nextInt();
    int[] population_towns = new int[numberOfTowns];
    for (int i = 0; i < numberOfTowns; i++) {
      population_towns[i] = scanner.nextInt();
    }

    /** 
    * Key: Town location. 
    * Value: Town object. 
    */
    Map<Integer, Town> location_towns = new HashMap<Integer, Town>();
    for (int i = 0; i < numberOfTowns; i++) {
      int location = scanner.nextInt();
      if (!location_towns.containsKey(location)) {
        location_towns.put(location, new Town(location, (long) population_towns[i]));
      } else {
        location_towns.get(location).population += (long) population_towns[i];
      }
    }

    Town[] towns = location_towns.values().toArray(new Town[location_towns.values().size()]);
    Arrays.sort(towns);

    int numberOfClouds = scanner.nextInt();
    int[] location_cloud = new int[numberOfClouds];
    for (int i = 0; i < numberOfClouds; i++) {
      location_cloud[i] = scanner.nextInt();
    }

    Set<Integer> location_towns_withMoreThanOneCloud = new HashSet<Integer>();

    outerLoop_cloudsRange:
    for (int i = 0; i < numberOfClouds; i++) {
      int town_lastLocation = towns[towns.length - 1].location;
      int range = scanner.nextInt();
      int startCloud = location_cloud[i] - range < 0 ? 0 : location_cloud[i] - range;
      int endCloud = location_cloud[i] + range > town_lastLocation ? town_lastLocation : location_cloud[i] + range;
      
      /**
       * A binary search to find any town in range of the current cloud. If such town is found,
       * start searching from this point backwards and forwards for any other towns in range of
       * the current cloud.
       */
      int index = binarySearch(0, towns.length - 1, startCloud, endCloud, towns);
      if (index == -1) {
        continue;
      }

      int decrease = index;
      while (decrease >= 0 && towns[decrease].location >= startCloud) {
        if (towns[decrease].totalCloudsOverTown < 2) {
          towns[decrease].totalCloudsOverTown++;
          towns[decrease].cloudLocation = location_cloud[i];

          if (towns[decrease].totalCloudsOverTown == 2) {
            location_towns_withMoreThanOneCloud.add(towns[decrease].location);
            if (location_towns_withMoreThanOneCloud.size() == towns.length) {
              break outerLoop_cloudsRange;
            }
          }
        }

        decrease--;
      }

      int increase = index + 1;
      while (increase <= towns.length - 1 && towns[increase].location <= endCloud) {
        if (towns[increase].totalCloudsOverTown < 2) {
          towns[increase].totalCloudsOverTown++;
          towns[increase].cloudLocation = location_cloud[i];

          if (towns[increase].totalCloudsOverTown == 2) {
            location_towns_withMoreThanOneCloud.add(towns[increase].location);
            if (location_towns_withMoreThanOneCloud.size() == towns.length) {
              break outerLoop_cloudsRange;
            }
          }
        }

        increase++;
      }
    }
    scanner.close();

    long result = maximumPeople_inSunnyTowns(location_towns, location_towns_withMoreThanOneCloud);
    System.out.println(result);
  }

  /**
   * Finds the maximum people that could be in towns without clouds (sunny towns). 
   *
   * The value of the maximum people in sunny towns is formed as follows: 
   * 1. People in towns that do not have any clouds. 
   * 2. The maximum number of people in towns that could become sunny towns, if
   *    exactly one cloud is removed. Thus, the towns to be considered are those
   *    that have only one cloud.
   *
   * @return A long integer, representing maximum people that could be in sunny towns.
   */
  private static long maximumPeople_inSunnyTowns(
      Map<Integer, Town> location_towns, Set<Integer> location_towns_withMoreThanOneCloud) {

    if (location_towns_withMoreThanOneCloud.size() == location_towns.keySet().size()) {
      return 0;
    }

    long people_inTowns_withoutClouds = 0;
    long maxPeople_inTown_underOneCloud = 0;

    /**
     * Key: cloud location for a cloud that covers towns with one cloud.
     * Value: sum of people from all towns in range of this cloud. 
     *        This sum concerns only towns with one cloud.
     */
    Map<Integer, Long> cloud_maxCover = new HashMap<Integer, Long>();

    for (int location : location_towns.keySet()) {
      int totalCloudsOverTown = location_towns.get(location).totalCloudsOverTown;

      if (totalCloudsOverTown == 0) {
        people_inTowns_withoutClouds += location_towns.get(location).population;

      } else if (totalCloudsOverTown == 1) {
        int cloudLocation = location_towns.get(location).cloudLocation;

        if (!cloud_maxCover.containsKey(cloudLocation)) {
          cloud_maxCover.put(cloudLocation, location_towns.get(location).population);

        } else {
          long people = cloud_maxCover.get(cloudLocation) + location_towns.get(location).population;
          cloud_maxCover.put(cloudLocation, people);
        }

        if (maxPeople_inTown_underOneCloud < cloud_maxCover.get(cloudLocation)) {
          maxPeople_inTown_underOneCloud = cloud_maxCover.get(cloudLocation);
        }
      }
    }

    return people_inTowns_withoutClouds + maxPeople_inTown_underOneCloud;
  }

  /**
   * Searches for a town location that is in the range of the current cloud.
   *
   * @return If a town is found: the index of the town in array 'towns'.
   *         Otherwise: '-1'.
   */
  private static int binarySearch(
      int lowerIndex, int upperIndex, int startCloud, int endCloud, Town[] towns) {

    if (lowerIndex <= upperIndex) {
      int mid = lowerIndex + (upperIndex - lowerIndex) / 2;
      if (towns[mid].location >= startCloud && towns[mid].location <= endCloud) {
        return mid;
      }
      if (towns[mid].location > endCloud) {
        return binarySearch(lowerIndex, mid - 1, startCloud, endCloud, towns);
      }
      if (towns[mid].location < startCloud) {
        return binarySearch(mid + 1, upperIndex, startCloud, endCloud, towns);
      }
    }
    return -1;
  }

  /**
   * It is possible to have several towns at the one and the same location. 
   * In such cases, the class merges these towns into one town, combining their population.
   */
  static class Town implements Comparable<Town> {
    int location;
    long population;
    int totalCloudsOverTown;

    /**
     * Represents the location of the last found cloud over the current town. 
     * The information from this variable is applied when there is only one cloud over the town.
     * In such cases the location of the last found cloud over the town will be the location of 
     * the only cloud over this town.
     */
    int cloudLocation;

    public Town(int location, long population) {
      this.location = location;
      this.population = population;
    }

    /** 
    * Sort towns per increasing order of their locations. 
    */
    @Override
    public int compareTo(Town another) {
      return this.location - another.location;
    }
  }
}
