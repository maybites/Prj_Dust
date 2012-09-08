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


import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import java.applet.Applet;
import java.util.Random;
 
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLMem.Usage;
import org.bridj.Pointer;
 
import static org.bridj.Pointer.*;

public class Dust extends PApplet{

	// detector
	// by Martin Froehlich <http://www.maybites.ch>

	// network configuration
	int port = 5204;
	String ipadress = "169.254.53.166";

	boolean videoIsActive = true;
	boolean isServer = true;

	int ScreenMode = 3  ; // 1 = 1024x768, 2 = 800x600, 3 = 640 x 480
	int CameraMode = 1; // 1 = DV-PAL (720x576), 2 = 320x240

	// play parameters
	int waitOnBlendingIn = 10000;    // time for blending in
	int blendingInStepSize = 10;
	int waitForServer = 1000;       // time for the server to start the simutaltion after sending signal to client
	int waitOnSimulation = 30000;  // time for the simulation to run
	int waitOnShiftingOut = 3000;   // time for switch black <-> white
	float shiftingSpeed = 0.4f;
	int waitOnSimulationShifted = 30000;  // time for the simulation to run
	int waitOnBlendingOut = 10000;  // time for blending of
	float blendingSpeed = -0.02f;

	int simParticleCount = 40000;
	int particleLifeCycles = 580;
	int particleLifeRndom = 400;

	float[] shadowColor = {0 ,0, 0};
	//float[] lightColor = {200 ,184, 164};
	float[] lightColor = {255 ,255, 255};

	int mapFrame = 5;

	int curtainColumn = 80;
	int curtainRow = 100;


	int displaySetupSwitch = 2000; //millisecs

	// video 
	boolean hasNewVideoFrame = false;
	PImage myVideoFrame;

	colorSetup g_cs;
	colorSpaces g_css;
	imageAnalysis imgAn;
	displayScreen myDisplayScreen;
	curtain myCurtain;
	Mode mode;

	SmokeSim mySim;

	// main parameter

	int external_screen = 0; // 1 = attached screen or beamer.

	int simDisplayX = 0; // simulation display
	int simDisplayY = 50;


	// resolution of camera L
	int videoResWidthNative; 
	int videoResHeightNative;


	// parameters for monitor resolution
	int resolution;

	int simDisplayWidth; // simulation display
	int simDisplayHeight;

	int screenWidth;  // single screen size 
	int screenHeight;


	// screen setup

	int winSizeWidth;
	int winSizeHeight;

	int display01_posX;
	int display01_posY;
	int display01_Width;
	int display01_Height;

	int display02_posX;
	int display02_posY;
	int display02_Width;
	int display02_Height;

	// video setup

	int videoResWidth;
	int videoResHeight;
	int videoResHCut;


	int timer;
	int timerOld;

	int trapTL_X;
	int trapTL_Y;
	int trapDL_X;
	int trapDL_Y;
	int trapTR_X;
	int trapTR_Y;
	int trapDR_X;
	int trapDR_Y;

	interpolator myRed;
	interpolator myGreen;
	interpolator myBlue;

	int runStep = 1;

	public void setup() 
	{
	 // resolution of camera 
	  if(CameraMode == 1){
	    videoResWidthNative = 720; // native camera resolution
	    videoResHeightNative = 576;
	  } else {
	    videoResWidthNative = 320; // native camera resolution
	    videoResHeightNative = 240;
	  }

	  if(ScreenMode == 1){
	    // parameters for 1024X768 monitor resolution ideal for both video (5:4) and internet(4:3) camera
	    resolution = 6; // 2, 3, 4, 6, 8, (5, 10)
	    simDisplayWidth = 960; // simulation display
	    simDisplayHeight = 600;
	    screenWidth = 1024;  // single screen size 
	    screenHeight = 768;
	  } else if(ScreenMode == 2){
	    // parameters for alternative 800x600 monitor resolution with video camera
	    resolution = 4; // 2, 4, 6, 8, 9, (5)
	    simDisplayWidth = 780; // simulation display
	    simDisplayHeight = 520;
	    screenWidth = 800;  // single screen size 
	    screenHeight = 600;
	  } else {
	    // parameters for optimal 640x480 monitor resolution with video camera
	    resolution = 2; // 5, 10, 20, (2, 4, 8)
	    simDisplayWidth = 600; // simulation display
	    simDisplayHeight = 400;
	    screenWidth = 640;  // single screen size 
	    screenHeight = 480;
	  }

	  // screen setup

	  winSizeWidth = screenWidth + screenWidth * external_screen;
	  winSizeHeight = screenHeight;

	  display01_posX = 0;
	  display01_posY = 0;
	  display01_Width = screenWidth;
	  display01_Height = screenHeight;

	  display02_posX = external_screen * screenWidth;
	  display02_posY = 0;
	  display02_Width = screenWidth;
	  display02_Height = screenHeight;

	  // video setup
	  videoResWidth = simDisplayWidth / resolution;
	  videoResHeight = videoResWidth * videoResHeightNative / videoResWidthNative;
	  videoResHCut = simDisplayHeight / resolution;

	  // Trapez setup
	  trapTL_X = display02_Width - 20;
	  trapTL_Y = + 20;
	  trapDL_X = + 20;
	  trapDL_Y = + 20;
	  trapTR_X = display02_Width - 20;
	  trapTR_Y = display02_Height - 20;
	  trapDR_X = + 20;
	  trapDR_Y = display02_Height - 20;

	  if(isServer){
	    beginNet(port); // Starts a Server
	  } else {
	    // beginNet(ipadress, port); // Starts a client
	  }

	  if(videoIsActive) beginVideo(videoResWidth, videoResHeight, 8);
	  
	  timerOld = millis();

	  background(#000000);
	 
	  size(winSizeWidth, winSizeHeight);
	  framerate(10);

	  mode = new Mode();
	  g_css = new colorSpaces("colorData.txt");
	  g_cs = new colorSetup("Univers66.vlw.gz", g_css);
	  imgAn = new imageAnalysis(g_css);
	  myDisplayScreen = new displayScreen();
	}

	public void loop() 
	{
	  if(mode.selected == mode.RUNSERVER){
	    runProgServer();
	  }
	  if(mode.selected == mode.RUNCLIENT){
	    runProgClient();
	  }
	  else if(mode.selected == mode.COLORSETUP){
	    if(g_cs.needsDisplayUpdate){
	      background(255);
	      g_cs.display();
	    }
	    if(hasNewVideoFrame) {
	      g_cs.video();
	    }
	    g_cs.onKeyPressed();
	  }
	  else if(mode.selected == mode.SCREENSETUP){
	    timer = millis();
	    if (timer-timerOld < displaySetupSwitch/2){
	      myDisplayScreen.videoSetup(20);
	    }
	    else if (timer-timerOld < displaySetupSwitch*2){
	      if(hasNewVideoFrame) {
	        myImage mb = new myImage(videoResWidth, videoResHCut);
	        mb.copyImage(myVideoFrame);
	        mb.display(simDisplayX, simDisplayY, 10, resolution);
	        hasNewVideoFrame = false;
	      }
	    }
	    else{
	      timerOld = timer;
	    }
	  }
	  else if(mode.selected == mode.BWSETUPLEFT){
	    myDisplayScreen.bwSetupLeft();
	    if(myDisplayScreen.onKeyPressedForBWColor()){
	      println("lightcolor : red(" + lightColor[0] + ") green("  + lightColor[1] + ") blue(" + lightColor[2] + ")");
	      println("shadowColor : red(" + shadowColor[0] + ") green("  + shadowColor[1] + ") blue(" + shadowColor[2] + ")");
	    }
	  }
	  else if(mode.selected == mode.BWSETUPRIGHT){
	    myDisplayScreen.bwSetupRight();
	    if(myDisplayScreen.onKeyPressedForBWColor()){
	      println("lightcolor : red(" + lightColor[0] + ") green("  + lightColor[1] + ") blue(" + lightColor[2] + ")");
	      println("shadowColor : red(" + shadowColor[0] + ") green("  + shadowColor[1] + ") blue(" + shadowColor[2] + ")");
	    }
	  }
	  else if(mode.selected == mode.SETUPTRAPEZ){
	    background(lightColor[0], lightColor[1], lightColor[2]);
	    myDisplayScreen.trapezMap();
	    if(myDisplayScreen.onKeyPressedForTrapez()){
	      println("Trapez Data correction");
	      println(" trapTL_X : " + (trapTL_X - display02_Width));
	      println(" trapTL_Y : " + trapTL_Y);
	      println(" trapDL_X : " + trapDL_X);
	      println(" trapDL_Y : " + trapDL_Y);
	      println(" trapTR_X : " + (trapTR_X - display02_Width));
	      println(" trapTR_Y : " + (trapTR_Y - display02_Height));
	      println(" trapDR_X : " + trapDR_X);
	      println(" trapDR_Y : " + (trapDR_Y - display02_Height));
	    }
	  }
	}


	public void keyPressed(){
	  mode.onKeyPressed();
	}

	public void mousePressed(){   
	  g_cs.onMousePressed();
	}  

	public void videoEvent()
	{  
	    if(!hasNewVideoFrame){
	      myVideoFrame = video;
	      if(myVideoFrame != null){
	        hasNewVideoFrame = true;
	      }
	    }
	}

	void runProgServer(){
	  if(runStep == 1){
	    if(keyPressed) {
	      // red
	      if (key == 32) { // space bar
	        println(" Server: ...Triggered");
	        runStep = 2;  
	      }
	    }   
	  }
	  else if(runStep == 2){
	    println(" Server: Sending data to client");
	    // send signal to 2nd compi
	    timerOld = millis();
	    runStep = 3;
	    
	    println(" Server: Blending in....");
	  }
	  else if(runStep == 3){
	    timer = millis();
	    if (timer-timerOld < waitOnBlendingIn){

	      background(0);
	    } else {
	      if(hasNewVideoFrame) {
	        println(" Server: ...Create simulation...");
	        imgAn.setNewImage(myVideoFrame, videoResHCut);
	        if(imgAn.hasTriggerColor(g_css.MAP, 100)){
	          myImage b = imgAn.getImageMap(g_css.MAP, mapFrame);
	          mySim = new SmokeSim(simParticleCount, particleLifeCycles, particleLifeRndom, display02_posX + simDisplayX, display02_posY + simDisplayY, simDisplayWidth, simDisplayHeight, b, resolution, 1);
	          runStep = 4;
	        }
	        println(" Server: ...Created");
	        hasNewVideoFrame = false;
	      }
	    }
	  }
	  else if(runStep == 4){
	    // send signal to 2nd compi
	    println(" Server: Sending data to client");
	    timerOld = millis();
	    runStep = 5;
	    println(" Server: Animation Start...");
	  }
	  else if(runStep == 5){
	    timer = millis();
	    if (timer-timerOld < waitOnSimulation){
	      noStroke();
	      mySim.updateGust();
	      mySim.calculate();
	      mySim.display(0, 0);
	    } else {
	      println(" Server: ...shifting...");
	      runStep = 6;
	      timerOld = millis();
	    }
	  }
	  else if(runStep == 6){
	    timer = millis();
	    if (timer-timerOld < waitOnShiftingOut){
	      noStroke();
	      mySim.updateGust();
	      mySim.calculate();
	      mySim.display(shiftingSpeed, 0);
	    } else {
	      println(" Server: ...keep on going...");
	      timerOld = millis();
	      runStep = 7;
	    }
	  }
	  else if(runStep == 7){
	    timer = millis();
	    if (timer-timerOld < waitOnSimulationShifted){
	      noStroke();
	      mySim.updateGust();
	      mySim.calculate();
	      mySim.display(0, 0);
	    } else {
	      println(" Server: ...blending out...");
	      runStep = 8;
	      timerOld = millis();
	    }
	  }
	  else if(runStep == 8){
	    timer = millis();
	    if (timer-timerOld < waitOnBlendingOut){
	      noStroke();
	      mySim.updateGust();
	      mySim.calculate();
	      mySim.display(0, blendingSpeed);
	    } else {
	      println(" Server: ...animation stopped");
	      background(0);
	      runStep = 1;
	    }
	  }
	}


	class displayScreen{

	  displayScreen(){
	  }
	  
	  void whiteBackground(){
	    fill(255, 255, 255);
	    noStroke();
	    rect(simDisplayX, simDisplayY, simDisplayWidth, simDisplayHeight);
	  }
	  
	  void videoSetup(int frame){
	    whiteBackground();
	    fill(0, 0, 0);
	    rect(simDisplayX, simDisplayY + (frame * resolution), simDisplayWidth, resolution * 2);
	    rect(simDisplayX, simDisplayY + simDisplayHeight - (frame * resolution), simDisplayWidth, - resolution * 2);
	    rect(simDisplayX + (frame * resolution), simDisplayY, resolution * 2, simDisplayHeight);
	    rect(simDisplayX + simDisplayWidth - (frame * resolution), simDisplayY, - resolution * 2, simDisplayHeight);
	  }
	  
	  void bwSetupLeft(){
	    noStroke();
	    fill(lightColor[0], lightColor[1], lightColor[2]);
	    rect(simDisplayX, simDisplayY, simDisplayWidth, simDisplayHeight / 2);
	    fill(shadowColor[0], shadowColor[1], shadowColor[2]);
	    ellipse(simDisplayX + (simDisplayHeight / 4), simDisplayY + (simDisplayHeight / 4), simDisplayHeight / 2, simDisplayHeight / 2);
	    fill(0, 0, 0);
	    rect(simDisplayX, simDisplayY + (simDisplayHeight / 2), simDisplayWidth, simDisplayHeight);
	  }    

	  void bwSetupRight(){
	    noStroke();
	    fill(lightColor[0], lightColor[1], lightColor[2]);
	    rect(simDisplayX, simDisplayY + (simDisplayHeight / 2), simDisplayWidth, simDisplayHeight / 2);
	    fill(shadowColor[0], shadowColor[1], shadowColor[2]);
	    ellipse(simDisplayX + (simDisplayHeight / 4), simDisplayY + (simDisplayHeight / 4), simDisplayHeight / 2, simDisplayHeight / 2);
	    fill(0, 0, 0);
	    rect(simDisplayX, simDisplayY, simDisplayWidth, simDisplayHeight / 2);
	  }    


	  void trapezMap(){
	    noStroke();
	    scale(1);
	    fill(shadowColor[0], shadowColor[1], shadowColor[2]); 
	    beginShape(POLYGON);
	    vertex(display02_posX + trapDL_X, display02_posY);
	    vertex(display02_posX + trapDL_X, display02_posY + trapDL_Y);
	    vertex(display02_posX + trapTL_X, display02_posY + trapTL_Y);
	    vertex(display02_posX + trapTL_X, display02_posY);
	    endShape();
	    beginShape(POLYGON);
	    vertex(display02_posX + trapDR_X, display02_posY + display02_Height);
	    vertex(display02_posX + trapDR_X, display02_posY + trapDR_Y);
	    vertex(display02_posX + trapTR_X, display02_posY + trapTR_Y);
	    vertex(display02_posX + trapTR_X, display02_posY + display02_Height);
	    endShape();
	    beginShape(POLYGON);
	    vertex(display02_posX + trapTR_X, display02_posY + display02_Height);
	    vertex(display02_posX + trapTR_X, display02_posY + trapTR_Y);
	    vertex(display02_posX + trapTL_X, display02_posY + trapTL_Y);
	    vertex(display02_posX + trapTL_X, display02_posY);
	    vertex(display02_posX + display02_Width, display02_posY);
	    vertex(display02_posX + display02_Width, display02_posY + display02_Height);
	    endShape();
	    beginShape(POLYGON);
	    vertex(display02_posX + trapDR_X, display02_posY + display02_Height);
	    vertex(display02_posX + trapDR_X, display02_posY + trapDR_Y);
	    vertex(display02_posX + trapDL_X, display02_posY + trapDL_Y);
	    vertex(display02_posX + trapDL_X, display02_posY);
	    vertex(display02_posX, display02_posY);
	    vertex(display02_posX, display02_posY + display02_Height);
	    endShape();
	    //pop();
	  }    

	  boolean onKeyPressedForTrapez(){
	    if(keyPressed) {
	      // trapTL
	      if (key == 'q') {
	        trapTL_X -= 1;
	      } else if (key == 'w') {
	        trapTL_X += 1;
	      } else if (key == 'e') {
	        trapTL_Y -= 1;
	      } else if (key == 'r') {
	        trapTL_Y += 1;
	      }
	      // trapDL
	      else if (key == 'a') {
	        trapDL_X -= 1;
	      } else if (key == 's') {
	        trapDL_X += 1;
	      } else if (key == 'd') {
	        trapDL_Y -= 1;
	      } else if (key == 'f') {
	        trapDL_Y += 1;
	      }
	      // trapTR
	      else if (key == 'u') {
	        trapTR_X -= 1;
	      } else if (key == 'i') {
	        trapTR_X += 1;
	      } else if (key == 'o') {
	        trapTR_Y -= 1;
	      } else if (key == 'p') {
	        trapTR_Y += 1;
	      }
	      // trapDR
	      else if (key == 'h') {
	        trapDR_X -= 1;
	      } else if (key == 'j') {
	        trapDR_X += 1;
	      } else if (key == 'k') {
	        trapDR_Y -= 1;
	      } else if (key == 'l') {
	        trapDR_Y += 1;
	      }
	      return true;
	    }
	    return false;
	  }

	  boolean onKeyPressedForBWColor(){
	    if(keyPressed) {
	      // red
	      if (key == 'w') {
	        lightColor[0] = lightColor[0] - 1;
	      } else if (key == 'e') {
	        lightColor[0] = lightColor[0] + 1;
	      } else if (key == 't') {
	        shadowColor[0] = shadowColor[0] - 1;
	      } else if (key == 'z') {
	        shadowColor[0] = shadowColor[0] + 1;
	      }
	      // green
	      else if (key == 'd') {
	        lightColor[1] = lightColor[1] - 1;
	      } else if (key == 'f') {
	        lightColor[1] = lightColor[1] + 1;
	      } else if (key == 'h') {
	        shadowColor[1] = shadowColor[1] - 1;
	      } else if (key == 'j') {
	        shadowColor[1] = shadowColor[1] + 1;
	      }
	      // blue
	      else if (key == 'c') {
	        lightColor[2] = lightColor[2] - 1;
	      } else if (key == 'v') {
	        lightColor[2] = lightColor[2] + 1;
	      } else if (key == 'n') {
	        shadowColor[2] = shadowColor[2] - 1;
	      } else if (key == 'm') {
	        shadowColor[2] = shadowColor[2] + 1;
	      }
	      return true;
	    }
	    return false;
	  }
	  
	  void frameWithFadeOut(){
	    background(0);
	    
	    whiteBackground();
	    scale(1);
	    beginShape(QUADS);
	    fill(255, 255, 255); vertex(simDisplayX + simDisplayWidth, simDisplayY);
	    fill(255, 255, 255); vertex(simDisplayX + simDisplayWidth, simDisplayY + simDisplayHeight);
	    fill(0, 0, 0); vertex(screenWidth, simDisplayY + simDisplayHeight);
	    fill(0, 0, 0); vertex(screenWidth, simDisplayY);
	    endShape();
	  }
	}

	class Mode{
	  
	  int selected; 
	  
	  int RUNCLIENT = 0;
	  int RUNSERVER = 1;
	  int SIMULATION = 2;
	  int COLORSETUP = 3;
	  int SCREENSETUP = 4;
	  int BWSETUPLEFT = 5;
	  int BWSETUPRIGHT = 6;
	  int SETUPTRAPEZ = 7;

	  Mode(){
	    selected = RUNSERVER;
	  }
	  
	  void onKeyPressed(){
	    if(keyPressed) {
	      // red
	      if (key == '1') {
	        selected = RUNSERVER;
	        background(0);
	      } else if (key == '0') {
	        selected = RUNCLIENT;
	        background(0);
	      } else if (key == '2') {
	        background(0);
	        selected = COLORSETUP;
	        g_cs.needsDisplayUpdate = true;
	      } else if (key == '3') {
	        selected = SCREENSETUP;
	        background(0);
	      } else if (key == '4') {
	        selected = BWSETUPRIGHT;
	        background(0);
	      } else if (key == '5') {
	        selected = BWSETUPLEFT;
	        background(0);
	      } else if (key == '6') {
	        selected = SETUPTRAPEZ;
	        background(0);
	      } 
	    }
	  }
	}  

	class imageAnalysis{

	  PImage pict;
	  colorSpaces cSpaces;
	  myImage mask;
	  int imageCut;

	  imageAnalysis(colorSpaces thisSpaces){
	    cSpaces = thisSpaces;
	  }
	  
	  void setNewImage(PImage thisImage, int tImageCut){
	    pict = thisImage;
	    imageCut = tImageCut;
	    mask = new myImage(pict.width, imageCut);    
	  }    
	  
	  boolean hasTriggerColor(int triggerColor, int count){
	    int trigger = 0;
	    cSpaces.setCurrentSpace(triggerColor);
	    for(int j=0; j < imageCut; j++) {
	      for(int i=0; i < pict.width; i++) {
	        Color pix = pict.pixels[j * pict.width + i];
	        if(cSpaces.isInCurrentSpace(int(red(pix)), int(green(pix)), int(blue(pix)))){
	          trigger++;
	        }
	      }
	    }
	    if(trigger >= count){
	      return true;
	    }
	    return false;
	  }    

	  myImage getImageMap(int mapColor, int frame){
	    cSpaces.setCurrentSpace(mapColor);
	    for(int j=0; j < imageCut; j++) {
	      for(int i=0; i < pict.width; i++) {
	        mask.setPixel(j * pict.width + i, color(255, 255, 255));
	      }
	    }
	    for(int j=frame; j < imageCut - frame; j++) {
	      for(int i=frame; i < pict.width - frame; i++) {
	        color pix = pict.pixels[j * pict.width + i];
	        if(cSpaces.isInCurrentSpace(int(red(pix)), int(green(pix)), int(blue(pix)))){
	          mask.setPixel(j * pict.width + i, color(0, 0, 0));
	        }
	      }
	    }
	    return mask;
	  }
	}

	class colorSpaces{

	  int[][][] cD;// definition of detection color space
	  int spaceCount;
	  int currentSpace;
	  String myData;

	  colorSpaces(String fileName){
	    myData = fileName;
	    loadData();
	  }  

	  void setRedTop(int v){
	    cD[currentSpace][0][1] = v;
	  }
	  void setRedDown(int v){
	    cD[currentSpace][0][0] = v;
	  }
	  void setGreenTop(int v){
	    cD[currentSpace][1][1] = v;
	  }
	  void setGreenDown(int v){
	    cD[currentSpace][1][0] = v;
	  }
	  void setBlueTop(int v){
	    cD[currentSpace][2][1] = v;
	  }
	  void setBlueDown(int v){
	    cD[currentSpace][2][0] = v;
	  }

	  int getRedTop(){
	    return cD[currentSpace][0][1];
	  }
	  int getRedDown(){
	    return cD[currentSpace][0][0];
	  }
	  int getGreenTop(){
	    return cD[currentSpace][1][1];
	  }
	  int getGreenDown(){
	    return cD[currentSpace][1][0];
	  }
	  int getBlueTop(){
	    return cD[currentSpace][2][1];
	  }
	  int getBlueDown(){
	    return cD[currentSpace][2][0];
	  }

	  int nextSpace(){
	    currentSpace = (currentSpace + 1) % spaceCount;
	    return currentSpace;
	  }  

	  int lastSpace(){
	    currentSpace = (spaceCount + currentSpace - 1) % spaceCount;
	    return currentSpace;
	  }  
	  
	  boolean setCurrentSpace(int current){
	    if(current < spaceCount){
	      currentSpace = current;
	      return true;
	    }
	    return false;
	  }
	  
	  void swapSpace(){
	    int nextSpace = (currentSpace + 1) % spaceCount;
	    int[][][] temp = cD;
	    temp[nextSpace][0][0] = cD[currentSpace][0][0];
	    temp[nextSpace][0][1] = cD[currentSpace][0][1];
	    temp[nextSpace][1][0] = cD[currentSpace][1][0];
	    temp[nextSpace][1][1] = cD[currentSpace][1][1];
	    temp[nextSpace][2][0] = cD[currentSpace][2][0];
	    temp[nextSpace][2][1] = cD[currentSpace][2][1];
	    temp[currentSpace][0][0] = cD[nextSpace][0][0];
	    temp[currentSpace][0][1] = cD[nextSpace][0][1];
	    temp[currentSpace][1][0] = cD[nextSpace][1][0];
	    temp[currentSpace][1][1] = cD[nextSpace][1][1];
	    temp[currentSpace][2][0] = cD[nextSpace][2][0];
	    temp[currentSpace][2][1] = cD[nextSpace][2][1];
	    cD = temp;
	  }

	  int currentSpace(){
	    return currentSpace;
	  }  

	  int count(){
	    return spaceCount;
	  }  

	  void addColorSpace(){
	    int[][][] temp = cD;
	    cD = new int[spaceCount + 1][3][2];
	    for(int i = 0; i < spaceCount; i++){
	      cD[i][0][0] = temp[i][0][0];
	      cD[i][0][1] = temp[i][0][1];
	      cD[i][1][0] = temp[i][1][0];
	      cD[i][1][1] = temp[i][1][1];
	      cD[i][2][0] = temp[i][2][0];
	      cD[i][2][1] = temp[i][2][1];
	    }
	    spaceCount++;
	    currentSpace = spaceCount - 1;
	  }
	  
	  void removeColorSpace(){
	    if(spaceCount > 1){
	      int[][][] temp = new int[spaceCount - 1][3][2];
	      for(int i = 0; i < currentSpace; i++){
	        temp[i][0][0] = cD[i][0][0];
	        temp[i][0][1] = cD[i][0][1];
	        temp[i][1][0] = cD[i][1][0];
	        temp[i][1][1] = cD[i][1][1];
	        temp[i][2][0] = cD[i][2][0];
	        temp[i][2][1] = cD[i][2][1];
	      }
	      for(int i = (currentSpace + 1); i < spaceCount; i++){
	        temp[i - 1][0][0] = cD[i][0][0];
	        temp[i - 1][0][1] = cD[i][0][1];
	        temp[i - 1][1][0] = cD[i][1][0];
	        temp[i - 1][1][1] = cD[i][1][1];
	        temp[i - 1][2][0] = cD[i][2][0];
	        temp[i - 1][2][1] = cD[i][2][1];
	      }
	      cD = temp;
	      spaceCount--;
	      currentSpace = 0;
	    }
	  }

	  void loadData(){
	  println("try to load data...");
	    byte lines[] = loadBytes(myData);
	    spaceCount = int(lines[0]); 
	    println("loads " + spaceCount + " colorSpaces");
	    cD = new int[spaceCount][3][2]; 
	    for(int i=0; i < spaceCount; i++){
	      cD[i][0][0] = int(lines[i*6 + 1] & 0xff);
	      cD[i][0][1] = int(lines[i*6 + 2] & 0xff);
	      cD[i][1][0] = int(lines[i*6 + 3] & 0xff);
	      cD[i][1][1] = int(lines[i*6 + 4] & 0xff);
	      cD[i][2][0] = int(lines[i*6 + 5] & 0xff);
	      cD[i][2][1] = int(lines[i*6 + 6] & 0xff);
	      println(i);
	    }
	  println("...data loaded");
	    currentSpace = 0;
	  }

	  void saveData(){
	  println("try to save data...");
	    byte[] content = new byte[spaceCount * 6 + 1];
	    content[0] = byte(spaceCount);
	    println("saves " + spaceCount + " colorSpaces");
	    for(int i=0; i < spaceCount; i++){
	      content[i*6 + 1] = byte(cD[i][0][0]);
	      content[i*6 + 2] = byte(cD[i][0][1]);
	      content[i*6 + 3] = byte(cD[i][1][0]);
	      content[i*6 + 4] = byte(cD[i][1][1]);
	      content[i*6 + 5] = byte(cD[i][2][0]);
	      content[i*6 + 6] = byte(cD[i][2][1]);
	      println(i);
	    } 
	    saveBytes(myData, content); 
	  println("...data saved");
	  }
	  
	  boolean isInCurrentSpace(int colorR, int colorG, int colorB){
	    if (colorR >= getRedDown() && getRedTop() >= colorR){
	      if (colorG >= getGreenDown() && getGreenTop() >= colorG){
	        if (colorB >= getBlueDown() && getBlueTop() >= colorB){
	          return true;
	        }
	      }
	    }
	    return false;
	  }  

	  int MAP = 0;
	  int MAPTRIGGER = 1;
	  int TRIGGER02 = 2;
	  int TRIGGER03 = 3;

	  String getTriggerType(int number){
	    if(number == 0){
	      return "MAP";
	    } else if(number == 1){
	      return "MAPTRIGGER";
	    } else if(number == 2){
	      return "TRIGGER02";
	    } else if(number == 3){
	      return "TRIGGER03";
	    }
	    return "none";
	  }
	}

	class colorSetup 
	{
	  Picker myPick;
	  PFont myFont;
	  
	  int polyGraphPosX = 150;        // x pos of the polygone
	  int polyGraphPosY = 100;        // y pos of the polygone
	  int infoTextPosX  = 50;        // x pos of the infotext
	  int infoTextPosY  =  200;        // y pos of the infotext
	  int colorPolyScale = 30; // scale of the color polygone
	  int myTextSize = 15; 
	  int myTextColor = 0; 
	  boolean showText = true;
	    
	  boolean needsDisplayUpdate;
	  
	  imageAnalysis myImgAn;

	  colorSpaces css;

	  colorSetup(String font, colorSpaces thiscs) {
	    css = thiscs;
	    myImgAn = new imageAnalysis(css);
	    myFont = loadFont(font);
	    needsDisplayUpdate = true;
	    myPick = new Picker( myFont, polyGraphPosX - 100, 350, polyGraphPosX, polyGraphPosY);
	  }
	  
	  void display(){
	    fill(255);
	    noStroke();
	    rect(0,0,390,700);
	    drawColorInfoText();
	    drawColorPolyGraph();
	    if(myPick.needsDisplayUpdate){
	      myPick.display();
	    }
	    needsDisplayUpdate = false;
	  }

	  void video(){
	    noFill();
	    stroke(0);
	    image(myVideoFrame, 400, 50);
	    rect(400 - 1,50 -1 , videoResWidth + 1, videoResHCut  + 1);
	        
	        
	    myImgAn.setNewImage(myVideoFrame, videoResHeight);
	    myImage b = myImgAn.getImageMap(css.currentSpace(), 0);
	    b.display(400, 400);
	    rect(400 - 1,400 - 1, videoResWidth + 1, videoResHCut + 1);
	    hasNewVideoFrame = false;
	  }
	  
	  void drawColorInfoText(){
	    fill(myTextColor);
	    textFont(myFont, myTextSize);
	    
	    text("Detection Color No: " + css.currentSpace() + " of " + css.count() , infoTextPosX, infoTextPosY + 0 * myTextSize + 0 * 10);
	    text("Trigger Type: " + css.getTriggerType(css.currentSpace()), infoTextPosX, infoTextPosY + 1 * myTextSize + 0 * 10);
	    text("+red   (t...z): " + css.getRedTop(), infoTextPosX, infoTextPosY + 2 * myTextSize + 1 * 10);
	    text("-red   (w...e): " + css.getRedDown(), infoTextPosX, infoTextPosY + 3 * myTextSize + 1 * 10);
	    text("+green (h...j): " + css.getGreenTop(), infoTextPosX, infoTextPosY + 4 * myTextSize + 2 * 10);
	    text("-green (d...f): " + css.getGreenDown(), infoTextPosX, infoTextPosY + 5 * myTextSize + 2 * 10);
	    text("+blue  (n...m): " + css.getBlueTop(), infoTextPosX, infoTextPosY + 6 * myTextSize + 3 * 10);
	    text("-blue  (c...v): " + css.getBlueDown(), infoTextPosX, infoTextPosY + 7 * myTextSize + 3 * 10);

	    text("Other functions: ", infoTextPosX + 160, infoTextPosY + 0 * myTextSize + 0 * 10);
	    text("  -save color spaces = s", infoTextPosX + 160, infoTextPosY + 1 * myTextSize + 1 * 10);
	    text("  -load color spaces = l", infoTextPosX + 160, infoTextPosY + 2 * myTextSize + 1 * 10);
	    text("  -next color space = right a.", infoTextPosX + 160, infoTextPosY + 3 * myTextSize + 2 * 10);
	    text("  -prev color space = left a.", infoTextPosX + 160, infoTextPosY + 4 * myTextSize + 2 * 10);
	    text("  -del color space = down a.", infoTextPosX + 160, infoTextPosY + 5 * myTextSize + 3 * 10);
	    text("  -create color space = up a.", infoTextPosX + 160, infoTextPosY + 6 * myTextSize + 3 * 10);
	    text("  -swap colorspace = u", infoTextPosX + 160, infoTextPosY + 7 * myTextSize + 4 * 10);
	    //text("  -set as trigger 02 = i", infoTextPosX + 160, infoTextPosY + 8 * myTextSize + 4 * 10);
	    //text("  -set as trigger 03 = o", infoTextPosX + 160, infoTextPosY + 9 * myTextSize + 5 * 10);
	    //text("  -set as mapp = p", infoTextPosX + 160, infoTextPosY + 10 * myTextSize + 5 * 10);
	  }

	  
	  void drawColorPolyGraph(){
	  // draws the color polygon with the current color perimeters
	  // it shows most of the colors the color detection parameter is looking for
	    noStroke();
	    push();

	    translate(polyGraphPosX, polyGraphPosY);
	    scale(colorPolyScale);

	    beginShape(POLYGON);
	    fill(css.getRedTop() , css.getGreenDown(), css.getBlueDown()); vertex(    0,  -2);
	    fill(css.getRedTop() , css.getGreenTop(), css.getBlueDown()); vertex(  1.7,  -1);
	    fill(css.getRedDown() , css.getGreenTop(), css.getBlueDown()); vertex(  1.7,   1);
	    fill(css.getRedDown() , css.getGreenTop(), css.getBlueTop()); vertex(    0,   2);
	    fill(css.getRedDown() , css.getGreenDown(), css.getBlueTop()); vertex( -1.7,   1);
	    fill(css.getRedTop() , css.getGreenDown(), css.getBlueTop()); vertex( -1.7,  -1);
	    endShape();

	    pop();

	    if (showText){
	      fill(myTextColor);
	      textFont(myFont, myTextSize);
	      text("+red (t...z): "   + css.getRedTop(), polyGraphPosX + (0 * colorPolyScale)    -30, polyGraphPosY + (-2 * colorPolyScale) -10);
	      text("+green (h...j): " + css.getGreenTop(), polyGraphPosX + (1.7 * colorPolyScale)  -0, polyGraphPosY + (1 * colorPolyScale) +10 );
	      text("+blue (n...m): "  + css.getBlueTop(), polyGraphPosX + (-1.7 * colorPolyScale) -100, polyGraphPosY + (1 * colorPolyScale) +10);
	      text("-red (w...e): "   + css.getRedDown(), polyGraphPosX + (0 * colorPolyScale)    -30, polyGraphPosY + (2 * colorPolyScale) +10);
	      text("-green (d...f): " + css.getGreenDown(), polyGraphPosX + (-1.7 * colorPolyScale) -100, polyGraphPosY + (-1 * colorPolyScale) -5);
	      text("-blue (c...v): "  + css.getBlueDown(), polyGraphPosX + (1.7 * colorPolyScale)  -0, polyGraphPosY + (-1 * colorPolyScale)  -5);
	    }
	  }

	  void onMousePressed(){
	    if (mousePressed) {
	      color c = get(mouseX, mouseY);
	      myPick.saveColor(int(red(c)),int(green(c)),int(blue(c)));
	      needsDisplayUpdate = true;
	    }
	  }
	  
	  void onKeyPressed(){
	  // changes the color value for red:   min- = w, min+ = r, max- = t, max+ = z
	  // changes the color value for green: min- = d, min+ = f, max- = h, max+ = j
	  // changes the color value for blue:  min- = c, min+ = v, max- = n, max+ = m
	    if(keyPressed) {
	      // red
	      if (key == 'w') {
	        css.setRedDown(css.getRedDown() - 1);
	      } else if (key == 'e') {
	        if (css.getRedDown() < css.getRedTop()){css.setRedDown(css.getRedDown() + 1);}
	      } else if (key == 't') {
	        if (css.getRedDown() < css.getRedTop()){css.setRedTop(css.getRedTop() - 1);}
	      } else if (key == 'z') {
	        css.setRedTop(css.getRedTop() + 1);
	      }
	      // puts the max and min value of the pickerswatch for red
	      else if (key == 'r') {
	        css.setRedDown(myPick.getMinValue(0));
	        css.setRedTop(myPick.getMaxValue(0));
	      }
	      // green
	      else if (key == 'd') {
	        css.setGreenDown(css.getGreenDown() - 1);
	      } 
	      else if (key == 'f') {
	        if (css.getGreenDown() < css.getGreenTop()){css.setGreenDown(css.getGreenDown() + 1);}
	      } else if (key == 'h') {
	        if (css.getGreenDown() < css.getGreenTop()){css.setGreenTop(css.getGreenTop() - 1);}
	      } else if (key == 'j') {
	        css.setGreenTop(css.getGreenTop() + 1);
	      }
	      // puts the max and min value of the pickerswatch for green
	      else if (key == 'g') {
	        css.setGreenDown(myPick.getMinValue(1));
	        css.setGreenTop(myPick.getMaxValue(1));
	      }
	      // blue
	      else if (key == 'c') {
	        css.setBlueDown(css.getBlueDown() - 1);
	      } else if (key == 'v') {
	        if (css.getBlueDown() < css.getBlueTop()){css.setBlueDown(css.getBlueDown() + 1);}
	      } else if (key == 'n') {
	        if (css.getBlueDown() < css.getBlueTop()){css.setBlueTop(css.getBlueTop() - 1);}
	      } else if (key == 'm') {
	        css.setBlueTop(css.getBlueTop() + 1);
	      }
	      // puts the max and min value of the pickerswatch for blue
	      else if (key == 'b') {
	        css.setBlueDown(myPick.getMinValue(2));
	        css.setBlueTop(myPick.getMaxValue(2));
	      }
	      else if (key == 's') {
	        css.saveData();
	      }
	      else if (key == 'l') {
	        css.loadData();
	      }
	      else if (key == 'u') {
	        css.swapSpace();
	      }
	      else if (key == 'i') {
	        //css.setAsTrigger(css.TRIGGER02);
	      }
	      else if (key == 'o') {
	        //css.setAsTrigger(css.TRIGGER03);
	      }
	      else if (key == 'p') {
	        //css.setAsMap();
	      }
	      else if (key == 37) {
	        css.lastSpace();
	      }
	      else if (key == 38) {
	        css.addColorSpace();
	      }
	      else if (key == 40) {
	        css.removeColorSpace();
	      }
	      else if (key == 39) {
	        css.nextSpace();
	      }
	      else {
	        println("key:" + key);
	        return;
	      }
	      needsDisplayUpdate = true;
	      return;
	    }
	    return;
	  }  
	}



	class Picker
	{
	  int cPSize = 3;
	  int cPIndex = 0;
	  int cPPixelSize = 10;
	  int[][][] cP; // color Picker Matrix, stores cPSize x cPSize colors in rgb
	  
	  BFont myFont;
	  Pixel myPix;
	  int myTextSize = 15;
	  int myTextColor = 0;
	  
	  int mySwatchPosX;  
	  int mySwatchPosY;
	  
	  int myTextPosX;  
	  int myTextPosY;
	  
	  boolean needsDisplayUpdate = true;

	  Picker(BFont font, int textX, int textY, int swatchX, int swatchY){
	    myFont = font;
	    mySwatchPosX = swatchX;
	    mySwatchPosY = swatchY;
	    myTextPosX = textX;
	    myTextPosY = textY;

	    cP = new int[cPSize][cPSize][3];
	    myPix = new Pixel(cPPixelSize, true, true);
	  }
	  
	  void saveColor(int r, int g, int b){
	    cPIndex = (cPIndex + 1) % (cPSize * cPSize);
	    cP[cPIndex / cPSize][cPIndex % cPSize][0]=r;
	    cP[cPIndex / cPSize][cPIndex % cPSize][1]=g;
	    cP[cPIndex / cPSize][cPIndex % cPSize][2]=b;
	    needsDisplayUpdate = true;
	  }

	  int getMaxValue(int selColor){
	    int search = 0;
	    for(int j=0; j<cPSize; j=j+1) {
	      for(int i=0; i<cPSize; i=i+1) {
	        if (cP[j][i][selColor] > search) {search = cP[j][i][selColor];}
	      }
	    }
	    return search;
	  }

	  int getMinValue(int selColor){
	    int search = 255;
	    for(int j=0; j<cPSize; j=j+1) {
	      for(int i=0; i<cPSize; i=i+1) {
	        if (cP[j][i][selColor] < search) {search = cP[j][i][selColor];}
	      }
	    }
	    return search;
	  }
	  
	  void display(){
	    textFont(myFont, myTextSize);
	    for(int j=0; j<cPSize; j=j+1) {
	      for(int i=0; i<cPSize; i=i+1) {
	        myPix.setColor(cP[j][i][0], cP[j][i][1],cP[j][i][2]);
	        myPix.drawAtRef(mySwatchPosX - cPPixelSize*cPSize/2, mySwatchPosY - cPPixelSize*cPSize/2, j, i, 0, 0, 1);
	        fill(myTextColor);
	        text("Swatch no:"+ (j * 3 + i)       , myTextPosX, myTextPosY + (j * 3 + i)*40);
	        text("           red  :"+ cP[j][i][0], myTextPosX, myTextPosY + (j * 3 + i)*40 + 10);
	        text("           green:"+ cP[j][i][1], myTextPosX, myTextPosY + (j * 3 + i)*40 + 20);
	        text("           blue :"+ cP[j][i][2], myTextPosX, myTextPosY + (j * 3 + i)*40 + 30);
	      }
	    }
	    needsDisplayUpdate = false;
	  }
	  
	}

	class Pixel
	{
	  int myColor;
	  
	  int mySize;
	  
	  boolean drawFrame; // draws frame if true
	  boolean drawBody;  // draws body if true
	  
	  Pixel(int thisSize, boolean frame, boolean body){
	    drawFrame = frame;
	    drawBody = body;
	    mySize = thisSize;
	  }

	  void setColor(color c){
	    myColor = c;
	  }
	  
	  void setColor(int r, int g, int b){
	    myColor = color(r, g, b);
	  }

	  void setColor(float r, float g, float b){
	    myColor = color(r, g, b);
	  }
	  
	  void setupDrawing(){
	    if (drawFrame){
	      stroke(myColor);
	    }else{
	      noStroke();
	    }
	    if (drawBody){
	      fill(myColor);
	    }else{
	      noFill();
	    }
	  }
	    
	  void drawAtRef(int refX, int refY, int coordX, int coordY, int transX, int transY, float resize){
	    setupDrawing();
	    //if(mySize > 1){
	      rect( refX + (mySize*(coordX + transX)) + (mySize/2) - (mySize*resize/2),
	            refY + (mySize*(coordY + transY)) + (mySize/2) - (mySize*resize/2),
	            mySize*resize,
	            mySize*resize );
	    //}else{
	    //  point(refX+coordX ,refY+coordY);
	    //}
	  }
	}


	class Timer{
	  int m = 0;
	  int d;
	  String myName;

	  Timer(String name){
	    myName = name;
	  }
	  
	  void startIt(){
	    if(m == 0){
	      m = millis();
	    }
	  }
	  
	  void stopIt(){
	    if(m != 0){
	      d = millis() - m;
	      println("Timer ("+myName+"): "+d+" milliseconds at "+millis());
	      m = 0;
	    }
	  }
	}
	/********************************************************

	SMOKE SIMULATION (by Glen Murphy <http://www.bodytag.org>)

	reorganized by Martin Fršhlich

	**********************************************************/



	class SmokeSim{
	  int res = 6;
	  int penSize = 30;
	  int lwidth;
	  int lheight;
	  int pnum;
	  vsquare[][] v;
	  vbuffer[][] vbuf;
	  particle[] p;
	  int pcount = 0;
	  
	  int mouseXvel = 0;
	  int mouseYvel = 0;
	  
	  int xCoord;
	  int yCoord;
	  
	  int multiplier;

	  randomGust rg;
	  
	  int lifeCycle;
	  int lifeRndom;
	  
	  SmokeSim(int tpnum, int lc, int lr, int xPos, int yPos, int twidth, int theight, myImage img, int tres, int m){
	    multiplier = m;
	    pnum = tpnum;
	    lifeCycle = lc;
	    lifeRndom = lr;
	    res = tres;
	    xCoord = xPos;
	    yCoord = yPos;
	    lwidth = twidth / res;
	    lheight = theight / res;
	    rg = new randomGust();
	    v = new vsquare[lwidth+1][lheight+1];
	    vbuf = new vbuffer[lwidth+1][lheight+1];
	    
	    setupParticle(img);

	     // sets the new particles where the black pixel inside the pict are
	    for(int i = 0; i <= lwidth; i++) {
	      for(int u = 0; u <= lheight; u++) {
	        v[i][u] = new vsquare(xPos + i*res, yPos + u*res);
	        vbuf[i][u] = new vbuffer(xPos + i*res, yPos + u*res);
	      }
	    }
	  }

	  void updateGust(){
	    int axvel = mouseX-pmouseX;
	    int ayvel = mouseY-pmouseY;

	    mouseXvel = (axvel != mouseXvel) ? axvel : 0;
	    mouseYvel = (ayvel != mouseYvel) ? ayvel : 0;

	    rg.update();
	  }
	  
	  void calculate(){  
	  
	    // takes about 7 millis.
	    for(int i = 0; i < (lwidth); i++) {
	      for(int u = 0; u < (lheight); u++) {
	        vbuf[i][u].updatebuf(v, lwidth, lheight, i, u);
	        v[i][u].col = 0; // sets the virtual screen empty
	      }
	    }
	  
	    // takes about 250! millis
	    for(int i = 0; i < pnum-1; i++) {
	      p[i].updatepos(v, res, lwidth, lheight);
	    }
	  }
	  
	  void removeParticles(float factor){
	    if(factor < 1 && factor > 0){
	      int pnum_buf = pnum * (int)(factor);
	      particle[] p_buffer = new particle[pnum_buf];
	      for(int i = 0; i < pnum_buf-1; i++) {
	        p_buffer[i] = p[i];
	      }
	      pnum = pnum_buf;
	      p = p_buffer;
	    }
	  }
	  
	  void display(float shift, float blend){

	   // takes about 75 millis
	    for(int i = 0; i < lwidth; i++) {
	      for(int u = 0; u < lheight; u++) {
	        v[i][u].addbuffer(vbuf, lwidth, lheight, i, u);
	        v[i][u].updatevels(rg, penSize, res, mouseXvel, mouseYvel);
	        if(shift != 0) v[i][u].shift(shift);
	        if(blend != 0) v[i][u].blend(blend);
	        v[i][u].display(i, u, lwidth, lheight, v, res); // displays the virtual screen
	      }
	    }
	    rg.randomGust = 0;
	  }
	  
	  void setupParticle(myImage img){
	    color c;
	    int countB = 0;
	    for(int i = 0; i < (img.getWidth() * img.getHeight()) ; i++) {
	      c = img.getPixel(i);
	      if(brightness(c) == 0){
	        countB++;
	      }
	    }
	    pnum = (countB > pnum) ? countB : pnum;
	    p = new particle[pnum];
	    int pPerPix = pnum / countB ;
	    int addColour = (pPerPix >= 1) ? (196 / pPerPix) : 196;
	    countB = 0;
	    int countp = 0;
	    int step = 6;
	    for(int t = 0; t < pPerPix; t++) {
	      for(int z = 0; z < step; z++){
	        for(int i = z; i < img.getWidth(); i+=step) {
	          for(int u = 0; u < img.getHeight(); u++) {
	            c = img.getPixel(u*img.getWidth() + i);
	            if(brightness(c) == 0){
	             p[countp++] = new particle(i * res,u * res, addColour, lifeCycle - int(random(lifeRndom)));
	            }
	          }
	        }
	      }
	    }
	    pnum = countp;
	  }
	}


	class randomGust{

	  int randomGust = 0;
	  int randomGustMax;
	  float randomGustX;
	  float randomGustY;
	  float randomGustSize;
	  float randomGustXvel;
	  float randomGustYvel;


	  randomGust(){
	    randomGust = 0;
	  }

	  void update(){  

	    // creates a random gust that weakens over the time, only to be set again randomly
	    // the gust is defined through the size, its start position and its velocity
	    if(randomGust <= 0) {
	        randomGustMax = (int)random(5,12);
	        randomGust = randomGustMax;
	        randomGustX = random(0,width);
	        randomGustY = random(0,height);
	        randomGustSize = random(0,40);
	        randomGustXvel = random(-10,0);
	        randomGustYvel = random(-6 ,6);
	        // this snipplet directs the gust always into the center
	        /*
	        if(randomGustX > (width/2 * 1.2)) {
	          randomGustXvel = randomGustXvel * -1;
	        } else {
	        }
	        if(randomGustY > height/2) {
	          randomGustYvel = randomGustYvel * -1;
	        }
	        */
	        
	      randomGust--;
	    }
	  }
	}


	class particle 
	{
	  float x;
	  float y;
	  float xvel;
	  float yvel;
	  float temp;
	  int pos;
	  int addColour;
	  int lifeCycles;
	  
	  particle(float xIn, float yIn, int ac, int l) {
	    x = xIn;
	    y = yIn;
	    addColour = ac;
	    lifeCycles = l;
	  }

	  void reposition() {
	    x = width/2+random(-20,20);
	    y = random(height-10,height);

	    xvel = random(-1,1);
	    yvel = random(-1,1);
	  }

	  void updatepos(vsquare[][] v, int res, int lwidth, int lheight) {
	    if(lifeCycles-- > 0){
	      int vi = (int)(x/res);
	      int vu = (int)(y/res);

	      if(vi > 0 && vi < lwidth && vu > 0 && vu < lheight) {
	    
	        // color adding
	        v[vi][vu].addcolour(addColour);

	        float ax = (x%res)/res;
	        float ay = (y%res)/res;

	        xvel += (1-ax)*v[vi][vu].xvel*0.05;
	        yvel += (1-ay)*v[vi][vu].yvel*0.05;

	        xvel += ax*v[vi+1][vu].xvel*0.05;
	        yvel += ax*v[vi+1][vu].yvel*0.05;

	        xvel += ay*v[vi][vu+1].xvel*0.05;  
	        yvel += ay*v[vi][vu+1].yvel*0.05;

	        v[vi][vu].yvel -= (1-ay)*0.003;
	        v[vi+1][vu].yvel -= ax*0.003;

	        if(v[vi][vu].yvel < 0) v[vi][vu].yvel *= 1.00025;

	        x += xvel;  
	        y += yvel;
	      } 
	      else {
	        reposition();
	      }
	      if(random(0,400) < 1) {
	        reposition();
	      }
	      xvel *= 0.6;
	      yvel *= 0.6;
	    }
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

	  void updatebuf(vsquare[][] v, int lwidth, int lheight, int i, int u) {
	    if(i>0 && i<lwidth && u>0 && u<lheight) {
	      pressurex = (v[i-1][u-1].xvel*0.5 + v[i-1][u].xvel + v[i-1][u+1].xvel*0.5 - v[i+1][u-1].xvel*0.5 - v[i+1][u].xvel - v[i+1][u+1].xvel*0.5);
	      pressurey = (v[i-1][u-1].yvel*0.5 + v[i][u-1].yvel + v[i+1][u-1].yvel*0.5 - v[i-1][u+1].yvel*0.5 - v[i][u+1].yvel - v[i+1][u+1].yvel*0.5);
	      pressure = (pressurex + pressurey)*0.25;
	    }
	  }
	}

	class vsquare {
	  int x;
	  int y;
	  float xvel;
	  float yvel;
	  float col;
	  float colorShift = 0; // til 2
	  float colorBlend = 1; // til 0
	  int colorOld = -1;

	  vsquare(int xIn,int yIn) {
	    x = xIn;
	    y = yIn;
	  }

	  void addbuffer(vbuffer[][] vbuf, int lwidth, int lheight, int i, int u) {
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

	  void updatevels(randomGust rg, int penSize, int res, int mvelX, int mvelY) {
	    float adj;
	    float opp;
	    float dist;
	    float mod;

	    if(rg.randomGust > 0) {
	      adj = x - rg.randomGustX;
	      opp = y - rg.randomGustY;
	      dist = sqrt(opp*opp + adj*adj);
	      if(dist < rg.randomGustSize) {
	        if(dist < res*2) dist = rg.randomGustSize;
	        mod = rg.randomGustSize/dist;
	        xvel += (rg.randomGustMax-rg.randomGust)*rg.randomGustXvel*mod;
	        yvel += (rg.randomGustMax-rg.randomGust)*rg.randomGustYvel*mod;
	      }
	    }
	    xvel *= 0.99;
	    yvel *= 0.98;
	  }

	  void addcolour(int amt) {
	    col += amt;
	    if(col > 196) col = 196;
	  }

	  void display(int i, int u, int lwidth, int lheight, vsquare[][] v, int res) {
	    float tcol = 0;
	    if(i>0 && i<lwidth-1 && u>0 && u<lheight-1) {

	      tcol = (+ v[i][u+1].col
	      + v[i+1][u].col
	      + v[i+1][u+1].col*0.5
	      )*0.3;
	      tcol = (int)(tcol+col*0.5);
	    }

	    int c = int((255 - (128 * colorShift) + (tcol * (colorShift - 1))) * colorBlend);
	    if(c != colorOld){
	      fill(c, c, c);
	      //fill(255-tcol, 255-tcol, 255-tcol);
	      rect(x, y, res, res);
	    }
	    col = 0;
	    colorOld = c;
	  }
	  
	  void shift(float step){
	    colorShift += step;
	    if(colorShift > 2) colorShift = 2;
	  }
	  
	  void blend(float step){
	    colorBlend += step;
	    if(colorBlend < 0) colorBlend = 0;
	  }
	}

	class myImage
	{
	  float[][] pixel;
	  int myWidth;
	  int myHeight;
	  
	  myImage(int tWidth, int tHeight){
	    pixel = new float[tWidth * tHeight][3];
	    myWidth = tWidth;
	    myHeight = tHeight;
	  }
	  
	  int getHeight(){
	    return myHeight;
	  }
	  
	  int getWidth(){
	    return myWidth;
	  }
	  
	  color getPixel(int i){
	    return color(pixel[i][0], pixel[i][1], pixel[i][2]);
	  }
	  
	  void setPixel(int i, color c){
	    pixel[i][0] = red(c);
	    pixel[i][1] = green(c);
	    pixel[i][2] = blue(c);
	  }
	  
	  void copyImage(BImage original){
	    int workingWidth = (original.width < myWidth) ? original.width : myWidth;
	    int workingHeight = (original.height < myHeight) ? original.height : myHeight;
	    for(int j=0; j < workingHeight; j++) {
	      for(int i=0; i < workingWidth; i++) {
	        color c = original.pixels[j * original.width + i];
	        setPixel(j * myWidth + i , c);
	      }
	    }
	  }

	  void display(int x, int y){  
	    for(int j=0; j < myHeight; j++) {
	      for(int i=0; i < myWidth; i++) {
	        pixels[(y + j)*width + (x + i)] = color(pixel[i + j * myWidth][0], pixel[i + j * myWidth][1], pixel[i + j * myWidth][2]);
	      }
	    }
	  }
	  
	  void display(int x, int y, int frame, int pixelSize){
	    for(int j=frame; j < (myHeight - frame); j++) {
	      for(int i=frame; i < (myWidth - frame); i++) {
	        fill(pixel[i + j * myWidth][0], pixel[i + j * myWidth][1], pixel[i + j * myWidth][2]);
	        rect(x + i * pixelSize, y + j * pixelSize, pixelSize, pixelSize);
	      }
	    }
	  }
	}

	class interpolator{
	  float myMin;
	  float myMax;
	  float myStepSize;
	  float returnVal;

	  interpolator(float start, float stop, int step){
	    myMin = start;
	    myMax = stop;
	    myStepSize = float((myMax - myMin) / step);
	    returnVal = myMin;
	  }
	  
	  float nextStep(){
	    returnVal += myStepSize;
	    return (returnVal < myMax) ? returnVal : myMax;
	  }
	}


	class curtain{
	  int[] posX;
	  int[] posY;
	  
	  curtainParticle[] p;

	  int columnNumber;
	  int rowNumber;

	  // TL, DL, TR, DR
	  curtain(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, int colNum, int rowNum, float[] colorStrart, float[] colorStop){
	    int [] pX = {x1, x2, x3, x4};
	    int [] pY = {y1, y2, y3, y4};
	    posX = pX;
	    posY = pY;
	    columnNumber = colNum;
	    rowNumber = rowNum;
	    
	    int age = 10;
	    int step = 15;
	    int speed = 2;
	    int randomSize = 25;
	    
	    p = new curtainParticle[columnNumber * rowNumber];
	    for(int i=0; i < columnNumber; i++){
	      for(int j=0; j < rowNumber; j++){
	        float[] pos1 = getPos( j, i);
	        float[] pos2 = getPos( j + 1, i);
	        float[] pos3 = getPos( j + 1, i + 1);
	        float[] pos4 = getPos( j, i + 1);
	        p[j + i * rowNumber] = new curtainParticle(pos1[0], pos1[1], pos2[0], pos2[1], pos3[0], pos3[1], pos4[0], pos4[1], age + int(sqrt(j)) + int(random(randomSize)), colorStrart, colorStop, step);
	      }
	    }
	  }
	 
	  void display(){
	    for(int i=0; i < columnNumber; i++){
	      for(int j=0; j < rowNumber; j++){
	        p[j + i * rowNumber].update(int(random(3)));
	        p[j + i * rowNumber].display();
	      }
	    }
	  }
	    
	  float[] getPos(int row, int col){
	    float[] p1 = getInterPos(posX[0], posY[0], posX[1], posY[1], rowNumber, row);
	    float[] p2 = getInterPos(posX[2], posY[2], posX[3], posY[3], rowNumber, row);
	    return getInterPos(p1[0], p1[1], p2[0], p2[1], columnNumber, col);
	  }
	  
	  float[] getInterPos(float x1, float y1, float x2, float y2, int seg, int step){
	    float[] x = {interpolate(x1, x2, seg, step), interpolate(y1, y2, seg, step)};
	    return x;
	  }
	    
	  float interpolate(float val1, float val2, int seg, int step){
	    if(step < seg){
	      return val1 + (val2 - val1) / seg * step;
	    } else {
	      return val2;
	    }
	  }
	}

	class curtainParticle 
	{
	  float[] posX;
	  float[] posY;
	  
	  float[] colorStart;
	  float[] colorStop;
	  int stepSize;
	  int myAge;
	  int dying;
	  boolean death;
	  
	  color c;
	  color oldC;
	  
	  curtainParticle(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int age, float[] colStart, float[] colStop, int step) {
	    float [] pX = {x1, x2, x3, x4};
	    float [] pY = {y1, y2, y3, y4};
	    posX = pX;
	    posY = pY;
	    colorStart = colStart;
	    colorStop = colStop;
	    stepSize = step;
	    myAge = age;
	    dying = 0;
	    death = false;
	    oldC = color(0,0,0);
	  }
	  
	  void update(int jump){
	    if(myAge-- > 0){
	      c = color(colorStart[0], colorStart[1], colorStart[2]);
	    } else if(death){
	      c = color(colorStop[0], colorStop[1], colorStop[2]);
	    } else {
	      if(dying++ < stepSize) {
	        c = color(interpolate(colorStart[0], colorStop[0], stepSize, dying + jump),
	                  interpolate(colorStart[1], colorStop[1], stepSize, dying + jump),
	                  interpolate(colorStart[2], colorStop[2], stepSize, dying + jump));
	      } else {
	        death = true;
	      }
	    } 
	  }
	    
	  void display(){
	    if(oldC != c){
	      noStroke();
	      scale(1);
	      fill(c); 
	      beginShape(POLYGON);
	      vertex(posX[0], posY[0]);
	      vertex(posX[1], posY[1]);
	      vertex(posX[2], posY[2]);
	      vertex(posX[3], posY[3]);
	      endShape();
	    }
	    oldC = c;
	  }

	  float interpolate(float val1, float val2, int seg, int step){
	    if(step < seg){
	      return val1 + (val2 - val1) / seg * step;
	    } else {
	      return val2;
	    }
	  }

	}

	static public void main(String[] passedArgs) {
	    String[] appletArgs = new String[] { "ParticleSystem" };
	    if (passedArgs != null) {
	      PApplet.main(concat(appletArgs, passedArgs));
	    } else {
	      PApplet.main(appletArgs);
	    }
	  }

}
