
#define BTSerial Serial
#define DebugSerial Serial

// первый двигатель
const int enA = 11;
const int in1 = 10;
const int in2 = 9;

// второй двигатель
const int enB = 6;
const int in3 = 8;
const int in4 = 7;

const char* ssid = "MERAMAX";
const char* password = "89625361161";

typedef enum {FORW, REW} DIRECTION;

void runMotor(int motorNum, DIRECTION dir, int rotSpeed) {
  if (motorNum == 1) {
    // запуск двигателя
    if (dir == FORW) {
      digitalWrite(in1, HIGH);
      digitalWrite(in2, LOW);
    } else {
      digitalWrite(in1, LOW);
      digitalWrite(in2, HIGH);      
    }
    // устанавливаем скорость
    analogWrite(enA, rotSpeed);      
  } else if (motorNum == 2) {
    // запуск двигателя
    if (dir == FORW) {
      digitalWrite(in3, HIGH);
      digitalWrite(in4, LOW);
    } else {
      digitalWrite(in3, LOW);
      digitalWrite(in4, HIGH);      
    }
    // устанавливаем скорость
    analogWrite(enB, rotSpeed);      
  }  
}

void stopMotor(int motorNum) {
  switch (motorNum) {
    case 1:
      digitalWrite(in1, LOW);
      digitalWrite(in2, LOW);
      break;
    case 2:
      digitalWrite(in3, LOW);
      digitalWrite(in4, LOW);
      break;
  }      
}

int convertSpeedLevel(int speedLevel) {
  return (int)(25.5*(1 + speedLevel));
}

void moveForward(int rotSpeed) {
  DebugSerial.print("moveForward, speed = "); DebugSerial.println(rotSpeed);
  runMotor(1, FORW, rotSpeed);
  runMotor(2, FORW, rotSpeed);
}

void moveBack(int rotSpeed) {
  //DebugSerial.print("moveForward, speed = "); DebugSerial.println(rotSpeed);
  runMotor(1, REW, rotSpeed);
  runMotor(2, REW, rotSpeed);
}

void moveRight(int rotSpeed) {
  stopMotor(2);
  runMotor(1, FORW, rotSpeed);
}

void moveLeft(int rotSpeed) {
  stopMotor(1);
  runMotor(2, FORW, rotSpeed);
}

void stopMotion() {
  stopMotor(1);
  stopMotor(2);
}

void setup() {
  // инициализируем все пины для управления двигателями как outputs
  pinMode(enA, OUTPUT);
  pinMode(enB, OUTPUT);
  pinMode(in1, OUTPUT);
  pinMode(in2, OUTPUT);
  pinMode(in3, OUTPUT);
  pinMode(in4, OUTPUT);
  
  DebugSerial.begin(9600);
  BTSerial.begin(9600);
  BTSerial.setTimeout(10);
  delay(10);

  //DebugSerial.println("setup");
 
}
int speedLevel;
String cmdStr;

void loop() {
  if (BTSerial.available()) {
    cmdStr = BTSerial.readString();
    
    if (cmdStr[0] == 'f') {
      stopMotion();
      speedLevel = cmdStr[1] - '0'; // получаем скорость от 0 до 9 в виде символа и переводим в целое число
      if (speedLevel < 0 || speedLevel > 9) speedLevel = 4;
      moveForward(convertSpeedLevel(speedLevel));
    } else if (cmdStr[0] == 'b') {
      stopMotion();
      speedLevel = cmdStr[1] - '0'; // получаем скорость от 0 до 9 в виде символа и переводим в целое число
      if (speedLevel < 0 || speedLevel > 9) speedLevel = 4;
      moveBack(convertSpeedLevel(speedLevel));
    } else if (cmdStr[0] == 's') {
      stopMotion();
    }
  }  
  /*Serial.println("runMotor");
  runMotor(1, FORW, 200);
  delay(2000);
  stopMotor(1);
  delay(2000);  */
}
