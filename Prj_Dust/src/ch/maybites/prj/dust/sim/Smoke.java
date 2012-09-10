package ch.maybites.prj.dust.sim;

import processing.core.*;
import processing.opengl.*;
import processing.data.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

import codeanticode.syphon.*;

public class Smoke  extends PApplet{

	// smoke2 by Glen Murphy.
	// View the applet in use at http://bodytag.org/
	// Code has not been optimised, and will run fairly slowly.

	int WIDTH = 600;
	int HEIGHT = 1200;

	int RES = 1;
	int PENSIZE = 30;

	int lwidth = WIDTH/RES;
	int lheight = HEIGHT/RES;
	int PNUM = 200000;
	vsquare[][] v = new vsquare[lwidth+1][lheight+1];
	vbuffer[][] vbuf = new vbuffer[lwidth+1][lheight+1];

	particle[] p = new particle[PNUM];
	int pcount = 0;
	int mouseXvel = 0;
	int mouseYvel = 0;

	int randomGust = 0;
	int randomGustMax;
	float randomGustX;
	float randomGustY;
	float randomGustSize;
	float randomGustXvel;
	float randomGustYvel;

	SyphonServer server;
	SyphonClient client;

	PImage img;
	PGraphics canvas;


	class particle {
		float x;
		float y;
		float xvel;
		float yvel;
		float temp;
		int pos;

		particle(float xIn, float yIn) {
			x = xIn;
			y = yIn;
		}

		void reposition() {
			x = WIDTH/2+random(-20,20);
			y = random(HEIGHT-10,HEIGHT);

			xvel = random(-1,1);
			yvel = random(-1,1);
		}

		void updatepos() {
			int vi = (int)(x/RES);
			int vu = (int)(y/RES);

			if(vi > 0 && vi < lwidth && vu > 0 && vu < lheight) {
				v[vi][vu].addcolour(2);

				float ax = (x%RES)/RES;
				float ay = (y%RES)/RES;

				xvel += (1-ax)*v[vi][vu].xvel*0.05;
				yvel += (1-ay)*v[vi][vu].yvel*0.05;

				xvel += ax*v[vi+1][vu].xvel*0.05;
				yvel += ax*v[vi+1][vu].yvel*0.05;

				xvel += ay*v[vi][vu+1].xvel*0.05;
				yvel += ay*v[vi][vu+1].yvel*0.05;

				v[vi][vu].yvel -= (1-ay)*0.003;
				v[vi+1][vu].yvel -= ax*0.003;
				//v[vi][vu+1].yvel -= ay*0.003;

				if(v[vi][vu].yvel < 0) v[vi][vu].yvel *= 1.00025;

				x += xvel;
				y += yvel;
			}
			else {
				reposition();
			}
			if(random(0,400) < 1) reposition();

			xvel *= 0.6;
			yvel *= 0.6;
		}
	}

	class vbuffer
	{
		int x;
		int y;
		float xvel;
		float yvel;
		float pressurex = 0;
		float pressurey = 0;
		float pressure = 0;

		vbuffer(int xIn,int yIn) {
			x = xIn;
			y = yIn;
			pressurex = 0;
			pressurey = 0;
		}

		void updatebuf(int i, int u) {
			if(i>0 && i<lwidth && u>0 && u<lheight) {
				pressurex = (v[i-1][u-1].xvel*0.5f + v[i-1][u].xvel + v[i-1][u+1].xvel*0.5f - v[i+1][u-1].xvel*0.5f - v[i+1][u].xvel - v[i+1][u+1].xvel*0.5f);
				pressurey = (v[i-1][u-1].yvel*0.5f + v[i][u-1].yvel + v[i+1][u-1].yvel*0.5f - v[i-1][u+1].yvel*0.5f - v[i][u+1].yvel - v[i+1][u+1].yvel*0.5f);
				pressure = (pressurex + pressurey)*0.25f;
			}
		}
	}

	class vsquare {
		int x;
		int y;
		float xvel;
		float yvel;
		float col;

		vsquare(int xIn,int yIn) {
			x = xIn;
			y = yIn;
		}

		void addbuffer(int i, int u) {
			if(i>0 && i<lwidth && u>0 && u<lheight) {
				xvel += (vbuf[i-1][u-1].pressure*0.5
						+vbuf[i-1][u].pressure
						+vbuf[i-1][u+1].pressure*0.5
						-vbuf[i+1][u-1].pressure*0.5
						-vbuf[i+1][u].pressure
						-vbuf[i+1][u+1].pressure*0.5
						)*0.49;
				yvel += (vbuf[i-1][u-1].pressure*0.5
						+vbuf[i][u-1].pressure
						+vbuf[i+1][u-1].pressure*0.5
						-vbuf[i-1][u+1].pressure*0.5
						-vbuf[i][u+1].pressure
						-vbuf[i+1][u+1].pressure*0.5
						)*0.49;
			}
		}

		void updatevels(int mvelX, int mvelY) {
			//stroke(#000000);
			//line(x,y,x+xvel,y+yvel);
			float adj;
			float opp;
			float dist;
			float mod;

			if(mousePressed) {
				adj = x - mouseX;
				opp = y - mouseY;
				dist = sqrt(opp*opp + adj*adj);
				if(dist < PENSIZE) {
					if(dist < 4) dist = PENSIZE;
					mod = PENSIZE/dist;
					xvel += mvelX*mod;
					yvel += mvelY*mod;
				}
			}
			if(randomGust > 0) {
				adj = x - randomGustX;
				opp = y - randomGustY;
				dist = sqrt(opp*opp + adj*adj);
				if(dist < randomGustSize) {
					if(dist < RES*2) dist = randomGustSize;
					mod = randomGustSize/dist;
					xvel += (randomGustMax-randomGust)*randomGustXvel*mod;
					yvel += (randomGustMax-randomGust)*randomGustYvel*mod;
				}
			}
			xvel *= 0.99f;
			yvel *= 0.98f;
		}

		void addcolour(int amt) {
			col += amt;
			if(col > 196) col = 196;
		}

		int display(int i, int u) {
			float tcol = 0;
			if(i>0 && i<lwidth-1 && u>0 && u<lheight-1) {
				//tcol = 255-(int)((v[i-1][u].col + v[i-1][u-1].col  + v[i][u-1].col + v[i][u].col*2)*0.2);
				//setPixel(x,y,color(tcol, tcol, tcol));

				//tcol = 255-(int)((v[i][u-1].col + v[i+1][u-1].col  + v[i+1][u].col + v[i][u].col*2)*0.2);
				//setPixel(x+1,y, color(tcol, tcol, tcol));

				//tcol = 255-(int)((v[i][u+1].col + v[i+1][u+1].col  + v[i+1][u].col + v[i][u].col*2)*0.2);
				//setPixel(x+1,y+1, color(tcol, tcol, tcol));

				//tcol = 255-(int)((v[i-1][u].col + v[i-1][u+1].col  + v[i][u+1].col + v[i][u].col*2)*0.2);
				//setPixel(x,y+1, color(tcol, tcol, tcol));

				tcol = (+ v[i][u+1].col
						+ v[i+1][u].col
						+ v[i+1][u+1].col*0.5f
						)*0.3f;
				tcol = (int)(tcol+col*0.5);
			}
			//else {
			//tcol = (int)col;
			//}
			//canvas.fill(255, 255-tcol, 255-tcol, 255-tcol);
			//canvas.rect(x,y,RES,RES); // uses too much time to draw!!!!
			col = 0;
			return color(255-tcol, 255-tcol, 255-tcol);
		}
	}

	public void setup() {
		size(WIDTH,HEIGHT, P3D);
		canvas = createGraphics(WIDTH, HEIGHT, P3D);

		textFont(createFont("faucet", 24));

		background(100);
		noStroke();
		for(int i = 0; i < PNUM; i++) {
			p[i] = new particle(random(WIDTH/2-20,WIDTH/2+20),random(HEIGHT-20,HEIGHT));
		}
		for(int i = 0; i <= lwidth; i++) {
			for(int u = 0; u <= lheight; u++) {
				v[i][u] = new vsquare(i*RES,u*RES);
				vbuf[i][u] = new vbuffer(i*RES,u*RES);
			}
		}

		// Create syhpon client to receive frames 
		// from running server with given name: 
		client = new SyphonClient(this, "of_Dust_Kin2Syp");

		// Create syhpon server to send frames out.
		server = new SyphonServer(this, "ProcessingSyphon");

	}

	public void draw() {
		 
		if (client.available()) {
			// The first time getImage() is called with 
			// a null argument, it will initialize the PImage
			// object with the correct size.
			img = client.getImage(img); // load the pixels array with the updated image info (slow)
			//canvas.image(img, 0, 0);
			//img = client.getImage(img, false); // does not load the pixels array (faster)
			//image(img, 0, 0);
		}

		background(0);
		fill(255);
		text("fps:" + this.frameRate, 10, 20);


		canvas.beginDraw();
		canvas.background(0);
		//canvas.lights();

		int axvel = mouseX-pmouseX;
		int ayvel = mouseY-pmouseY;

		mouseXvel = (axvel != mouseXvel) ? axvel : 0;
		mouseYvel = (ayvel != mouseYvel) ? ayvel : 0;

		if(randomGust <= 0) {
			if(random(0,10)<1) {
				randomGustMax = (int)random(5,12);
				randomGust = randomGustMax;
				randomGustX = random(0,WIDTH);
				randomGustY = random(0,HEIGHT-10);
				randomGustSize = random(0,50);
				if(randomGustX > WIDTH/2) randomGustXvel = random(-8,0);
				else randomGustXvel = random(0,8);
				randomGustYvel = random(-2,1);
			}
			randomGust--;
		}
		
		for(int i = 0; i < lwidth; i++) {
			for(int u = 0; u < lheight; u++) {
				vbuf[i][u].updatebuf(i,u);
				v[i][u].col = 0;
			}
		}
		
		for(int i = 0; i < PNUM-1; i++) {
			p[i].updatepos();
		}
		
		canvas.loadPixels();
		
		for(int i = 0; i < lwidth; i++) {
			for(int u = 0; u < lheight; u++) {
				v[i][u].addbuffer(i, u);
				v[i][u].updatevels(mouseXvel, mouseYvel);
				canvas.pixels[i+u*lwidth] = v[i][u].display(i, u);
			}
		}
		canvas.updatePixels();
		
		randomGust = 0;
				
		canvas.endDraw();
		
		//image(canvas, 0, 0);

		server.sendImage(canvas);

	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "ch.maybites.prj.dust.sim.Smoke" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}

}
