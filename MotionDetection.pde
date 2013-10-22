import codeanticode.gsvideo.*;

GSCapture cam;
int numPixels;
int max, min, c;
int[] backgroundPixels;
float threshold = 0.5;
int t_c = 0;
int detected = 0;

// FLAGS

/* 0 = no save
 * 1 = save 1/1 fps
 * 2 = save 1/2 fps 
 * 3 = save 1/3 fps
 * ... 
 */
int fSAVE = 0;

/* 0 = false
 * 1 = true
 */
int fDEBUG = 0;

/* 0 = false
 * 1 = true
 */
int fCONTINUOUS = 0;

/* 0 = false
 * 1 = true
 */
int fCOLOR = 0;

boolean fDETECTED = false;
// ----

void getResolutionsAndFps() {
  int[][] res = cam.resolutions();
  for (int i = 0; i < res.length; i++) {
    println(res[i][0] + "x" + res[i][1]);
  } 
  String[] fps = cam.framerates();
  for (int i = 0; i < fps.length; i++) {
    println(fps[i]);
  } 
}

boolean detectMovement() {
  if(c<=(width*height)/100*threshold) {
    t_c = 0;
    fDETECTED = false;
    return false;
  }
  else {
    if (t_c >= 3) {
      fDETECTED = true;
      return true;
    } else {
      t_c++;
      fDETECTED = false;
      return false;
    }
  }
}

int MAX(int n, int m) {return n>m ? n : m;}

int MIN(int n, int m) {return n<m ? n : m;}

void setup() {
  size(640, 480);
  cam = new GSCapture(this, 640, 480, "/dev/video0");
  cam.start();
  numPixels = cam.width * cam.height;
  backgroundPixels = new int[numPixels];
  loadPixels();
  cam.loadPixels();
  arraycopy(cam.pixels, backgroundPixels);
  
  noStroke();
  textFont(createFont("Arial",12,true));
}

void drawFlags() {
  rectMode(CORNERS);
  fill(0, 150);
  rect(0, height-105, 95, height);
  fill(255);
  String str = "Saving rate: " + (fSAVE!=0 ? 1.0/fSAVE : 0)
  + "\nMotion: " + (fDETECTED ? "yes" : "no")
  + "\nDeubg: " + (fDEBUG==1 ? "yes" : "no")
  + "\nColor: " + (fCOLOR==1 ? "yes" : "no")
  + "\nContinuous: " + (fCONTINUOUS==1 ? "yes" : "no");
  text(str, 5, height-100, 95, height);
}

void draw() {
  if (cam.available() == true) {    
    cam.read();
    cam.loadPixels();
    int presenceSum = 0;
    c = 0;
    int sumDiff = 0;
    for(int i = 0; i<numPixels; i++) {
      color currColor = cam.pixels[i];
      color bkgdColor = backgroundPixels[i];
      
      int currR = (currColor >> 16) & 0xFF;
      int currG = (currColor >> 8) & 0xFF;
      int currB = currColor & 0xFF;
      
      int bkgdR = (bkgdColor >> 16) & 0xFF;
      int bkgdG = (bkgdColor >> 8) & 0xFF;
      int bkgdB = bkgdColor & 0xFF;
      
      int diffR = abs(currR - bkgdR);
      int diffG = abs(currG - bkgdG);
      int diffB = abs(currB - bkgdB);
      
      int diff = diffR + diffG + diffB;
      sumDiff += diff;
      if (diff>50) {
        pixels[i] = color(255,255,255);
        c++;
      }
      else {
        if(fCOLOR == 0) {
          pixels[i]=color(0,0,0);
        } else {
          pixels[i]=backgroundPixels[i];
        }
      }
    }
    if(detectMovement()) {
      if(fDEBUG==1) {println("Detected ("+c+"p "+sumDiff+"c)");}
      if(fSAVE!=0 && detected%fSAVE==0) {
        cam.save((detected/fSAVE)+".png");
        if(fDEBUG==1) {println("Saved in "+(detected/fSAVE)+".png");}
      }
      detected++;
    } else {
      if(fDEBUG==1) {println("Not detected");}
    }
    updatePixels();
    if(fCONTINUOUS==1) {
      arraycopy(cam.pixels, backgroundPixels);
    }
    drawFlags();
  }
}

void keyPressed() {
  switch(key) {
  case 's':
    fSAVE = fSAVE==10 ? 0 : 10;
    break;
  case 'd':
    fDEBUG = fDEBUG==1 ? 0 : 1;
    break;
  case 'k':
    fCONTINUOUS = fCONTINUOUS==1 ? 0 : 1;
    break;
  case 'K':
    updatePixels();
    arraycopy(cam.pixels, backgroundPixels);
    break;
  case 'c':
    fCOLOR = fCOLOR==1 ? 0 : 1;
    break;
  }
}
