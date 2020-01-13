import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;

public class Map {
	protected int []grid;
	protected int width;
	protected int height;

	// Empty space
	public static final int EMPTY = 0;

	// White walls
	public static final int R = 1;

	public Map(String filename) {
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			ArrayList<Integer> tempGrid = new ArrayList<Integer>();			
			String line;
			while ((line = br.readLine()) != null) {
				String []cells = line.split("");
				this.width = Math.max(this.width, cells.length);

				for (String cell : cells) {
					if ("R".equals(cell)) {
						tempGrid.add(R);
					} else if (".".equals(cell)) {
						tempGrid.add(EMPTY);
					}
				}
				this.height++;
			}

			grid = tempGrid.stream()
				.mapToInt(Integer::intValue)
				.toArray();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int get(double x, double y) {
		int xi = (int)x;
		int yi = (int)y;

		if (x < 0 || x > this.width - 1 || y < 0 || y > this.height - 1)
			return -1;
		
		return this.grid[yi * width + xi];
	}
}