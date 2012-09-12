package ch.maybites.prj.dust.sim;

// smoke2 code by Glen Murphy, performance improvement by Martin Fršhlich
// View the applet in use at http://bodytag.org/

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

import javax.script.*;
import codeanticode.syphon.*;
import oscP5.*;
import netP5.*;

public class Smoke  extends PApplet{

	// final parameters
	final int PMODE_SETUP = 0;
	final int PMODE_WAITING = 1;
	final int PMODE_TRIGGER = 2;
	final int PMODE_SIMULATION = 3;
	final int PMODE_DISSOLVE = 4;

	final int UVX1 = 0;
	final int UVY1 = 1;
	final int UVX2 = 2;
	final int UVY2 = 3;

	int WIDTH = 400;
	int HEIGHT = 800;

	int MIN_AGE = 300; 
	int MAX_AGE = 450;
	
	int RES = 1;
	int PENSIZE = 30;
	
	int TRANSPARENCY = 40;
		
	int lwidth = WIDTH/RES;
	int lheight = HEIGHT/RES;
	int PNUM = 400000;
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

	boolean flagDrawSyphonCapture = false;
	boolean flagRefreshCapture = false;
	boolean flagSimSetup = false;
	
	Fader globalBlend = new Fader(0, 255);
	
	Player player;
	
	SyphonServer server;
	SyphonClient client;

	PImage syphonCapture;
	DustImage dust;
	float[] syphonUV = {0.2f, 0.0f, 0.8f, 1.0f}; //x1, y1, x2, y2 from upper left corner!!!
	float[] dustUV = {0.0f, 0.3f, 1.0f, 1.0f}; //x1, y1, x2, y2 from upper left corner!!!
	
	
	PGraphics canvas;

	// create OSC Server and Client
	OscP5 oscP5;
	NetAddress myRemoteLocation;
	
	gust myGust = new gust();

	public void setup() {		    
		size(WIDTH,HEIGHT, P3D);
		canvas = createGraphics(WIDTH, HEIGHT, P3D);

		textFont(createFont("faucet", 24));

		player = new Player();
		
		background(100);
		noStroke();

		// Create syhpon client to receive frames 
		// from running server with given name: 
		client = new SyphonClient(this, "of_Dust_Kin2Syp");

		// Create syhpon server to send frames out.
		server = new SyphonServer(this, "ProcessingSyphon");

		/* start oscP5, listening for incoming messages at port 12345 */
		oscP5 = new OscP5(this,12345);

		/* myRemoteLocation is a NetAddress. a NetAddress takes 2 parameters,
		 * an ip address and a port number. myRemoteLocation is used as parameter in
		 * oscP5.send() when sending osc packets to another computer, device, 
		 * application.
		 */
		myRemoteLocation = new NetAddress("127.0.0.1",54321);
		
		dust = new DustImage(WIDTH, HEIGHT,RGB);
	}

	public void draw() {
		player.update();
		globalBlend.update();
		
		if(flagRefreshCapture){
			captureShadow();
			flagRefreshCapture = false;
		}

		background(255);
		
		updateSimulation();

		canvas.beginDraw();
		canvas.background(0);
		drawSimulation();
		canvas.endDraw();

		//draw canvas on preview
		image(canvas, 0, 0);

		//send canvas to fassade
		server.sendImage(canvas);

		//Everything from here onwards is not displayed on the fassade
		
		myGust.draw(5, 2);
		
		fill(0);
		text("fps:" + this.frameRate, 10, 25);
	}

	void captureShadow(){
		if (client.available()) {
			// The first time getImage() is called with 
			// a null argument, it will initialize the PImage
			// object with the correct size.
			syphonCapture = client.getImage(syphonCapture, false); 
			
			syphonCapture.loadPixels();
			syphonCapture.updatePixels();
						
			dust.clear();
			
			dust.copy(syphonCapture, 
					(int)(syphonUV[UVX1] * syphonCapture.width), 
					(int)(syphonUV[UVY1] * syphonCapture.height),
					(int)((syphonUV[UVX2]-syphonUV[UVX1]) * syphonCapture.width), 
					(int)((syphonUV[UVY2]-syphonUV[UVY1]) * syphonCapture.height),
					(int)(dustUV[UVX1] * dust.width), 
					(int)(dustUV[UVY1] * dust.height),
					(int)((dustUV[UVX2]-dustUV[UVX1]) * dust.width), 
					(int)((dustUV[UVY2]-dustUV[UVY1]) * dust.height));
			
			setupSimulation();
								
		}
	}
	
	private void setupSimulation(){
		int particleCount = dust.countParticles();
		println("found " + particleCount + " dust-particles");
		if(particleCount > 0){
			for(int i = 0; i < PNUM; i++) {
				PVector position = dust.getPosition(i % particleCount);
				p[i] = new particle(position.x, position.y, (int)random(MIN_AGE, MAX_AGE));
			}
			for(int i = 0; i <= lwidth; i++) {
				for(int u = 0; u <= lheight; u++) {
					v[i][u] = new vsquare(i*RES,u*RES);
					vbuf[i][u] = new vbuffer(i*RES,u*RES);
				}
			}
			flagSimSetup = true;
		}
	}

	private void updateSimulation(){
		if(flagSimSetup){
			if(myGust.isAlive()){
				mouseXvel = (int)myGust.vel.x;
				mouseYvel = (int)myGust.vel.y;
			}else{
				int axvel = mouseX-pmouseX;
				int ayvel = mouseY-pmouseY;
				mouseXvel = (axvel != mouseXvel) ? axvel : 0;
				mouseYvel = (ayvel != mouseYvel) ? ayvel : 0;
			}

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
				if(p[i].isAlive()){
					p[i].age();
					p[i].updatepos();
				}
			}
		}
	}

	private void drawSimulation(){
		if(flagSimSetup){
			canvas.loadPixels();		
			for(int i = 0; i < lwidth; i++) {
				for(int u = 0; u < lheight; u++) {
					v[i][u].addbuffer(i, u);
					v[i][u].updatevels(mouseXvel, mouseYvel);
					canvas.pixels[i+u*lwidth] = v[i][u].display(i, u);
					//canvas.pixels[i+u*lwidth] = canvas.pixels[i+u*lwidth] & 0x11ffffff;
				}
			}
			canvas.updatePixels();
		}
		randomGust = 0;
	}
	
	/* incoming osc message are forwarded to the oscEvent method. */
	void oscEvent(OscMessage theOscMessage) {
		//check first if already a Syphen capture has occured
		if(syphonCapture != null && dust != null){
			if(theOscMessage.checkAddrPattern("/gust")) {
				/* check if the typetag is the right one. */
				if(theOscMessage.checkTypetag("iiiii")) {
					int posx = (int)map(theOscMessage.get(1).intValue(), 
							(int)(syphonUV[UVX1] * syphonCapture.width),
							(int)(syphonUV[UVX2] * syphonCapture.width), 
							(int)(dustUV[UVX1] * dust.width), 
							(int)(dustUV[UVX2]* dust.width));
					int posy = (int)map(theOscMessage.get(2).intValue(),
							(int)(syphonUV[UVY1] * syphonCapture.height),
							(int)(syphonUV[UVY2] * syphonCapture.height), 
							(int)(dustUV[UVY1] * dust.height), 
							(int)(dustUV[UVY2] * dust.height));
					int velx = theOscMessage.get(3).intValue();
					int vely = theOscMessage.get(4).intValue();

					myGust.set(posx, posy, velx, vely);

					//println("### received an osc message /gust with typetag iiiii: x=" + posx + " y=" + posy + " vx=" + velx + " vy=" + vely);
				}  
			} 
		}		
		if(theOscMessage.checkAddrPattern("/kinectstarted")) {
			player.kinectStarted();
		}
		if(theOscMessage.checkAddrPattern("/persondetected")) {
			player.personDetected();
		}
		//println("OSC message received: " + theOscMessage.addrPattern());
	}
	
	void killAndRestartKinect(){
		runScriptFile("killNRestart.txt");
	}
	
	void runScriptFile(String filename){
		String lines[] = loadStrings(filename);
		StringBuffer runScript = new StringBuffer();
		for (int i =0 ; i < lines.length; i++) {
			runScript.append(lines[i]);
			if(i < lines.length - 1)
				runScript.append("\n");
		}
		applescript(runScript.toString());
	}
			  		  
	void applescript(String script){
		  ScriptEngineManager mgr = new ScriptEngineManager();
		  ScriptEngine engine = mgr.getEngineByName("AppleScript");
		  try{
		    engine.eval(script);
		  } catch (Exception e){;}
	}
	
	public void keyPressed()
	{
		if( key == 'r') {
			flagRefreshCapture = true;
		} 
	}
	
	class Fader{
		private int min, max;
		private boolean flagBlend;
		private int dir;
		private int steps;
		private int step;
		
		public int value;
		
		Fader(int _min, int _max){
			min = _min;
			max = _max;
			flagBlend = false;
			value = _min;
		}
		
		void start(int _steps, int _dir){
			flagBlend = true;
			dir = _dir;
			steps = _steps;
			step = (dir > 0) ? 0: steps;
		}
		
		void update(){
			if(flagBlend){
				step += dir;
				if(0 <= step && step <= steps){
					value = min + (max - min) / steps * step;
				}else{
					flagBlend = false;
				}
			}
		}		
	}
	
	class Player{
		int timer;
		boolean trigger_person;
		boolean trigger_kinect_started;		
		boolean trigger_captured_shadow;		
		
		final int PMODE_SETUP = 0;
		final int PMODE_SETUP_KIN_STARTED = 1;
		final int PMODE_TRIGGER_PERSON = 2;
		final int PMODE_START_LED = 3;
		final int PMODE_RESTART_KINECT = 4;
		final int PMODE_TRIGGER_KIN_RESTARTED = 5;
		final int PMODE_SETUP_SIMULATION = 6;
		final int PMODE_SIMULATION = 7;
		final int PMODE_DISSOLVE = 8;
		
		int playmode;
		
		Player(){
			timer = 0;
			playmode = PMODE_SETUP;
			println("Playmode = PMODE_SETUP");
			resetTrigger();
		}
		
		void kinectStarted(){
			trigger_kinect_started = true;
		}

		void personDetected(){
			trigger_person = true;
		}
		
		void capturedShadow(){
			trigger_captured_shadow = true;
		}

		void update(){
			switch(playmode){
			case PMODE_SETUP:
				if(isItTime(5)){
					killAndRestartKinect();
					playmode = PMODE_SETUP_KIN_STARTED;
					resetTrigger();
					println("Playmode = PMODE_SETUP_KIN_STARTED");
				}
				break;
			case PMODE_SETUP_KIN_STARTED:
				if(trigger_kinect_started){
					playmode = PMODE_TRIGGER_PERSON;
					resetTrigger();
					println("Playmode = PMODE_TRIGGER_PERSON");
				}else if(isItTime(30)){
					println("kinect failed to start, restart again");
					playmode = PMODE_SETUP;
					resetTrigger();
					println("Playmode = PMODE_SETUP");
				}
				break;
			case PMODE_TRIGGER_PERSON:
				if(trigger_person){
					playmode = PMODE_START_LED;
					resetTrigger();
					println("Playmode = PMODE_START_LED");
				}else if(isItTime(30)){
					println("kinect might not work anymore, restart again");
					playmode = PMODE_SETUP;
					resetTrigger();
					println("Playmode = PMODE_SETUP");
				}
				break;
			case PMODE_START_LED:
				if(isItTime(5)){
					//StartLED
					playmode = PMODE_RESTART_KINECT;
					resetTrigger();
					println("Playmode = PMODE_RESTART_KINECT");
				}
				break;
			case PMODE_RESTART_KINECT:
				killAndRestartKinect();
				playmode = PMODE_TRIGGER_KIN_RESTARTED;
				resetTrigger();
				println("Playmode = PMODE_TRIGGER_KIN_RESTARTED");
				break;
			case PMODE_TRIGGER_KIN_RESTARTED:
				if(trigger_kinect_started){
					playmode = PMODE_SETUP_SIMULATION;
					resetTrigger();
					println("Playmode = PMODE_SETUP_SIMULATION");
				}
				break;
			case PMODE_SETUP_SIMULATION:
				if(isItTime(1)){
					//StopLED
					captureShadow();
					globalBlend.start(10, 1);
					playmode = PMODE_SIMULATION;
					resetTrigger();
					println("Playmode = PMODE_SIMULATION");
				}
				break;
			case PMODE_SIMULATION:
				if(isItTime(30)){
					playmode = PMODE_DISSOLVE;
					globalBlend.start(100, -1);
					resetTrigger();
					println("Playmode = PMODE_DISSOLVE");
				}
				break;
			case PMODE_DISSOLVE:
				if(isItTime(10)){
					playmode = PMODE_TRIGGER_PERSON;
					resetTrigger();
					println("Playmode = PMODE_TRIGGER_PERSON");
				}
				break;
			}
		}
		
		boolean isItTime(int sec){
			return (timer+sec*1000 < millis());
		}
		
		void resetTrigger(){
			timer = millis();
			trigger_person = false;
			trigger_kinect_started = false;		
		}
	}
	
	class DustImage extends PImage{
		Vector<PVector> positions;
		int particleCount;
		
		DustImage(int w, int h, int type){
			super(w, h, type);
		}
		
		void clear(){
			int i = 0;
			this.loadPixels();
			while(i < dust.pixels.length){
				this.pixels[i++] = 0xffffff;
			}
			this.updatePixels();
		}
		
		int countParticles(){
			int i = 0;
			positions = new Vector<PVector>();
			this.loadPixels();
			while(i < dust.pixels.length){
				if(this.pixels[i] <= - 20000000){
					positions.add(new PVector(i % width, i / width));
				}
				i++;
			}
			this.updatePixels();
			return positions.size();
		}
		
		PVector getPosition(int index){
			return positions.elementAt(index);
		}		
	}
	
	class gust {
		PVector pos;
		PVector vel;
		
		int live;
		
		gust(){
			pos = new PVector();
			vel = new PVector();
			live = 255;
		}
		
		boolean isAlive(){
			return (live < 255)? true: false;
		}
		
		void set(float x, float y, float vx, float vy){
			pos.set(x, y, 0);
			vel.set(vx, vy, 0);
			live = 0;
		}
		
		void age(int cycles){
			live += cycles;
		}
		
		void draw(int diameter, int size){
			if(isAlive()){
				age(64);
				fill(live);
				stroke(live);
				ellipse(pos.x, pos.y, diameter, diameter);
				line(pos.x, pos.y, (pos.x + vel.x), (pos.y + vel.y));
			}
		}
		
	}
	
	class particle {
		float x;
		float y;
		float xvel;
		float yvel;
		float temp;
		int pos;
		int age;
		int death;
		boolean isalive;

		particle(float xIn, float yIn, int _death) {
			x = xIn;
			y = yIn;
			death = _death;
			age = 0;
			isalive = true;
		}
		
		boolean isAlive(){
			if(isalive)
				isalive = (age < death)? true: false;
			return isalive;
		}
		
		void age(){
			age++;
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
				v[vi][vu].addcolour(TRANSPARENCY);

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
				isalive = false;
				//reposition();
			}
			//if(random(0,400) < 1) reposition();

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
		
		int bg_r, bg_g, bg_b;
		
		vsquare(int xIn,int yIn) {
			x = xIn;
			y = yIn;
			setBackgroundColor(255);
		}

		void setBackgroundColor(int bgcolor){
			setBackgroundColor(bgcolor, bgcolor, bgcolor);
		}
		
		void setBackgroundColor(int r, int g, int b){
			bg_r = r;
			bg_g = g;
			bg_b = b;
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
			if(myGust.isAlive()) {
				adj = x - myGust.pos.x;
				opp = y - myGust.pos.y;
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
			if(col > 255) col = 255;
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
			col = 0;
			return color(bg_r-tcol, bg_g-tcol, bg_b-tcol, globalBlend.value);
		}
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
