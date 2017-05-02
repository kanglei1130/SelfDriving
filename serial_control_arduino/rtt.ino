#include <Servo.h>


/**
 * battery type: LiPo
 * charging: long press start and then press start (one time)
 */

Servo throttleOut;
Servo steeringOut;
int throttlePin = 5;
int steeringPin = 2;
int lastButtonState = 0;
int buttonState = 0; 

void setup() {
  Serial.begin(115200);
  throttleOut.attach(throttlePin);
  steeringOut.attach(steeringPin);
  while(!Serial); //Wait till serial connection is ready
  //Serial.println("Serial ready"); //Debugging
  Serial.setTimeout(0);
/*
  pinMode(16, OUTPUT);
  digitalWrite(16, 0);
  pinMode(12, INPUT_PULLUP);
  pinMode(13, OUTPUT);
  digitalWrite(13, 1);
  */
}


String buffer = "";
void loop() {

  if(Serial.available() > 0) {
     String input = Serial.readString();//read input
     buffer += input;
    
  } 
  if(buffer.length() >= 7) {
    Serial.write("1234");
    
    buffer = buffer.substring(7);
  }
}
