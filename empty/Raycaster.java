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

		for (int i = 0; i < framebuffer.length; ++i) {
			framebuffer[i] = 0xFFFFFFFF;
		}
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

	/* reads user input and updates the players position */
	private void update(double dt) {
		/* fill in this method */
	}

	/* raycasts and draws appropriate walls */
	private void drawWalls() {
		/* fill in this method */
	}

	/* draws the floor and ceiling */
	private void drawFloorAndCeiling() {
		/* fill in this method */
	}

	/* draws the minimap in the top left corner */
	private void drawMinimap() {
		/* fill in this method */
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