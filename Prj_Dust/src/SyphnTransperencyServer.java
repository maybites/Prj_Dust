import java.applet.Applet;

import javax.media.opengl.GL;

import javax.swing.JFrame;

import jsyphon.JSyphonServer;

import codeanticode.glgraphics.GLConstants;

import codeanticode.glgraphics.GLGraphicsOffScreen;

import codeanticode.glgraphics.GLTexture;

import processing.core.PApplet;

@SuppressWarnings("serial")
public class SyphnTransperencyServer extends PApplet {

	private int w, h;

	private JSyphonServer syphon;

	private GLGraphicsOffScreen canvas;

	private GLTexture tex;

	public SyphnTransperencyServer(int w, int h) {

		this.w = w;

		this.h = h;

	}

	public void setup() {

		size(w, h, GLConstants.GLGRAPHICS);

		frameRate(60);

		canvas = new GLGraphicsOffScreen(this, w, h);

		tex = new GLTexture(this);

		initSyphon();

	}

	public void initSyphon() {

		if (syphon != null) {

			syphon.stop();

		}

		syphon = new JSyphonServer();

		syphon.initWithName("OffscreenTest");

	}

	public void draw() {

		background(255, 0, 0);

		canvas.beginDraw();

		canvas.clear(0, 0, 0, 0);

		canvas.fill(0, 255, 255, random(50, 200));

		for (int i = 0; i < 100; i++) {

			canvas.ellipse(random(0, canvas.width), random(0, canvas.height),
					random(10, 50), random(10, 50));

		}

		canvas.endDraw();

		tex = canvas.getTexture();

		// image(tex,0,0);

		syphon.publishFrameTexture(tex.getTextureID(), tex.getTextureTarget(),
				0, 0, tex.width, tex.height, tex.width, tex.height, false);

	}

	public static void main(String[] args) {

		JFrame frame = new JFrame();

		Applet app = new SyphnTransperencyServer(1280, 720);

		frame.add(app);

		frame.setBounds(0, 0, 1280, 720);

		frame.setUndecorated(true);

		frame.setVisible(true);

		app.init();

	}

}
