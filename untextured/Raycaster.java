import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.awt.image.DataBufferInt;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;

public class Raycaster implements Runnable {
	public final static double TAU = Math.PI * 2;

	private final int VIRTUAL_WIDTH = 320, VIRTUAL_HEIGHT = 200;
	private final int WIDTH = 1280, HEIGHT = 720;	
	
	private BufferedImage bitmap;
	private int[] framebuffer;
	private Canvas canvas;
	private JFrame frame;
	private boolean running;
	private int fps;

	private Map map;	

	private double playerSpeed = 5f;
	private double playerX = 9;
	private double playerY = 9;
	private double playerFov = Math.toRadians(66.6);
	private double playerDirection = Math.toRadians(90);

	private boolean leftPressed, rightPressed, upPressed, downPressed;

	public static void main(String[] args) {
		new Raycaster();
	}

	public Raycaster() {
		initAssets();
		initUI();
		initBuffer();		

		start();
	}

	private void initUI() {
		// Use graphics hardware acceleration		
		System.setProperty("sun.java2d.opengl", "True");

		frame = new JFrame("Untextured raycasting demo. FPS: ~");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setIgnoreRepaint(true);
		frame.setResizable(true);		

		canvas = new Canvas();
		canvas.setIgnoreRepaint(true);
		canvas.setBackground(Color.BLACK);
		canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));		

		frame.add(canvas);
		frame.pack();

		canvas.addKeyListener(new KeyAdapter() {			
			@Override
			public void keyPressed(KeyEvent e) {				
				switch (e.getKeyCode()) {					
					case KeyEvent.VK_UP: upPressed = true; break;
					case KeyEvent.VK_LEFT: leftPressed = true; break;
					case KeyEvent.VK_DOWN: downPressed = true; break;
					case KeyEvent.VK_RIGHT: rightPressed = true; break;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_UP: upPressed = false; break;
					case KeyEvent.VK_LEFT: leftPressed = false; break;
					case KeyEvent.VK_DOWN: downPressed = false; break;
					case KeyEvent.VK_RIGHT: rightPressed = false; break;
				}
			}
		});

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);		

		canvas.createBufferStrategy(3);

		frame.requestFocus();
	}

	private void initAssets() {
		map = new Map("assets/map.txt");
	}

	private void start() {
		if (!running) {
			running = true;
			new Thread(this).start();
		}
	}

	private void initBuffer() {
		bitmap = new BufferedImage(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, 
			BufferedImage.TYPE_INT_RGB);

		framebuffer = ((DataBufferInt)bitmap.getRaster().getDataBuffer()).getData();		
	}

	@Override
	public void run() {
		long lastTime = System.nanoTime();
		long secondTime = System.currentTimeMillis() + 1000;

		int frames = 0;
		double delta = 0;

		BufferStrategy buffer = canvas.getBufferStrategy();

		while (running) {					
			long currentTime = System.nanoTime();
			long timeDifference = currentTime - lastTime;
			lastTime = currentTime;

			delta = timeDifference * 1e-9; // in seconds
					
			update(delta);

			do {					
				do {
					frames++;
					Graphics2D g = (Graphics2D) buffer.getDrawGraphics();

					render(g);
					g.dispose();
				} while (buffer.contentsRestored());

				buffer.show();
			} while (buffer.contentsLost());

			if (System.currentTimeMillis() >= secondTime) {
				fps = frames;
				frames = 0;

				frame.setTitle("Untextured raycasting demo. FPS: " + fps);

				secondTime = System.currentTimeMillis() + 1000;
			}
		}
	}

	private void update(double dt) {
		if (leftPressed)
			playerDirection = (playerDirection + (dt * 1.3) + TAU) % TAU;
		else if (rightPressed)
			playerDirection = (playerDirection - (dt * 1.3) + TAU) % TAU;
		
		if (upPressed) {
			double nextX = playerX + (Math.cos(playerDirection) * playerSpeed * dt);
			double nextY = playerY - (Math.sin(playerDirection) * playerSpeed * dt);

			if (map.get(nextX, nextY) == 0) {
				playerX = nextX;
				playerY = nextY;
			}
		}
		else if (downPressed) {
			double nextX = playerX - (Math.cos(playerDirection) * playerSpeed * dt);
			double nextY = playerY + (Math.sin(playerDirection) * playerSpeed * dt);			

			if (map.get(nextX, nextY) == 0) {
				playerX = nextX;
				playerY = nextY;
			}
		}
	}

	private void drawWalls() {
		double distToProjection = (VIRTUAL_WIDTH * 0.5) / Math.tan(0.5 * playerFov);

		castRays:
		for (int col = 0; col < VIRTUAL_WIDTH; ++col) {
			double alpha = playerDirection + (playerFov / 2) - 
				(playerFov * ((double)col / (double)(VIRTUAL_WIDTH - 1)));
			
			double rayDirX = Math.cos(alpha);
			double rayDirY = -Math.sin(alpha);

			int mapX = (int)playerX;
			int mapY = (int)playerY;

			double sideDistX;
			double sideDistY;

			double deltaDistX = Math.abs(1.0 / rayDirX);
			double deltaDistY = Math.abs(1.0 / rayDirY);

			int stepX = (int)Math.signum(rayDirX);
			int stepY = (int)Math.signum(rayDirY);

			if (rayDirX < 0) {
				sideDistX = (playerX - mapX) * deltaDistX;
			} else {
				sideDistX = (mapX + 1 - playerX) * deltaDistX;
			}

			if (rayDirY < 0) {
				sideDistY = (playerY - mapY) * deltaDistY;
			} else {
				sideDistY = (mapY + 1 - playerY) * deltaDistY;
			}

			boolean hit = false;
			int side = 0;

			while (!hit) {
				if (sideDistX < sideDistY) {
					sideDistX += deltaDistX;
					mapX += stepX;
					side = 0;
				} else {
					sideDistY += deltaDistY;
					mapY += stepY;
					side = 1;
				}
				
				int tile = map.get(mapX, mapY);
				if (tile == -1) continue castRays;
				if (tile > 0) hit = true;
			}

			double d;
			double p;

			if (side == 0) 	d = (mapX - playerX + (1 - stepX) / 2) / rayDirX;
			else			d = (mapY - playerY + (1 - stepY) / 2) / rayDirY;

			p = d * Math.cos(alpha - playerDirection);

			int sliceHeight = (int)(distToProjection / p);			

			int drawBegin = (VIRTUAL_HEIGHT / 2) - (sliceHeight / 2);
			int drawEnd = (VIRTUAL_HEIGHT / 2) + (sliceHeight / 2);

			drawBegin = Math.max(0, drawBegin);
			drawEnd = Math.min(VIRTUAL_HEIGHT, drawEnd);

			int tile = map.get(mapX, mapY);
			Color color = new Color(255, 255, 255);

			switch (tile) {
				case Map.R: color = new Color(150, 0, 0);
			}

			if (side == 1)	color = color.darker();
		
			int c = color.getRGB();

			for (int y = drawBegin; y < drawEnd; ++y) {
				framebuffer[y * VIRTUAL_WIDTH + col] = c;
			}
		}
	}

	private void drawFloorAndCeiling() {
		int c = 255;

		for (int r = 0; r < VIRTUAL_HEIGHT/2; ++r) {
			int color = new Color(c, 125, 225).getRGB();

			for (int col = 0; col < VIRTUAL_WIDTH; ++col) {
				framebuffer[VIRTUAL_WIDTH * r + col] = color;
			}

			c--;
		}

		c = 22;
		for (int r = VIRTUAL_HEIGHT / 2; r < VIRTUAL_HEIGHT; ++r) {
			int color = new Color(150 - c, 150 - c, 150 - c).getRGB();

			for (int col = 0; col < VIRTUAL_WIDTH; ++col) {
				framebuffer[VIRTUAL_WIDTH *r + col] = color;
			}

			c++;
		}
	}

	private void drawMinimap() {
		int white = new Color(255, 255, 255).getRGB();
		int black = new Color(0, 0, 0).getRGB();
		int red = new Color(255, 0, 0).getRGB();

		for (int r = 0; r < map.getHeight(); ++r) {
			for (int c = 0; c < map.getWidth(); ++c) {				
				switch (map.get(c, r)) {					
					case 0: framebuffer[r * VIRTUAL_WIDTH + c] = black; break;
					default: framebuffer[r * VIRTUAL_WIDTH + c] = white; break;
				}
			}
		}

		framebuffer[(int)playerY * VIRTUAL_WIDTH + (int)playerX] = red;
	}

	private void stop() {
		running = false;
	}

	private void render(Graphics2D g) {
		drawFloorAndCeiling();
		drawWalls();
		drawMinimap();

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
		
		int windowWidth = canvas.getWidth();
		int windowHeight = canvas.getHeight();

		float scaleWidth = (float)windowWidth / VIRTUAL_WIDTH;
		float scaleHeight = (float)windowHeight / VIRTUAL_HEIGHT;

		if (scaleWidth < scaleHeight) {
			scaleHeight = scaleWidth;
		} else {
			scaleWidth = scaleHeight;
		}		

		float realWidth = VIRTUAL_WIDTH * scaleWidth;
		float realHeight = VIRTUAL_HEIGHT * scaleHeight;

		int vpX = (int)((windowWidth - realWidth) * 0.5f);
		int vpY = (int)((windowHeight - realHeight) * 0.5f);

		g.translate(vpX, vpY);
		g.scale(scaleWidth, scaleHeight);
		g.drawImage(bitmap, 0, 0, null);
	}
}