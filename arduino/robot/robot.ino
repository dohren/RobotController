/**********************************************************************
  Filename    : SerialToSerialBT
  Description : ESP32 communicates with the phone by bluetooth and print phone's data via a serial port
  Auther      : www.freenove.com
  Modification: 2020/07/11
**********************************************************************/
#include "BluetoothSerial.h"

//
//const int l1=16,l2=17,r1=18,r2=5,speedl=19,speedr=4;
const int r1=17,r2=16,l1=5,l2=18,speedr=4,speedl=19;
const byte numChars = 32;
char receivedChars[numChars];

boolean newData = false;

const int freq = 30000;
const int pwmChannell = 1;
const int pwmChannelr = 0;
const int resolution = 8;
int dutyCyclel = 200;


BluetoothSerial SerialBT;

void setup() {
  Serial.begin(19200);
  SerialBT.begin("ESP32test"); //Bluetooth device name
  Serial.println("\nThe device started, now you can pair it with bluetooth!");

  pinMode(l1,OUTPUT);   //left motors forward
  pinMode(l2,OUTPUT);
  pinMode(r1,OUTPUT);   //right motors forward
  pinMode(r2,OUTPUT);   //right motors reverse
  pinMode(speedl,OUTPUT);
  pinMode(speedr,OUTPUT);


  // configure LED PWM functionalitites
  ledcSetup(pwmChannell, freq, resolution);
  ledcSetup(pwmChannelr, freq, resolution);

  // attach the channel to the GPIO to be controlled
  ledcAttachPin(speedl, pwmChannell);
  ledcAttachPin(speedr, pwmChannelr);

}

void loop() {
  recvWithStartEndMarkers();
  showNewData();

  if (Serial.available()) {
    SerialBT.write(Serial.read());
  }
  if (SerialBT.available()) {
    Serial.write(SerialBT.read());
  }
  delay(100);
}


void recvWithStartEndMarkers() {
    static boolean recvInProgress = false;
    static byte ndx = 0;
    char startMarker = '{';
    char endMarker = '}';
    char rc;

    while (SerialBT.available() > 0 && newData == false) {
        rc = SerialBT.read();
        if (recvInProgress == true) {
            if (rc != endMarker) {
                receivedChars[ndx] = rc;
                ndx++;
                if (ndx >= numChars) {
                    ndx = numChars - 1;
                }
            }
            else {
                receivedChars[ndx] = '\0'; // terminate the string
                recvInProgress = false;
                ndx = 0;
                newData = true;
            }
        }

        else if (rc == startMarker) {
            recvInProgress = true;
        }
    }
}


void showNewData() {
    if (!newData) return;

    newData = false;
    String left = getValue(receivedChars, ',', 0);
    String right = getValue(receivedChars, ',', 1);
    int leftspeed = left.toInt();
    int rightspeed = right.toInt();

    drive(leftspeed, rightspeed);

}

void drive(int left, int right) {


  if (left == 0) {
     ledcWrite(pwmChannell, 0);
     //analogWrite(speedl, 0);
  }
  else if (left > 0) {
    digitalWrite(l1,LOW);
    digitalWrite(l2,HIGH);
    ledcWrite(pwmChannell, left+150);
    //analogWrite(speedl, left+50);
  }
  else {
    digitalWrite(l1,HIGH);
    digitalWrite(l2,LOW);
    ledcWrite(pwmChannell, -left+150);
    //(speedl, -left+50);
  }

  if (right == 0) {
    ledcWrite(pwmChannelr, 0);
    //analogWrite(speedr, 0);
  }
  else if (right > 0) {
    digitalWrite(r1,LOW);
    digitalWrite(r2,HIGH);
    ledcWrite(pwmChannelr, right+150);
    //analogWrite(speedr, right+50);
  }
  else {
    digitalWrite(r1,HIGH);
    digitalWrite(r2,LOW);
    ledcWrite(pwmChannelr, -right+150);
    //analogWrite(speedr, -right+50);
  }
  delay(100);
}

String getValue(String data, char separator, int index)
{
    int found = 0;
    int strIndex[] = { 0, -1 };
    int maxIndex = data.length() - 1;

    for (int i = 0; i <= maxIndex && found <= index; i++) {
        if (data.charAt(i) == separator || i == maxIndex) {
            found++;
            strIndex[0] = strIndex[1] + 1;
            strIndex[1] = (i == maxIndex) ? i+1 : i;
        }
    }
    return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}
