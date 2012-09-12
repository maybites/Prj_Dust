/*
 Fade
 
 This example shows how to fade an LED on pin 9
 using the analogWrite() function.
 
 This example code is in the public domain.
 */

int led = 10;           // the pin that the LED is attached to
int brightness = 255;    // how bright the LED is

// the setup routine runs once when you press reset:
void setup()  { 
   // initialize serial:
  Serial.begin(9600);
 
  // declare pin 9 to be an output:
  pinMode(led, OUTPUT);
} 

// the loop routine runs over and over again forever:
void loop()  { 
    // if there's any serial available, read it:
  while (Serial.available() > 0) {

    brightness =  Serial.parseInt();
    brightness = random(255);
    
  }  
  
  analogWrite(led, brightness);    

}

