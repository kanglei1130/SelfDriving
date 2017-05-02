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

  Serial.setTimeout(0);
  //Serial.println("Serial ready"); //Debugging

  pinMode(16, OUTPUT);
  digitalWrite(16, 0);
  pinMode(12, INPUT_PULLUP);
  pinMode(13, OUTPUT);
  digitalWrite(13, 1);
}

//steering(0.0) to steering(1.0): left to right
void writeSteering(float s) {
  steeringOut.write(s * 180);
}
//throttle(0.0) to throttle(1.0): 
void writeThrottle(float t) {
  throttleOut.write(90 + (t - .5) * -15); 
} 

//steering(0.5) = straight
//steering(1.0) = right
//steering(0.0) = left
void handleNewLine(String input) {
  if(input.startsWith("steering")) {
    int left = input.indexOf('(');
    int right = input.indexOf(')');
    String value = input.substring(left + 1, right);
    double pos = value.toFloat();
    if(pos >= 0.0 & pos <= 1.0) {
      writeSteering(pos);
    } else {
      Serial.write("invalid steering angle");
    }
  } else if(input.startsWith("throttle")) {
    int left = input.indexOf('(');
    int right = input.indexOf(')');
    String value = input.substring(left + 1, right);
    float pos = value.toFloat();
    if(pos >= 0.0 & pos <= 1.2) {
      writeThrottle(pos);
    } else {
      Serial.write("invalid throttle value");
    }
  } else if(input.startsWith("time")) {
    Serial.println(input); 
  } else {
    Serial.write("unknown command");
  } 
}

String buffer = "";
void loop() {
  if(Serial.available() > 0) {
     String input = Serial.readString();//read input
     buffer += input;
  }  
  int nline = buffer.indexOf("\n");     
  if(nline > 0) {
     String comd = buffer.substring(0, nline);
     buffer = buffer.substring(nline + 1);
     handleNewLine(comd);
  }

  //Hall sensor read wheel rotation
  buttonState = digitalRead(12);
  if(buttonState != lastButtonState) {
    if(buttonState==1 && lastButtonState==0){
      Serial.write("rotation(1.0)");
    }
  } else {

  }
  lastButtonState = buttonState;
}
