import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import codeanticode.gsvideo.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class MotionDetection extends PApplet {



GSCapture cam;
int numPixels;
int[] backgroundPixels;
int detected = 0;
ArrayList<int[]> toSee;
int sumDiff;

int w = 640;
int h = 480;
// 0.5% = 5/1000
float threshold = 0.5f;

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
int fCONTINUOUS = 1;

/* 0 = false
 * 1 = true
 */
int fCOLOR = 1;

boolean fCALIBRATING = false;
// ----

public int area(int[] a) {
  return abs(a[0]-a[2])*abs(a[1]-a[3]);
}

public boolean detectMovement(int rx) {
  int[] r = toSee.get(rx);
  if(r[4]<=area(r)/100.0f*threshold) {
    r[6]=0;
    r[5]=0;
    return false;
  }
  else {
    if (r[6] >= 3) {
      r[5]=1;
      return true;
    } else {
      r[6]++;
      r[5]=0;
      return false;
    }
  }
}

public void setup() {
  size(w, h);
  cam = new GSCapture(this, w, h, "/dev/video0");
  cam.start();

  numPixels = cam.width * cam.height;
  backgroundPixels = new int[numPixels];
  loadPixels();
  cam.loadPixels();
  arraycopy(cam.pixels, backgroundPixels);
  
  textFont(createFont("Arial",12,true));
  
  toSee = new ArrayList<int[]>();
}

public void drawRectProp(int rx) {
  if(rx>=toSee.size()) {return;}
  noStroke();
  rectMode(CORNERS);

  int[] r = toSee.get(rx);
  String str = "Area: " + area(r)
  +"\nMotion: " + (r[5]==1 ? "yes" : "no")
  +"\n" + r[4] + "p "
  +"\n" + r[4]*100.0f/area(r) + "%"
  ;  
  fill(0, 150);
  rect(100*rx+0, 0, 100*rx+100, 100);
  fill(255);
  text(str, 100*rx+5, 5, 100*rx+100, 100);
  
}

public void drawFlags() {
  noStroke();
  rectMode(CORNERS);

  String str = "s) Saving rate: " + (fSAVE!=0 ? 1.0f/fSAVE : 0)
  + "\nd) Deubg: " + (fDEBUG==1 ? "yes" : "no")
  + "\nc) Color: " + (fCOLOR==1 ? "yes" : "no")
  + "\nk) Continuous: " + (fCONTINUOUS==1 ? "yes" : "no")
  ;
  fill(0, 150);
  rect(0, height-(20*str.split("\n").length)-5, 110, height);
  fill(255);
  text(str, 5, height-(20*str.split("\n").length), 110, height);
  
  for(int i=0; i<toSee.size(); i++) {
    drawRectProp(i);
  }
}

public void draw() {
  if (cam.available()) {    
    cam.read();
    cam.loadPixels();
    loadPixels();
    
    if(fCALIBRATING) {
      background(cam);
      updatePixels();
    }
    
    int currentRect;
    int presenceSum = 0;
    for(currentRect=0; currentRect<toSee.size(); currentRect++) {
      int[] cr = toSee.get(currentRect);
      cr[4]=0;
      sumDiff = 0;
      for(int tempy=cr[1]; tempy<cr[3]; tempy++) {
        for(int tempx=cr[0]; tempx<cr[2]; tempx++) {
          int i = tempx+tempy*width;
          if(i<0 || i>=cam.pixels.length) {continue;}
          
          int currColor = cam.pixels[i];
          int bkgdColor = backgroundPixels[i];
        
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
          //sumDiff += diff;
          if (diff>50) {
            pixels[i] = color(255,255,255);
            cr[4]++;
          }
          else {
            if(fCOLOR == 0) {
              pixels[i]=color(0,0,0);
            } else {
              pixels[i]=backgroundPixels[i];
            }
          }
        }
      }
      if(detectMovement(currentRect)) {
        if(fSAVE!=0 && detected%fSAVE==0) {
          cam.save((detected/fSAVE)+".png");
          if(fDEBUG==1) {println("Saved in "+(detected/fSAVE)+".png");}
        }
        detected++;
      }
      updatePixels();
    }

    if(fCONTINUOUS==1) {
      arraycopy(cam.pixels, backgroundPixels);
    }
    drawFlags();
  }
}

public void keyPressed() {
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
    arraycopy(cam.pixels, backgroundPixels);
    break;
  case 'c':
    fCOLOR = fCOLOR==1 ? 0 : 1;
    break;
  case 'w':
    fCALIBRATING = !fCALIBRATING;
    break;
  case 'l':
    toSee = new ArrayList<int[]>();
    background(150);
    draw();
  }
}

int x, y;
public void mousePressed() {
  if(!fCALIBRATING) {return;}
  x = mouseX;
  y = mouseY;
}

public void mouseDragged() {
  if(!fCALIBRATING) {return;}
  updatePixels();
  background(150);
  fill(255,0,0,150);
  stroke(255,0,0);
  rectMode(CORNERS);
  rect(x,y,mouseX,mouseY);
}

public void mouseReleased() {
  // point x, y
  // point x end, y end
  // how many pixels are detected as "moved"
  // 1 = rectangle is in movement
  // 3step counter to detect movement
  
  if(!fCALIBRATING) {return;}
  if(x<mouseX) {
    if(y<mouseY) {
      int[] tmp = {x,y,mouseX,mouseY,0,0,0};
      toSee.add(tmp);
    } else {
      int[] tmp = {x,mouseY,mouseX,y,0,0,0};
      toSee.add(tmp);
    }
  } else {
      if(y<mouseY) {
      int[] tmp = {mouseX,y,x,mouseY,0,0,0};
      toSee.add(tmp);
    } else {
      int[] tmp = {mouseX,mouseY,x,y,0,0,0};
      toSee.add(tmp);
    }
  }
  if(fDEBUG==1) {println("Saved rect "+x+";"+y+" -> "+mouseX+";"+mouseY);}
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "MotionDetection" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
