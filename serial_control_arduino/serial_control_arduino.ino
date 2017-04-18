#include <Servo.h>


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
  Serial.println("Serial ready"); //Debugging

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
      Serial.println("invalid steering angle");
    }
  } else if(input.startsWith("throttle")) {
    int left = input.indexOf('(');
    int right = input.indexOf(')');
    String value = input.substring(left + 1, right);
    float pos = value.toFloat();
    if(pos >= 0.0 & pos <= 1.2) {
      writeThrottle(pos);
    } else {
      Serial.println("invalid throttle value");
    }
  } else {
    Serial.println("unknown command");
  } 
}

String buffer = "";
void loop() {
 if(Serial.available()) {
     String input = Serial.readString();//read input
     buffer += input;
     //separte by '\n'

     int nline = buffer.indexOf("\n");     
     while(nline > 0) {
        String comd = buffer.substring(0, nline);
        buffer = buffer.substring(nline + 1);
        handleNewLine(comd);
        nline = buffer.indexOf("\n");
     }
  }  

  //Hall sensor read wheel rotation
  buttonState = digitalRead(12);
  if(buttonState != lastButtonState) {
    if(buttonState==1 && lastButtonState==0){
    Serial.println("rotation(1.0)");
      }
    }
   else {
    }
    lastButtonState = buttonState;
}
