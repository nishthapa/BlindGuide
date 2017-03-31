// ---------------------------------------------------------------------------
// Code developed by Handshke Inc.
// ---------------------------------------------------------------------------

#include <NewPing.h>
#include <SoftwareSerial.h>

#define TRIGGER_PIN  A2  // Arduino pin tied to trigger pin on the ultrasonic sensor.
#define ECHO_PIN     A3  // Arduino pin tied to echo pin on the ultrasonic sensor.
#define MAX_DISTANCE 200 // Maximum distance we want to ping for (in centimeters). Maximum sensor distance is rated at 400-500cm.
#define TX A4
#define RX A5

int DISTANCE = 0;
String DISTANCE_STR;

SoftwareSerial bluetooth (TX, RX);
NewPing sonar(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE); // NewPing setup of pins and maximum distance.

void setup()
{
  Serial.begin(9600); // Open serial monitor at 115200 baud to see ping results.
  bluetooth.begin(9600);
}

void loop()
{
  delay(50);                     // Wait 50ms between pings (about 20 pings/sec). 29ms should be the shortest delay between pings.
  //Serial.print("Ping: ");
  DISTANCE = sonar.ping_cm();
  //Serial.print(sonar.ping_cm()); // Send ping, get distance in cm and print result (0 = outside set distance range)
  //Serial.println(DISTANCE);
  if(DISTANCE > 150)
  {
    DISTANCE_STR = "NO OBSTACLE AHEAD";
  }
  else if(DISTANCE < 150)
  {
    if(DISTANCE > 50)
    {
      DISTANCE_STR = "OBSTACLE APPROACHING";
      //DISTANCE_STR = (String)DISTANCE;
    }
    else if(DISTANCE >= 1 && DISTANCE <= 50)
    {
      DISTANCE_STR = "OBSTACLE AHEAD,\n TURN LEFT OR RIGHT";
    }
    else if(DISTANCE == 0)
    {
      DISTANCE_STR = "NO OBSTACLE AHEAD";
    }
  }
  bluetooth.println(DISTANCE_STR);
  Serial.println(DISTANCE_STR);
  delay(100);
  
  //Serial.println("cm");
}
