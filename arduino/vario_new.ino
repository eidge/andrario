#include <Wire.h>
#include <tone.h>

#include <MS561101BA.h>

MS561101BA baro = MS561101BA();

unsigned long time = 0, old_time = 0, buzzer_timer, buzzer_sleep = 0;
float alt, old_alt, dif_alt, vario = 0, sum_vario = 0, last_vario;
int cycle_count = 1;

float buzz_silence;
long buzz_bip;

//----------------------------------//
//---------- Vario Config ----------//
//----------------------------------//
const float sea_press = 1013.25;    //
                                    //
// Barometer                        //
const float sensitivity = 0.02;     //
const int avg_cycles = 5;           //
                                    //
// Buzzer                           //
const short buzzer_pin = 7;         //
                                    //
const float up_limit = 0.2;         //
const float down_limit = -3.0;      //
                                    //
const float up_freq = 2000;         //
const float down_freq = 100;        //
                                    //
const int time_unit = 1000;         //
const float silence_ratio = 0.3;    //
const int freq_mult = 100;          //
//----------------------------------//
//----------------------------------//
//----------------------------------//

int x = 0;


void setup() {
  Wire.begin();
  Serial.begin(9600);
  delay(1000);

  baro.init(MS561101BA_ADDR_CSB_LOW);
  buzzer_timer = millis();
}

void loop() {
  sum_vario += update_vario();
  
  //Average avg_cycles readings before printing
  if(cycle_count == avg_cycles){
    last_vario = sum_vario / avg_cycles;
    
    //Cheat for a more stable zero
    if(last_vario < 0.09 && last_vario > - 0.09)
      last_vario = 0.0;
    
    //Passar last_vario -15:0.1:10.5 a 0:1:255
    Serial.write(varioFloatToByte(last_vario));

    cycle_count = 0;
    sum_vario = 0.0;
  }

  ++cycle_count;
  
  
  
  //Buzzer
  
  //Make vario vary between -15 and 10.5
  if(last_vario > 10.5)
    last_vario = 10.5;
  else if(last_vario < -15)
    last_vario = -15;
    
  long current_time = millis();
  buzz_silence = getSilenceLen(last_vario);
  buzz_bip      = getBipLen(last_vario);
                              //Set silent pauses
  if(last_vario > up_limit && buzzer_timer + buzz_silence + buzz_bip < current_time){
    tone(buzzer_pin, up_freq + last_vario * freq_mult, buzz_bip);
    buzzer_timer = current_time;
  }
  else if(last_vario < down_limit){
    tone(buzzer_pin, down_freq + last_vario * freq_mult/100);
  }
  else{
  noTone(buzzer_pin);
  }
  
}

float getAltitude(float press, float temp) {
  //return (1.0f - pow(press/101325.0f, 0.190295f)) * 4433000.0f;
  return ((pow((sea_press / press), 1/5.257) - 1.0) * (temp + 273.15)) / 0.0065;
}

float update_vario(){
  float temperature = NULL, pression = NULL;
  unsigned long dif_time;
  
  //Get temperature
  while(temperature == NULL) {
    temperature = baro.getTemperature(MS561101BA_OSR_4096);
  }
  
  //Get pressure
  while(pression == NULL) {
    pression = baro.getPressure(MS561101BA_OSR_4096);
  }
 
  alt = getAltitude(pression, temperature);
  time = millis();

  dif_alt  = alt - old_alt;
  dif_time = time - old_time;
  
  vario = (1 - sensitivity) * vario + sensitivity * dif_alt/dif_time*1000;
  
  old_time = time;
  old_alt  = alt;
  
  return vario;
}

byte varioFloatToByte(float vario_measure){
  if(vario_measure > 10.5)
    vario_measure = 10.5;
  if(vario_measure < -15)
    vario_measure = -15;
    
  return (vario_measure+15)/25.5*255;
}

int getBipLen(float vario_reading){
  return time_unit/(vario_reading + silence_ratio);
}

int getSilenceLen(float vario_reading){
  return (time_unit-getBipLen(vario_reading)*vario_reading)/vario_reading;
}


