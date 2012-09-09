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


public class SypClient  extends PApplet{

	PImage img;
	
	SyphonClient client;

	public void setup() {
	  size(480, 340, P3D);
	  
	  // Create syhpon client to receive frames 
	  // from running server with given name: 
	  client = new SyphonClient(this, "KinectDepth");
	  
	  background(0);
	}

	public void draw() {  
	  if (client.available()) {
	    // The first time getImage() is called with 
	    // a null argument, it will initialize the PImage
	    // object with the correct size.
	    img = client.getImage(img); // load the pixels array with the updated image info (slow)
	    //img = client.getImage(img, false); // does not load the pixels array (faster)
	    image(img, 0, 0);
	  }
	}

	public void keyPressed() {
	  if (key == ' ') {
	    client.stop();  
	  } else if (key == 'd') {
	    println(client.description());
	  }
	}
	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "ch.maybites.prj.dust.sim.SypClient" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}

}
