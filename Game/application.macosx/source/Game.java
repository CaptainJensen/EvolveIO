import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.io.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Game extends PApplet {


class Axon{
  final double MUTABILITY_MUTABILITY = 0.7f;
  final int mutatePower = 9;
  final double MUTATE_MULTI;
  
  double weight;
  double mutability;
  public Axon(double w, double m){
    weight = w;
    mutability = m;
    MUTATE_MULTI = Math.pow(0.5f,mutatePower);
  }
  
  public Axon mutateAxon(){
    double mutabilityMutate = Math.pow(0.5f,pmRan()*MUTABILITY_MUTABILITY);
    return new Axon(weight+r()*mutability/MUTATE_MULTI,mutability*mutabilityMutate);
  }
  public double r(){
    return Math.pow(pmRan(),mutatePower);
  }
  public double pmRan(){
    return (Math.random()*2-1);
  }
}


class Board{
  int boardWidth;
  int boardHeight;
  int creatureMinimum;
  Tile[][] tiles;
  double year = 0;
  float MIN_TEMPERATURE;
  float MAX_TEMPERATURE;
  final float THERMOMETER_MIN = -2;
  final float THERMOMETER_MAX = 2;
  final int ROCKS_TO_ADD;
  final float MIN_ROCK_ENERGY_BASE = 0.8f;
  final float MAX_ROCK_ENERGY_BASE = 1.6f;
  final float MIN_CREATURE_ENERGY = 1.2f;
  final float MAX_CREATURE_ENERGY = 2.0f;
  final float ROCK_DENSITY = 5;
  final float OBJECT_TIMESTEPS_PER_YEAR = 100;
  final int ROCK_COLOR = color(0,0,0.5f);
  final int BACKGROUND_COLOR = color(0,0,0.1f);
  final float MINIMUM_SURVIVABLE_SIZE = 0.06f;
  final float CREATURE_STROKE_WEIGHT = 0.6f;
  ArrayList[][] softBodiesInPositions;
  ArrayList<SoftBody> rocks;
  ArrayList<Creature> creatures;
  Creature selectedCreature = null;
  int creatureIDUpTo = 0;
  float[] letterFrequencies = {8.167f,1.492f,2.782f,4.253f,12.702f,2.228f,2.015f,6.094f,6.966f,0.153f,0.772f,4.025f,2.406f,6.749f,
  7.507f,1.929f,0.095f,5.987f,6.327f,9.056f,2.758f,0.978f,2.361f,0.150f,1.974f,10000.0f};//0.074};
  final int LIST_SLOTS = 6;
  int creatureRankMetric = 0;
  int buttonColor = color(0.82f,0.8f,0.7f);
  Creature[] list = new Creature[LIST_SLOTS];
  final int creatureMinimumIncrement = 5;
  String folder = "TEST";
  int[] fileSaveCounts;
  double[] fileSaveTimes;
  double imageSaveInterval = 1;
  double textSaveInterval = 1;
  final double FLASH_SPEED = 80;
  boolean userControl;
  double temperature;
  double MANUAL_BIRTH_SIZE = 1.2f;
  boolean wasPressingB = false;
  double timeStep; 
  int POPULATION_HISTORY_LENGTH = 200;
  int[] populationHistory;
  double recordPopulationEvery = 0.02f;
  int playSpeed = 1;
  public int threadsToFinish = 0;
 
  public Board(int w, int h, float stepSize, float min, float max, int rta, int cm, int SEED, String INITIAL_FILE_NAME, double ts){
    noiseSeed(SEED);
    randomSeed(SEED);
    boardWidth = w;
    boardHeight = h;
    tiles = new Tile[w][h];
    for(int x = 0; x < boardWidth; x++){
      for(int y = 0; y < boardHeight; y++){
        float bigForce = pow(((float)y)/boardHeight,0.5f);
        float fertility = noise(x*stepSize*3,y*stepSize*3)*(1-bigForce)*5.0f+noise(x*stepSize*0.5f,y*stepSize*0.5f)*bigForce*5.0f-1.5f;
        float climateType = noise(x*stepSize*0.2f+10000,y*stepSize*0.2f+10000)*1.63f-0.4f;
        climateType = min(max(climateType,0),0.8f);
        tiles[x][y] = new Tile(x,y,fertility,0,climateType,this);
      }
    }
    MIN_TEMPERATURE = min;
    MAX_TEMPERATURE = max;
    
    softBodiesInPositions = new ArrayList[boardWidth][boardHeight];
    for(int x = 0; x < boardWidth; x++){
      for(int y = 0; y < boardHeight; y++){
        softBodiesInPositions[x][y] = new ArrayList<SoftBody>(0);
      }
    }
    
    ROCKS_TO_ADD = rta;
    rocks = new ArrayList<SoftBody>(0);
    for(int i = 0; i < ROCKS_TO_ADD; i++){
      rocks.add(new SoftBody(random(0,boardWidth),random(0,boardHeight),0,0,
      getRandomSize(),ROCK_DENSITY,hue(ROCK_COLOR),saturation(ROCK_COLOR),brightness(ROCK_COLOR),this,year));
    }
    
    creatureMinimum = cm;
    creatures = new ArrayList<Creature>(0);
    maintainCreatureMinimum(false);
    for(int i = 0; i < LIST_SLOTS; i++){
      list[i] = null;
    }
    folder = INITIAL_FILE_NAME;
    fileSaveCounts = new int[4];
    fileSaveTimes = new double[4];
    for(int i = 0; i < 4; i++){
      fileSaveCounts[i] = 0;
      fileSaveTimes[i] = -999;
    }
    userControl = true;
    timeStep = ts;
    populationHistory = new int[POPULATION_HISTORY_LENGTH];
    for(int i = 0; i < POPULATION_HISTORY_LENGTH; i++){
      populationHistory[i] = 0;
    }
  }
  public void drawBoard(float scaleUp, float camZoom, int mX, int mY){
    for(int x = 0; x < boardWidth; x++){
      for(int y = 0; y < boardHeight; y++){
        tiles[x][y].drawTile(scaleUp, (mX == x && mY == y));
      }
    }
    for(int i = 0; i < rocks.size(); i++){
      rocks.get(i).drawSoftBody(scaleUp);
    }
    for(int i = 0; i < creatures.size(); i++){
      creatures.get(i).drawSoftBody(scaleUp, camZoom,true);
    }
  }
  public void drawBlankBoard(float scaleUp){
    fill(BACKGROUND_COLOR);
    rect(0,0,scaleUp*boardWidth,scaleUp*boardHeight);
  }
  public void drawUI(float scaleUp,double timeStep, int x1, int y1, int x2, int y2, PFont font){
    fill(0,0,0);
    noStroke();
    rect(x1,y1,x2-x1,y2-y1);
    
    pushMatrix();
    translate(x1,y1);
    
    fill(0,0,1);
    textAlign(LEFT);
    textFont(font,48);
    String yearText = "Year "+nf((float)year,0,2);
    text(yearText,10,48);
    float seasonTextXCoor = textWidth(yearText)+50;
    textFont(font,24);
    text("Population: "+creatures.size(),10,80);
    String[] seasons = {"Winter","Spring","Summer","Autumn"};
    text(seasons[(int)(getSeason()*4)],seasonTextXCoor,30);
    
    if(selectedCreature == null){
      for(int i = 0; i < LIST_SLOTS; i++){
        list[i] = null;
      }
      for(int i = 0; i < creatures.size(); i++){
        int lookingAt = 0;
        if(creatureRankMetric == 4){
          while(lookingAt < LIST_SLOTS && list[lookingAt] != null && list[lookingAt].name.compareTo(creatures.get(i).name) < 0){
            lookingAt++;
          }
        }else if(creatureRankMetric == 5){
          while(lookingAt < LIST_SLOTS && list[lookingAt] != null && list[lookingAt].name.compareTo(creatures.get(i).name) >= 0){
            lookingAt++;
          }
        }else{
          while(lookingAt < LIST_SLOTS && list[lookingAt] != null && list[lookingAt].measure(creatureRankMetric) > creatures.get(i).measure(creatureRankMetric)){
            lookingAt++;
          }
        }
        if(lookingAt < LIST_SLOTS){
          for(int j = LIST_SLOTS-1; j >= lookingAt+1; j--){
            list[j] = list[j-1];
          }
          list[lookingAt] = creatures.get(i);
        }
      }
      double maxEnergy = 0;
      for(int i = 0; i < LIST_SLOTS; i++){
        if(list[i] != null && list[i].energy > maxEnergy){
          maxEnergy = list[i].energy;
        }
      }
      for(int i = 0; i < LIST_SLOTS; i++){
        if(list[i] != null){
          list[i].preferredRank += (i-list[i].preferredRank)*0.4f;
          float y = y1+175+70*list[i].preferredRank;
          drawCreature(list[i],45,y+5,2.3f,scaleUp);
          textFont(font, 24);
          textAlign(LEFT);
          noStroke();
          fill(0.333f,1,0.4f);
          float multi = (x2-x1-200);
          if(list[i].energy > 0){
            rect(85,y+5,(float)(multi*list[i].energy/maxEnergy),25);
          }
          if(list[i].energy > 1){
            fill(0.333f,1,0.8f);
            rect(85+(float)(multi/maxEnergy),y+5,(float)(multi*(list[i].energy-1)/maxEnergy),25);
          }
          fill(0,0,1);
          text(list[i].getCreatureName()+" ["+list[i].id+"] ("+toAge(list[i].birthTime)+")",90,y);
          text("Energy: "+nf(100*(float)(list[i].energy),0,2),90,y+25);
        }
      }
      noStroke();
      fill(buttonColor);
      rect(10,95,220,40);
      rect(240,95,220,40);
      fill(0,0,1);
      textAlign(CENTER);
      text("Reset zoom",120,123);
      String[] sorts = {"Biggest","Smallest","Youngest","Oldest","A to Z","Z to A","Highest Gen","Lowest Gen"};
      text("Sort by: "+sorts[creatureRankMetric],350,123);
      
      textFont(font,19);
      String[] buttonTexts = {"Brain Control","Maintain pop. at "+creatureMinimum,
      "Screenshot now","-   Image every "+nf((float)imageSaveInterval,0,2)+" years   +",
      "Text file now","-    Text every "+nf((float)textSaveInterval,0,2)+" years    +",
      "-    Play Speed ("+playSpeed+"x)    +","This button does nothing"};
      if(userControl){
        buttonTexts[0] = "Keyboard Control";
      }
      for(int i = 0; i < 8; i++){
        float x = (i%2)*230+10;
        float y = floor(i/2)*50+570;
        fill(buttonColor);
        rect(x,y,220,40);
        if(i >= 2 && i < 6){
          double flashAlpha = 1.0f*Math.pow(0.5f,(year-fileSaveTimes[i-2])*FLASH_SPEED);
          fill(0,0,1,(float)flashAlpha);
          rect(x,y,220,40);
        }
        fill(0,0,1,1);
        text(buttonTexts[i],x+110,y+17);
        if(i == 0){
        }else if(i == 1){
          text("-"+creatureMinimumIncrement+
          "                    +"+creatureMinimumIncrement,x+110,y+37);
        }else if(i <= 5){
          text(getNextFileName(i-2),x+110,y+37);
        }
      }
    }else{
      float energyUsage = (float)selectedCreature.getEnergyUsage(timeStep);
      noStroke();
      if(energyUsage <= 0){
        fill(0,1,0.5f);
      }else{
        fill(0.33f,1,0.4f);
      }
      float EUbar = 20*energyUsage;
      rect(110,280,min(max(EUbar,-110),110),25);
      if(EUbar < -110){
        rect(0,280,25,(-110-EUbar)*20+25);
      }else if(EUbar > 110){
        float h = (EUbar-110)*20+25;
        rect(185,280-h,25,h);
      }
      fill(0,0,1);
      text("Name: "+selectedCreature.getCreatureName(),10,225);
      text("Energy: "+nf(100*(float)selectedCreature.energy,0,2)+" yums",10,250);
      text("E Change: "+nf(100*energyUsage,0,2)+" yums/year",10,275);
      
      text("ID: "+selectedCreature.id,10,325);
      text("X: "+nf((float)selectedCreature.px,0,2),10,350);
      text("Y: "+nf((float)selectedCreature.py,0,2),10,375);
      text("Rotation: "+nf((float)selectedCreature.rotation,0,2),10,400);
      text("B-day: "+toDate(selectedCreature.birthTime),10,425);
      text("("+toAge(selectedCreature.birthTime)+")",10,450);
      text("Generation: "+selectedCreature.gen,10,475);
      text("Parents: "+selectedCreature.parents,10,500,210,255);
      text("Hue: "+nf((float)(selectedCreature.hue),0,2),10,550,210,255);
      text("Mouth hue: "+nf((float)(selectedCreature.mouthHue),0,2),10,575,210,255);
      
      if(userControl){
        text("Controls:\nUp/Down: Move\nLeft/Right: Rotate\nSpace: Eat\nF: Fight\nV: Vomit\nU,J: Change color"+
        "\nI,K: Change mouth color\nB: Give birth (Not possible if under "+Math.round((MANUAL_BIRTH_SIZE+1)*100)+" yums)",10,625,250,400);
      }
      pushMatrix();
      translate(400,80);
      float apX = round((mouseX-400-x1)/46.0f);
      float apY = round((mouseY-80-y1)/46.0f);
      selectedCreature.drawBrain(font,46,(int)apX,(int)apY);
      popMatrix();
    }
    drawPopulationGraph(x1,x2,y1,y2);
    fill(0,0,0);
    textAlign(RIGHT);
    textFont(font,24);
    text("Population: "+creatures.size(),x2-x1-10,y2-y1-10);
    popMatrix();
    
    pushMatrix();
    translate(x2,y1);
    textAlign(RIGHT);
    textFont(font,24);
    text("Temperature",-10,24);
    drawThermometer(-45,30,20,660,temperature,THERMOMETER_MIN,THERMOMETER_MAX,color(0,1,1));
    popMatrix();
    
    if(selectedCreature != null){
      drawCreature(selectedCreature,x1+65,y1+147,2.3f,scaleUp);
    }
  }
  public void drawPopulationGraph(float x1, float x2, float y1, float y2){
    float barWidth = (x2-x1)/((float)(POPULATION_HISTORY_LENGTH));
    noStroke();
    fill(0.33333f,1,0.6f);
    int maxPopulation = 0;
    for(int i = 0; i < POPULATION_HISTORY_LENGTH; i++){
      if(populationHistory[i] > maxPopulation){
        maxPopulation = populationHistory[i];
      }
    }
    for(int i = 0; i < POPULATION_HISTORY_LENGTH; i++){
      float h = (((float)populationHistory[i])/maxPopulation)*(y2-770);
      rect((POPULATION_HISTORY_LENGTH-1-i)*barWidth,y2-h,barWidth,h);
    }
  }
  public String getNextFileName(int type){
    String[] modes = {"manualImgs","autoImgs","manualTexts","autoTexts"};
    String ending = ".png";
    if(type >= 2){
      ending = ".txt";
    }
    return folder+"/"+modes[type]+"/"+nf(fileSaveCounts[type],5)+ending;
  }
  public void iterate(double timeStep){
    double prevYear = year;
    year += timeStep;
    if(Math.floor(year/recordPopulationEvery) != Math.floor(prevYear/recordPopulationEvery)){
      for(int i = POPULATION_HISTORY_LENGTH-1; i >= 1; i--){
        populationHistory[i] = populationHistory[i-1];
      }
      populationHistory[0] = creatures.size();
    }
    temperature = getGrowthRate(getSeason());
    double tempChangeIntoThisFrame = temperature-getGrowthRate(getSeason()-timeStep);
    double tempChangeOutOfThisFrame = getGrowthRate(getSeason()+timeStep)-temperature;
    if(tempChangeIntoThisFrame*tempChangeOutOfThisFrame <= 0){ // Temperature change flipped direction.
      for(int x = 0; x < boardWidth; x++){
        for(int y = 0; y < boardHeight; y++){
          tiles[x][y].iterate();
        }
      }
    }
    /*for(int x = 0; x < boardWidth; x++){
      for(int y = 0; y < boardHeight; y++){
        tiles[x][y].iterate(this, year);
      }
    }*/
    for(int i = 0; i < creatures.size(); i++){
      creatures.get(i).setPreviousEnergy();
    }
    /*for(int i = 0; i < rocks.size(); i++){
      rocks.get(i).collide(timeStep*OBJECT_TIMESTEPS_PER_YEAR);
    }*/
    maintainCreatureMinimum(false);
    threadsToFinish = creatures.size();
    for(int i = 0; i < creatures.size(); i++){
      Creature me = creatures.get(i);
      //me.doThread(timeStep, userControl);
      me.collide(timeStep);
      me.metabolize(timeStep);
      me.useBrain(timeStep, !userControl);
      if(userControl){
        if(me == selectedCreature){
          if(keyPressed){
             if (key == CODED) {
              if (keyCode == UP) me.accelerate(0.04f,timeStep*OBJECT_TIMESTEPS_PER_YEAR);
              if (keyCode == DOWN) me.accelerate(-0.04f,timeStep*OBJECT_TIMESTEPS_PER_YEAR);
              if (keyCode == LEFT) me.turn(-0.1f,timeStep*OBJECT_TIMESTEPS_PER_YEAR);
              if (keyCode == RIGHT) me.turn(0.1f,timeStep*OBJECT_TIMESTEPS_PER_YEAR);
            }else{
              if(key == ' ') me.eat(0.1f,timeStep*OBJECT_TIMESTEPS_PER_YEAR);
              if(key == 'v') me.eat(-0.1f,timeStep*OBJECT_TIMESTEPS_PER_YEAR);
              if(key == 'f')  me.fight(0.5f,timeStep*OBJECT_TIMESTEPS_PER_YEAR);
              if(key == 'u') me.setHue(me.hue+0.02f);
              if(key == 'j') me.setHue(me.hue-0.02f);
              
              if(key == 'i') me.setMouthHue(me.mouthHue+0.02f);
              if(key == 'k') me.setMouthHue(me.mouthHue-0.02f);
              if(key == 'b'){
                if(!wasPressingB){
                  me.reproduce(MANUAL_BIRTH_SIZE, timeStep);
                }
                wasPressingB = true;
              }else{
                wasPressingB = false;
              }
            }
          }
        }
      }
      if(me.getRadius() < MINIMUM_SURVIVABLE_SIZE){
        me.returnToEarth();
        creatures.remove(me);
        i--;
      }
    }
    finishIterate(timeStep);
  }
  public void finishIterate(double timeStep){
    for(int i = 0; i < rocks.size(); i++){
      rocks.get(i).applyMotions(timeStep*OBJECT_TIMESTEPS_PER_YEAR);
    }
    for(int i = 0; i < creatures.size(); i++){
      creatures.get(i).applyMotions(timeStep*OBJECT_TIMESTEPS_PER_YEAR);
      creatures.get(i).see(timeStep*OBJECT_TIMESTEPS_PER_YEAR);
    }
    if(Math.floor(fileSaveTimes[1]/imageSaveInterval) != Math.floor(year/imageSaveInterval)){
      prepareForFileSave(1);
    }
    if(Math.floor(fileSaveTimes[3]/textSaveInterval) != Math.floor(year/textSaveInterval)){
      prepareForFileSave(3);
    }
  }
  private double getGrowthRate(double theTime){
    double temperatureRange = MAX_TEMPERATURE-MIN_TEMPERATURE;
    return MIN_TEMPERATURE+temperatureRange*0.5f-temperatureRange*0.5f*Math.cos(theTime*2*Math.PI);
  }
  private double getGrowthOverTimeRange(double startTime, double endTime){
    double temperatureRange = MAX_TEMPERATURE-MIN_TEMPERATURE;
    double m = MIN_TEMPERATURE+temperatureRange*0.5f;
    return (endTime-startTime)*m+(temperatureRange/Math.PI/4.0f)*
    (Math.sin(2*Math.PI*startTime)-Math.sin(2*Math.PI*endTime));
  }
  private double getSeason(){
    return (year%1.0f);
  }
  private void drawThermometer(float x1, float y1, float w, float h, double prog, double min, double max,
  int fillColor){
    noStroke();
    fill(0,0,0.2f);
    rect(x1,y1,w,h);
    fill(fillColor);
    double proportionFilled = (prog-min)/(max-min);
    rect(x1,(float)(y1+h*(1-proportionFilled)),w,(float)(proportionFilled*h));
    
    
    double zeroHeight = (0-min)/(max-min);
    double zeroLineY = y1+h*(1-zeroHeight);
    textAlign(RIGHT);
    stroke(0,0,1);
    strokeWeight(3);
    line(x1,(float)(zeroLineY),x1+w,(float)(zeroLineY));
    double minY = y1+h*(1-(MIN_TEMPERATURE-min)/(max-min));
    double maxY = y1+h*(1-(MAX_TEMPERATURE-min)/(max-min));
    fill(0,0,0.8f);
    line(x1,(float)(minY),x1+w*1.8f,(float)(minY));
    line(x1,(float)(maxY),x1+w*1.8f,(float)(maxY));
    line(x1+w*1.8f,(float)(minY),x1+w*1.8f,(float)(maxY));
    
    fill(0,0,1);
    text("Zero",x1-5,(float)(zeroLineY+8));
    text(nf(MIN_TEMPERATURE,0,2),x1-5,(float)(minY+8));
    text(nf(MAX_TEMPERATURE,0,2),x1-5,(float)(maxY+8));
  }
  private void drawVerticalSlider(float x1, float y1, float w, float h, double prog, int fillColor, int antiColor){
    noStroke();
    fill(0,0,0.2f);
    rect(x1,y1,w,h);
    if(prog >= 0){
      fill(fillColor);
    }else{
      fill(antiColor);
    }
    rect(x1,(float)(y1+h*(1-prog)),w,(float)(prog*h));
  }
  private boolean setMinTemperature(float temp){
    MIN_TEMPERATURE = tempBounds(THERMOMETER_MIN+temp*(THERMOMETER_MAX-THERMOMETER_MIN));
    if(MIN_TEMPERATURE > MAX_TEMPERATURE){
      float placeHolder = MAX_TEMPERATURE;
      MAX_TEMPERATURE = MIN_TEMPERATURE;
      MIN_TEMPERATURE = placeHolder;
      return true;
    }
    return false;
  }
  private boolean setMaxTemperature(float temp){
    MAX_TEMPERATURE = tempBounds(THERMOMETER_MIN+temp*(THERMOMETER_MAX-THERMOMETER_MIN));
    if(MIN_TEMPERATURE > MAX_TEMPERATURE){
      float placeHolder = MAX_TEMPERATURE;
      MAX_TEMPERATURE = MIN_TEMPERATURE;
      MIN_TEMPERATURE = placeHolder;
      return true;
    }
    return false;
  }
  private float tempBounds(float temp){
    return min(max(temp,THERMOMETER_MIN),THERMOMETER_MAX);
  }
  private float getHighTempProportion(){
    return (MAX_TEMPERATURE-THERMOMETER_MIN)/(THERMOMETER_MAX-THERMOMETER_MIN);
  }
  private float getLowTempProportion(){
    return (MIN_TEMPERATURE-THERMOMETER_MIN)/(THERMOMETER_MAX-THERMOMETER_MIN);
  }
  private String toDate(double d){
    return "Year "+nf((float)(d),0,2);
  }
  private String toAge(double d){
    return nf((float)(year-d),0,2)+" yrs old";
  }
  private void maintainCreatureMinimum(boolean choosePreexisting){
    while(creatures.size() < creatureMinimum){
      if(choosePreexisting){
        Creature c = getRandomCreature();
        c.addEnergy(c.SAFE_SIZE);
        c.reproduce(c.SAFE_SIZE, timeStep);
      }else{
        creatures.add(new Creature(random(0,boardWidth),random(0,boardHeight),0,0,
        random(MIN_CREATURE_ENERGY,MAX_CREATURE_ENERGY),1,random(0,1),1,1,
        this,year,random(0,2*PI),0,"","[PRIMORDIAL]",true,null,null,1,random(0,1)));
      }
    }
  }
  private Creature getRandomCreature(){
    int index = (int)(random(0,creatures.size()));
    return creatures.get(index);
  }
  private double getRandomSize(){
    return pow(random(MIN_ROCK_ENERGY_BASE,MAX_ROCK_ENERGY_BASE),4);
  }
  private void drawCreature(Creature c, float x, float y, float scale, float scaleUp){
    pushMatrix();
    float scaleIconUp = scaleUp*scale;
    translate((float)(-c.px*scaleIconUp),(float)(-c.py*scaleIconUp));
    translate(x,y);
    c.drawSoftBody(scaleIconUp, 40.0f/scale,false);
    popMatrix();
  }
  private void prepareForFileSave(int type){
    fileSaveTimes[type] = -999999;
  }
  private void fileSave(){
    for(int i = 0; i < 4; i++){
      if(fileSaveTimes[i] < -99999){
        fileSaveTimes[i] = year;
        if(i < 2){
          saveFrame(getNextFileName(i));
        }else{
          String[] data = this.toBigString();
          saveStrings(getNextFileName(i),data);
        }
        fileSaveCounts[i]++;
      }
    }
  }
  public String[] toBigString(){ // Convert current evolvio board into string. Does not work
    String[] placeholder = {"Goo goo","Ga ga"};
    return placeholder;
  }
  public void unselect(){
    selectedCreature = null;
  }
}
class Creature extends SoftBody{
  double ACCELERATION_ENERGY = 0.18f;
  double ACCELERATION_BACK_ENERGY = 0.24f;
  double SWIM_ENERGY = 0.008f;
  double TURN_ENERGY = 0.05f;
  double EAT_ENERGY = 0.05f;
  double EAT_SPEED = 0.5f; // 1 is instant, 0 is nonexistent, 0.001 is verrry slow.
  double EAT_WHILE_MOVING_INEFFICIENCY_MULTIPLIER = 2.0f; // The bigger this number is, the less effiently creatures eat when they're moving.
  double FIGHT_ENERGY = 0.06f;
  double INJURED_ENERGY = 0.25f;
  double METABOLISM_ENERGY = 0.004f;
  String name;
  String parents;
  int gen;
  int id;
  double MAX_VISION_DISTANCE = 10;
  double currentEnergy;
  final int ENERGY_HISTORY_LENGTH = 6;
  final double SAFE_SIZE = 1.25f;
  double[] previousEnergy = new double[ENERGY_HISTORY_LENGTH];
  final double MATURE_AGE = 0.01f;
  final double STARTING_AXON_VARIABILITY = 1.0f;
  final double FOOD_SENSITIVITY = 0.3f;
  
  double vr = 0;
  double rotation = 0;
  final int BRAIN_WIDTH = 3;
  final int BRAIN_HEIGHT = 13;
  final double AXON_START_MUTABILITY = 0.0005f;
  final int MIN_NAME_LENGTH = 3;
  final int MAX_NAME_LENGTH = 10;
  final float BRIGHTNESS_THRESHOLD = 0.7f;
  Axon[][][] axons;
  double[][] neurons;
  
  float preferredRank = 8;
  double[] visionAngles = {0,-0.4f,0.4f};
  double[] visionDistances = {0,0.7f,0.7f};
  //double visionAngle;
  //double visionDistance;
  double[] visionOccludedX = new double[visionAngles.length];
  double[] visionOccludedY = new double[visionAngles.length];
  double visionResults[] = new double[9];
  int MEMORY_COUNT = 1;
  double[] memories;
  
  float CROSS_SIZE = 0.022f;
  
  double mouthHue;
  CreatureThread thread;
  
  public Creature(double tpx, double tpy, double tvx, double tvy, double tenergy,
  double tdensity, double thue, double tsaturation, double tbrightness, Board tb, double bt,
  double rot, double tvr, String tname,String tparents, boolean mutateName,
  Axon[][][] tbrain, double[][] tneurons, int tgen, double tmouthHue){
    super(tpx,tpy,tvx,tvy,tenergy,tdensity,thue, tsaturation, tbrightness,tb, bt);
    if(tbrain == null){
      axons = new Axon[BRAIN_WIDTH-1][BRAIN_HEIGHT][BRAIN_HEIGHT-1];
      neurons = new double[BRAIN_WIDTH][BRAIN_HEIGHT];
      for(int x = 0; x < BRAIN_WIDTH-1; x++){
        for(int y = 0; y < BRAIN_HEIGHT; y++){
          for(int z = 0; z < BRAIN_HEIGHT-1; z++){
            double startingWeight = 0;
            if(y == BRAIN_HEIGHT-1){
              startingWeight = (Math.random()*2-1)*STARTING_AXON_VARIABILITY;
            }
            axons[x][y][z] = new Axon(startingWeight,AXON_START_MUTABILITY);
          }
        }
      }
      neurons = new double[BRAIN_WIDTH][BRAIN_HEIGHT];
      for(int x = 0; x < BRAIN_WIDTH; x++){
        for(int y = 0; y < BRAIN_HEIGHT; y++){
          if(y == BRAIN_HEIGHT-1){
            neurons[x][y] = 1;
          }else{
            neurons[x][y] = 0;
          }
        }
      }
    }else{
      axons = tbrain;
      neurons = tneurons;
    }
    rotation = rot;
    vr = tvr;
    isCreature = true;
    id = board.creatureIDUpTo+1;
    if(tname.length() >= 1){
      if(mutateName){
        name = mutateName(tname);
      }else{
        name = tname;
      }
      name = sanitizeName(name);
    }else{
      name = createNewName();
    }
    parents = tparents;
    board.creatureIDUpTo++;
    //visionAngle = 0;
    //visionDistance = 0;
    //visionEndX = getVisionStartX();
    //visionEndY = getVisionStartY();
    for(int i = 0; i < 9; i++){
      visionResults[i] = 0;
    }
    memories = new double[MEMORY_COUNT];
    for(int i = 0; i < MEMORY_COUNT; i++){
      memories[i] = 0;
    }
    gen = tgen;
    mouthHue = tmouthHue;
  }
  public void drawBrain(PFont font, float scaleUp, int mX, int mY){
    final float neuronSize = 0.4f;
    noStroke();
    fill(0,0,0.4f);
    rect((-1.7f-neuronSize)*scaleUp,-neuronSize*scaleUp,(2.4f+BRAIN_WIDTH+neuronSize*2)*scaleUp,(BRAIN_HEIGHT+neuronSize*2)*scaleUp);
    
    ellipseMode(RADIUS);
    strokeWeight(2);
    textFont(font,0.58f*scaleUp);
    fill(0,0,1);
    String[] inputLabels = {"0Hue","0Sat","0Bri","1Hue",
    "1Sat","1Bri","2Hue","2Sat","2Bri","Size","MHue","Mem","Const."};
    String[] outputLabels = {"BHue","Accel.","Turn","Eat","Fight","Birth","How funny?",
    "How popular?","How generous?","How smart?","MHue","Mem","Const."};
    for(int y = 0; y < BRAIN_HEIGHT; y++){
      textAlign(RIGHT);
      text(inputLabels[y],(-neuronSize-0.1f)*scaleUp,(y+(neuronSize*0.6f))*scaleUp);
      textAlign(LEFT);
      text(outputLabels[y],(BRAIN_WIDTH-1+neuronSize+0.1f)*scaleUp,(y+(neuronSize*0.6f))*scaleUp);
    }
    textAlign(CENTER);
    for(int x = 0; x < BRAIN_WIDTH; x++){
      for(int y = 0; y < BRAIN_HEIGHT; y++){
        noStroke();
        double val = neurons[x][y];
        fill(neuronFillColor(val));
        ellipse(x*scaleUp,y*scaleUp,neuronSize*scaleUp,neuronSize*scaleUp);
        fill(neuronTextColor(val));
        text(nf((float)val,0,1),x*scaleUp,(y+(neuronSize*0.6f))*scaleUp);
      }
    }
    if(mX >= 0 && mX < BRAIN_WIDTH && mY >= 0 && mY < BRAIN_HEIGHT){
      for(int y = 0; y < BRAIN_HEIGHT; y++){
        if(mX >= 1 && mY < BRAIN_HEIGHT-1){
          drawAxon(mX-1,y,mX,mY,scaleUp);
        }
        if(mX < BRAIN_WIDTH-1 && y < BRAIN_HEIGHT-1){
          drawAxon(mX,mY,mX+1,y,scaleUp);
        }
      }
    }
  }
  public void drawAxon(int x1, int y1, int x2, int y2, float scaleUp){
    stroke(neuronFillColor(axons[x1][y1][y2].weight*neurons[x1][y1]));
    
    line(x1*scaleUp,y1*scaleUp,x2*scaleUp,y2*scaleUp);
  }
  public void useBrain(double timeStep, boolean useOutput){
    for(int i = 0; i < 9; i++){
      neurons[0][i] = visionResults[i];
    }
    neurons[0][9] = energy;
    neurons[0][10] = mouthHue;
    for(int i = 0; i < MEMORY_COUNT; i++){
      neurons[0][11+i] = memories[i];
    }
    for(int x = 1; x < BRAIN_WIDTH; x++){
      for(int y = 0; y < BRAIN_HEIGHT-1; y++){
        double total = 0;
        for(int input = 0; input < BRAIN_HEIGHT; input++){
          total += neurons[x-1][input]*axons[x-1][input][y].weight;
        }
        if(x == BRAIN_WIDTH-1){
          neurons[x][y] = total;
        }else{
          neurons[x][y] = sigmoid(total);
        }
      }
    }
    if(useOutput){
      int end = BRAIN_WIDTH-1;
      hue = Math.min(Math.max(neurons[end][0],0),1);
      accelerate(neurons[end][1],timeStep);
      turn(neurons[end][2],timeStep);
      eat(neurons[end][3],timeStep);
      fight(neurons[end][4],timeStep);
      if(neurons[end][5] > 0 && board.year-birthTime >= MATURE_AGE && energy > SAFE_SIZE){
        reproduce(SAFE_SIZE, timeStep);
      }
      mouthHue = Math.min(Math.max(neurons[end][10],0),1);
      for(int i = 0; i < MEMORY_COUNT; i++){
        memories[i] = neurons[end][11+i];
      }
    }
  }
  public double sigmoid(double input){
    return 1.0f/(1.0f+Math.pow(2.71828182846f,-input));
  }
  public int neuronFillColor(double d){
    if(d >= 0){
      return color(0,0,1,(float)(d));
    }else{
      return color(0,0,0,(float)(-d));
    }
  }
   public int neuronTextColor(double d){
    if(d >= 0){
      return color(0,0,0);
    }else{
      return color(0,0,1);
    }
  }
  public void drawSoftBody(float scaleUp, float camZoom, boolean showVision){
    ellipseMode(RADIUS);
    double radius = getRadius();
    if(showVision){
      for(int i = 0; i < visionAngles.length; i++){
        int visionUIcolor = color(0,0,1);
        if(visionResults[i*3+2] > BRIGHTNESS_THRESHOLD){
          visionUIcolor = color(0,0,0);
        }
        stroke(visionUIcolor);
        strokeWeight(board.CREATURE_STROKE_WEIGHT);
        float endX = (float)getVisionEndX(i);
        float endY = (float)getVisionEndY(i);
        line((float)(px*scaleUp),(float)(py*scaleUp),endX*scaleUp,endY*scaleUp);
        noStroke();
        fill(visionUIcolor);
        ellipse((float)(visionOccludedX[i]*scaleUp),(float)(visionOccludedY[i]*scaleUp),
        2*CROSS_SIZE*scaleUp,2*CROSS_SIZE*scaleUp);
        stroke((float)(visionResults[i*3]),(float)(visionResults[i*3+1]),(float)(visionResults[i*3+2]));
        strokeWeight(board.CREATURE_STROKE_WEIGHT);
        line((float)((visionOccludedX[i]-CROSS_SIZE)*scaleUp),(float)((visionOccludedY[i]-CROSS_SIZE)*scaleUp),
        (float)((visionOccludedX[i]+CROSS_SIZE)*scaleUp),(float)((visionOccludedY[i]+CROSS_SIZE)*scaleUp));
        line((float)((visionOccludedX[i]-CROSS_SIZE)*scaleUp),(float)((visionOccludedY[i]+CROSS_SIZE)*scaleUp),
        (float)((visionOccludedX[i]+CROSS_SIZE)*scaleUp),(float)((visionOccludedY[i]-CROSS_SIZE)*scaleUp));
      }
    }
    noStroke();
    if(fightLevel > 0){
      fill(0,1,1,(float)(fightLevel*0.8f));
      ellipse((float)(px*scaleUp),(float)(py*scaleUp),(float)(FIGHT_RANGE*radius*scaleUp),(float)(FIGHT_RANGE*radius*scaleUp));
    }
    strokeWeight(board.CREATURE_STROKE_WEIGHT);
    stroke(0,0,1);
    fill(0,0,1);
    if(this == board.selectedCreature){
      ellipse((float)(px*scaleUp),(float)(py*scaleUp),
      (float)(radius*scaleUp+1+75.0f/camZoom),(float)(radius*scaleUp+1+75.0f/camZoom));
    }
    super.drawSoftBody(scaleUp);
    noFill();
    strokeWeight(board.CREATURE_STROKE_WEIGHT);
    stroke(0,0,1);
    ellipseMode(RADIUS);
    ellipse((float)(px*scaleUp),(float)(py*scaleUp),
      (float)(board.MINIMUM_SURVIVABLE_SIZE*scaleUp),(float)(board.MINIMUM_SURVIVABLE_SIZE*scaleUp));
    pushMatrix();
    translate((float)(px*scaleUp),(float)(py*scaleUp));
    scale((float)radius);
    rotate((float)rotation);
    strokeWeight((float)(board.CREATURE_STROKE_WEIGHT/radius));
    stroke(0,0,0);
    fill((float)mouthHue,1.0f,1.0f);
    ellipse(0.6f*scaleUp,0,0.37f*scaleUp,0.37f*scaleUp);
    /*rect(-0.7*scaleUp,-0.2*scaleUp,1.1*scaleUp,0.4*scaleUp);
    beginShape();
    vertex(0.3*scaleUp,-0.5*scaleUp);
    vertex(0.3*scaleUp,0.5*scaleUp);
    vertex(0.8*scaleUp,0.0*scaleUp);
    endShape(CLOSE);*/
    popMatrix();
    if(showVision){
      fill(0,0,1);
      textFont(font,0.2f*scaleUp);
      textAlign(CENTER);
      text(getCreatureName(),(float)(px*scaleUp),(float)((py-getRadius()*1.4f-0.07f)*scaleUp));
    }
  }
  public void doThread(double timeStep, Boolean userControl){ // just kidding, multithreading doesn't really help here.
    //collide(timeStep);
    //metabolize(timeStep);
    //useBrain(timeStep, !userControl);
    thread = new CreatureThread("Thread "+id, this, timeStep, userControl);
    thread.start();
  }
  public void metabolize(double timeStep){
    loseEnergy(energy*METABOLISM_ENERGY*timeStep);
  }
  public void accelerate(double amount, double timeStep){
    double multiplied = amount*timeStep/getMass();
    vx += Math.cos(rotation)*multiplied;
    vy += Math.sin(rotation)*multiplied;
    if(amount >= 0){
      loseEnergy(amount*ACCELERATION_ENERGY*timeStep);
    }else{
      loseEnergy(Math.abs(amount*ACCELERATION_BACK_ENERGY*timeStep));
    }
  }
  public void turn(double amount, double timeStep){
    vr += 0.04f*amount*timeStep/getMass();
    loseEnergy(Math.abs(amount*TURN_ENERGY*energy*timeStep));
  }
  public Tile getRandomCoveredTile(){
    double radius = (float)getRadius();
    double choiceX = 0;
    double choiceY = 0;
    while(dist((float)px,(float)py,(float)choiceX,(float)choiceY) > radius){
      choiceX = (Math.random()*2*radius-radius)+px;
      choiceY = (Math.random()*2*radius-radius)+py;
    }
    int x = xBound((int)choiceX);
    int y = yBound((int)choiceY);
    return board.tiles[x][y];
  }
  public void eat(double attemptedAmount, double timeStep){
    double amount = attemptedAmount/(1.0f+distance(0,0,vx,vy)*EAT_WHILE_MOVING_INEFFICIENCY_MULTIPLIER); // The faster you're moving, the less efficiently you can eat.
    if(amount < 0){
      dropEnergy(-amount*timeStep);
      loseEnergy(-attemptedAmount*EAT_ENERGY*timeStep);
    }else{
      Tile coveredTile = getRandomCoveredTile();
      double foodToEat = coveredTile.foodLevel*(1-Math.pow((1-EAT_SPEED),amount*timeStep));
      if(foodToEat > coveredTile.foodLevel){
        foodToEat = coveredTile.foodLevel;
      }
      coveredTile.removeFood(foodToEat, true);
      double foodDistance = Math.abs(coveredTile.foodType-mouthHue);
      double multiplier = 1.0f-foodDistance/FOOD_SENSITIVITY;
      if(multiplier >= 0){
        addEnergy(foodToEat*multiplier);
      }else{
        loseEnergy(-foodToEat*multiplier);
      }
      loseEnergy(attemptedAmount*EAT_ENERGY*timeStep);
    }
  }
  public void fight(double amount, double timeStep){
    if(amount > 0 && board.year-birthTime >= MATURE_AGE){
      fightLevel = amount;
      loseEnergy(fightLevel*FIGHT_ENERGY*energy*timeStep);
      for(int i = 0; i < colliders.size(); i++){
        SoftBody collider = colliders.get(i);
        if(collider.isCreature){
          float distance = dist((float)px,(float)py,(float)collider.px,(float)collider.py);
          double combinedRadius = getRadius()*FIGHT_RANGE+collider.getRadius();
          if(distance < combinedRadius){
            ((Creature)collider).dropEnergy(fightLevel*INJURED_ENERGY*timeStep);
          }
        }
      }
    }else{
      fightLevel = 0;
    }
  }
  public void loseEnergy(double energyLost){
    if(energyLost > 0){
      energy -= energyLost;
    }
  }
  public void dropEnergy(double energyLost){
    if(energyLost > 0){
      energyLost = Math.min(energyLost, energy);
      energy -= energyLost;
      getRandomCoveredTile().addFood(energyLost,hue,true);
    }
  }
  public void see(double timeStep){
    for(int k = 0; k < visionAngles.length; k++){
      double visionStartX = px;
      double visionStartY = py;
      double visionTotalAngle = rotation+visionAngles[k];
      
      double endX = getVisionEndX(k);
      double endY = getVisionEndY(k);
      
      visionOccludedX[k] = endX;
      visionOccludedY[k] = endY;
      int c = getColorAt(endX,endY);
      visionResults[k*3] = hue(c);
      visionResults[k*3+1] = saturation(c);
      visionResults[k*3+2] = brightness(c);
      
      int tileX = 0;
      int tileY = 0;
      int prevTileX = -1;
      int prevTileY = -1;
      ArrayList<SoftBody> potentialVisionOccluders = new ArrayList<SoftBody>();
      for(int DAvision = 0; DAvision < visionDistances[k]+1; DAvision++){
        tileX = (int)(visionStartX+Math.cos(visionTotalAngle)*DAvision);
        tileY = (int)(visionStartY+Math.sin(visionTotalAngle)*DAvision);
        if(tileX != prevTileX || tileY != prevTileY){
          addPVOs(tileX,tileY,potentialVisionOccluders);
          if(prevTileX >= 0 && tileX != prevTileX && tileY != prevTileY){
            addPVOs(prevTileX,tileY,potentialVisionOccluders);
            addPVOs(tileX,prevTileY,potentialVisionOccluders);
          }
        }
        prevTileX = tileX;
        prevTileY = tileY;
      }
      double[][] rotationMatrix = new double[2][2];
      rotationMatrix[1][1] = rotationMatrix[0][0] = Math.cos(-visionTotalAngle);
      rotationMatrix[0][1] = Math.sin(-visionTotalAngle);
      rotationMatrix[1][0] = -rotationMatrix[0][1];
      double visionLineLength = visionDistances[k];
      for(int i = 0; i < potentialVisionOccluders.size(); i++){
        SoftBody body = potentialVisionOccluders.get(i);
        double x = body.px-px;
        double y = body.py-py;
        double r = body.getRadius();
        double translatedX = rotationMatrix[0][0]*x+rotationMatrix[1][0]*y;
        double translatedY = rotationMatrix[0][1]*x+rotationMatrix[1][1]*y;
        if(Math.abs(translatedY) <= r){
          if((translatedX >= 0 && translatedX < visionLineLength && translatedY < visionLineLength) ||
          distance(0,0,translatedX,translatedY) < r ||
          distance(visionLineLength,0,translatedX,translatedY) < r){ // YES! There is an occlussion.
            visionLineLength = translatedX-Math.sqrt(r*r-translatedY*translatedY);
            visionOccludedX[k] = visionStartX+visionLineLength*Math.cos(visionTotalAngle);
            visionOccludedY[k] = visionStartY+visionLineLength*Math.sin(visionTotalAngle);
            visionResults[k*3] = body.hue;
            visionResults[k*3+1] = body.saturation;
            visionResults[k*3+2] = body.brightness;
          }
        }
      }
    }
  }
  public int getColorAt(double x, double y){
    if(x >= 0 && x < board.boardWidth && y >= 0 && y < board.boardHeight){
      return board.tiles[(int)(x)][(int)(y)].getColor();
    }else{
      return board.BACKGROUND_COLOR;
    }
  }
  public double distance(double x1, double y1, double x2, double y2){
    return(Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)));
  }
  public void addPVOs(int x, int y, ArrayList<SoftBody> PVOs){
    if(x >= 0 && x < board.boardWidth && y >= 0 && y < board.boardHeight){
      for(int i = 0; i < board.softBodiesInPositions[x][y].size(); i++){
        SoftBody newCollider = (SoftBody)board.softBodiesInPositions[x][y].get(i);
        if(!PVOs.contains(newCollider) && newCollider != this){
          PVOs.add(newCollider);
        }
      }
    }
  }
  public void returnToEarth(){
    int pieces = 20;
    double radius = (float)getRadius();
    for(int i = 0; i < pieces; i++){
      getRandomCoveredTile().addFood(energy/pieces,hue,true);
    }
    for(int x = SBIPMinX; x <= SBIPMaxX; x++){
      for(int y = SBIPMinY; y <= SBIPMaxY; y++){
        board.softBodiesInPositions[x][y].remove(this);
      }
    }
    if(board.selectedCreature == this){
      board.unselect();
    }
  }
  public void reproduce(double babySize, double timeStep){
    if(colliders == null){
      collide(timeStep);
    }
    int highestGen = 0;
    if(babySize >= 0){
      ArrayList<Creature> parents = new ArrayList<Creature>(0);
      parents.add(this);
      double availableEnergy = getBabyEnergy();
      for(int i = 0; i < colliders.size(); i++){
        SoftBody possibleParent = colliders.get(i);
        if(possibleParent.isCreature && ((Creature)possibleParent).neurons[BRAIN_WIDTH-1][9] > -1){ // Must be a WILLING creature to also give birth.
          float distance = dist((float)px,(float)py,(float)possibleParent.px,(float)possibleParent.py);
          double combinedRadius = getRadius()*FIGHT_RANGE+possibleParent.getRadius();
          if(distance < combinedRadius){
            parents.add((Creature)possibleParent);
            availableEnergy += ((Creature)possibleParent).getBabyEnergy();
          }
        }
      }
      if(availableEnergy > babySize){
        double newPX = random(-0.01f,0.01f);
        double newPY = random(-0.01f,0.01f); //To avoid landing directly on parents, resulting in division by 0)
        double newHue = 0;
        double newSaturation = 0;
        double newBrightness = 0;
        double newMouthHue = 0;
        int parentsTotal = parents.size();
        String[] parentNames = new String[parentsTotal];
        Axon[][][] newBrain = new Axon[BRAIN_WIDTH-1][BRAIN_HEIGHT][BRAIN_HEIGHT-1];
        double[][] newNeurons = new double[BRAIN_WIDTH][BRAIN_HEIGHT];
        float randomParentRotation = random(0,1);
        for(int x = 0; x < BRAIN_WIDTH-1; x++){
          for(int y = 0; y < BRAIN_HEIGHT; y++){
            for(int z = 0; z < BRAIN_HEIGHT-1; z++){
              float axonAngle = atan2((y+z)/2.0f-BRAIN_HEIGHT/2.0f,x-BRAIN_WIDTH/2)/(2*PI)+PI;
              Creature parentForAxon = parents.get((int)(((axonAngle+randomParentRotation)%1.0f)*parentsTotal));
              newBrain[x][y][z] = parentForAxon.axons[x][y][z].mutateAxon();
            }
          }
        }
        for(int x = 0; x < BRAIN_WIDTH; x++){
          for(int y = 0; y < BRAIN_HEIGHT; y++){
            float axonAngle = atan2(y-BRAIN_HEIGHT/2.0f,x-BRAIN_WIDTH/2)/(2*PI)+PI;
            Creature parentForAxon = parents.get((int)(((axonAngle+randomParentRotation)%1.0f)*parentsTotal));
            newNeurons[x][y] = parentForAxon.neurons[x][y];
          }
        }
        for(int i = 0; i < parentsTotal; i++){
          int chosenIndex = (int)random(0,parents.size());
          Creature parent = parents.get(chosenIndex);
          parents.remove(chosenIndex);
          parent.energy -= babySize*(parent.getBabyEnergy()/availableEnergy);
          newPX += parent.px/parentsTotal;
          newPY += parent.py/parentsTotal;
          newHue += parent.hue/parentsTotal;
          newSaturation += parent.saturation/parentsTotal;
          newBrightness += parent.brightness/parentsTotal;
          newMouthHue += parent.mouthHue/parentsTotal;
          parentNames[i] = parent.name;
          if(parent.gen > highestGen){
            highestGen = parent.gen;
          }
        }
        newSaturation = 1;
        newBrightness = 1;
        board.creatures.add(new Creature(newPX,newPY,0,0,
          babySize,density,newHue,newSaturation,newBrightness,board,board.year,random(0,2*PI),0,
          stitchName(parentNames),andifyParents(parentNames),true,
          newBrain,newNeurons,highestGen+1,newMouthHue));
      }
    }
  }
  public String stitchName(String[] s){
    String result = "";
    for(int i = 0; i < s.length; i++){
      float portion = ((float)s[i].length())/s.length;
      int start = (int)min(max(round(portion*i),0),s[i].length());
      int end = (int)min(max(round(portion*(i+1)),0),s[i].length());
      result = result+s[i].substring(start,end);
    }
    return result;
  }
  public String andifyParents(String[] s){
    String result = "";
    for(int i = 0; i < s.length; i++){
      if(i >= 1){
        result = result + " & ";
      }
      result = result + capitalize(s[i]);
    }
    return result;
  }
  public String createNewName(){
    String nameSoFar = "";
    int chosenLength = (int)(random(MIN_NAME_LENGTH,MAX_NAME_LENGTH));
    for(int i = 0; i < chosenLength; i++){
      nameSoFar += getRandomChar();
    }
    return sanitizeName(nameSoFar);
  }
  public char getRandomChar(){
    float letterFactor = random(0,100);
    int letterChoice = 0;
    while(letterFactor > 0){
      letterFactor -= board.letterFrequencies[letterChoice];
      letterChoice++;
    }
    return (char)(letterChoice+96);
  }
  public String sanitizeName(String input){
    String output = "";
    int vowelsSoFar = 0;
    int consonantsSoFar = 0;
    for(int i = 0; i < input.length(); i++){
      char ch = input.charAt(i);
      if(isVowel(ch)){
        consonantsSoFar = 0;
        vowelsSoFar++;
      }else{
        vowelsSoFar = 0;
        consonantsSoFar++;
      }
      if(vowelsSoFar <= 2 && consonantsSoFar <= 2){
        output = output+ch;
      }else{
        double chanceOfAddingChar = 0.5f;
        if(input.length() <= MIN_NAME_LENGTH){
          chanceOfAddingChar = 1.0f;
        }else if(input.length() >= MAX_NAME_LENGTH){
          chanceOfAddingChar = 0.0f;
        }
        if(random(0,1) < chanceOfAddingChar){
          char extraChar = ' ';
          while(extraChar == ' ' || (isVowel(ch) == isVowel(extraChar))){
            extraChar = getRandomChar();
          }
          output = output+extraChar+ch;
          if(isVowel(ch)){
            consonantsSoFar = 0;
            vowelsSoFar = 1;
          }else{
            consonantsSoFar = 1;
            vowelsSoFar = 0;
          }
        }else{ // do nothing
        }
      }
    }
    return output;
  }
  public String getCreatureName(){
    return capitalize(name);
  }
  public String capitalize(String n){
    return n.substring(0,1).toUpperCase()+n.substring(1,n.length());
  }
  public boolean isVowel(char a){
    return (a == 'a' || a == 'e' || a == 'i' || a == 'o' || a == 'u' || a == 'y');
  }
  public String mutateName(String input){
    if(input.length() >= 3){
      if(random(0,1) < 0.2f){
        int removeIndex = (int)random(0,input.length());
        input = input.substring(0,removeIndex)+input.substring(removeIndex+1,input.length());
      }
    }
    if(input.length() <= 9){
      if(random(0,1) < 0.2f){
        int insertIndex = (int)random(0,input.length()+1);
        input = input.substring(0,insertIndex)+getRandomChar()+input.substring(insertIndex,input.length());
      }
    }
    int changeIndex = (int)random(0,input.length());
    input = input.substring(0,changeIndex)+getRandomChar()+input.substring(changeIndex+1,input.length());
    return input;
  }
  public void applyMotions(double timeStep){
    if(getRandomCoveredTile().fertility > 1){
      loseEnergy(SWIM_ENERGY*energy);
    }
    super.applyMotions(timeStep);
    rotation += vr;
    vr *= Math.max(0,1-FRICTION/getMass());
  }
  public double getEnergyUsage(double timeStep){
    return (energy-previousEnergy[ENERGY_HISTORY_LENGTH-1])/ENERGY_HISTORY_LENGTH/timeStep;
  }
  public double getBabyEnergy(){
    return energy-SAFE_SIZE;
  }
  public void addEnergy(double amount){
    energy += amount;
  }
  public void setPreviousEnergy(){
    for(int i = ENERGY_HISTORY_LENGTH-1; i >= 1; i--){
      previousEnergy[i] = previousEnergy[i-1];
    }
    previousEnergy[0] = energy;
  }
  public double measure(int choice){
    int sign = 1-2*(choice%2);
    if(choice < 2){
      return sign*energy;
    }else if(choice < 4){
      return sign*birthTime;
    }else if(choice == 6 || choice == 7){
      return sign*gen;
    }
    return 0;
  }
  public void setHue(double set){
    hue = Math.min(Math.max(set,0),1);
  }
  public void setMouthHue(double set){
    mouthHue = Math.min(Math.max(set,0),1);
  }
  public void setSaturarion(double set){
    saturation = Math.min(Math.max(set,0),1);
  }
  public void setBrightness(double set){
    brightness = Math.min(Math.max(set,0),1);
  }
  /*public void setVisionAngle(double set){
    visionAngle = set;//Math.min(Math.max(set,-Math.PI/2),Math.PI/2);
    while(visionAngle < -Math.PI){
      visionAngle += Math.PI*2;
    }
    while(visionAngle > Math.PI){
      visionAngle -= Math.PI*2;
    }
  }
  public void setVisionDistance(double set){
    visionDistance = Math.min(Math.max(set,0),MAX_VISION_DISTANCE);
  }*/
  /*public double getVisionStartX(){
    return px;//+getRadius()*Math.cos(rotation);
  }
  public double getVisionStartY(){
    return py;//+getRadius()*Math.sin(rotation);
  }*/
  public double getVisionEndX(int i){
    double visionTotalAngle = rotation+visionAngles[i];
    return px+visionDistances[i]*Math.cos(visionTotalAngle);
  }
  public double getVisionEndY(int i){
    double visionTotalAngle = rotation+visionAngles[i];
    return py+visionDistances[i]*Math.sin(visionTotalAngle);
  }
}
class CreatureThread extends Thread {
   private Thread t;
   private String threadName;
   private Creature threadOwner;
   double timeStep;
   Boolean userControl;
   
   public CreatureThread(String name, Creature creature, double ts, Boolean uc) {
      threadName = name;
      threadOwner = creature;
      timeStep = ts;
      userControl = uc;
   }
   
   public void run() {
     threadOwner.collide(timeStep);
     threadOwner.metabolize(timeStep);
     threadOwner.useBrain(timeStep, !userControl);
     threadOwner.board.threadsToFinish--;
     if(threadOwner.board.threadsToFinish == 0){
       threadOwner.board.finishIterate(timeStep);
     }
   }
   
   public void start () {
      if (t == null) {
         t = new Thread (this, threadName);
         t.start ();
      }
   }
}

/*void setup(){
    ThreadDemo T1 = new ThreadDemo( "Thread-1");
    T1.start();
    
    ThreadDemo T2 = new ThreadDemo( "Thread-2");
    T2.start();
}*/
Board evoBoard;
final int SEED = 51;
final float NOISE_STEP_SIZE = 0.1f;
final int BOARD_WIDTH = 100;
final int BOARD_HEIGHT = 100;

final int WINDOW_WIDTH = 1920;
final int WINDOW_HEIGHT = 1080;
final float SCALE_TO_FIX_BUG = 100;
final float GROSS_OVERALL_SCALE_FACTOR = ((float)WINDOW_HEIGHT)/BOARD_HEIGHT/SCALE_TO_FIX_BUG;

final double TIME_STEP = 0.001f;
final float MIN_TEMPERATURE = -0.5f;
final float MAX_TEMPERATURE = 1.0f;

final int ROCKS_TO_ADD = 0;
final int CREATURE_MINIMUM = 60;

float cameraX = BOARD_WIDTH*0.5f;
float cameraY = BOARD_HEIGHT*0.5f;
float cameraR = 0;
float zoom = 1;
PFont font;
int dragging = 0; // 0 = no drag, 1 = drag screen, 2 and 3 are dragging temp extremes.
float prevMouseX;
float prevMouseY;
boolean draggedFar = false;
final String INITIAL_FILE_NAME = "PIC";
public void setup() {
  colorMode(HSB,1.0f);
  font = loadFont("Jygquip1-48.vlw");
  size(WINDOW_WIDTH, WINDOW_HEIGHT);
  evoBoard = new Board(BOARD_WIDTH, BOARD_HEIGHT, NOISE_STEP_SIZE, MIN_TEMPERATURE, MAX_TEMPERATURE, 
  ROCKS_TO_ADD, CREATURE_MINIMUM, SEED, INITIAL_FILE_NAME, TIME_STEP);
  resetZoom();
}
public void draw() {
  for (int iteration = 0; iteration < evoBoard.playSpeed; iteration++) {
    evoBoard.iterate(TIME_STEP);
  }
  if (dist(prevMouseX, prevMouseY, mouseX, mouseY) > 5) {
    draggedFar = true;
  }
  if (dragging == 1) {
    cameraX -= toWorldXCoordinate(mouseX, mouseY)-toWorldXCoordinate(prevMouseX, prevMouseY);
    cameraY -= toWorldYCoordinate(mouseX, mouseY)-toWorldYCoordinate(prevMouseX, prevMouseY);
  } else if (dragging == 2) { //UGLY UGLY CODE.  Do not look at this
    if (evoBoard.setMinTemperature(1.0f-(mouseY-30)/660.0f)) {
      dragging = 3;
    }
  } else if (dragging == 3) {
    if (evoBoard.setMaxTemperature(1.0f-(mouseY-30)/660.0f)) {
      dragging = 2;
    }
  }
  if (evoBoard.userControl && evoBoard.selectedCreature != null) {
    cameraX = (float)evoBoard.selectedCreature.px;
    cameraY = (float)evoBoard.selectedCreature.py;
    cameraR = -PI/2.0f-(float)evoBoard.selectedCreature.rotation;
  }else{
    cameraR = 0;
  }
  pushMatrix();
  scale(GROSS_OVERALL_SCALE_FACTOR);
  evoBoard.drawBlankBoard(SCALE_TO_FIX_BUG);
  translate(BOARD_WIDTH*0.5f*SCALE_TO_FIX_BUG, BOARD_HEIGHT*0.5f*SCALE_TO_FIX_BUG);
  scale(zoom);
  if (evoBoard.userControl && evoBoard.selectedCreature != null) {
    rotate(cameraR);
  }
  translate(-cameraX*SCALE_TO_FIX_BUG, -cameraY*SCALE_TO_FIX_BUG);
  evoBoard.drawBoard(SCALE_TO_FIX_BUG, zoom, (int)toWorldXCoordinate(mouseX, mouseY), (int)toWorldYCoordinate(mouseX, mouseY));
  popMatrix();
  evoBoard.drawUI(SCALE_TO_FIX_BUG, TIME_STEP, WINDOW_HEIGHT, 0, WINDOW_WIDTH, WINDOW_HEIGHT, font);

  evoBoard.fileSave();
  prevMouseX = mouseX;
  prevMouseY = mouseY;
}
public void mouseWheel(MouseEvent event) {
  float delta = event.getCount();
  if (delta >= 0.5f) {
    setZoom(zoom*0.90909f, mouseX, mouseY);
  } else if (delta <= -0.5f) {
    setZoom(zoom*1.1f, mouseX, mouseY);
  }
}
public void mousePressed() {
  if (mouseX < WINDOW_HEIGHT) {
    dragging = 1;
  } else {
    if (abs(mouseX-(WINDOW_HEIGHT+65)) <= 60 && abs(mouseY-147) <= 60 && evoBoard.selectedCreature != null) {
        cameraX = (float)evoBoard.selectedCreature.px;
        cameraY = (float)evoBoard.selectedCreature.py;
        zoom = 16;
    } else if (mouseY >= 95 && mouseY < 135 && evoBoard.selectedCreature == null) {
      if (mouseX >= WINDOW_HEIGHT+10 && mouseX < WINDOW_HEIGHT+230) {
        resetZoom();
      } else if (mouseX >= WINDOW_HEIGHT+240 && mouseX < WINDOW_HEIGHT+460) {
        evoBoard.creatureRankMetric = (evoBoard.creatureRankMetric+1)%8;
      }
    } else if (mouseY >= 570) {
      float x = (mouseX-(WINDOW_HEIGHT+10));
      float y = (mouseY-570);
      boolean clickedOnLeft = (x%230 < 110);
      if (x >= 0 && x < 2*230 && y >= 0 && y < 4*50 && x%230 < 220 && y%50 < 40) {
        int mX = (int)(x/230);
        int mY = (int)(y/50);
        int buttonNum = mX+mY*2;
        if (buttonNum == 0) {
          evoBoard.userControl = !evoBoard.userControl;
        } else if (buttonNum == 1) {
          if (clickedOnLeft) {
            evoBoard.creatureMinimum -= evoBoard.creatureMinimumIncrement;
          } else {
            evoBoard.creatureMinimum += evoBoard.creatureMinimumIncrement;
          }
        } else if (buttonNum == 2) {
          evoBoard.prepareForFileSave(0);
        } else if (buttonNum == 3) {
          if (clickedOnLeft) {
            evoBoard.imageSaveInterval *= 0.5f;
          } else {
            evoBoard.imageSaveInterval *= 2.0f;
          }
          if (evoBoard.imageSaveInterval >= 0.7f) {
            evoBoard.imageSaveInterval = Math.round(evoBoard.imageSaveInterval);
          }
        } else if (buttonNum == 4) {
          evoBoard.prepareForFileSave(2);
        } else if (buttonNum == 5) {
          if (clickedOnLeft) {
            evoBoard.textSaveInterval *= 0.5f;
          } else {
            evoBoard.textSaveInterval *= 2.0f;
          }
          if (evoBoard.textSaveInterval >= 0.7f) {
            evoBoard.textSaveInterval = Math.round(evoBoard.textSaveInterval);
          }
        }else if(buttonNum == 6){
          if (clickedOnLeft) {
            if(evoBoard.playSpeed >= 2){
              evoBoard.playSpeed /= 2;
            }else{
              evoBoard.playSpeed = 0;
            }
          } else {
            if(evoBoard.playSpeed == 0){
              evoBoard.playSpeed = 1;
            }else{
              evoBoard.playSpeed *= 2;
            }
          }
        }
      }
    } else if (mouseX >= height+10 && mouseX < width-50 && evoBoard.selectedCreature == null) {
      int listIndex = (mouseY-150)/70;
      if (listIndex >= 0 && listIndex < evoBoard.LIST_SLOTS) {
        evoBoard.selectedCreature = evoBoard.list[listIndex];
        cameraX = (float)evoBoard.selectedCreature.px;
        cameraY = (float)evoBoard.selectedCreature.py;
        zoom = 16;
      }
    }
    if (mouseX >= width-50) {
      float toClickTemp = (mouseY-30)/660.0f;
      float lowTemp = 1.0f-evoBoard.getLowTempProportion();
      float highTemp = 1.0f-evoBoard.getHighTempProportion();
      if (abs(toClickTemp-lowTemp) < abs(toClickTemp-highTemp)) {
        dragging = 2;
      } else {
        dragging = 3;
      }
    }
  }
  draggedFar = false;
}
public void mouseReleased() {
  if (!draggedFar) {
    if (mouseX < WINDOW_HEIGHT) { // DO NOT LOOK AT THIS CODE EITHER it is bad
      dragging = 1;
      float mX = toWorldXCoordinate(mouseX, mouseY);
      float mY = toWorldYCoordinate(mouseX, mouseY);
      int x = (int)(floor(mX));
      int y = (int)(floor(mY));
      evoBoard.unselect();
      cameraR = 0;
      if (x >= 0 && x < BOARD_WIDTH && y >= 0 && y < BOARD_HEIGHT) {
        for (int i = 0; i < evoBoard.softBodiesInPositions[x][y].size (); i++) {
          SoftBody body = (SoftBody)evoBoard.softBodiesInPositions[x][y].get(i);
          if (body.isCreature) {
            float distance = dist(mX, mY, (float)body.px, (float)body.py);
            if (distance <= body.getRadius()) {
              evoBoard.selectedCreature = (Creature)body;
              zoom = 16;
            }
          }
        }
      }
    }
  }
  dragging = 0;
}
public void resetZoom() {
  cameraX = BOARD_WIDTH*0.5f;
  cameraY = BOARD_HEIGHT*0.5f;
  zoom = 1;
}
public void setZoom(float target, float x, float y) {
  float grossX = grossify(x, BOARD_WIDTH);
  cameraX -= (grossX/target-grossX/zoom);
  float grossY = grossify(y, BOARD_HEIGHT);
  cameraY -= (grossY/target-grossY/zoom);
  zoom = target;
}
public float grossify(float input, float total) { // Very weird function
  return (input/GROSS_OVERALL_SCALE_FACTOR-total*0.5f*SCALE_TO_FIX_BUG)/SCALE_TO_FIX_BUG;
}
public float toWorldXCoordinate(float x, float y) {
  float w = WINDOW_HEIGHT/2;
  float angle = atan2(y-w, x-w);
  float dist = dist(w, w, x, y);
  return cameraX+grossify(cos(angle-cameraR)*dist+w, BOARD_WIDTH)/zoom;
}
public float toWorldYCoordinate(float x, float y) {
  float w = WINDOW_HEIGHT/2;
  float angle = atan2(y-w, x-w);
  float dist = dist(w, w, x, y);
  return cameraY+grossify(sin(angle-cameraR)*dist+w, BOARD_HEIGHT)/zoom;
}


class SoftBody{
  double px;
  double py;
  double vx;
  double vy;
  double energy;
  float ENERGY_DENSITY; //set so when a creature is of minimum size, it equals one.
  double density;
  double hue;
  double saturation;
  double brightness;
  double birthTime;
  boolean isCreature = false;
  final float FRICTION = 0.004f;
  final float COLLISION_FORCE = 0.01f;
  final float FIGHT_RANGE = 2.0f;
  double fightLevel = 0;
  
  int prevSBIPMinX;
  int prevSBIPMinY;
  int prevSBIPMaxX;
  int prevSBIPMaxY;
  int SBIPMinX;
  int SBIPMinY;
  int SBIPMaxX;
  int SBIPMaxY;
  ArrayList<SoftBody> colliders;
  Board board;
  public SoftBody(double tpx, double tpy, double tvx, double tvy, double tenergy, double tdensity,
  double thue, double tsaturation, double tbrightness, Board tb, double bt){
    px = tpx;
    py = tpy;
    vx = tvx;
    vy = tvy;
    energy = tenergy;
    density = tdensity;
    hue = thue;
    saturation = tsaturation;
    brightness = tbrightness;
    board = tb;
    setSBIP(false);
    setSBIP(false); // just to set previous SBIPs as well.
    birthTime = bt;
    ENERGY_DENSITY = 1.0f/(tb.MINIMUM_SURVIVABLE_SIZE*tb.MINIMUM_SURVIVABLE_SIZE*PI);
  }
  public void setSBIP(boolean shouldRemove){
    double radius = getRadius()*FIGHT_RANGE;
    prevSBIPMinX = SBIPMinX;
    prevSBIPMinY = SBIPMinY;
    prevSBIPMaxX = SBIPMaxX;
    prevSBIPMaxY = SBIPMaxY;
    SBIPMinX = xBound((int)(Math.floor(px-radius)));
    SBIPMinY = yBound((int)(Math.floor(py-radius)));
    SBIPMaxX = xBound((int)(Math.floor(px+radius)));
    SBIPMaxY = yBound((int)(Math.floor(py+radius)));
    if(prevSBIPMinX != SBIPMinX || prevSBIPMinY != SBIPMinY || 
    prevSBIPMaxX != SBIPMaxX || prevSBIPMaxY != SBIPMaxY){
      if(shouldRemove){
        for(int x = prevSBIPMinX; x <= prevSBIPMaxX; x++){
          for(int y = prevSBIPMinY; y <= prevSBIPMaxY; y++){
            if(x < SBIPMinX || x > SBIPMaxX || 
            y < SBIPMinY || y > SBIPMaxY){
              board.softBodiesInPositions[x][y].remove(this);
            }
          }
        }
      }
      for(int x = SBIPMinX; x <= SBIPMaxX; x++){
        for(int y = SBIPMinY; y <= SBIPMaxY; y++){
          if(x < prevSBIPMinX || x > prevSBIPMaxX || 
          y < prevSBIPMinY || y > prevSBIPMaxY){
            board.softBodiesInPositions[x][y].add(this);
          }
        }
      }
    }
  }
  public int xBound(int x){
    return Math.min(Math.max(x,0),board.boardWidth-1);
  }
  public int yBound(int y){
    return Math.min(Math.max(y,0),board.boardHeight-1);
  }
  public double xBodyBound(double x){
    double radius = getRadius();
    return Math.min(Math.max(x,radius),board.boardWidth-radius);
  }
  public double yBodyBound(double y){
    double radius = getRadius();
    return Math.min(Math.max(y,radius),board.boardHeight-radius);
  }
  public void collide(double timeStep){
    colliders = new ArrayList<SoftBody>(0);
    for(int x = SBIPMinX; x <= SBIPMaxX; x++){
      for(int y = SBIPMinY; y <= SBIPMaxY; y++){
        for(int i = 0; i < board.softBodiesInPositions[x][y].size(); i++){
          SoftBody newCollider = (SoftBody)board.softBodiesInPositions[x][y].get(i);
          if(!colliders.contains(newCollider) && newCollider != this){
            colliders.add(newCollider);
          }
        }
      }
    }
    for(int i = 0; i < colliders.size(); i++){
      SoftBody collider = colliders.get(i);
      float distance = dist((float)px,(float)py,(float)collider.px,(float)collider.py);
      double combinedRadius = getRadius()+collider.getRadius();
      if(distance < combinedRadius){
        double force = combinedRadius*COLLISION_FORCE;
        vx += ((px-collider.px)/distance)*force/getMass();
        vy += ((py-collider.py)/distance)*force/getMass();
      }
    }
    fightLevel = 0;
  }
  public void applyMotions(double timeStep){
    px = xBodyBound(px+vx*timeStep);
    py = yBodyBound(py+vy*timeStep);
    vx *= Math.max(0,1-FRICTION/getMass());
    vy *= Math.max(0,1-FRICTION/getMass());
    setSBIP(true);
  }
  public void drawSoftBody(float scaleUp){
    double radius = getRadius();
    stroke(0);
    strokeWeight(board.CREATURE_STROKE_WEIGHT);
    fill((float)hue, (float)saturation, (float)brightness);
    ellipseMode(RADIUS);
    ellipse((float)(px*scaleUp),(float)(py*scaleUp),(float)(radius*scaleUp),(float)(radius*scaleUp));
  }
  public double getRadius(){
    if(energy <= 0){
      return 0;
    }else{
      return Math.sqrt(energy/ENERGY_DENSITY/Math.PI);
    }
  }
  public double getMass(){
    return energy/ENERGY_DENSITY*density;
  }
}
class Tile{
  public final int barrenColor = color(0,0,1);
  public final int fertileColor = color(0,0,0.2f);
  public final int blackColor = color(0,1,0);
  public final int waterColor = color(0,0,0);
  public final float FOOD_GROWTH_RATE = 1.0f;
  
  private double fertility;
  private double foodLevel;
  private final float maxGrowthLevel = 3.0f;
  private int posX;
  private int posY;
  private double lastUpdateTime = 0;
  
  public double climateType;
  public double foodType;
  
  Board board;
  
  public Tile(int x, int y, double f, float food, float type, Board b){
    posX = x;
    posY = y;
    fertility = Math.max(0,f);
    foodLevel = Math.max(0,food);
    climateType = foodType = type;
    board = b;
  }
  public double getFertility(){
    return fertility;
  }
  public double getFoodLevel(){
    return foodLevel;
  }
  public void setFertility(double f){
    fertility = f;
  }
  public void setFoodLevel(double f){
    foodLevel = f;
  }
  public void drawTile(float scaleUp, boolean showEnergy){
    stroke(0,0,0,1);
    strokeWeight(2);
    int landColor = getColor();
    fill(landColor);
    rect(posX*scaleUp,posY*scaleUp,scaleUp,scaleUp);
    if(showEnergy){
      if(brightness(landColor) >= 0.7f){
        fill(0,0,0,1);
      }else{
        fill(0,0,1,1);
      }
      textAlign(CENTER);
      textFont(font,21);
      text(nf((float)(100*foodLevel),0,2)+" yums",(posX+0.5f)*scaleUp,(posY+0.3f)*scaleUp);
      text("Clim: "+nf((float)(climateType),0,2),(posX+0.5f)*scaleUp,(posY+0.6f)*scaleUp);
      text("Food: "+nf((float)(foodType),0,2),(posX+0.5f)*scaleUp,(posY+0.9f)*scaleUp);
    }
  }
  public void iterate(){
    double updateTime = board.year;
    if(Math.abs(lastUpdateTime-updateTime) >= 0.00001f){
      double growthChange = board.getGrowthOverTimeRange(lastUpdateTime,updateTime);
      if(fertility > 1){ // This means the tile is water.
        foodLevel = 0;
      }else{
        if(growthChange > 0){ // Food is growing. Exponentially approach maxGrowthLevel.
          if(foodLevel < maxGrowthLevel){
            double newDistToMax = (maxGrowthLevel-foodLevel)*Math.pow(2.71828182846f,-growthChange*fertility*FOOD_GROWTH_RATE);
            double foodGrowthAmount = (maxGrowthLevel-newDistToMax)-foodLevel;
            addFood(foodGrowthAmount,climateType,false);
          }
        }else{ // Food is dying off. Exponentially approach 0.
          removeFood(foodLevel-foodLevel*Math.pow(2.71828182846f,growthChange*FOOD_GROWTH_RATE),false);
        }
        /*if(growableTime > 0){
          if(foodLevel < maxGrowthLevel){
            double foodGrowthAmount = (maxGrowthLevel-foodLevel)*fertility*FOOD_GROWTH_RATE*timeStep*growableTime;
            addFood(foodGrowthAmount,climateType);
          }
        }else{
          foodLevel += maxGrowthLevel*foodLevel*FOOD_GROWTH_RATE*timeStep*growableTime;
        }*/
      }
      foodLevel = Math.max(foodLevel,0);
      lastUpdateTime = updateTime;
    }
  }
  public void addFood(double amount, double addedFoodType, boolean canCauseIteration){
    if(canCauseIteration){
      iterate();
    }
    foodLevel += amount;
    /*if(foodLevel > 0){
      foodType += (addedFoodType-foodType)*(amount/foodLevel); // We're adding new plant growth, so we gotta "mix" the colors of the tile.
    }*/
  }
  public void removeFood(double amount, boolean canCauseIteration){
    if(canCauseIteration){
      iterate();
    }
    foodLevel -= amount;
  }
  public int getColor(){
    iterate();
    int foodColor = color((float)(foodType),1,1);
    if(fertility > 1){
      return waterColor;
    }else if(foodLevel < maxGrowthLevel){
      return interColorFixedHue(interColor(barrenColor,fertileColor,fertility),foodColor,foodLevel/maxGrowthLevel,hue(foodColor));
    }else{
      return interColorFixedHue(foodColor,blackColor,1.0f-maxGrowthLevel/foodLevel,hue(foodColor));
    }
  }
  public int interColor(int a, int b, double x){
    double hue = inter(hue(a),hue(b),x);
    double sat = inter(saturation(a),saturation(b),x);
    double bri = inter(brightness(a),brightness(b),x); // I know it's dumb to do interpolation with HSL but oh well
    return color((float)(hue),(float)(sat),(float)(bri));
  }
  public int interColorFixedHue(int a, int b, double x, double hue){
    double satB = saturation(b);
    if(brightness(b) == 0){ // I want black to be calculated as 100% saturation
      satB = 1;
    }
    double sat = inter(saturation(a),satB,x);
    double bri = inter(brightness(a),brightness(b),x); // I know it's dumb to do interpolation with HSL but oh well
    return color((float)(hue),(float)(sat),(float)(bri));
  }
  public double inter(double a, double b, double x){
    return a + (b-a)*x;
  }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--full-screen", "--bgcolor=#666666", "--stop-color=#FC1F1F", "Game" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
